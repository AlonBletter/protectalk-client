package com.protectalk.protectalk.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallStateReceiver"
    }

    private var lastCallState = TelephonyManager.CALL_STATE_IDLE
    private var lastIncomingNumber: String? = null
    private var lastProcessedTime = 0L // To prevent duplicate processing

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "Phone state changed: $state (last state was $lastCallState)")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                onCallRinging()
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                onCallAnswered()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Prevent duplicate processing within 2 seconds
                if (currentTime - lastProcessedTime > 2000) {
                    onCallEnded(context)
                    lastProcessedTime = currentTime
                } else {
                    Log.d(TAG, "Ignoring duplicate IDLE event (${currentTime - lastProcessedTime}ms ago)")
                }
            }
        }

        lastCallState = when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            else -> TelephonyManager.CALL_STATE_IDLE
        }
    }

    private fun onCallRinging() {
        Log.d(TAG, "Call ringing - will get number from call log when call ends")
        // We'll get the number from call log when the call ends instead
    }

    private fun onCallAnswered() {
        Log.d(TAG, "Call answered")
    }

    private fun onCallEnded(context: Context) {
        Log.d(TAG, "Call ended - checking recent call log")

        // Only process if we had a call (not just going from offhook to idle)
        if (lastCallState == TelephonyManager.CALL_STATE_RINGING ||
            lastCallState == TelephonyManager.CALL_STATE_OFFHOOK) {

            // Get the most recent call from call log
            CoroutineScope(Dispatchers.IO).launch {
                getRecentCallNumber(context)?.let { phoneNumber ->
                    handleIncomingCallEnded(context, phoneNumber)
                }
            }
        }

        // Reset state
        lastIncomingNumber = null
    }

    /**
     * Gets the most recent call number from the call log (modern approach)
     */
    private fun getRecentCallNumber(context: Context): String? {
        try {
            Log.d(TAG, "Querying call log for recent calls...")

            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                Log.d(TAG, "Call log query returned ${it.count} records")

                var recordCount = 0
                while (it.moveToNext() && recordCount < 5) { // Check first 5 records for debugging
                    recordCount++

                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                    if (numberIndex >= 0 && typeIndex >= 0 && dateIndex >= 0) {
                        val number = it.getString(numberIndex) ?: "null"
                        val type = it.getInt(typeIndex)
                        val date = it.getLong(dateIndex)
                        val duration = if (durationIndex >= 0) it.getLong(durationIndex) else -1

                        val typeString = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                            CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                            CallLog.Calls.MISSED_TYPE -> "MISSED"
                            else -> "UNKNOWN($type)"
                        }

                        val timeAgo = System.currentTimeMillis() - date
                        Log.d(TAG, "Call record #$recordCount: number=$number, type=$typeString, ${timeAgo}ms ago, duration=${duration}s")

                        // Return the first incoming or missed call we find
                        if (type == CallLog.Calls.INCOMING_TYPE || type == CallLog.Calls.MISSED_TYPE) {
                            Log.i(TAG, "Found recent incoming/missed call from: $number (type: $typeString)")
                            return number
                        }
                    }
                }

                if (recordCount == 0) {
                    Log.w(TAG, "No call records found in call log")
                } else {
                    Log.d(TAG, "No incoming/missed calls found in the last $recordCount records")
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied to read call log", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading call log", e)
        }

        Log.d(TAG, "No recent incoming call found")
        return null
    }

    private fun handleIncomingCallEnded(context: Context, phoneNumber: String) {
        Log.d(TAG, "Processing ended call from: $phoneNumber")

        // Check if this is an unknown number
        val isKnownNumber = ContactChecker.isKnownContact(context, phoneNumber)

        if (!isKnownNumber) {
            Log.i(TAG, "Unknown number detected: $phoneNumber")

            // Trigger the alert flow for unknown caller
            AlertFlowManager.handleUnknownCallEnded(context, phoneNumber)
        } else {
            Log.d(TAG, "Known contact, no alert needed")
        }
    }
}
