package com.panjirai0110.shared.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

internal class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    override fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUser>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("Data pengguna tidak tersedia.")))
                } else {
                    onResult(Result.success(user.toAuthUser()))
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun registerWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUser>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("Data pengguna tidak tersedia.")))
                } else {
                    onResult(Result.success(user.toAuthUser()))
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun signInWithGoogle(
        idToken: String,
        onResult: (Result<AuthUser>) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("Data pengguna tidak tersedia.")))
                } else {
                    onResult(Result.success(user.toAuthUser()))
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun sendPasswordResetEmail(
        email: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    id = uid,
    email = email,
    displayName = displayName
)
