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
     * @deprecated Use findAndPrepareLatestRecording(context) instead
     */
    @Deprecated("Use findAndPrepareLatestRecording(context) instead")
    fun findLatestRecording(): File? {
        return findLatestRecordingFile()
    }
}
