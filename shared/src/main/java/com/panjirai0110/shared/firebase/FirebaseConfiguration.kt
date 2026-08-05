package com.panjirai0110.shared.firebase

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseConfiguration {
    fun isAvailable(context: Context): Boolean =
        FirebaseApp.getApps(context.applicationContext).isNotEmpty()
}
