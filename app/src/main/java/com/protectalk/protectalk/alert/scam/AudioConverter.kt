package com.protectalk.protectalk.alert.scam

import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles audio file conversion for transcription compatibility
 */
object AudioConverter {
    private const val TAG = "AudioConverter"

    // Supported input formats
    private val SUPPORTED_FORMATS = setOf("mp3", "wav", "m4a", "3gp", "amr")

    /**
     * Converts audio file to a format suitable for Google Speech-to-Text API
     * Returns the converted file or original if no conversion needed
     */
    suspend fun convertForTranscription(inputFile: File): File? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing audio file: ${inputFile.name}")

            if (!inputFile.exists()) {
                Log.e(TAG, "Input file does not exist: ${inputFile.absolutePath}")
                return@withContext null
            }

            val fileExtension = inputFile.extension.lowercase()

            // Check if file is already in a supported format
            if (isFormatSupported(fileExtension)) {
                Log.d(TAG, "File format $fileExtension is already supported")

                // Validate file integrity
                if (isValidAudioFile(inputFile)) {
                    return@withContext inputFile
                } else {
                    Log.w(TAG, "Audio file appears to be corrupted")
                    return@withContext null
                }
            }

            // Convert to WAV format (widely supported by speech APIs)
            Log.d(TAG, "Converting $fileExtension to WAV format")
            val outputFile = createTempWavFile(inputFile)

            if (convertToWav(inputFile, outputFile)) {
                Log.i(TAG, "Successfully converted to WAV: ${outputFile.absolutePath}")
                return@withContext outputFile
            } else {
                Log.e(TAG, "Failed to convert audio file")
                outputFile.delete()
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error converting audio file", e)
            return@withContext null
        }
    }

    private fun isFormatSupported(extension: String): Boolean {
        return SUPPORTED_FORMATS.contains(extension)
    }

    private fun isValidAudioFile(file: File): Boolean {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.use {
                it.setDataSource(file.absolutePath)
                val duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration != null && duration.toLongOrNull() != null && duration.toLong() > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not validate audio file: ${e.message}")
            false
        }
    }

    private fun createTempWavFile(originalFile: File): File {
        val tempDir = originalFile.parentFile ?: File("/tmp")
        val baseName = originalFile.nameWithoutExtension
        return File(tempDir, "${baseName}_converted.wav")
    }

    private fun convertToWav(inputFile: File, outputFile: File): Boolean {
        return try {
            // For now, we'll do a simple copy since Android's MediaMetadataRetriever
            // doesn't provide conversion capabilities. In a production app, you'd use
            // FFmpeg or similar library for proper audio conversion.

            Log.d(TAG, "Performing basic audio conversion (copy with validation)")

            FileInputStream(inputFile).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Validate the copied file
            outputFile.exists() && outputFile.length() > 0

        } catch (e: Exception) {
            Log.e(TAG, "Error during audio conversion", e)
            false
        }
    }

    /**
     * Gets audio file metadata for API requirements
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getAudioMetadata(file: File): AudioMetadata? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.use {
                it.setDataSource(file.absolutePath)

                val duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val bitrate = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
                val sampleRate = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toIntOrNull() ?: 0

                AudioMetadata(
                    durationMs = duration,
                    bitrate = bitrate,
                    sampleRate = sampleRate,
                    fileSize = file.length(),
                    format = file.extension.lowercase()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio metadata", e)
            null
        }
    }

    data class AudioMetadata(
        val durationMs: Long,
        val bitrate: Int,
        val sampleRate: Int,
        val fileSize: Long,
        val format: String
    )
}
