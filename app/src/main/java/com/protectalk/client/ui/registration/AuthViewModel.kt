package com.protectalk.client.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val phone: String = "",
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    val error: String? = null,

    // Firebase bits
    val verificationId: String? = null,   // TODO(Firebase): set in onCodeSent
    val resendToken: Any? = null,         // TODO(Firebase): replace Any with PhoneAuthProvider.ForceResendingToken

    // Countdown for "Resend"
    val secondsLeft: Int = 60
)

class AuthViewModel : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    // --- Public API for your screens ---

    fun setPhone(phone: String) {
        _ui.value = _ui.value.copy(phone = phone, error = null)
    }

    fun startPhoneVerification() {
        val phone = _ui.value.phone.trim()
        if (phone.isBlank()) {
            _ui.value = _ui.value.copy(error = "Phone is empty")
            return
        }

        // Optimistically start sending
        _ui.value = _ui.value.copy(isSendingCode = true, error = null)

        // TODO(Firebase): build PhoneAuthOptions with activity + callbacks
        //  val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
        //      .setPhoneNumber(phone)
        //      .setTimeout(60L, TimeUnit.SECONDS)
        //      .setActivity(activity) // pass from Composable call-site
        //      .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        //          override fun onVerificationCompleted(credential: PhoneAuthCredential) { /* auto-retrieval */ }
        //          override fun onVerificationFailed(e: FirebaseException) {
        //              _ui.value = _ui.value.copy(isSendingCode = false, error = e.message)
        //          }
        //          override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
        //              _ui.value = _ui.value.copy(
        //                  isSendingCode = false,
        //                  verificationId = verificationId,
        //                  resendToken = token,
        //                  secondsLeft = 60
        //              )
        //              startCountdown()  // kick off UI countdown
        //          }
        //      })
        //  PhoneAuthProvider.verifyPhoneNumber(options)

        // TEMP (UI-only demo): pretend code sent successfully
        _ui.value = _ui.value.copy(isSendingCode = false, verificationId = "demo_ver_id", secondsLeft = 60)
        startCountdown()
    }

    fun verifyCode(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (code.length != 6) {
            onError("Code must be 6 digits")
            return
        }
        _ui.value = _ui.value.copy(isVerifying = true, error = null)

        // TODO(Firebase):
        // val credential = PhoneAuthProvider.getCredential(_ui.value.verificationId!!, code)
        // FirebaseAuth.getInstance().signInWithCredential(credential)
        //   .addOnCompleteListener { task ->
        //       _ui.value = _ui.value.copy(isVerifying = false)
        //       if (task.isSuccessful) onSuccess() else onError(task.exception?.message ?: "Verification failed")
        //   }

        // TEMP (UI-only demo)
        viewModelScope.launch {
            delay(700)
            _ui.value = _ui.value.copy(isVerifying = false)
            if (code == "000000") onError("Invalid code (demo)") else onSuccess()
        }
    }

    fun resendCode() {
        if (_ui.value.secondsLeft > 0) return
        // TODO(Firebase): reuse resendToken in PhoneAuthOptions.withResendToken(...)
        // PhoneAuthProvider.verifyPhoneNumber(updatedOptions)

        // TEMP (UI-only demo)
        _ui.value = _ui.value.copy(secondsLeft = 60, error = null)
        startCountdown()
    }

    // --- Countdown driver (shared state for Verification UI) ---
    private fun startCountdown() {
        viewModelScope.launch {
            var s = 60
            while (s > 0) {
                delay(1_000)
                s--
                _ui.value = _ui.value.copy(secondsLeft = s)
            }
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
