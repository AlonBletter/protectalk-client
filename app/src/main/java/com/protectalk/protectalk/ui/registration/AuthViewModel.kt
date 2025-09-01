package com.protectalk.protectalk.ui.registration

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.protectalk.protectalk.data.model.ResultModel
import com.protectalk.protectalk.data.remote.network.AuthInterceptor
import com.protectalk.protectalk.domain.CompleteRegistrationUseCase
import com.protectalk.protectalk.push.PushManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false,
    val pendingRegistration: PendingRegistrationData? = null
)

data class PendingRegistrationData(
    val email: String,
    val password: String
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

    fun signOut() {
        Log.d(TAG, "signOut()")
        // Clear Firebase authentication
        auth.signOut()
        // Clear cached JWT token from AuthInterceptor
        AuthInterceptor.instance.clearCachedToken()
        // Clear cached FCM tokens to ensure fresh tokens for next user
        PushManager.clearTokens()
        // Update UI state
        _ui.value = _ui.value.copy(isSignedIn = false)
        Log.d(TAG, "Logout complete - Firebase auth, JWT cache, and FCM tokens cleared")
    }

    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "signIn: $email")
        _ui.value = _ui.value.copy(isSubmitting = true, error = null)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _ui.value = _ui.value.copy(isSubmitting = false)
                if (task.isSuccessful) {
                    Log.d(TAG, "signIn success: uid=${task.result?.user?.uid}")
                    _ui.value = _ui.value.copy(isSignedIn = true, error = null)
                    onSuccess()
                } else {
                    val msg = task.exception?.localizedMessage ?: "Sign in failed"
                    Log.e(TAG, "signIn failed: $msg", task.exception)
                    _ui.value = _ui.value.copy(error = msg)
                    onError(msg)
                }
            }
    }

    fun setPendingRegistration(email: String, password: String) {
        Log.d(TAG, "setPendingRegistration: $email")
        _ui.value = _ui.value.copy(
            pendingRegistration = PendingRegistrationData(email, password),
            error = null
        )
    }

    fun clearPendingRegistration() {
        Log.d(TAG, "clearPendingRegistration")
        _ui.value = _ui.value.copy(pendingRegistration = null)
    }

    fun completeFullRegistration(
        name: String,
        phoneNumber: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val pendingData = _ui.value.pendingRegistration
        if (pendingData == null) {
            onError("No pending registration data found")
            return
        }

        Log.d(TAG, "completeFullRegistration: ${pendingData.email}, $name, $phoneNumber")
        _ui.value = _ui.value.copy(isSubmitting = true, error = null)

        // First create Firebase account
        auth.createUserWithEmailAndPassword(pendingData.email, pendingData.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase account created: uid=${task.result?.user?.uid}")
                    // Now complete the full registration with server
                    viewModelScope.launch {
                        try {
                            val completeRegistrationUseCase = CompleteRegistrationUseCase()
                            val result = completeRegistrationUseCase(
                                context = context,
                                name = name,
                                phoneNumber = phoneNumber
                            )

                            when (result) {
                                is ResultModel.Ok -> {
                                    _ui.value = _ui.value.copy(
                                        isSubmitting = false,
                                        isSignedIn = true,
                                        error = null,
                                        pendingRegistration = null
                                    )
                                    onSuccess()
                                }
                                is ResultModel.Err -> {
                                    // Registration failed, clean up Firebase account
                                    auth.currentUser?.delete()
                                    val msg = "Registration failed: ${result.message}"
                                    _ui.value = _ui.value.copy(isSubmitting = false, error = msg)
                                    onError(msg)
                                }
                            }
                        } catch (e: Exception) {
                            // Registration failed, clean up Firebase account
                            auth.currentUser?.delete()
                            val msg = "Registration failed: ${e.message}"
                            Log.e(TAG, "Registration exception", e)
                            _ui.value = _ui.value.copy(isSubmitting = false, error = msg)
                            onError(msg)
                        }
                    }
                } else {
                    val msg = task.exception?.localizedMessage ?: "Account creation failed"
                    Log.e(TAG, "Firebase account creation failed: $msg", task.exception)
                    _ui.value = _ui.value.copy(isSubmitting = false, error = msg)
                    onError(msg)
                }
            }
    }
}
