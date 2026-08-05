package com.panjirai0110.shared

import com.panjirai0110.shared.auth.AuthRepository
import com.panjirai0110.shared.auth.FirebaseAuthRepository
import com.panjirai0110.shared.data.FirebaseTravelRepository
import com.panjirai0110.shared.data.TravelRepository

object SharedServices {
    fun authRepository(): AuthRepository = FirebaseAuthRepository()

    fun travelRepository(): TravelRepository = FirebaseTravelRepository()
}
