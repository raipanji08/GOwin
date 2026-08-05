package com.panjirai0110.gowin.screen

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.panjirai0110.gowin.ui.component.GowinPrimaryActionButton
import com.panjirai0110.gowin.ui.component.GowinSecondaryActionButton
import com.panjirai0110.gowin.util.generateQR
import com.panjirai0110.gowin.util.generateTicketPdf
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.PaymentStatus
import com.panjirai0110.shared.ui.theme.GowinAmber
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinRed
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DigitalTicketScreen(
    bookingId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onContinuePayment: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    var pdfToSave by remember { mutableStateOf<File?>(null) }
    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destination ->
        val source = pdfToSave
        if (destination != null && source != null) {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Lokasi file tidak dapat dibuka.")
            }.onSuccess {
                Toast.makeText(
                    context,
                    "Tiket berhasil disimpan.",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    "Tiket gagal disimpan.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        pdfToSave = null
    }

    DisposableEffect(bookingId) {
        val observationGeneration = viewModel.observeBooking(bookingId)
        onDispose {
            viewModel.stopObservingBooking(observationGeneration)
        }
    }

    BackHandler(onBack = onBack)

    val booking = viewModel.currentBooking
    if (booking == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.paymentError != null) {
                Text(
                    text = viewModel.paymentError.orEmpty(),
                    color = GowinRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                CircularProgressIndicator(color = GowinBlue)
            }
        }
        return
    }

    val schedule = viewModel.getScheduleById(booking.scheduleId)
    val displayBooking = booking.copy(
        routeFrom = booking.routeFrom.ifBlank { schedule?.from.orEmpty() },
        routeTo = booking.routeTo.ifBlank { schedule?.to.orEmpty() },
        departureTime = booking.departureTime.ifBlank { schedule?.time.orEmpty() },
        ticketPrice = booking.ticketPrice.takeIf { it > 0 } ?: schedule?.price ?: 0,
        totalAmount = booking.totalAmount.takeIf { it > 0 }
            ?: (schedule?.price ?: 0) + booking.adminFee
    )
    val code = displayBooking.bookingCode.ifBlank {
        "GW${displayBooking.id.take(8).uppercase()}"
    }

    DigitalTicketContent(
        booking = displayBooking,
        bookingCode = code,
        onDownload = {
            val pdf = generateTicketPdf(context, displayBooking)
            pdfToSave = pdf
            savePdfLauncher.launch("GO-WIN-$code.pdf")
        },
        onShare = {
            val pdf = generateTicketPdf(context, displayBooking)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                pdf
            )
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Bagikan tiket GO-WIN"
                )
            )
        },
        onBack = onBack,
        onContinuePayment = onContinuePayment,
        onBackHome = onBackHome
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DigitalTicketContent(
    booking: Booking,
    bookingCode: String,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onContinuePayment: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val isPaid = booking.status == BookingStatus.Paid ||
        booking.paymentStatus == PaymentStatus.Paid
    val isPendingVerification =
        booking.status == BookingStatus.PendingVerification
    val isPendingPayment = booking.status == BookingStatus.PendingPayment

    val statusColor = when {
        isPaid -> GowinGreen
        isPendingPayment -> GowinBlue
        isPendingVerification -> GowinAmber
        else -> GowinRed
    }
    val statusIcon = when {
        isPaid -> Icons.Default.Check
        isPendingPayment -> Icons.Default.AccessTime
        isPendingVerification -> Icons.Default.HourglassTop
        else -> Icons.Default.Close
    }
    val statusTitle = when {
        isPaid -> "Pembayaran Berhasil!"
        isPendingPayment -> "Menunggu Pembayaran"
        isPendingVerification -> "Menunggu Verifikasi"
        else -> "Pembayaran Tidak Berhasil"
    }
    val statusSubtitle = when {
        isPaid -> "Tiket Anda telah dipesan."
        isPendingPayment -> "Selesaikan pembayaran untuk menerbitkan tiket."
        isPendingVerification -> "Pembayaran Anda sedang diperiksa oleh admin."
        else -> "Kursi telah dilepas dan dapat dipesan kembali."
    }
    val travelDate = booking.travelDate.takeIf { it > 0L } ?: booking.createdAt
    val dateText = SimpleDateFormat(
        "dd MMM yyyy",
        Locale.forLanguageTag("id-ID")
    ).format(Date(travelDate))
    val qrBitmap = remember(booking.id, bookingCode) {
        generateQR("gowin://ticket/${booking.id}?code=$bookingCode")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Kembali",
                            tint = if (isPaid) Color.White else GowinDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isPaid) GowinGreen else Color.White
                )
            )
        },
        containerColor = if (isPaid) GowinGreen else Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isPaid) Color.White else statusColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = if (isPaid) GowinGreen else Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusTitle,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPaid) Color.White else GowinDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusSubtitle,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (isPaid) {
                    Color.White.copy(alpha = 0.9f)
                } else {
                    GowinGray
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = if (isPaid) {
                    null
                } else {
                    BorderStroke(1.dp, GowinBorder)
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${booking.routeFrom}  →  ${booking.routeTo}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GowinDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TicketMeta(
                            icon = Icons.Default.CalendarMonth,
                            text = dateText
                        )
                        TicketMeta(
                            icon = Icons.Default.AccessTime,
                            text = booking.departureTime
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TicketValueRow(
                        label = "Kursi",
                        value = booking.seatNumber
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TicketValueRow(
                        label = "Kode Booking",
                        value = bookingCode
                    )

                    if (isPaid) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR tiket $bookingCode",
                            modifier = Modifier
                                .size(168.dp)
                                .align(Alignment.CenterHorizontally),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = GowinBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TicketActionButton(
                                text = "Download Tiket",
                                icon = Icons.Default.Download,
                                onClick = onDownload,
                                modifier = Modifier.weight(1f)
                            )
                            TicketActionButton(
                                text = "Bagikan",
                                icon = Icons.Default.Share,
                                onClick = onShare,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = GowinBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isPendingPayment) {
                            GowinPrimaryActionButton(
                                text = "LANJUTKAN PEMBAYARAN",
                                onClick = { onContinuePayment(booking.id) },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GowinSecondaryActionButton(
                                text = "KEMBALI KE BERANDA",
                                onClick = onBackHome,
                            )
                        } else {
                            GowinPrimaryActionButton(
                                text = "KEMBALI KE BERANDA",
                                onClick = onBackHome,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketMeta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GowinGreen,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            color = GowinDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TicketValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = GowinGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = GowinDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TicketActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GowinBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = GowinBlue
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
