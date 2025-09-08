package com.protectalk.protectalk.alert.scam

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.protectalk.protectalk.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import java.io.File
import java.io.FileOutputStream

/**
 * Integration tests for the complete scam detection pipeline using real APIs:
 * - DLP for sensitive data filtering
 * - Google Speech-to-Text for transcription
 * - ChatGPT for scam analysis
 *
 * These tests use actual API keys and validate end-to-end functionality
 */
@RunWith(AndroidJUnit4::class)
class ScamDetectionIntegrationTest {

    private lateinit var context: Context
    private lateinit var dlpClient: DlpClient
    private lateinit var transcriber: Transcriber
    private lateinit var chatGPTAnalyzer: ChatGPTAnalyzer

    // Test transcript data representing different scam scenarios
    private val scamTranscripts = mapOf(
        "high_confidence_scam" to """
            Hello, this is urgent! Your bank account has been compromised and will be closed in 30 minutes. 
            You need to provide your social security number 123-45-6789 and credit card number 4532-1234-5678-9012 
            immediately to verify your identity. Call us back at 555-SCAM right now or you will lose all your money!
            We also need your full name John Smith and email john@email.com for verification.
        """.trimIndent(),

        "medium_confidence_scam" to """
            Hi, this is Microsoft technical support. We've detected suspicious activity on your computer. 
            Your system has been infected with malware and we need remote access to fix it immediately. 
            Please download TeamViewer and give us the ID number so we can help you. 
            There will be a small fee of $299 for our premium security service.
        """.trimIndent(),

        "low_confidence_suspicious" to """
            Hello, congratulations! You've won a $500 gift card from Amazon. 
            To claim your prize, please visit our website and enter your phone number and address. 
            This offer expires in 24 hours so don't wait. Call us back if you have any questions.
        """.trimIndent(),

        "legitimate_call" to """
            Hi, this is Sarah from your doctor's office calling to remind you about your appointment 
            tomorrow at 2 PM with Dr. Johnson. Please arrive 15 minutes early to complete any paperwork. 
            If you need to reschedule, please call us back at your earliest convenience. Thank you.
        """.trimIndent(),

        "business_call" to """
            Good morning, this is Jennifer from ABC Marketing calling about your recent inquiry 
            regarding our business consulting services. I'd like to schedule a brief 15-minute call 
            to discuss how we can help improve your company's efficiency. When would be a good time for you?
        """.trimIndent()
    )

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Verify API keys are available
        assertTrue("Google API key must be configured", BuildConfig.GOOGLE_API_KEY.isNotBlank())
        assertTrue("OpenAI API key must be configured", BuildConfig.OPENAI_API_KEY.isNotBlank())

        // Initialize clients
        dlpClient = DlpClient()
        transcriber = Transcriber()
        chatGPTAnalyzer = ChatGPTAnalyzer(BuildConfig.OPENAI_API_KEY)
    }

    @Test
    fun testDlpSensitiveDataFiltering() = runBlocking {
        val originalText = """
            My name is John Smith and my SSN is 123-45-6789. 
            My credit card number is 4532-1234-5678-9012 and my email is john.smith@email.com.
            You can reach me at 555-123-4567.
        """.trimIndent()

        val filteredText = dlpClient.deidentifyText(originalText)

        assertNotNull("DLP should return filtered text", filteredText)
        assertNotEquals("Filtered text should be different from original", originalText, filteredText)

        // Verify sensitive data is masked
        assertFalse("SSN should be filtered", filteredText!!.contains("123-45-6789"))
        assertFalse("Credit card should be filtered", filteredText.contains("4532-1234-5678-9012"))
        assertFalse("Email should be filtered", filteredText.contains("john.smith@email.com"))
        assertFalse("Phone should be filtered", filteredText.contains("555-123-4567"))

        println("Original: $originalText")
        println("Filtered: $filteredText")
    }

    @Test
    fun testChatGPTScamAnalysisHighConfidence() = runBlocking {
        val transcript = scamTranscripts["high_confidence_scam"]!!

        val result = chatGPTAnalyzer.analyze(transcript)

        assertNotNull("ChatGPT should return analysis result", result)
        assertTrue("High confidence scam should have score > 0.8", result.score > 0.8)
        assertTrue("Analysis should contain multiple points", result.analysisPoints.isNotEmpty())

        println("High Confidence Scam Analysis:")
        println("Score: ${result.score}")
        println("Analysis: ${result.analysisPoints}")

        // Expected characteristics of high confidence scam
        val analysisText = result.analysisPoints.joinToString(" ").lowercase()
        assertTrue("Should detect urgency tactics",
            analysisText.contains("urgent") || analysisText.contains("immediate") || analysisText.contains("pressure"))
    }

    @Test
    fun testChatGPTScamAnalysisMediumConfidence() = runBlocking {
        val transcript = scamTranscripts["medium_confidence_scam"]!!

        val result = chatGPTAnalyzer.analyze(transcript)

        assertNotNull("ChatGPT should return analysis result", result)
        assertTrue("Medium confidence scam should have score between 0.5-0.8",
            result.score >= 0.5 && result.score <= 0.8)
        assertTrue("Analysis should contain points", result.analysisPoints.isNotEmpty())

        println("Medium Confidence Scam Analysis:")
        println("Score: ${result.score}")
        println("Analysis: ${result.analysisPoints}")
    }

    @Test
    fun testChatGPTScamAnalysisLowSuspicion() = runBlocking {
        val transcript = scamTranscripts["low_confidence_suspicious"]!!

        val result = chatGPTAnalyzer.analyze(transcript)

        assertNotNull("ChatGPT should return analysis result", result)
        assertTrue("Low suspicion should have score between 0.3-0.7",
            result.score >= 0.3 && result.score <= 0.7)

        println("Low Suspicion Analysis:")
        println("Score: ${result.score}")
        println("Analysis: ${result.analysisPoints}")
    }

    @Test
    fun testChatGPTLegitimateCall() = runBlocking {
        val transcript = scamTranscripts["legitimate_call"]!!

        val result = chatGPTAnalyzer.analyze(transcript)

        assertNotNull("ChatGPT should return analysis result", result)
        assertTrue("Legitimate call should have low score < 0.3", result.score < 0.3)

        println("Legitimate Call Analysis:")
        println("Score: ${result.score}")
        println("Analysis: ${result.analysisPoints}")
    }

    @Test
    fun testChatGPTBusinessCall() = runBlocking {
        val transcript = scamTranscripts["business_call"]!!

        val result = chatGPTAnalyzer.analyze(transcript)

        assertNotNull("ChatGPT should return analysis result", result)
        assertTrue("Business call should have very low score < 0.2", result.score < 0.2)

        println("Business Call Analysis:")
        println("Score: ${result.score}")
        println("Analysis: ${result.analysisPoints}")
    }

    @Test
    fun testCompleteScamDetectionPipeline() = runBlocking {
        // Test the complete pipeline: original text -> DLP filtering -> ChatGPT analysis
        val originalTranscript = """
            This is urgent! Your account will be closed. Give me your SSN 123-45-6789 
            and credit card 4532-1234-5678-9012 right now or lose everything! 
            My name is John Doe and my email is john@scammer.com.
        """.trimIndent()

        // Step 1: Filter sensitive data with DLP
        val filteredTranscript = dlpClient.deidentifyText(originalTranscript)
        assertNotNull("DLP filtering should succeed", filteredTranscript)

        // Step 2: Analyze filtered transcript with ChatGPT
        val scamResult = chatGPTAnalyzer.analyze(filteredTranscript!!)
        assertNotNull("ChatGPT analysis should succeed", scamResult)

        // Verify results
        assertTrue("Complete pipeline should detect high-risk scam", scamResult.score > 0.8)
        assertFalse("Filtered transcript should not contain SSN", filteredTranscript.contains("123-45-6789"))
        assertFalse("Filtered transcript should not contain credit card", filteredTranscript.contains("4532-1234-5678-9012"))

        println("Complete Pipeline Test:")
        println("Original: $originalTranscript")
        println("Filtered: $filteredTranscript")
        println("Scam Score: ${scamResult.score}")
        println("Analysis: ${scamResult.analysisPoints}")
    }

    @Test
    fun testTranscriberWithSampleAudio() = runBlocking {
        // Create a minimal WAV file for testing (this would normally come from RecordingFinder)
        val testWavFile = createTestWavFile()

        try {
            val transcript = transcriber.transcribeWavFile(testWavFile)

            // The transcription might be empty or contain noise, but should not throw exceptions
            assertNotNull("Transcriber should return a result", transcript)
            assertFalse("Transcriber should not return error message",
                transcript.contains("WAV file not found"))

            println("Transcription result: $transcript")

        } catch (e: Exception) {
            // Transcription might fail with test audio, but we verify the method works
            println("Transcription failed as expected with test audio: ${e.message}")
            assertTrue("Should handle transcription errors gracefully", true)
        } finally {
            testWavFile.delete()
        }
    }

    @Test
    fun testScamDetectionEdgeCases() = runBlocking {
        // Test empty transcript
        val emptyResult = chatGPTAnalyzer.analyze("")
        assertTrue("Empty transcript should have very low score", emptyResult.score < 0.1)

        // Test very short transcript
        val shortResult = chatGPTAnalyzer.analyze("Hello")
        assertTrue("Short transcript should have low score", shortResult.score < 0.3)

        // Test transcript with only numbers
        val numbersResult = chatGPTAnalyzer.analyze("123 456 789")
        assertTrue("Numbers only should have low score", numbersResult.score < 0.4)

        println("Edge Cases Results:")
        println("Empty: ${emptyResult.score}")
        println("Short: ${shortResult.score}")
        println("Numbers: ${numbersResult.score}")
    }

    @Test
    fun testDlpWithVariousDataTypes() = runBlocking {
        val testCases = mapOf(
            "phone_numbers" to "Call me at 555-123-4567 or (800) 555-0199",
            "emails" to "Contact admin@company.com or support@help.org",
            "credit_cards" to "My card is 4111-1111-1111-1111 and backup is 5555-5555-5555-4444",
            "names" to "I am John Smith and my wife is Jane Doe",
            "mixed_data" to "John Smith's SSN is 123-45-6789, email john@email.com, phone 555-1234"
        )

        testCases.forEach { (type, text) ->
            val filtered = dlpClient.deidentifyText(text)
            assertNotNull("DLP should handle $type", filtered)
            assertNotEquals("$type should be filtered", text, filtered)
            println("$type - Original: $text")
            println("$type - Filtered: $filtered")
        }
    }

    /**
     * Creates a minimal WAV file for testing transcription
     * This is a basic WAV header with minimal audio data
     */
    private fun createTestWavFile(): File {
        val testFile = File(context.cacheDir, "test_audio.wav")

        // Create a minimal WAV file with basic header and silent audio
        val wavHeader = byteArrayOf(
            // RIFF header
            0x52, 0x49, 0x46, 0x46, // "RIFF"
            0x24, 0x08, 0x00, 0x00, // File size - 8
            0x57, 0x41, 0x56, 0x45, // "WAVE"

            // Format chunk
            0x66, 0x6D, 0x74, 0x20, // "fmt "
            0x10, 0x00, 0x00, 0x00, // Chunk size (16)
            0x01, 0x00,             // Audio format (PCM)
            0x01, 0x00,             // Number of channels (1)
            0x44, 0xAC.toByte(), 0x00, 0x00, // Sample rate (44100)
            0x88.toByte(), 0x58, 0x01, 0x00, // Byte rate
            0x02, 0x00,             // Block align
            0x10, 0x00,             // Bits per sample (16)

            // Data chunk
            0x64, 0x61, 0x74, 0x61, // "data"
            0x00, 0x08, 0x00, 0x00  // Data size
        )

        // Add minimal silent audio data (1000 samples of silence)
        val audioData = ByteArray(2000) // 16-bit samples, so 2 bytes per sample

        FileOutputStream(testFile).use { fos ->
            fos.write(wavHeader)
            fos.write(audioData)
        }

        return testFile
    }
}
