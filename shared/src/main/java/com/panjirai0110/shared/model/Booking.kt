package com.panjirai0110.shared.model

data class Booking(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val scheduleId: String = "",
    val seatNumber: String = "",
    val routeFrom: String = "",
    val routeTo: String = "",
    val departureTime: String = "",
    val vehicleName: String = "Hiace Premio",
    val travelDate: Long = 0L,
    val ticketPrice: Int = 0,
    val adminFee: Int = DEFAULT_ADMIN_FEE,
    val totalAmount: Int = 0,
    val paymentMethod: String = PaymentMethod.Legacy,
    val paymentStatus: String = PaymentStatus.Paid,
    val status: String = BookingStatus.Paid,
    val paymentReference: String = "",
    val midtransTransactionId: String = "",
    val midtransTransactionStatus: String = "",
    val midtransQrString: String = "",
    val midtransQrUrl: String = "",
    val midtransDeeplinkUrl: String = "",
    val virtualAccountNumber: String = "",
    val virtualAccountBank: String = "",
    val paymentExpiresAt: Long = 0L,
    val bookingCode: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

const val DEFAULT_ADMIN_FEE = 2_500

object BookingStatus {
    const val PendingPayment = "pending_payment"
    const val PendingVerification = "pending_verification"
    const val Paid = "paid"
    const val Cancelled = "cancelled"
    const val Rejected = "rejected"
    const val Expired = "expired"

    fun isActive(value: String): Boolean =
        value == PendingPayment || value == PendingVerification || value == Paid
}

object PaymentStatus {
    const val Pending = "pending"
    const val Verification = "verification"
    const val Paid = "paid"
    const val Failed = "failed"
    const val Expired = "expired"
    const val Refunded = "refunded"
}

object PaymentMethod {
    const val Legacy = "legacy"
    const val ManualTransfer = "manual_transfer"
    const val MidtransQris = "midtrans_qris"
    const val MidtransEWallet = "midtrans_ewallet"
    const val MidtransVirtualAccount = "midtrans_virtual_account"

    fun displayName(value: String): String = when (value) {
        ManualTransfer -> "Transfer Bank"
        MidtransQris -> "QRIS"
        MidtransEWallet -> "E-Wallet"
        MidtransVirtualAccount -> "Virtual Account"
        else -> "Pembayaran lama"
    }
}
