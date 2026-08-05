package com.panjirai0110.gowin.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.panjirai0110.gowin.components.GowinBottomBar
import com.panjirai0110.gowin.screen.BookingDetailScreen
import com.panjirai0110.gowin.screen.CompleteProfileScreen
import com.panjirai0110.gowin.screen.DigitalTicketScreen
import com.panjirai0110.gowin.screen.HistoryScreen
import com.panjirai0110.gowin.screen.HomeScreen
import com.panjirai0110.gowin.screen.LoginScreen
import com.panjirai0110.gowin.screen.MidtransCheckoutScreen
import com.panjirai0110.gowin.screen.PaymentScreen
import com.panjirai0110.gowin.screen.ProfileScreen
import com.panjirai0110.gowin.screen.RegisterScreen
import com.panjirai0110.gowin.screen.ScheduleScreen
import com.panjirai0110.gowin.screen.SeatSelectionScreen
import com.panjirai0110.gowin.screen.SplashScreen
import com.panjirai0110.gowin.screen.TicketScreen
import com.panjirai0110.gowin.viewmodel.MainViewModel

private object TicketNavigationOrigin {
    const val BookingFlow = "booking_flow"
    const val TicketTab = "ticket_tab"
    const val HistoryTab = "history_tab"
}

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")
    data object Login : AppRoute("login")
    data object Register : AppRoute("register")
    data object Home : AppRoute("home")
    data object Ticket : AppRoute("ticket")
    data object History : AppRoute("history")
    data object Profile : AppRoute("profile")
    data object Schedule : AppRoute("schedule")

    data object ProfileSetup : AppRoute("profile_setup/{mode}") {
        const val OnboardingMode = "onboarding"
        const val EditMode = "edit"

        fun createRoute(mode: String) = "profile_setup/${Uri.encode(mode)}"
    }

    data object Seat : AppRoute("seat/{scheduleId}") {
        fun createRoute(scheduleId: String) = "seat/${Uri.encode(scheduleId)}"
    }

    data object BookingDetail : AppRoute("booking_detail/{scheduleId}/{seatNumber}") {
        fun createRoute(scheduleId: String, seatNumber: String) =
            "booking_detail/${Uri.encode(scheduleId)}/${Uri.encode(seatNumber)}"
    }

    data object Payment : AppRoute("payment/{scheduleId}/{seatNumber}") {
        fun createRoute(scheduleId: String, seatNumber: String) =
            "payment/${Uri.encode(scheduleId)}/${Uri.encode(seatNumber)}"
    }

    data object MidtransCheckout : AppRoute("payment_checkout/{bookingId}?origin={origin}") {
        fun createRoute(
            bookingId: String,
            origin: String = TicketNavigationOrigin.BookingFlow
        ) = "payment_checkout/${Uri.encode(bookingId)}?origin=${Uri.encode(origin)}"
    }

    data object DigitalTicket : AppRoute("digital_ticket/{bookingId}?origin={origin}") {
        fun createRoute(
            bookingId: String,
            origin: String = TicketNavigationOrigin.BookingFlow
        ) = "digital_ticket/${Uri.encode(bookingId)}?origin=${Uri.encode(origin)}"
    }
}

@Composable
fun GowinNavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel()
) {
    val activity = LocalContext.current.findActivity()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = setOf(
        AppRoute.Home.route,
        AppRoute.Ticket.route,
        AppRoute.History.route,
        AppRoute.Profile.route
    )

    Scaffold(
        contentWindowInsets = if (currentRoute in bottomBarRoutes) {
            WindowInsets.statusBars
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                GowinBottomBar(
                    currentRoute = currentRoute.orEmpty(),
                    onNavigate = { targetRoute ->
                        if (targetRoute != currentRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(AppRoute.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(
                            if (mainViewModel.isSignedIn) {
                                AppRoute.Home.route
                            } else {
                                AppRoute.Login.route
                            }
                        ) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoute.Login.route) {
                LoginScreen(
                    viewModel = mainViewModel,
                    onLoginSuccess = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                        }
                    },
                    onRegister = { navController.navigate(AppRoute.Register.route) },
                    onBack = {
                        // Login is the root destination after Splash. Returning
                        // to Splash would immediately route here again, so the
                        // explicit chevron has the same result as system back:
                        // close the task.
                        activity?.finish() ?: navController.popBackStack()
                    }
                )
            }

            composable(AppRoute.Register.route) {
                RegisterScreen(
                    viewModel = mainViewModel,
                    onRegisterSuccess = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                        }
                    },
                    onBack = navController::popBackStack
                )
            }

            composable(AppRoute.Home.route) {
                LaunchedEffect(
                    mainViewModel.profileLoading,
                    mainViewModel.needsProfileOnboarding
                ) {
                    if (mainViewModel.needsProfileOnboarding) {
                        navController.navigate(
                            AppRoute.ProfileSetup.createRoute(
                                AppRoute.ProfileSetup.OnboardingMode
                            )
                        ) {
                            launchSingleTop = true
                        }
                    }
                }
                HomeScreen(
                    viewModel = mainViewModel,
                    userName = mainViewModel.currentUserName.orEmpty(),
                    onSelectSchedule = {
                        navController.navigate(AppRoute.Seat.createRoute(it))
                    },
                    onViewAllSchedules = {
                        navController.navigate(AppRoute.Schedule.route)
                    }
                )
            }

            composable(AppRoute.Ticket.route) {
                TicketScreen(
                    viewModel = mainViewModel,
                    onOpenTicket = {
                        navController.navigate(
                            AppRoute.DigitalTicket.createRoute(
                                bookingId = it,
                                origin = TicketNavigationOrigin.TicketTab
                            )
                        )
                    }
                )
            }

            composable(AppRoute.History.route) {
                HistoryScreen(
                    viewModel = mainViewModel,
                    onOpenTicket = {
                        navController.navigate(
                            AppRoute.DigitalTicket.createRoute(
                                bookingId = it,
                                origin = TicketNavigationOrigin.HistoryTab
                            )
                        )
                    }
                )
            }

            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    viewModel = mainViewModel,
                    userName = mainViewModel.currentUserName.orEmpty(),
                    userEmail = mainViewModel.currentUserEmail.orEmpty(),
                    onEditProfile = {
                        navController.navigate(
                            AppRoute.ProfileSetup.createRoute(
                                AppRoute.ProfileSetup.EditMode
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(AppRoute.History.route) {
                            launchSingleTop = true
                        }
                    },
                    onSignOut = {
                        mainViewModel.signOut()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(AppRoute.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = AppRoute.ProfileSetup.route,
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { entry ->
                val isOnboarding = entry.arguments?.getString("mode") ==
                    AppRoute.ProfileSetup.OnboardingMode
                CompleteProfileScreen(
                    viewModel = mainViewModel,
                    isOnboarding = isOnboarding,
                    onBack = navController::popBackStack,
                    onFinished = {
                        if (isOnboarding) {
                            navController.popBackStack(AppRoute.Home.route, inclusive = false)
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(AppRoute.Schedule.route) {
                ScheduleScreen(
                    viewModel = mainViewModel,
                    onBack = navController::popBackStack,
                    onSelectSchedule = {
                        navController.navigate(AppRoute.Seat.createRoute(it))
                    }
                )
            }

            composable(
                route = AppRoute.Seat.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { entry ->
                val scheduleId = entry.arguments?.getString("scheduleId").orEmpty()
                SeatSelectionScreen(
                    scheduleId = scheduleId,
                    viewModel = mainViewModel,
                    onBack = navController::popBackStack,
                    onContinue = {
                        navController.navigate(
                            AppRoute.BookingDetail.createRoute(scheduleId, it)
                        )
                    }
                )
            }

            composable(
                route = AppRoute.BookingDetail.route,
                arguments = listOf(
                    navArgument("scheduleId") { type = NavType.StringType },
                    navArgument("seatNumber") { type = NavType.StringType }
                )
            ) { entry ->
                val scheduleId = entry.arguments?.getString("scheduleId").orEmpty()
                val seatNumber = entry.arguments?.getString("seatNumber").orEmpty()
                BookingDetailScreen(
                    scheduleId = scheduleId,
                    seatNumber = seatNumber,
                    viewModel = mainViewModel,
                    onBack = navController::popBackStack,
                    onProceedToPayment = {
                        navController.navigate(
                            AppRoute.Payment.createRoute(scheduleId, seatNumber)
                        )
                    }
                )
            }

            composable(
                route = AppRoute.Payment.route,
                arguments = listOf(
                    navArgument("scheduleId") { type = NavType.StringType },
                    navArgument("seatNumber") { type = NavType.StringType }
                )
            ) { entry ->
                val scheduleId = entry.arguments?.getString("scheduleId").orEmpty()
                val seatNumber = entry.arguments?.getString("seatNumber").orEmpty()
                PaymentScreen(
                    scheduleId = scheduleId,
                    seatNumber = seatNumber,
                    viewModel = mainViewModel,
                    onBack = navController::popBackStack,
                    onBookingReady = { bookingId, usesMidtrans ->
                        if (usesMidtrans) {
                            navController.navigate(
                                AppRoute.MidtransCheckout.createRoute(bookingId)
                            ) {
                                popUpTo(AppRoute.Payment.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(
                                AppRoute.DigitalTicket.createRoute(bookingId)
                            ) {
                                popUpTo(AppRoute.Payment.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = AppRoute.MidtransCheckout.route,
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType },
                    navArgument("origin") {
                        type = NavType.StringType
                        defaultValue = TicketNavigationOrigin.BookingFlow
                    }
                )
            ) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                val origin = entry.arguments?.getString("origin")
                    ?: TicketNavigationOrigin.BookingFlow
                val navigateToTicket = {
                    navController.navigate(
                        AppRoute.DigitalTicket.createRoute(bookingId, origin)
                    ) {
                        popUpTo(AppRoute.MidtransCheckout.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                MidtransCheckoutScreen(
                    bookingId = bookingId,
                    viewModel = mainViewModel,
                    onPaymentFinished = navigateToTicket
                )
            }

            composable(
                route = AppRoute.DigitalTicket.route,
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType },
                    navArgument("origin") {
                        type = NavType.StringType
                        defaultValue = TicketNavigationOrigin.BookingFlow
                    }
                )
            ) { entry ->
                val bookingId = entry.arguments?.getString("bookingId").orEmpty()
                val origin = entry.arguments?.getString("origin")
                    ?: TicketNavigationOrigin.BookingFlow
                DigitalTicketScreen(
                    bookingId = bookingId,
                    viewModel = mainViewModel,
                    onBack = { navController.navigateToTicketOrigin(origin) },
                    onContinuePayment = { pendingBookingId ->
                        navController.navigate(
                            AppRoute.MidtransCheckout.createRoute(
                                bookingId = pendingBookingId,
                                origin = origin
                            )
                        ) {
                            popUpTo(AppRoute.DigitalTicket.route) { inclusive = true }
                        }
                    },
                    onBackHome = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun NavHostController.navigateToTicketOrigin(origin: String) {
    when (origin) {
        TicketNavigationOrigin.TicketTab -> {
            if (!popBackStack()) {
                navigate(AppRoute.Ticket.route) {
                    popUpTo(AppRoute.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        TicketNavigationOrigin.HistoryTab -> {
            if (!popBackStack()) {
                navigate(AppRoute.History.route) {
                    popUpTo(AppRoute.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        else -> {
            navigate(AppRoute.Schedule.route) {
                popUpTo(AppRoute.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
}
