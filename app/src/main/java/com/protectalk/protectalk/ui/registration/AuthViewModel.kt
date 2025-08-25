package com.protectalk.protectalk.ui.registration

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class AuthUiState(
    val phone: String = "",
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    val error: String? = null,
    val verificationId: String? = null,
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val secondsLeft: Int = 0,
    val isSignedIn: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    private val TAG = "AuthVM"

    init {
        val signedIn = auth.currentUser != null
        Log.d(TAG, "init: currentUser=${auth.currentUser?.uid}, signedIn=$signedIn")
        _ui.value = _ui.value.copy(isSignedIn = signedIn)
    }

    fun setPhone(phone: String) {
        Log.d(TAG, "setPhone: $phone")
        _ui.value = _ui.value.copy(phone = phone, error = null)
    }

    fun markSignedIn() {
        Log.d(TAG, "markSignedIn() called")
        _ui.value = _ui.value.copy(isSignedIn = true)
    }

    fun startPhoneVerification(activity: Activity) {
        val phone = _ui.value.phone.trim()
        if (phone.isBlank()) {
            Log.w(TAG, "startPhoneVerification: phone is blank")
            _ui.value = _ui.value.copy(error = "Phone is empty")
            return
        }

        Log.d(TAG, "startPhoneVerification: requesting code for $phone")
        _ui.value = _ui.value.copy(isSendingCode = true, error = null)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: auto-retrieval or instant verification")
                // Optional: auth.signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                _ui.value = _ui.value.copy(isSendingCode = false, error = e.localizedMessage ?: "Verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "onCodeSent: verificationId=$verificationId, token=$token")
                _ui.value = _ui.value.copy(
                    isSendingCode = false,
                    verificationId = verificationId,
                    resendToken = token,
                    secondsLeft = 60
                )
                startCountdown()
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                Log.w(TAG, "onCodeAutoRetrievalTimeOut for verificationId=$verificationId")
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val verificationId = _ui.value.verificationId
        if (verificationId.isNullOrBlank()) {
            Log.e(TAG, "verifyCode: verificationId is null/blank")
            onError("No verification ID")
            return
        }
        if (code.length != 6) {
            Log.w(TAG, "verifyCode: invalid code length=${code.length}")
            onError("Code must be 6 digits")
            return
        }

        Log.d(TAG, "verifyCode: using verificationId=$verificationId, code=$code")
        _ui.value = _ui.value.copy(isVerifying = true, error = null)
        val credential = PhoneAuthProvider.getCredential(verificationId, code)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _ui.value = _ui.value.copy(isVerifying = false)
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential: success uid=${task.result?.user?.uid}")
                    _ui.value = _ui.value.copy(isSignedIn = true, error = null)
                    onSuccess()
                } else {
                    val msg = task.exception?.localizedMessage ?: "Verification failed"
                    Log.e(TAG, "signInWithCredential: failed: $msg", task.exception)
                    _ui.value = _ui.value.copy(error = msg)
                    onError(msg)
                }
            }
    }

    fun resendCode(activity: Activity) {
        if (_ui.value.secondsLeft > 0) {
            Log.w(TAG, "resendCode: too early, secondsLeft=${_ui.value.secondsLeft}")
            return
        }
        val token = _ui.value.resendToken ?: run {
            Log.w(TAG, "resendCode: no resendToken available")
            _ui.value = _ui.value.copy(error = "Resend not available yet")
            return
        }

        Log.d(TAG, "resendCode: requesting resend for ${_ui.value.phone}")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "resendCode: onVerificationCompleted")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "resendCode: onVerificationFailed: ${e.message}", e)
                _ui.value = _ui.value.copy(error = e.localizedMessage ?: "Resend failed")
            }

            override fun onCodeSent(verificationId: String, t: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "resendCode: onCodeSent verificationId=$verificationId")
                _ui.value = _ui.value.copy(verificationId = verificationId, resendToken = t, secondsLeft = 60)
                startCountdown()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(_ui.value.phone.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun startCountdown() {
        Log.d(TAG, "startCountdown: 60s")
        viewModelScope.launch {
            var s = 60
            while (s > 0) {
                delay(1_000)
                s--
                _ui.value = _ui.value.copy(secondsLeft = s)
                if (s % 10 == 0 || s < 5) {
                    Log.d(TAG, "countdown: $s")
                }
            }
        }
    }
}
