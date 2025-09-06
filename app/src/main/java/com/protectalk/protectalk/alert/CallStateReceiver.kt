package com.protectalk.protectalk.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallStateReceiver"
    }

    private var lastCallState = TelephonyManager.CALL_STATE_IDLE
    private var lastIncomingNumber: String? = null
    private var lastProcessedTime = 0L // To prevent duplicate processing
    private var lastCallDuration = 0 // To store the duration of the last call
    private var callStartTime = 0L // Track when the call started

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
        callStartTime = System.currentTimeMillis()
    }

    private fun onCallAnswered() {
        Log.d(TAG, "Call answered")
        if (callStartTime == 0L) {
            callStartTime = System.currentTimeMillis() // Fallback if we missed ringing
        }
    }

    private fun onCallEnded(context: Context) {
        Log.d(TAG, "Call ended - checking call log for recent call")

        // Only process if we had a call (not just going from offhook to idle)
        if (lastCallState == TelephonyManager.CALL_STATE_RINGING ||
            lastCallState == TelephonyManager.CALL_STATE_OFFHOOK) {

            // Always use call log lookup since EXTRA_INCOMING_NUMBER is deprecated
            val phoneNumber = getRecentCallNumberWithTimeFilter(context)

            phoneNumber?.let { number ->
                handleIncomingCallEnded(context, number)
            } ?: run {
                Log.w(TAG, "Could not determine phone number for ended call")
            }
        }

        // Reset state
        lastIncomingNumber = null
        callStartTime = 0L
    }

    /**
     * Gets the most recent call from call log with time-based filtering to ensure we get the call that just ended
     */
    private fun getRecentCallNumberWithTimeFilter(context: Context): String? {
        try {
            Log.d(TAG, "Querying call log for recent calls with time filter...")

            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            // Look for calls that started within the last 5 minutes and are very recent
            val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
            val selection = "${CallLog.Calls.DATE} > ?"
            val selectionArgs = arrayOf(fiveMinutesAgo.toString())

            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                Log.d(TAG, "Call log query returned ${it.count} records")

                var recordCount = 0
                while (it.moveToNext() && recordCount < 3) { // Check first 3 recent records
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
                        Log.d(TAG, "Recent call record #$recordCount: number=$number, type=$typeString, ${timeAgo}ms ago, duration=${duration}s")

                        // Return the first incoming or missed call we find that's very recent
                        if ((type == CallLog.Calls.INCOMING_TYPE || type == CallLog.Calls.MISSED_TYPE) && timeAgo < 30000) { // Within 30 seconds
                            Log.i(TAG, "Found recent incoming/missed call from: $number (type: $typeString, duration: ${duration}s)")
                            lastCallDuration = duration.toInt()
                            return number
                        }
                    }
                }

                if (recordCount == 0) {
                    Log.w(TAG, "No recent call records found in call log")
                } else {
                    Log.d(TAG, "No recent incoming/missed calls found in the last $recordCount records")
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
        Log.d(TAG, "Processing ended call from: $phoneNumber (duration: ${lastCallDuration}s)")

        // Check if this is an unknown number
        val isKnownNumber = ContactChecker.isKnownContact(context, phoneNumber)

        Log.d(TAG, "Contact check result for $phoneNumber: isKnown=$isKnownNumber")

        if (!isKnownNumber) {
            Log.i(TAG, "Unknown number detected: $phoneNumber")

            // Trigger the alert flow for unknown caller with duration
            AlertFlowManager.handleUnknownCallEnded(context, phoneNumber, lastCallDuration)
        } else {
            Log.d(TAG, "Known contact ($phoneNumber), no alert needed")
        }
    }
}
