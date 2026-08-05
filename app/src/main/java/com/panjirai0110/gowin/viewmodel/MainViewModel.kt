package com.panjirai0110.gowin.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.panjirai0110.gowin.payment.MidtransCheckout
import com.panjirai0110.gowin.payment.MidtransPaymentService
import com.panjirai0110.gowin.payment.MidtransStatus
import com.panjirai0110.gowin.payment.WorkerMidtransPaymentService
import com.panjirai0110.shared.SharedServices
import com.panjirai0110.shared.auth.AuthRepository
import com.panjirai0110.shared.auth.AuthUser
import com.panjirai0110.shared.data.SeatAlreadyBookedException
import com.panjirai0110.shared.data.Subscription
import com.panjirai0110.shared.data.TravelRepository
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.DEFAULT_ADMIN_FEE
import com.panjirai0110.shared.model.PaymentMethod
import com.panjirai0110.shared.model.PaymentStatus
import com.panjirai0110.shared.model.ProfileOnboardingStatus
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.model.UserGender
import com.panjirai0110.shared.model.UserProfile
import com.panjirai0110.shared.validation.InputValidator
import java.util.Calendar

class MainViewModel(
    private val travelRepository: TravelRepository = SharedServices.travelRepository(),
    private val authRepository: AuthRepository = SharedServices.authRepository(),
    private val midtransPaymentService: MidtransPaymentService =
        WorkerMidtransPaymentService()
) : ViewModel() {

    var schedules by mutableStateOf<List<Schedule>>(emptyList())
        private set
    var schedulesLoading by mutableStateOf(true)
        private set
    var schedulesError by mutableStateOf<String?>(null)
        private set

    var selectedTravelDate by mutableLongStateOf(startOfToday())
        private set
    var selectedPassengerCount by mutableIntStateOf(1)
        private set

    var bookedSeats by mutableStateOf<Set<String>>(emptySet())
        private set
    var seatsLoading by mutableStateOf(false)
        private set
    var seatsError by mutableStateOf<String?>(null)
        private set

    var bookingInProgress by mutableStateOf(false)
        private set
    var paymentStatusRefreshing by mutableStateOf(false)
        private set
    var paymentError by mutableStateOf<String?>(null)
        private set
    var paymentStatusMessage by mutableStateOf<String?>(null)
        private set
    var currentBooking by mutableStateOf<Booking?>(null)
        private set
    var currentMidtransCheckout by mutableStateOf<MidtransCheckout?>(null)
        private set

    var authLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    var authMessage by mutableStateOf<String?>(null)
        private set

    var userBookings by mutableStateOf<List<Booking>>(emptyList())
        private set
    var userBookingsLoading by mutableStateOf(false)
        private set
    var userBookingsError by mutableStateOf<String?>(null)
        private set

    var userProfile by mutableStateOf<UserProfile?>(null)
        private set
    var profileLoading by mutableStateOf(false)
        private set
    var profileSaving by mutableStateOf(false)
        private set
    var profileMessage by mutableStateOf<String?>(null)
        private set
    var profileError by mutableStateOf<String?>(null)
        private set

    val isSignedIn: Boolean
        get() = authRepository.currentUser != null

    val currentUserName: String?
        get() = userProfile?.name?.takeIf(String::isNotBlank)
            ?: authRepository.currentUser?.displayName
            ?: authRepository.currentUser?.email?.substringBefore("@")

    val currentUserEmail: String?
        get() = userProfile?.email?.takeIf(String::isNotBlank)
            ?: authRepository.currentUser?.email

    val needsProfileOnboarding: Boolean
        get() = !profileLoading &&
            userProfile?.onboardingStatus == ProfileOnboardingStatus.Pending

    private var schedulesSubscription: Subscription = Subscription.None
    private var seatsSubscription: Subscription = Subscription.None
    private var userBookingsSubscription: Subscription = Subscription.None
    private var bookingSubscription: Subscription = Subscription.None
    private var bookingObservationGeneration = 0L
    private var profileSubscription: Subscription = Subscription.None

    init {
        if (isSignedIn) {
            observeAuthenticatedData()
        } else {
            schedulesLoading = false
        }
    }

    fun setTravelDate(value: Long) {
        selectedTravelDate = maxOf(startOfToday(), startOfDay(value))
    }

    fun setPassengerCount(value: Int) {
        selectedPassengerCount = value.coerceIn(1, 1)
    }

    fun observeSchedules() {
        schedulesSubscription.dispose()
        schedulesLoading = true
        schedulesError = null
        schedulesSubscription = travelRepository.observeSchedules { result ->
            schedulesLoading = false
            result.onSuccess {
                schedules = it
                schedulesError = null
            }.onFailure {
                schedulesError = it.userMessage("Jadwal gagal dimuat.")
            }
        }
    }

    fun getScheduleById(scheduleId: String): Schedule? =
        schedules.find { it.id == scheduleId }

    fun observeSeats(scheduleId: String, travelDate: Long) {
        seatsSubscription.dispose()
        bookedSeats = emptySet()
        seatsError = null
        if (scheduleId.isBlank()) {
            seatsLoading = false
            seatsError = "Jadwal tidak valid."
            return
        }

        seatsLoading = true
        seatsSubscription = travelRepository.observeBookedSeats(
            scheduleId,
            travelDate
        ) { result ->
            seatsLoading = false
            result.onSuccess {
                bookedSeats = it
                seatsError = null
            }.onFailure {
                seatsError = it.userMessage("Status kursi gagal dimuat.")
            }
        }
    }

    fun stopObservingSeats() {
        seatsSubscription.dispose()
        seatsSubscription = Subscription.None
        seatsLoading = false
    }

    fun createManualTransferBooking(
        scheduleId: String,
        seatNumber: String,
        onSuccess: (Booking) -> Unit
    ) {
        if (bookingInProgress) return
        val booking = buildBooking(
            scheduleId = scheduleId,
            seatNumber = seatNumber,
            paymentMethod = PaymentMethod.ManualTransfer,
            status = BookingStatus.PendingVerification,
            paymentStatus = PaymentStatus.Verification,
            paymentReference = "MANUAL-${System.currentTimeMillis()}"
        ) ?: return

        bookingInProgress = true
        paymentError = null
        travelRepository.createBooking(booking) { result ->
            bookingInProgress = false
            result.onSuccess { persisted ->
                bookedSeats = bookedSeats + seatNumber
                currentBooking = persisted
                onSuccess(persisted)
            }.onFailure {
                paymentError = when (it) {
                    is SeatAlreadyBookedException -> it.message
                    else -> it.userMessage("Pemesanan transfer manual gagal dibuat.")
                }
            }
        }
    }

    fun startMidtransPayment(
        scheduleId: String,
        seatNumber: String,
        paymentMethod: String,
        onSuccess: (MidtransCheckout) -> Unit
    ) {
        if (bookingInProgress) return
        val schedule = schedules.find { it.id == scheduleId }
        if (schedule == null || seatNumber.isBlank()) {
            paymentError = "Jadwal atau kursi tidak valid."
            return
        }
        if (seatNumber in bookedSeats) {
            paymentError = "Kursi $seatNumber sudah dipesan."
            return
        }
        if (authRepository.currentUser == null) {
            paymentError = "Sesi login telah berakhir. Silakan masuk kembali."
            return
        }

        bookingInProgress = true
        paymentError = null
        paymentStatusMessage = null
        midtransPaymentService.createCheckout(
            scheduleId = scheduleId,
            seatNumber = seatNumber,
            travelDate = selectedTravelDate,
            paymentMethod = paymentMethod
        ) { result ->
            bookingInProgress = false
            result.onSuccess { checkout ->
                bookedSeats = bookedSeats + seatNumber
                currentMidtransCheckout = checkout
                onSuccess(checkout)
            }.onFailure {
                paymentError = it.userMessage(
                    "Pembayaran online belum tersedia. Pilih Transfer Bank atau coba kembali nanti."
                )
            }
        }
    }

    fun observeBooking(bookingId: String): Long {
        val generation = ++bookingObservationGeneration
        bookingSubscription.dispose()
        if (bookingId.isBlank()) {
            currentBooking = null
            currentMidtransCheckout = null
            return generation
        }
        val isDifferentBooking = currentBooking?.id != bookingId &&
            currentMidtransCheckout?.bookingId != bookingId
        if (isDifferentBooking) {
            paymentError = null
            paymentStatusMessage = null
        }
        currentBooking = currentBooking
            ?.takeIf { it.id == bookingId }
            ?: userBookings.firstOrNull { it.id == bookingId }
        currentMidtransCheckout = currentMidtransCheckout
            ?.takeIf { it.bookingId == bookingId }
            ?: currentBooking?.let(MidtransCheckout::fromBooking)
        bookingSubscription = travelRepository.observeBooking(bookingId) { result ->
            if (generation != bookingObservationGeneration) {
                return@observeBooking
            }
            result.onSuccess {
                currentBooking = it
                val persistedCheckout = it?.let(MidtransCheckout::fromBooking)
                if (persistedCheckout != null) {
                    currentMidtransCheckout = persistedCheckout
                }
                paymentError = null
            }.onFailure {
                paymentError = it.userMessage("Status pembayaran gagal dimuat.")
            }
        }
        return generation
    }

    fun stopObservingBooking(generation: Long? = null) {
        if (
            generation != null &&
            generation != bookingObservationGeneration
        ) {
            return
        }
        bookingObservationGeneration++
        bookingSubscription.dispose()
        bookingSubscription = Subscription.None
    }

    fun refreshMidtransStatus(
        bookingId: String,
        onResult: (MidtransStatus) -> Unit = {}
    ) {
        if (bookingId.isBlank()) {
            paymentError = "ID pemesanan tidak valid."
            return
        }
        if (paymentStatusRefreshing) return
        paymentStatusRefreshing = true
        paymentError = null
        paymentStatusMessage = null
        midtransPaymentService.syncStatus(bookingId) { result ->
            paymentStatusRefreshing = false
            result.onSuccess { status ->
                currentBooking = currentBooking?.copy(
                    status = status.bookingStatus,
                    paymentStatus = status.paymentStatus
                )
                paymentStatusMessage = when (status.bookingStatus) {
                    BookingStatus.PendingPayment ->
                        "Status terbaru: pembayaran masih menunggu."
                    BookingStatus.Paid ->
                        "Pembayaran berhasil dikonfirmasi."
                    BookingStatus.Expired ->
                        "Pembayaran telah kedaluwarsa."
                    BookingStatus.Cancelled,
                    BookingStatus.Rejected ->
                        "Pembayaran tidak berhasil diproses."
                    else -> "Status pembayaran berhasil diperbarui."
                }
                onResult(status)
            }.onFailure {
                paymentError = it.userMessage(
                    "Status pembayaran belum dapat diperbarui."
                )
            }
        }
    }

    fun clearPaymentError() {
        paymentError = null
        paymentStatusMessage = null
        seatsError = null
    }

    fun observeUserBookings() {
        userBookingsSubscription.dispose()
        val userId = authRepository.currentUser?.id
        if (userId.isNullOrBlank()) {
            userBookings = emptyList()
            userBookingsLoading = false
            return
        }
        userBookingsLoading = true
        userBookingsError = null
        userBookingsSubscription = travelRepository.observeUserBookings(userId) { result ->
            userBookingsLoading = false
            result.onSuccess {
                userBookings = it
                userBookingsError = null
            }.onFailure {
                userBookings = emptyList()
                userBookingsError = it.userMessage("Riwayat perjalanan gagal dimuat.")
            }
        }
    }

    fun stopObservingUserBookings() {
        userBookingsSubscription.dispose()
        userBookingsSubscription = Subscription.None
        userBookingsLoading = false
    }

    fun observeProfile() {
        profileSubscription.dispose()
        val authUser = authRepository.currentUser ?: run {
            userProfile = null
            profileLoading = false
            return
        }
        profileLoading = true
        profileSubscription = travelRepository.observeUserProfile(authUser.id) { result ->
            profileLoading = false
            result.onSuccess { profile ->
                userProfile = profile ?: defaultProfile(authUser).also {
                    travelRepository.saveUserProfile(it) { }
                }
            }.onFailure {
                profileError = it.userMessage("Profil gagal dimuat.")
            }
        }
    }

    fun saveProfile(
        name: String,
        phone: String,
        gender: String,
        requirePhone: Boolean = false,
        completeOnboarding: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        val user = authRepository.currentUser
        if (user == null) {
            profileError = "Sesi login telah berakhir."
            return
        }
        if (name.trim().length < 2) {
            profileError = "Nama minimal 2 karakter."
            return
        }
        val normalizedPhone = phone.filter(Char::isDigit)
        if (requirePhone && normalizedPhone.isBlank()) {
            profileError = "Nomor ponsel wajib diisi."
            return
        }
        if (
            normalizedPhone.isNotBlank() &&
            !normalizedPhone.matches(Regex("^08[0-9]{8,11}$"))
        ) {
            profileError = "Nomor ponsel harus diawali 08 dan berisi 10–13 digit."
            return
        }
        if (!UserGender.isSupported(gender)) {
            profileError = "Pilihan gender tidak valid."
            return
        }
        if (completeOnboarding && gender == UserGender.Unspecified) {
            profileError = "Pilih gender untuk menentukan avatar profil."
            return
        }

        val profile = UserProfile(
            userId = user.id,
            name = name.trim(),
            email = user.email.orEmpty(),
            phone = normalizedPhone,
            gender = gender,
            onboardingStatus = if (completeOnboarding) {
                ProfileOnboardingStatus.Completed
            } else {
                userProfile?.onboardingStatus
                    ?.takeIf(ProfileOnboardingStatus::isSupported)
                    ?: ProfileOnboardingStatus.Completed
            }
        )
        profileSaving = true
        profileError = null
        profileMessage = null
        travelRepository.saveUserProfile(
            profile
        ) { result ->
            profileSaving = false
            result.onSuccess {
                userProfile = profile
                profileMessage = "Profil berhasil diperbarui."
                onSuccess()
            }.onFailure {
                profileError = it.userMessage("Profil gagal diperbarui.")
            }
        }
    }

    fun skipProfileOnboarding(onSuccess: () -> Unit) {
        val user = authRepository.currentUser
        if (user == null) {
            profileError = "Sesi login telah berakhir."
            return
        }
        val profile = (userProfile ?: defaultProfile(user)).copy(
            onboardingStatus = ProfileOnboardingStatus.Skipped
        )
        profileSaving = true
        profileError = null
        profileMessage = null
        travelRepository.saveUserProfile(profile) { result ->
            profileSaving = false
            result.onSuccess {
                userProfile = profile
                onSuccess()
            }.onFailure {
                profileError = it.userMessage("Profil gagal diperbarui.")
            }
        }
    }

    fun clearProfileFeedback() {
        profileError = null
        profileMessage = null
    }

    fun dismissProfileMessage() {
        profileMessage = null
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        val validationError = InputValidator.credentialError(email, password)
        if (validationError != null) {
            authError = validationError
            return
        }
        runAuthOperation(onSuccess) { callback ->
            authRepository.signInWithEmail(email, password, callback)
        }
    }

    fun register(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        val validationError = InputValidator.credentialError(email, password)
        if (validationError != null) {
            authError = validationError
            return
        }
        runAuthOperation(onSuccess) { callback ->
            authRepository.registerWithEmail(email, password, callback)
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        if (idToken.isBlank()) {
            authError = "Token Google tidak valid."
            return
        }
        runAuthOperation(onSuccess) { callback ->
            authRepository.signInWithGoogle(idToken, callback)
        }
    }

    fun sendPasswordReset(email: String) {
        val validationError = InputValidator.emailError(email)
        if (validationError != null) {
            authError = validationError
            return
        }
        if (authLoading) return
        authLoading = true
        authError = null
        authMessage = null
        authRepository.sendPasswordResetEmail(email) { result ->
            authLoading = false
            result.onSuccess {
                authMessage = "Tautan reset kata sandi telah dikirim."
            }.onFailure {
                authError = it.userMessage("Tautan reset gagal dikirim.")
            }
        }
    }

    fun reportAuthError(message: String) {
        authError = message
        authMessage = null
    }

    fun clearAuthFeedback() {
        authError = null
        authMessage = null
    }

    fun signOut() {
        schedulesSubscription.dispose()
        schedulesSubscription = Subscription.None
        stopObservingSeats()
        stopObservingUserBookings()
        stopObservingBooking()
        profileSubscription.dispose()
        profileSubscription = Subscription.None
        authRepository.signOut()
        schedules = emptyList()
        bookedSeats = emptySet()
        userBookings = emptyList()
        userProfile = null
        currentBooking = null
        currentMidtransCheckout = null
        bookingInProgress = false
        paymentStatusRefreshing = false
        schedulesLoading = false
        clearAuthFeedback()
        clearPaymentError()
        clearProfileFeedback()
    }

    private fun observeAuthenticatedData() {
        observeSchedules()
        observeUserBookings()
        observeProfile()
    }

    private fun buildBooking(
        scheduleId: String,
        seatNumber: String,
        paymentMethod: String,
        status: String,
        paymentStatus: String,
        paymentReference: String
    ): Booking? {
        val schedule = schedules.find { it.id == scheduleId }
        if (schedule == null || seatNumber.isBlank()) {
            paymentError = "Jadwal atau kursi tidak valid."
            return null
        }
        if (seatNumber in bookedSeats) {
            paymentError = "Kursi $seatNumber sudah dipesan."
            return null
        }
        val user = authRepository.currentUser
        if (user == null) {
            paymentError = "Sesi login telah berakhir. Silakan masuk kembali."
            return null
        }
        val timestamp = System.currentTimeMillis()
        return Booking(
            userId = user.id,
            userName = currentUserName ?: user.email?.substringBefore("@").orEmpty(),
            userEmail = user.email.orEmpty(),
            scheduleId = schedule.id,
            seatNumber = seatNumber,
            routeFrom = schedule.from,
            routeTo = schedule.to,
            departureTime = schedule.time,
            vehicleName = schedule.vehicleName,
            travelDate = selectedTravelDate,
            ticketPrice = schedule.price,
            adminFee = DEFAULT_ADMIN_FEE,
            totalAmount = schedule.price + DEFAULT_ADMIN_FEE,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            status = status,
            paymentReference = paymentReference,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    private fun runAuthOperation(
        onSuccess: () -> Unit,
        operation: (((Result<AuthUser>) -> Unit) -> Unit)
    ) {
        if (authLoading) return
        authLoading = true
        authError = null
        authMessage = null
        operation { result ->
            authLoading = false
            result.onSuccess {
                observeAuthenticatedData()
                onSuccess()
            }.onFailure {
                authError = it.userMessage("Autentikasi gagal.")
            }
        }
    }

    private fun defaultProfile(user: AuthUser): UserProfile =
        UserProfile(
            userId = user.id,
            name = user.displayName
                ?: user.email
                    ?.substringBefore("@")
                    ?.replaceFirstChar(Char::uppercase)
                    .orEmpty(),
            email = user.email.orEmpty(),
            onboardingStatus = ProfileOnboardingStatus.Pending
        )

    override fun onCleared() {
        schedulesSubscription.dispose()
        seatsSubscription.dispose()
        userBookingsSubscription.dispose()
        bookingSubscription.dispose()
        profileSubscription.dispose()
        super.onCleared()
    }

    private companion object {
        fun startOfDay(timestamp: Long): Long = Calendar.getInstance().run {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

        fun startOfToday(): Long = Calendar.getInstance().run {
            startOfDay(timeInMillis)
        }
    }
}

private fun Throwable.userMessage(fallback: String): String =
    localizedMessage?.takeIf(String::isNotBlank)?.take(180) ?: fallback
