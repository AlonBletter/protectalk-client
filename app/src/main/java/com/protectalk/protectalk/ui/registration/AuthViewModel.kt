package com.protectalk.protectalk.ui.registration

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
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

    fun setEmail(email: String) {
        Log.d(TAG, "setEmail: $email")
        _ui.value = _ui.value.copy(email = email, error = null)
    }

    /** Call after a successful sign-in/up when you want to force the gate open (Splash will also pick up currentUser). */
    fun markSignedIn() {
        Log.d(TAG, "markSignedIn() called")
        _ui.value = _ui.value.copy(isSignedIn = true)
    }

    fun signOut() {
        Log.d(TAG, "signOut()")
        auth.signOut()
        _ui.value = _ui.value.copy(isSignedIn = false)
    }

    // --- Email/Password: Create account ---
    fun signUp(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "signUp: $email")
        _ui.value = _ui.value.copy(isSubmitting = true, error = null)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _ui.value = _ui.value.copy(isSubmitting = false)
                if (task.isSuccessful) {
                    Log.d(TAG, "signUp success: uid=${task.result?.user?.uid}")
                    _ui.value = _ui.value.copy(isSignedIn = true, error = null)
                    // TODO(Optional): send verification email
                    // task.result?.user?.sendEmailVerification()
                    onSuccess()
                } else {
                    val msg = task.exception?.localizedMessage ?: "Sign up failed"
                    Log.e(TAG, "signUp failed: $msg", task.exception)
                    _ui.value = _ui.value.copy(error = msg)
                    onError(msg)
                }
            }
    }

    // --- Email/Password: Sign in (for future "Already have an account?" flow) ---
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

    // Optional helper if you later gate on email verification:
    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true
}
