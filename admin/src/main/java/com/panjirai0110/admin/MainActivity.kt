package com.panjirai0110.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import com.panjirai0110.admin.navigation.AdminNavGraph
import com.panjirai0110.shared.firebase.FirebaseConfiguration
import com.panjirai0110.shared.ui.FirebaseConfigurationScreen
import com.panjirai0110.shared.ui.theme.GowinBrand
import com.panjirai0110.shared.ui.theme.GowinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GowinTheme(brand = GowinBrand.Admin) {
                Surface {
                    val firebaseAvailable = remember {
                        BuildConfig.HAS_FIREBASE_CONFIG &&
                            FirebaseConfiguration.isAvailable(this@MainActivity)
                    }
                    if (firebaseAvailable) {
                        AdminNavGraph()
                    } else {
                        FirebaseConfigurationScreen(packageName = packageName)
                    }
                }
            }
        }
    }
}
