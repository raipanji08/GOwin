package com.panjirai0110.shared.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.model.UserProfile

internal class FirebaseTravelRepository(
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TravelRepository {

    override fun observeSchedules(
        onResult: (Result<List<Schedule>>) -> Unit
    ): Subscription {
        val registration = database.collection(SCHEDULES)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> onResult(Result.failure(error))
                    snapshot == null -> onResult(Result.success(emptyList()))
                    else -> {
                        val schedules = snapshot.documents
                            .mapNotNull { document ->
                                document.toObject(Schedule::class.java)
                                    ?.copy(id = document.id)
                            }
                            .sortedWith(compareBy(Schedule::from, Schedule::to, Schedule::time))
                        onResult(Result.success(schedules))
                    }
                }
        }
        return Subscription { registration.remove() }
    }

    override fun observeUserBookings(
        userId: String,
        onResult: (Result<List<Booking>>) -> Unit
    ): Subscription {
        if (userId.isBlank()) {
            onResult(Result.success(emptyList()))
            return Subscription.None
        }
        val registration = database.collection(BOOKINGS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> onResult(Result.failure(error))
                    snapshot == null -> onResult(Result.success(emptyList()))
                    else -> onResult(
                        Result.success(
                            snapshot.documents
                                .mapNotNull { document ->
                                    document.toObject(Booking::class.java)
                                        ?.copy(id = document.id)
                                }
                                .sortedByDescending(Booking::createdAt)
                        )
                    )
                }
            }
        return Subscription { registration.remove() }
    }

    override fun observeBooking(
        bookingId: String,
        onResult: (Result<Booking?>) -> Unit
    ): Subscription {
        if (bookingId.isBlank()) {
            onResult(Result.success(null))
            return Subscription.None
        }
        val registration = database.collection(BOOKINGS)
            .document(bookingId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> onResult(Result.failure(error))
                    snapshot == null || !snapshot.exists() -> onResult(Result.success(null))
                    else -> onResult(
                        Result.success(
                            snapshot.toObject(Booking::class.java)?.copy(id = snapshot.id)
                        )
                    )
                }
            }
        return Subscription { registration.remove() }
    }

    override fun observeBookings(
        onResult: (Result<List<Booking>>) -> Unit
    ): Subscription {
        val registration = database.collection(BOOKINGS)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> onResult(Result.failure(error))
                    snapshot == null -> onResult(Result.success(emptyList()))
                    else -> {
                        val bookings = snapshot.documents
                            .mapNotNull { document ->
                                document.toObject(Booking::class.java)
                                    ?.copy(id = document.id)
                            }
                            .sortedByDescending(Booking::createdAt)
                        onResult(Result.success(bookings))
                    }
                }
            }
        return Subscription { registration.remove() }
    }

    override fun observeBookedSeats(
        scheduleId: String,
        travelDate: Long,
        onResult: (Result<Set<String>>) -> Unit
    ): Subscription {
        val registration = database.collection(SEAT_RESERVATIONS)
            .whereEqualTo("scheduleId", scheduleId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> onResult(Result.failure(error))
                    snapshot == null -> onResult(Result.success(emptySet()))
                    else -> {
                        val seats = snapshot.documents
                            .filter { document ->
                                val reservedDate = document.getLong("travelDate")
                                reservedDate == null || reservedDate == travelDate
                            }
                            .mapNotNull { it.getString("seatNumber") }
                            .filter(String::isNotBlank)
                            .toSet()
                        onResult(Result.success(seats))
                    }
                }
            }
        return Subscription { registration.remove() }
    }

    override fun checkAdminAccess(
        userId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        database.collection(ADMINS)
            .document(userId)
            .get()
            .addOnSuccessListener { onResult(Result.success(it.exists())) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun addSchedule(
        schedule: Schedule,
        onResult: (Result<Unit>) -> Unit
    ) {
        database.collection(SCHEDULES)
            .add(schedule.copy(id = ""))
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun deleteSchedule(
        scheduleId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        database.collection(BOOKINGS)
            .whereEqualTo("scheduleId", scheduleId)
            .get()
            .addOnSuccessListener { bookings ->
                val hasActiveBookings = bookings.documents.any { document ->
                    val booking = document.toObject(Booking::class.java)
                    booking != null && bookingBlocksScheduleDeletion(booking)
                }
                if (hasActiveBookings) {
                    onResult(Result.failure(ScheduleHasBookingsException()))
                    return@addOnSuccessListener
                }

                database.collection(SCHEDULES)
                    .document(scheduleId)
                    .delete()
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { onResult(Result.failure(it)) }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun createBooking(
        booking: Booking,
        onResult: (Result<Booking>) -> Unit
    ) {
        val bookingDocument = booking.id
            .takeIf(String::isNotBlank)
            ?.let { database.collection(BOOKINGS).document(it) }
            ?: database.collection(BOOKINGS).document()
        val documentId = bookingDocument.id
        val reservationId =
            seatReservationDocumentId(
                booking.scheduleId,
                booking.seatNumber,
                booking.travelDate
            )
        val reservationDocument =
            database.collection(SEAT_RESERVATIONS).document(reservationId)
        val legacyReservationDocument = database.collection(SEAT_RESERVATIONS)
            .document(
                legacySeatReservationDocumentId(
                    booking.scheduleId,
                    booking.seatNumber
                )
            )
        val timestamp = booking.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val persistedBooking = booking.copy(
            id = documentId,
            bookingCode = booking.bookingCode.takeIf(String::isNotBlank)
                ?: "GW${documentId.take(8).uppercase()}",
            createdAt = timestamp,
            updatedAt = booking.updatedAt.takeIf { it > 0L } ?: timestamp
        )

        database.runTransaction { transaction ->
            val reservationExists = transaction.get(reservationDocument).exists()
            val legacyReservationExists =
                transaction.get(legacyReservationDocument).exists()
            if (reservationExists || legacyReservationExists) {
                throw SeatAlreadyBookedException()
            }
            transaction.set(
                reservationDocument,
                mapOf(
                    "scheduleId" to persistedBooking.scheduleId,
                    "seatNumber" to persistedBooking.seatNumber,
                    "bookingId" to persistedBooking.id,
                    "userId" to persistedBooking.userId,
                    "travelDate" to persistedBooking.travelDate,
                    "createdAt" to persistedBooking.createdAt
                )
            )
            transaction.set(bookingDocument, persistedBooking)
        }
            .addOnSuccessListener { onResult(Result.success(persistedBooking)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun updateBookingStatus(
        bookingId: String,
        status: String,
        paymentStatus: String,
        paymentReference: String,
        expectedStatus: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        val bookingDocument = database.collection(BOOKINGS).document(bookingId)
        database.runTransaction { transaction ->
            val bookingSnapshot = transaction.get(bookingDocument)
            val booking = bookingSnapshot.toObject(Booking::class.java)
                ?: throw IllegalStateException("Pemesanan tidak ditemukan.")
            if (expectedStatus != null && booking.status != expectedStatus) {
                throw BookingStatusChangedException()
            }
            val reservationDocument = database.collection(SEAT_RESERVATIONS)
                .document(
                    seatReservationDocumentId(
                        booking.scheduleId,
                        booking.seatNumber,
                        booking.travelDate
                    )
                )
            val legacyReservationDocument = database.collection(SEAT_RESERVATIONS)
                .document(
                    legacySeatReservationDocumentId(
                        booking.scheduleId,
                        booking.seatNumber
                    )
                )
            val reservationSnapshot = transaction.get(reservationDocument)
            val legacyReservationSnapshot = transaction.get(legacyReservationDocument)

            transaction.update(
                bookingDocument,
                mapOf(
                    "status" to status,
                    "paymentStatus" to paymentStatus,
                    "paymentReference" to paymentReference,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            if (!BookingStatus.isActive(status) && reservationSnapshot.exists()) {
                transaction.delete(reservationDocument)
            }
            if (
                !BookingStatus.isActive(status) &&
                legacyReservationSnapshot.exists() &&
                legacyReservationSnapshot.getString("bookingId") == bookingId
            ) {
                transaction.delete(legacyReservationDocument)
            }
        }
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun observeUserProfile(
        userId: String,
        onResult: (Result<UserProfile?>) -> Unit
    ): Subscription {
        if (userId.isBlank()) {
            onResult(Result.success(null))
            return Subscription.None
        }
        val registration = database.collection(USER_PROFILES)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> onResult(Result.failure(error))
                    snapshot == null || !snapshot.exists() -> onResult(Result.success(null))
                    else -> onResult(
                        Result.success(
                            snapshot.toObject(UserProfile::class.java)?.copy(userId = snapshot.id)
                        )
                    )
                }
            }
        return Subscription { registration.remove() }
    }

    override fun saveUserProfile(
        profile: UserProfile,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (profile.userId.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("ID pengguna tidak valid.")))
            return
        }
        val persistedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        database.collection(USER_PROFILES)
            .document(profile.userId)
            .set(persistedProfile, SetOptions.merge())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun migrateLegacySeatReservations(onResult: (Result<Unit>) -> Unit) {
        database.collection(SEAT_RESERVATIONS)
            .get()
            .addOnSuccessListener { snapshot ->
                val legacyReservations = snapshot.documents.filter {
                    it.getLong("travelDate") == null &&
                        !it.getString("bookingId").isNullOrBlank()
                }
                migrateLegacyReservationAt(
                    reservations = legacyReservations,
                    index = 0,
                    onResult = onResult
                )
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    private fun migrateLegacyReservationAt(
        reservations: List<com.google.firebase.firestore.DocumentSnapshot>,
        index: Int,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (index >= reservations.size) {
            onResult(Result.success(Unit))
            return
        }
        val legacySnapshot = reservations[index]
        val bookingId = legacySnapshot.getString("bookingId").orEmpty()
        database.collection(BOOKINGS)
            .document(bookingId)
            .get()
            .addOnSuccessListener { bookingSnapshot ->
                val booking = bookingSnapshot.toObject(Booking::class.java)
                if (booking == null || booking.travelDate <= 0L) {
                    migrateLegacyReservationAt(reservations, index + 1, onResult)
                    return@addOnSuccessListener
                }
                if (!BookingStatus.isActive(booking.status)) {
                    legacySnapshot.reference.delete()
                        .addOnSuccessListener {
                            migrateLegacyReservationAt(
                                reservations,
                                index + 1,
                                onResult
                            )
                        }
                        .addOnFailureListener { onResult(Result.failure(it)) }
                    return@addOnSuccessListener
                }
                val target = database.collection(SEAT_RESERVATIONS)
                    .document(
                        seatReservationDocumentId(
                            booking.scheduleId,
                            booking.seatNumber,
                            booking.travelDate
                        )
                    )
                database.runTransaction { transaction ->
                    val existing = transaction.get(target)
                    if (
                        existing.exists() &&
                        existing.getString("bookingId") != bookingId
                    ) {
                        throw SeatAlreadyBookedException()
                    }
                    if (!existing.exists()) {
                        transaction.set(
                            target,
                            mapOf(
                                "scheduleId" to booking.scheduleId,
                                "seatNumber" to booking.seatNumber,
                                "bookingId" to bookingId,
                                "userId" to booking.userId,
                                "travelDate" to booking.travelDate,
                                "createdAt" to booking.createdAt
                            )
                        )
                    }
                    transaction.delete(legacySnapshot.reference)
                }.addOnSuccessListener {
                    migrateLegacyReservationAt(reservations, index + 1, onResult)
                }.addOnFailureListener { onResult(Result.failure(it)) }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    private companion object {
        const val SCHEDULES = "schedules"
        const val BOOKINGS = "bookings"
        const val SEAT_RESERVATIONS = "seat_reservations"
        const val ADMINS = "admins"
        const val USER_PROFILES = "user_profiles"
    }
}

private fun bookingBlocksScheduleDeletion(booking: Booking): Boolean {
    if (!BookingStatus.isActive(booking.status)) return false
    if (booking.status != BookingStatus.Paid) return true
    val travelDate = booking.travelDate.takeIf { it > 0L } ?: booking.createdAt
    return travelDate >= startOfToday()
}

private fun startOfToday(): Long = java.util.Calendar.getInstance().run {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
    timeInMillis
}
