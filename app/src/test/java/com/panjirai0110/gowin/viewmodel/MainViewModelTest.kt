package com.panjirai0110.gowin.viewmodel

import com.panjirai0110.gowin.payment.MidtransCheckout
import com.panjirai0110.gowin.payment.MidtransPaymentService
import com.panjirai0110.gowin.payment.MidtransStatus
import com.panjirai0110.shared.auth.AuthRepository
import com.panjirai0110.shared.auth.AuthUser
import com.panjirai0110.shared.data.Subscription
import com.panjirai0110.shared.data.TravelRepository
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.PaymentMethod
import com.panjirai0110.shared.model.PaymentStatus
import com.panjirai0110.shared.model.ProfileOnboardingStatus
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    @Test
    fun authenticatedDataIsObservedOnlyOnceDuringCheckoutCreation() {
        val repository = FakeTravelRepository()
        val paymentService = FakePaymentService()
        val viewModel = MainViewModel(repository, FakeAuthRepository(), paymentService)

        assertEquals(1, repository.scheduleObservationCount)
        assertEquals(1, repository.userBookingObservationCount)
        assertEquals(1, repository.profileObservationCount)

        var checkout: MidtransCheckout? = null
        viewModel.startMidtransPayment(
            scheduleId = repository.schedule.id,
            seatNumber = "A1",
            paymentMethod = PaymentMethod.MidtransQris
        ) { checkout = it }

        assertNotNull(checkout)
        assertEquals(1, repository.userBookingObservationCount)
        assertEquals(0, repository.singleBookingObservationCount)
    }

    @Test
    fun openingTicketKeepsCachedBookingWhileRealtimeListenerConnects() {
        val repository = FakeTravelRepository()
        val viewModel = MainViewModel(
            repository,
            FakeAuthRepository(),
            FakePaymentService()
        )

        viewModel.observeBooking(repository.booking.id)

        assertEquals(repository.booking.id, viewModel.currentBooking?.id)
        assertEquals(1, repository.singleBookingObservationCount)
    }

    @Test
    fun statusRefreshIsSingleFlightAndReportsPendingResult() {
        val paymentService = FakePaymentService(completeStatusImmediately = false)
        val repository = FakeTravelRepository()
        val viewModel = MainViewModel(repository, FakeAuthRepository(), paymentService)
        viewModel.observeBooking(repository.booking.id)

        viewModel.refreshMidtransStatus(repository.booking.id)
        viewModel.refreshMidtransStatus(repository.booking.id)

        assertTrue(viewModel.paymentStatusRefreshing)
        assertEquals(1, paymentService.statusRequestCount)

        paymentService.completeStatus(
            MidtransStatus(
                bookingStatus = BookingStatus.PendingPayment,
                paymentStatus = PaymentStatus.Pending
            )
        )

        assertFalse(viewModel.paymentStatusRefreshing)
        assertEquals(
            "Status terbaru: pembayaran masih menunggu.",
            viewModel.paymentStatusMessage
        )
    }
}

private class FakeAuthRepository : AuthRepository {
    override val currentUser = AuthUser(
        id = "user-1",
        email = "user@example.com",
        displayName = "Pengguna"
    )

    override fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUser>) -> Unit
    ) = onResult(Result.success(currentUser))

    override fun registerWithEmail(
        email: String,
        password: String,
        onResult: (Result<AuthUser>) -> Unit
    ) = onResult(Result.success(currentUser))

    override fun signInWithGoogle(
        idToken: String,
        onResult: (Result<AuthUser>) -> Unit
    ) = onResult(Result.success(currentUser))

    override fun sendPasswordResetEmail(
        email: String,
        onResult: (Result<Unit>) -> Unit
    ) = onResult(Result.success(Unit))

    override fun signOut() = Unit
}

private class FakePaymentService(
    private val completeStatusImmediately: Boolean = true
) : MidtransPaymentService {
    var statusRequestCount = 0
        private set
    private var pendingStatusResult: ((Result<MidtransStatus>) -> Unit)? = null

    override fun createCheckout(
        scheduleId: String,
        seatNumber: String,
        travelDate: Long,
        paymentMethod: String,
        onResult: (Result<MidtransCheckout>) -> Unit
    ) {
        onResult(
            Result.success(
                MidtransCheckout(
                    bookingId = "booking-new",
                    bookingCode = "GWNEW",
                    paymentMethod = paymentMethod,
                    transactionStatus = "snap_pending",
                    transactionId = "",
                    qrString = "",
                    qrUrl = "",
                    deeplinkUrl =
                        "https://app.sandbox.midtrans.com/snap/v4/redirection/token",
                    virtualAccountNumber = "",
                    virtualAccountBank = "",
                    expiresAt = System.currentTimeMillis() + 1_800_000
                )
            )
        )
    }

    override fun syncStatus(
        bookingId: String,
        onResult: (Result<MidtransStatus>) -> Unit
    ) {
        statusRequestCount++
        if (completeStatusImmediately) {
            onResult(
                Result.success(
                    MidtransStatus(
                        BookingStatus.PendingPayment,
                        PaymentStatus.Pending
                    )
                )
            )
        } else {
            pendingStatusResult = onResult
        }
    }

    fun completeStatus(status: MidtransStatus) {
        pendingStatusResult?.invoke(Result.success(status))
        pendingStatusResult = null
    }
}

private class FakeTravelRepository : TravelRepository {
    val schedule = Schedule(
        id = "schedule-1",
        from = "Bandung",
        to = "Garut",
        time = "09:00",
        price = 75_000
    )
    val booking = Booking(
        id = "booking-1",
        bookingCode = "GWBOOKING",
        userId = "user-1",
        userName = "Pengguna",
        userEmail = "user@example.com",
        scheduleId = schedule.id,
        seatNumber = "B3",
        routeFrom = schedule.from,
        routeTo = schedule.to,
        departureTime = schedule.time,
        travelDate = System.currentTimeMillis() + 86_400_000,
        ticketPrice = schedule.price,
        totalAmount = 77_500,
        paymentMethod = PaymentMethod.MidtransQris,
        paymentStatus = PaymentStatus.Pending,
        status = BookingStatus.PendingPayment,
        midtransDeeplinkUrl =
            "https://app.sandbox.midtrans.com/snap/v4/redirection/token"
    )

    var scheduleObservationCount = 0
        private set
    var userBookingObservationCount = 0
        private set
    var profileObservationCount = 0
        private set
    var singleBookingObservationCount = 0
        private set

    override fun observeSchedules(onResult: (Result<List<Schedule>>) -> Unit): Subscription {
        scheduleObservationCount++
        onResult(Result.success(listOf(schedule)))
        return Subscription.None
    }

    override fun observeBookings(onResult: (Result<List<Booking>>) -> Unit) =
        Subscription.None

    override fun observeUserBookings(
        userId: String,
        onResult: (Result<List<Booking>>) -> Unit
    ): Subscription {
        userBookingObservationCount++
        onResult(Result.success(listOf(booking)))
        return Subscription.None
    }

    override fun observeBooking(
        bookingId: String,
        onResult: (Result<Booking?>) -> Unit
    ): Subscription {
        singleBookingObservationCount++
        return Subscription.None
    }

    override fun observeBookedSeats(
        scheduleId: String,
        travelDate: Long,
        onResult: (Result<Set<String>>) -> Unit
    ) = Subscription.None

    override fun checkAdminAccess(userId: String, onResult: (Result<Boolean>) -> Unit) =
        onResult(Result.success(false))

    override fun addSchedule(schedule: Schedule, onResult: (Result<Unit>) -> Unit) =
        onResult(Result.success(Unit))

    override fun deleteSchedule(scheduleId: String, onResult: (Result<Unit>) -> Unit) =
        onResult(Result.success(Unit))

    override fun createBooking(booking: Booking, onResult: (Result<Booking>) -> Unit) =
        onResult(Result.success(booking))

    override fun updateBookingStatus(
        bookingId: String,
        status: String,
        paymentStatus: String,
        paymentReference: String,
        expectedStatus: String?,
        onResult: (Result<Unit>) -> Unit
    ) = onResult(Result.success(Unit))

    override fun observeUserProfile(
        userId: String,
        onResult: (Result<UserProfile?>) -> Unit
    ): Subscription {
        profileObservationCount++
        onResult(
            Result.success(
                UserProfile(
                    userId = userId,
                    name = "Pengguna",
                    email = "user@example.com",
                    onboardingStatus = ProfileOnboardingStatus.Completed
                )
            )
        )
        return Subscription.None
    }

    override fun saveUserProfile(
        profile: UserProfile,
        onResult: (Result<Unit>) -> Unit
    ) = onResult(Result.success(Unit))

    override fun migrateLegacySeatReservations(onResult: (Result<Unit>) -> Unit) =
        onResult(Result.success(Unit))
}
