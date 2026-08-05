package com.panjirai0110.admin.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.panjirai0110.shared.SharedServices
import com.panjirai0110.shared.auth.AuthRepository
import com.panjirai0110.shared.data.Subscription
import com.panjirai0110.shared.data.TravelRepository
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.PaymentMethod
import com.panjirai0110.shared.model.PaymentStatus
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.validation.InputValidator

class AdminViewModel(
    private val travelRepository: TravelRepository = SharedServices.travelRepository(),
    private val authRepository: AuthRepository = SharedServices.authRepository()
) : ViewModel() {

    var schedules by mutableStateOf<List<Schedule>>(emptyList())
        private set
    var bookings by mutableStateOf<List<Booking>>(emptyList())
        private set
    var dataLoading by mutableStateOf(true)
        private set
    var dataError by mutableStateOf<String?>(null)
        private set

    var authLoading by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
        private set

    var operationInProgress by mutableStateOf(false)
        private set
    var operationMessage by mutableStateOf<String?>(null)
        private set
    var operationError by mutableStateOf<String?>(null)
        private set

    private var scheduleSubscription: Subscription = Subscription.None
    private var bookingSubscription: Subscription = Subscription.None
    private var schedulesLoaded = false
    private var bookingsLoaded = false
    private var schedulesLoadError: String? = null
    private var bookingsLoadError: String? = null
    private var legacyMigrationError: String? = null
    private var legacyMigrationStarted = false

    init {
        dataLoading = false
    }

    fun continueFromWelcome(
        onAdminSession: () -> Unit,
        onLoginRequired: () -> Unit
    ) {
        val user = authRepository.currentUser
        if (user == null) {
            onLoginRequired()
            return
        }
        if (authLoading) return

        authLoading = true
        travelRepository.checkAdminAccess(user.id) { result ->
            authLoading = false
            result.onSuccess { isAdmin ->
                if (isAdmin) {
                    observeData()
                    onAdminSession()
                } else {
                    authRepository.signOut()
                    authError = "Sesi sebelumnya bukan akun admin."
                    onLoginRequired()
                }
            }.onFailure {
                authRepository.signOut()
                authError = it.userMessage("Sesi admin gagal diverifikasi.")
                onLoginRequired()
            }
        }
    }

    fun observeData() {
        scheduleSubscription.dispose()
        bookingSubscription.dispose()
        schedulesLoaded = false
        bookingsLoaded = false
        schedulesLoadError = null
        bookingsLoadError = null
        dataLoading = true
        dataError = null

        if (!legacyMigrationStarted) {
            legacyMigrationStarted = true
            travelRepository.migrateLegacySeatReservations { result ->
                result.onSuccess {
                    legacyMigrationError = null
                }.onFailure {
                    legacyMigrationStarted = false
                    legacyMigrationError = it.userMessage(
                        "Migrasi reservasi kursi lama gagal."
                    )
                }
                updateLoadingState()
            }
        }

        scheduleSubscription = travelRepository.observeSchedules { result ->
            schedulesLoaded = true
            result.onSuccess {
                schedules = it
                schedulesLoadError = null
            }.onFailure {
                schedulesLoadError = it.userMessage("Jadwal gagal dimuat.")
            }
            updateLoadingState()
        }
        bookingSubscription = travelRepository.observeBookings { result ->
            bookingsLoaded = true
            result.onSuccess {
                bookings = it
                bookingsLoadError = null
            }.onFailure {
                bookingsLoadError = it.userMessage("Data pemesanan gagal dimuat.")
            }
            updateLoadingState()
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (authLoading) return
        val validationError = InputValidator.credentialError(email, password)
        if (validationError != null) {
            authError = validationError
            return
        }

        authLoading = true
        authError = null
        authRepository.signInWithEmail(email, password) { result ->
            result.onSuccess { user ->
                travelRepository.checkAdminAccess(user.id) { accessResult ->
                    authLoading = false
                    accessResult.onSuccess { isAdmin ->
                        if (isAdmin) {
                            observeData()
                            onSuccess()
                        } else {
                            authRepository.signOut()
                            authError = "Akun ini tidak memiliki akses admin."
                        }
                    }.onFailure {
                        authRepository.signOut()
                        authError = it.userMessage("Akses admin gagal diverifikasi.")
                    }
                }
            }.onFailure {
                authLoading = false
                authError = it.userMessage("Login admin gagal.")
            }
        }
    }

    fun addSchedule(
        from: String,
        to: String,
        time: String,
        price: String,
        onSuccess: () -> Unit
    ) {
        if (operationInProgress) return
        val validationError = InputValidator.scheduleError(from, to, time, price)
        if (validationError != null) {
            operationError = validationError
            operationMessage = null
            return
        }

        operationInProgress = true
        clearOperationFeedback()
        travelRepository.addSchedule(
            Schedule(
                from = from.trim(),
                to = to.trim(),
                time = time.trim(),
                price = price.toInt()
            )
        ) { result ->
            operationInProgress = false
            result.onSuccess {
                operationMessage = "Jadwal berhasil ditambahkan."
                onSuccess()
            }.onFailure {
                operationError = it.userMessage("Jadwal gagal ditambahkan.")
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        if (operationInProgress) return
        if (scheduleId.isBlank()) {
            operationError = "ID jadwal tidak valid."
            return
        }

        operationInProgress = true
        clearOperationFeedback()
        travelRepository.deleteSchedule(scheduleId) { result ->
            operationInProgress = false
            result.onSuccess {
                operationMessage = "Jadwal berhasil dihapus."
            }.onFailure {
                operationError = it.userMessage("Jadwal gagal dihapus.")
            }
        }
    }

    fun approveManualTransfer(booking: Booking) {
        updateManualTransfer(
            booking = booking,
            status = BookingStatus.Paid,
            paymentStatus = PaymentStatus.Paid,
            successMessage = "Transfer disetujui dan tiket telah diterbitkan."
        )
    }

    fun rejectManualTransfer(booking: Booking) {
        updateManualTransfer(
            booking = booking,
            status = BookingStatus.Rejected,
            paymentStatus = PaymentStatus.Failed,
            successMessage = "Transfer ditolak dan kursi telah dilepas."
        )
    }

    fun clearAuthError() {
        authError = null
    }

    fun clearOperationFeedback() {
        operationMessage = null
        operationError = null
    }

    fun signOut() {
        scheduleSubscription.dispose()
        bookingSubscription.dispose()
        scheduleSubscription = Subscription.None
        bookingSubscription = Subscription.None
        authRepository.signOut()
        schedules = emptyList()
        bookings = emptyList()
        schedulesLoaded = false
        bookingsLoaded = false
        schedulesLoadError = null
        bookingsLoadError = null
        legacyMigrationError = null
        legacyMigrationStarted = false
        dataLoading = false
        operationInProgress = false
        clearOperationFeedback()
        clearAuthError()
    }

    private fun updateLoadingState() {
        dataLoading = !(schedulesLoaded && bookingsLoaded)
        dataError = schedulesLoadError ?: bookingsLoadError ?: legacyMigrationError
    }

    private fun updateManualTransfer(
        booking: Booking,
        status: String,
        paymentStatus: String,
        successMessage: String
    ) {
        if (operationInProgress) return
        if (booking.id.isBlank()) {
            operationError = "ID pemesanan tidak valid."
            return
        }
        if (
            booking.paymentMethod != PaymentMethod.ManualTransfer ||
            booking.status != BookingStatus.PendingVerification
        ) {
            operationError = "Transfer ini sudah diproses atau tidak valid."
            return
        }
        operationInProgress = true
        clearOperationFeedback()
        travelRepository.updateBookingStatus(
            bookingId = booking.id,
            status = status,
            paymentStatus = paymentStatus,
            paymentReference = booking.paymentReference,
            expectedStatus = BookingStatus.PendingVerification
        ) { result ->
            operationInProgress = false
            result.onSuccess {
                operationMessage = successMessage
            }.onFailure {
                operationError = it.userMessage("Status transfer gagal diperbarui.")
            }
        }
    }

    override fun onCleared() {
        scheduleSubscription.dispose()
        bookingSubscription.dispose()
        super.onCleared()
    }
}

private fun Throwable.userMessage(fallback: String): String =
    localizedMessage?.takeIf(String::isNotBlank)?.take(180) ?: fallback
