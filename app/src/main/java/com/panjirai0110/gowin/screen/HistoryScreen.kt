package com.panjirai0110.gowin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.PaymentStatus
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.ui.theme.GowinAmber
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    All("Semua"),
    Upcoming("Akan Datang"),
    Completed("Selesai")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onOpenTicket: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.All) }

    val today = remember { startOfDay(System.currentTimeMillis()) }
    val filteredBookings = viewModel.userBookings.filter { booking ->
        val travelDate = booking.travelDate.takeIf { it > 0L } ?: booking.createdAt
        when (selectedFilter) {
            HistoryFilter.All -> true
            HistoryFilter.Upcoming ->
                BookingStatus.isActive(booking.status) && travelDate >= today
            HistoryFilter.Completed ->
                booking.status == BookingStatus.Paid && travelDate < today
        }
    }.let { bookings ->
        when (selectedFilter) {
            HistoryFilter.Upcoming -> bookings.sortedBy {
                it.travelDate.takeIf { value -> value > 0L } ?: it.createdAt
            }
            HistoryFilter.All,
            HistoryFilter.Completed -> bookings.sortedByDescending {
                it.travelDate.takeIf { value -> value > 0L } ?: it.createdAt
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Perjalanan Saya",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GowinDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    val selected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                if (selected) GowinBlue else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedFilter = filter },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            filter.label,
                            color = if (selected) Color.White else GowinGray,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Medium
                            }
                        )
                    }
                }
            }

            when {
                viewModel.userBookingsLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GowinBlue)
                    }
                }

                viewModel.userBookingsError != null -> {
                    EmptyHistory(
                        title = "Riwayat gagal dimuat",
                        subtitle = viewModel.userBookingsError.orEmpty()
                    )
                }

                filteredBookings.isEmpty() -> {
                    EmptyHistory(
                        title = "Belum ada perjalanan",
                        subtitle = when (selectedFilter) {
                            HistoryFilter.Upcoming ->
                                "Pesanan aktif dan perjalanan yang akan datang muncul di sini."
                            HistoryFilter.Completed ->
                                "Perjalanan yang telah selesai akan tersimpan di sini."
                            HistoryFilter.All ->
                                "Pesan jadwal pertama Anda dari halaman Beranda."
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 2.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredBookings,
                            key = { it.id.ifBlank { "${it.scheduleId}-${it.seatNumber}" } }
                        ) { booking ->
                            TravelBookingCard(
                                booking = booking,
                                schedule = viewModel.getScheduleById(booking.scheduleId),
                                onClick = {
                                    if (booking.id.isNotBlank()) onOpenTicket(booking.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TravelBookingCard(
    booking: Booking,
    schedule: Schedule?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val from = booking.routeFrom.ifBlank { schedule?.from.orEmpty() }.ifBlank { "Asal" }
    val to = booking.routeTo.ifBlank { schedule?.to.orEmpty() }.ifBlank { "Tujuan" }
    val departureTime = booking.departureTime.ifBlank { schedule?.time.orEmpty() }.ifBlank { "--:--" }
    val displayDepartureTime = departureTime.replace(
        Regex("\\s*WIB\\s*$", RegexOption.IGNORE_CASE),
        ""
    )
    val travelDate = booking.travelDate.takeIf { it > 0L } ?: booking.createdAt
    val dateText = remember(travelDate) {
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.forLanguageTag("id-ID")
        ).format(Date(travelDate))
    }
    val status = bookingVisualStatus(booking)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = booking.id.isNotBlank(), onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, GowinBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(status.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    status.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$from  →  $to",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GowinDark
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "$dateText • $displayDepartureTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = GowinGray
                )
                Text(
                    "Kursi ${booking.seatNumber.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GowinGray
                )
            }
            Box(
                modifier = Modifier
                    .background(status.background, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = status.color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GowinDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = GowinGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class BookingVisualStatus(
    val label: String,
    val color: Color,
    val background: Color,
    val icon: ImageVector
)

private fun bookingVisualStatus(booking: Booking): BookingVisualStatus = when {
    booking.status == BookingStatus.Paid || booking.paymentStatus == PaymentStatus.Paid ->
        BookingVisualStatus(
            label = "Selesai",
            color = GowinGreen,
            background = Color(0xFFE7F8ED),
            icon = Icons.Default.Check
        )

    booking.status == BookingStatus.PendingVerification ->
        BookingVisualStatus(
            label = "Verifikasi",
            color = GowinAmber,
            background = Color(0xFFFFF5D8),
            icon = Icons.Default.HourglassTop
        )

    booking.status == BookingStatus.PendingPayment ->
        BookingVisualStatus(
            label = "Bayar",
            color = GowinBlue,
            background = Color(0xFFE8F1FF),
            icon = Icons.Default.HourglassTop
        )

    else ->
        BookingVisualStatus(
            label = when (booking.status) {
                BookingStatus.Expired -> "Kedaluwarsa"
                BookingStatus.Rejected -> "Ditolak"
                else -> "Dibatalkan"
            },
            color = GowinRed,
            background = Color(0xFFFFEAEA),
            icon = Icons.Default.Close
        )
}

private fun startOfDay(timestamp: Long): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
