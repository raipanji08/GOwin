package com.panjirai0110.gowin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.panjirai0110.gowin.navigation.GowinNavGraph
import com.panjirai0110.shared.firebase.FirebaseConfiguration
import com.panjirai0110.shared.ui.FirebaseConfigurationScreen
import com.panjirai0110.shared.ui.theme.GowinBrand
import com.panjirai0110.shared.ui.theme.GowinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GowinTheme(brand = GowinBrand.User) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val firebaseAvailable = remember {
                        FirebaseConfiguration.isAvailable(this@MainActivity)
                    }
                    if (firebaseAvailable) {
                        GowinNavGraph()
                    } else {
                        FirebaseConfigurationScreen(packageName = packageName)
                    }
                }
            }
        }
    }
}
