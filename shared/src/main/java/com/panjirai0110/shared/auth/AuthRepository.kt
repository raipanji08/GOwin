package com.panjirai0110.shared.auth

data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?
)

interface AuthRepository {
    val currentUser: AuthUser?

    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUser>) -> Unit
    )

    fun registerWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUser>) -> Unit
    )

    fun signInWithGoogle(
        idToken: String,
        onResult: (Result<AuthUser>) -> Unit
    )

    fun sendPasswordResetEmail(
        email: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun signOut()
}
