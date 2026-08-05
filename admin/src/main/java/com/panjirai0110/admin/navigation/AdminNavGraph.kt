package com.panjirai0110.admin.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.panjirai0110.admin.screen.AdminDashboardScreen
import com.panjirai0110.admin.screen.AdminLoginScreen
import com.panjirai0110.admin.viewmodel.AdminViewModel
import com.panjirai0110.shared.R
import com.panjirai0110.shared.ui.WelcomeScreen

private object AdminRoute {
    const val Welcome = "welcome"
    const val Login = "admin_login"
    const val Dashboard = "admin_dashboard"
}

@Composable
fun AdminNavGraph(
    navController: NavHostController = rememberNavController(),
    adminViewModel: AdminViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = AdminRoute.Welcome) {
        composable(AdminRoute.Welcome) {
            WelcomeScreen(
                appName = "GO-WIN Admin",
                subtitle = "Kelola jadwal dan pantau pemesanan perjalanan.",
                appLogo = R.drawable.ic_gowin_logo,
                continueEnabled = !adminViewModel.authLoading,
                continueLoading = adminViewModel.authLoading,
                onContinue = {
                    adminViewModel.continueFromWelcome(
                        onAdminSession = {
                            navController.navigate(AdminRoute.Dashboard) {
                                popUpTo(AdminRoute.Welcome) { inclusive = true }
                            }
                        },
                        onLoginRequired = {
                            navController.navigate(AdminRoute.Login) {
                                popUpTo(AdminRoute.Welcome) { inclusive = true }
                            }
                        }
                    )
                }
            )
        }
        composable(AdminRoute.Login) {
            AdminLoginScreen(
                viewModel = adminViewModel,
                onLoginSuccess = {
                    navController.navigate(AdminRoute.Dashboard) {
                        popUpTo(AdminRoute.Login) { inclusive = true }
                    }
                }
            )
        }
        composable(AdminRoute.Dashboard) {
            AdminDashboardScreen(
                viewModel = adminViewModel,
                onSignOut = {
                    adminViewModel.signOut()
                    navController.navigate(AdminRoute.Login) {
                        popUpTo(AdminRoute.Dashboard) { inclusive = true }
                    }
                }
            )
        }
    }
}
