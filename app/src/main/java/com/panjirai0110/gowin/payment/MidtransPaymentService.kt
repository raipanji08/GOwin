package com.panjirai0110.gowin.payment

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.panjirai0110.gowin.BuildConfig
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.PaymentMethod
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

data class MidtransCheckout(
    val bookingId: String,
    val bookingCode: String,
    val paymentMethod: String,
    val transactionStatus: String,
    val transactionId: String,
    val qrString: String,
    val qrUrl: String,
    val deeplinkUrl: String,
    val virtualAccountNumber: String,
    val virtualAccountBank: String,
    val expiresAt: Long
) {
    companion object {
        fun fromBooking(booking: Booking): MidtransCheckout? {
            if (
                booking.id.isBlank() ||
                booking.paymentMethod == PaymentMethod.ManualTransfer
            ) {
                return null
            }
            return MidtransCheckout(
                bookingId = booking.id,
                bookingCode = booking.bookingCode,
                paymentMethod = booking.paymentMethod,
                transactionStatus = booking.midtransTransactionStatus,
                transactionId = booking.midtransTransactionId,
                qrString = booking.midtransQrString,
                qrUrl = booking.midtransQrUrl,
                deeplinkUrl = booking.midtransDeeplinkUrl,
                virtualAccountNumber = booking.virtualAccountNumber,
                virtualAccountBank = booking.virtualAccountBank,
                expiresAt = booking.paymentExpiresAt
            )
        }
    }
}

data class MidtransStatus(
    val bookingStatus: String,
    val paymentStatus: String
)

interface MidtransPaymentService {
    fun createCheckout(
        scheduleId: String,
        seatNumber: String,
        travelDate: Long,
        paymentMethod: String,
        onResult: (Result<MidtransCheckout>) -> Unit
    )

    fun syncStatus(
        bookingId: String,
        onResult: (Result<MidtransStatus>) -> Unit
    )
}

class WorkerMidtransPaymentService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val backendBaseUrl: String = BuildConfig.MIDTRANS_BACKEND_URL
) : MidtransPaymentService {
    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun createCheckout(
        scheduleId: String,
        seatNumber: String,
        travelDate: Long,
        paymentMethod: String,
        onResult: (Result<MidtransCheckout>) -> Unit
    ) {
        callBackend(
            endpoint = "create",
            payload = mapOf(
                "scheduleId" to scheduleId,
                "seatNumber" to seatNumber,
                "travelDate" to travelDate,
                "paymentMethod" to paymentMethod
            )
        ) { result ->
            result
                .mapCatching { data ->
                    val checkout = data.toCheckout()
                        ?: error("Instruksi pembayaran Midtrans tidak lengkap.")
                    if (!checkout.hasRequiredInstructions()) {
                        error("Instruksi pembayaran Midtrans tidak lengkap.")
                    }
                    checkout
                }
                .also(onResult)
        }
    }

    override fun syncStatus(
        bookingId: String,
        onResult: (Result<MidtransStatus>) -> Unit
    ) {
        callBackend(
            endpoint = "status",
            payload = mapOf("bookingId" to bookingId)
        ) { result ->
            result
                .mapCatching { data ->
                    val bookingStatus = data.optString("status")
                    val paymentStatus = data.optString("paymentStatus")
                    if (
                        bookingStatus.isBlank() ||
                        paymentStatus.isBlank()
                    ) {
                        error("Status pembayaran Midtrans tidak lengkap.")
                    }
                    MidtransStatus(
                        bookingStatus = bookingStatus,
                        paymentStatus = paymentStatus
                    )
                }
                .also(onResult)
        }
    }

    private fun callBackend(
        endpoint: String,
        payload: Map<String, Any>,
        onResult: (Result<JSONObject>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Sesi login telah berakhir. Silakan masuk kembali."
                    )
                )
            )
            return
        }
        if (backendBaseUrl.isBlank()) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Backend pembayaran belum dikonfigurasi."
                    )
                )
            )
            return
        }
        if (!backendBaseUrl.startsWith("https://", ignoreCase = true)) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Backend pembayaran wajib menggunakan HTTPS."
                    )
                )
            )
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->
                val idToken = tokenResult.token
                if (idToken.isNullOrBlank()) {
                    onResult(
                        Result.failure(
                            IllegalStateException(
                                "Token login tidak tersedia."
                            )
                        )
                    )
                    return@addOnSuccessListener
                }
                executor.execute {
                    val result = runCatching {
                        postJson(
                            endpoint = endpoint,
                            idToken = idToken,
                            payload = payload
                        )
                    }
                    mainHandler.post { onResult(result) }
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun postJson(
        endpoint: String,
        idToken: String,
        payload: Map<String, Any>
    ): JSONObject {
        val connection = (
            URL("${backendBaseUrl.trimEnd('/')}/$endpoint")
                .openConnection() as HttpURLConnection
            ).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $idToken")
            }
        return try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(JSONObject(payload).toString())
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            val response = responseText
                .takeIf(String::isNotBlank)
                ?.let(::JSONObject)
                ?: JSONObject()
            if (statusCode !in 200..299) {
                val message = response
                    .optJSONObject("error")
                    ?.optString("message")
                    .orEmpty()
                    .ifBlank {
                        "Backend pembayaran belum dapat memproses permintaan."
                    }
                error(message)
            }
            response
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject.toCheckout(): MidtransCheckout? {
    val bookingId = optString("bookingId")
    val bookingCode = optString("bookingCode")
    val paymentMethod = optString("paymentMethod")
    val transactionStatus = optString("transactionStatus")
    if (
        bookingId.isBlank() ||
        bookingCode.isBlank() ||
        paymentMethod.isBlank() ||
        transactionStatus.isBlank()
    ) {
        return null
    }
    return MidtransCheckout(
        bookingId = bookingId,
        bookingCode = bookingCode,
        paymentMethod = paymentMethod,
        transactionStatus = transactionStatus,
        transactionId = optString("midtransTransactionId"),
        qrString = optString("midtransQrString"),
        qrUrl = optString("midtransQrUrl"),
        deeplinkUrl = optString("midtransDeeplinkUrl"),
        virtualAccountNumber = optString("virtualAccountNumber"),
        virtualAccountBank = optString("virtualAccountBank"),
        expiresAt = optLong("paymentExpiresAt")
    )
}

private fun MidtransCheckout.hasRequiredInstructions(): Boolean {
    if (
        deeplinkUrl.startsWith(
            "https://app.sandbox.midtrans.com/",
            ignoreCase = true
        )
    ) {
        return true
    }
    return when (paymentMethod) {
        PaymentMethod.MidtransQris ->
            qrString.isNotBlank() || qrUrl.isNotBlank()

        PaymentMethod.MidtransEWallet -> deeplinkUrl.isNotBlank()
        PaymentMethod.MidtransVirtualAccount ->
            virtualAccountNumber.isNotBlank()

        else -> false
    }
}
