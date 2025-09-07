package com.protectalk.protectalk.alert.scam

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.Locale

/**
 * Utility class responsible for locating the most recent call recording
 * and converting it to WAV format in the app's cache directory.
 */
object RecordingFinder {

    // == Logging ==
    private const val LOG_TAG: String = "RecordingFinder"

    // == Supported audio extensions ==
    private val SUPPORTED_AUDIO_EXTENSIONS: Set<String> = setOf("amr", "m4a", "wav", "3gp", "mp4")

    // == Directories commonly used for call recordings ==
    private val COMMON_CALL_RECORDING_DIRECTORIES: List<File> = listOf(
        File(Environment.getExternalStorageDirectory(), "CallRecordings"),
        File(Environment.getExternalStorageDirectory(), "Android/data/com.android.soundrecorder/files"),
        File(Environment.getExternalStorageDirectory(), "MIUI/sound_recorder/call_rec"),
        File(Environment.getExternalStorageDirectory(), "MIUI/sound_recorder"),
        File(Environment.getExternalStorageDirectory(), "Recordings"),
        // Use backward-compatible approach for recordings directory
        File(Environment.getExternalStorageDirectory(), "Recordings/Call")
    )

    /**
     * Searches for the latest recording, converts it to WAV if needed, and stores it in app cache.
     * Compatible with API level 24+
     *
     * @param context The application context for accessing cache directory
     * @return The WAV file in app cache ready for transcription, or null if no recording found
     */
    fun findAndPrepareLatestRecording(context: Context): File? {
        Log.d(LOG_TAG, "🔍 Searching for latest recording and preparing for transcription...")

        // Step 1: Find the latest recording file
        val latestRecordingFile = findLatestRecordingFile()
        if (latestRecordingFile == null) {
            Log.w(LOG_TAG, "⚠️ No valid recording found in known locations.")
            return null
        }

        Log.d(LOG_TAG, "✅ Latest recording found: ${latestRecordingFile.absolutePath}")

        // Step 2: Convert to WAV and store in app cache
        return convertAndCacheRecording(context, latestRecordingFile)
    }

    /**
     * Searches predefined directories and returns the latest modified audio file.
     * Compatible with API level 24+
     *
     * @return The most recently modified audio file matching supported formats, or null if none found.
     */
    private fun findLatestRecordingFile(): File? {
        return COMMON_CALL_RECORDING_DIRECTORIES
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.walkTopDown() }
            .filter { file ->
                file.isFile &&
                        file.extension.lowercase(Locale.US) in SUPPORTED_AUDIO_EXTENSIONS
            }
            .maxByOrNull { it.lastModified() }
    }

    /**
     * Converts the audio file to WAV format and stores it in app cache directory.
     * If the file is already WAV, it copies it to cache for consistent handling.
     *
     * @param context The application context
     * @param sourceFile The original audio file
     * @return The WAV file in cache directory, or null if conversion failed
     */
    private fun convertAndCacheRecording(context: Context, sourceFile: File): File? {
        val cacheWavFile = File(context.cacheDir, "protectalk_recording_${System.currentTimeMillis()}.wav")

        return try {
            when (sourceFile.extension.lowercase(Locale.US)) {
                "m4a" -> {
                    Log.d(LOG_TAG, "🔄 Converting M4A to WAV in cache...")
                    val success = AudioConverter.convertM4aToWav(sourceFile, cacheWavFile)
                    if (success) {
                        Log.d(LOG_TAG, "✅ M4A converted to WAV: ${cacheWavFile.absolutePath}")
                        cacheWavFile
                    } else {
                        Log.e(LOG_TAG, "❌ Failed to convert M4A to WAV")
                        null
                    }
                }
                "wav" -> {
                    Log.d(LOG_TAG, "📋 Copying WAV file to cache...")
                    sourceFile.copyTo(cacheWavFile, overwrite = true)
                    Log.d(LOG_TAG, "✅ WAV file copied to cache: ${cacheWavFile.absolutePath}")
                    cacheWavFile
                }
                else -> {
                    Log.d(LOG_TAG, "📋 Copying ${sourceFile.extension.uppercase()} file to cache as WAV...")
                    sourceFile.copyTo(cacheWavFile, overwrite = true)
                    Log.d(LOG_TAG, "✅ Audio file copied to cache: ${cacheWavFile.absolutePath}")
                    cacheWavFile
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "💥 Error processing audio file: ${e.message}", e)
            // Clean up failed file
            if (cacheWavFile.exists()) {
                cacheWavFile.delete()
            }
            null
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    fun findLatestRecording(): File? {
        return findLatestRecordingFile()
    }

    /**
     * Checks if call recording is working by comparing the latest recording timestamp
     * with the last call time within a 60-second delta.
     * Uses the same logic as findAndPrepareLatestRecording to ensure consistency.
     * Automatically retrieves call duration from call logs for accurate comparison.
     *
     * @param context The application context for accessing call logs
     * @param lastCallStartTime The start time of the last call in milliseconds (System.currentTimeMillis())
     * @return True if call recording appears to be working, false otherwise
     */
    fun isCallRecordingWorking(context: Context, lastCallStartTime: Long): Boolean {
        Log.d(LOG_TAG, "🔍 Checking if call recording is working...")

        // Use the same logic as findAndPrepareLatestRecording to find the recording
        // This ensures we're checking the exact same file that would be used for analysis
        val latestRecordingFile = findLatestRecordingFile()
        if (latestRecordingFile == null) {
            Log.w(LOG_TAG, "❌ No recording files found - call recording not working")
            return false
        }

        Log.d(LOG_TAG, "📁 Found recording file for verification: ${latestRecordingFile.absolutePath}")

        // Validate that this file would actually be processable
        // (same validation as in findAndPrepareLatestRecording)
        if (!latestRecordingFile.exists() || !latestRecordingFile.isFile) {
            Log.w(LOG_TAG, "❌ Recording file is not valid - call recording not working")
            return false
        }

        // Extract timestamp from filename if possible, otherwise use file modification time
        val recordingTimestamp = extractTimestampFromFilename(latestRecordingFile)
            ?: latestRecordingFile.lastModified()

        // Get call duration from call logs to calculate expected call end time
        val callDurationMs = getLastCallDuration(context, lastCallStartTime)
        val expectedCallEndTime = lastCallStartTime + callDurationMs

        // Compare with expected call end time (60-second tolerance)
        val timeDelta = Math.abs(recordingTimestamp - expectedCallEndTime)
        val isWithinTolerance = timeDelta <= 60_000L // 60 seconds in milliseconds

        Log.d(LOG_TAG, "📊 Recording check - File: ${latestRecordingFile.name}")
        Log.d(LOG_TAG, "📊 Recording check - Call start: $lastCallStartTime, Duration: ${callDurationMs}ms, Expected end: $expectedCallEndTime")
        Log.d(LOG_TAG, "📊 Recording check - Recording time: $recordingTimestamp, Delta from expected end: ${timeDelta}ms")
        Log.d(LOG_TAG, if (isWithinTolerance) "✅ Call recording appears to be working" else "❌ Call recording not working - delta too large")

        return isWithinTolerance
    }

    /**
     * Retrieves the duration of the last call from call logs.
     *
     * @param context The application context for accessing call logs
     * @param callStartTime The start time of the call to find duration for
     * @return Duration in milliseconds, or 0 if not found
     */
    private fun getLastCallDuration(context: Context, callStartTime: Long): Long {
        try {
            val projection = arrayOf(
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.DURATION
            )

            // Query call logs for calls around the given start time (within 2 minutes tolerance)
            val selection = "${android.provider.CallLog.Calls.DATE} BETWEEN ? AND ?"
            val startWindow = callStartTime - 120_000L // 2 minutes before
            val endWindow = callStartTime + 120_000L // 2 minutes after
            val selectionArgs = arrayOf(startWindow.toString(), endWindow.toString())

            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${android.provider.CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val dateColumnIndex = it.getColumnIndex(android.provider.CallLog.Calls.DATE)
                    val durationColumnIndex = it.getColumnIndex(android.provider.CallLog.Calls.DURATION)

                    if (dateColumnIndex >= 0 && durationColumnIndex >= 0) {
                        val callDate = it.getLong(dateColumnIndex)
                        val durationSeconds = it.getInt(durationColumnIndex)
                        val durationMs = durationSeconds * 1000L

                        Log.d(LOG_TAG, "📞 Found call in logs - Date: $callDate, Duration: ${durationMs}ms")
                        return durationMs
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "⚠️ Failed to retrieve call duration from call logs: ${e.message}")
        }

        Log.w(LOG_TAG, "⚠️ No matching call found in logs - using 0ms duration")
        return 0L
    }

    /**
     * Attempts to extract timestamp from recording filename.
     * Common patterns include timestamps in various formats.
     *
     * @param file The recording file
     * @return Timestamp in milliseconds, or null if extraction failed
     */
    private fun extractTimestampFromFilename(file: File): Long? {
        val filename = file.nameWithoutExtension

        // Try various timestamp patterns commonly used in call recording filenames
        val timestampPatterns = listOf(
            // Pattern: YYYYMMDD_HHMMSS
            "\\d{8}_\\d{6}".toRegex(),
            // Pattern: YYYY-MM-DD-HH-MM-SS
            "\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}".toRegex(),
            // Pattern: Unix timestamp (10 digits)
            "\\d{10}".toRegex(),
            // Pattern: Unix timestamp with milliseconds (13 digits)
            "\\d{13}".toRegex()
        )

        for (pattern in timestampPatterns) {
            val match = pattern.find(filename)
            if (match != null) {
                return try {
                    val timestampStr = match.value
                    when (timestampStr.length) {
                        8 -> { // YYYYMMDD format, assume start of day
                            val year = timestampStr.substring(0, 4).toInt()
                            val month = timestampStr.substring(4, 6).toInt()
                            val day = timestampStr.substring(6, 8).toInt()
                            java.util.Calendar.getInstance().apply {
                                set(year, month - 1, day, 0, 0, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        10 -> timestampStr.toLong() * 1000L // Unix timestamp to milliseconds
                        13 -> timestampStr.toLong() // Already in milliseconds
                        else -> null
                    }
                } catch (_: Exception) {
                    Log.w(LOG_TAG, "Failed to parse timestamp from filename: ${match.value}")
                    null
                }
            }
        }

        return null
    }
}
