package com.panjirai0110.gowin.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.PaymentMethod
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun generateTicketPdf(context: Context, booking: Booking): File {
    val ticketDirectory = File(context.cacheDir, "tickets").apply { mkdirs() }
    val safeCode = booking.bookingCode.ifBlank { booking.id.take(10).uppercase() }
    val output = File(ticketDirectory, "GO-WIN-$safeCode.pdf")
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2563EB.toInt()
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1F2937.toInt()
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF374151.toInt()
        textSize = 15f
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt()
        textSize = 13f
    }
    val rupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    val date = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        .format(Date(booking.travelDate.takeIf { it > 0L } ?: booking.createdAt))

    canvas.drawText("GO-WIN", 42f, 64f, titlePaint)
    canvas.drawText("Tiket Digital Perjalanan", 42f, 96f, headingPaint)
    canvas.drawLine(42f, 116f, 553f, 116f, labelPaint)
    canvas.drawText(
        "${booking.routeFrom}  →  ${booking.routeTo}",
        42f,
        160f,
        headingPaint
    )

    val rows = listOf(
        "Kode Booking" to safeCode,
        "Tanggal" to date,
        "Jam" to booking.departureTime,
        "Kendaraan" to booking.vehicleName,
        "Kursi" to booking.seatNumber,
        "Penumpang" to booking.userName,
        "Pembayaran" to PaymentMethod.displayName(booking.paymentMethod),
        "Total" to rupiah.format(booking.totalAmount)
    )
    var y = 206f
    rows.forEach { (label, value) ->
        canvas.drawText(label, 42f, y, labelPaint)
        canvas.drawText(value, 210f, y, bodyPaint)
        y += 34f
    }

    val qr = generateQR(
        "gowin://ticket/${booking.id}?code=$safeCode",
        width = 480,
        height = 480
    )
    canvas.drawBitmap(qr, null, android.graphics.Rect(190, 500, 405, 715), null)
    canvas.drawText(
        "Tunjukkan QR ini kepada petugas sebelum keberangkatan.",
        104f,
        754f,
        labelPaint
    )
    canvas.drawText(
        "Dokumen dibuat oleh aplikasi GO-WIN.",
        182f,
        790f,
        labelPaint
    )

    document.finishPage(page)
    FileOutputStream(output).use(document::writeTo)
    document.close()
    return output
}
