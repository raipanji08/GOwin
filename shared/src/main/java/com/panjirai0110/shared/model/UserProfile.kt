package com.panjirai0110.shared.model

object UserGender {
    const val Male = "male"
    const val Female = "female"
    const val Unspecified = ""

    fun isSupported(value: String): Boolean =
        value == Male || value == Female || value == Unspecified
}

object ProfileOnboardingStatus {
    const val Pending = "pending"
    const val Completed = "completed"
    const val Skipped = "skipped"

    fun isSupported(value: String): Boolean =
        value == Pending || value == Completed || value == Skipped
}

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = UserGender.Unspecified,
    // Existing profile documents do not have this field and therefore remain complete.
    val onboardingStatus: String = ProfileOnboardingStatus.Completed,
    val updatedAt: Long = 0L
)
