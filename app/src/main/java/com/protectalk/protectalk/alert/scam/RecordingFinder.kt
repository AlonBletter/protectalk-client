package com.protectalk.protectalk.alert.scam

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import java.io.File

/**
 * Finds and locates call recording files on the device
 */
object RecordingFinder {
    private const val TAG = "RecordingFinder"

    // Common recording paths on Android devices
    private val COMMON_RECORDING_PATHS = arrayOf(
        "/storage/emulated/0/Call",
        "/storage/emulated/0/CallRecord",
        "/storage/emulated/0/PhoneRecord",
        "/storage/emulated/0/Recordings/Call",
        "/storage/emulated/0/MIUI/sound_recorder/call_rec",
        "/storage/emulated/0/Android/data/com.android.providers.downloads/files/Call",
        "/sdcard/Call",
        "/sdcard/CallRecord"
    )

    /**
     * Attempts to find the recording file for a specific call
     */
    suspend fun findRecordingForCall(
        context: Context,
        phoneNumber: String,
        callTime: Long
    ): File? {
        Log.d(TAG, "Searching for recording of call from $phoneNumber at $callTime")

        try {
            // Method 1: Search in common recording directories
            val recording = searchInCommonPaths(phoneNumber, callTime)
            if (recording != null) {
                Log.i(TAG, "Found recording in common path: ${recording.absolutePath}")
                return recording
            }

            // Method 2: Search using MediaStore (if available)
            val mediaStoreRecording = searchUsingMediaStore(context, phoneNumber, callTime)
            if (mediaStoreRecording != null) {
                Log.i(TAG, "Found recording via MediaStore: ${mediaStoreRecording.absolutePath}")
                return mediaStoreRecording
            }

            Log.w(TAG, "No recording found for call from $phoneNumber")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error searching for recording", e)
            return null
        }
    }

    private fun searchInCommonPaths(phoneNumber: String, callTime: Long): File? {
        val cleanNumber = phoneNumber.replace("+", "").replace("-", "").replace(" ", "")
        val timeWindow = 60000L // 1 minute window

        for (path in COMMON_RECORDING_PATHS) {
            try {
                val dir = File(path)
                if (!dir.exists() || !dir.isDirectory) continue

                val files = dir.listFiles { file ->
                    file.isFile &&
                    (file.name.endsWith(".mp3") || file.name.endsWith(".wav") ||
                     file.name.endsWith(".m4a") || file.name.endsWith(".3gp")) &&
                    (file.name.contains(cleanNumber) ||
                     Math.abs(file.lastModified() - callTime) < timeWindow)
                }

                files?.minByOrNull { Math.abs(it.lastModified() - callTime) }?.let {
                    return it
                }

            } catch (e: Exception) {
                Log.d(TAG, "Error searching path $path: ${e.message}")
            }
        }

        return null
    }

    private fun searchUsingMediaStore(context: Context, phoneNumber: String, callTime: Long): File? {
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media.DATA,
                android.provider.MediaStore.Audio.Media.DATE_ADDED,
                android.provider.MediaStore.Audio.Media.DISPLAY_NAME
            )

            val cursor: Cursor? = context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${android.provider.MediaStore.Audio.Media.DATE_ADDED} > ?",
                arrayOf(((callTime - 300000) / 1000).toString()), // 5 minutes before call
                "${android.provider.MediaStore.Audio.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val dataIndex = it.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                    val nameIndex = it.getColumnIndex(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)

                    if (dataIndex >= 0 && nameIndex >= 0) {
                        val filePath = it.getString(dataIndex)
                        val fileName = it.getString(nameIndex)

                        if (filePath != null && fileName != null) {
                            val file = File(filePath)
                            if (file.exists() &&
                                (fileName.contains("call", ignoreCase = true) ||
                                 fileName.contains(phoneNumber.takeLast(4)))) {
                                return file
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching MediaStore", e)
        }

        return null
    }
}
