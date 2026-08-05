package com.panjirai0110.shared.data

import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.model.UserProfile

interface TravelRepository {
    fun observeSchedules(onResult: (Result<List<Schedule>>) -> Unit): Subscription

    fun observeBookings(onResult: (Result<List<Booking>>) -> Unit): Subscription

    fun observeUserBookings(
        userId: String,
        onResult: (Result<List<Booking>>) -> Unit
    ): Subscription

    fun observeBooking(
        bookingId: String,
        onResult: (Result<Booking?>) -> Unit
    ): Subscription

    fun observeBookedSeats(
        scheduleId: String,
        travelDate: Long,
        onResult: (Result<Set<String>>) -> Unit
    ): Subscription

    fun checkAdminAccess(userId: String, onResult: (Result<Boolean>) -> Unit)

    fun addSchedule(schedule: Schedule, onResult: (Result<Unit>) -> Unit)

    fun deleteSchedule(scheduleId: String, onResult: (Result<Unit>) -> Unit)

    fun createBooking(booking: Booking, onResult: (Result<Booking>) -> Unit)

    fun updateBookingStatus(
        bookingId: String,
        status: String,
        paymentStatus: String,
        paymentReference: String = "",
        expectedStatus: String? = null,
        onResult: (Result<Unit>) -> Unit
    )

    fun observeUserProfile(
        userId: String,
        onResult: (Result<UserProfile?>) -> Unit
    ): Subscription

    fun saveUserProfile(profile: UserProfile, onResult: (Result<Unit>) -> Unit)

    fun migrateLegacySeatReservations(onResult: (Result<Unit>) -> Unit)
}

class SeatAlreadyBookedException :
    IllegalStateException("Kursi tersebut baru saja dipesan pengguna lain.")

class ScheduleHasBookingsException :
    IllegalStateException("Jadwal tidak dapat dihapus karena sudah memiliki pemesanan.")

class BookingStatusChangedException :
    IllegalStateException("Status pemesanan sudah berubah. Muat ulang data terbaru.")

fun seatReservationDocumentId(
    scheduleId: String,
    seatNumber: String,
    travelDate: Long
): String {
    fun String.safeDocumentPart(): String =
        trim().replace(Regex("[^A-Za-z0-9_-]"), "_")

    return "${scheduleId.safeDocumentPart()}_${travelDate}_${seatNumber.safeDocumentPart()}"
}

fun legacySeatReservationDocumentId(scheduleId: String, seatNumber: String): String =
    "${scheduleId.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")}_${
        seatNumber.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")
    }"

/**
 * Kept for existing tickets created before booking IDs became unique.
 * New code should use [seatReservationDocumentId] for seat locks.
 */
fun bookingDocumentId(scheduleId: String, seatNumber: String): String =
    legacySeatReservationDocumentId(scheduleId, seatNumber)
