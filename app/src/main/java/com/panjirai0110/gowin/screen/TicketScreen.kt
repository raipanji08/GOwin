package com.panjirai0110.gowin.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    viewModel: MainViewModel,
    onOpenTicket: (String) -> Unit
) {
    val today = startOfTicketDay(System.currentTimeMillis())
    val activeBookings = viewModel.userBookings.filter {
        val travelDate = it.travelDate.takeIf { value -> value > 0L } ?: it.createdAt
        BookingStatus.isActive(it.status) &&
            (it.status != BookingStatus.Paid || travelDate >= today)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tiket Saya",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GowinDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        when {
            viewModel.userBookingsLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GowinBlue)
                }
            }

            activeBookings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Belum ada tiket aktif",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GowinDark
                        )
                        Text(
                            "Pilih jadwal dari Beranda untuk memesan perjalanan.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeBookings, key = { it.id }) { booking ->
                        TravelBookingCard(
                            booking = booking,
                            schedule = viewModel.getScheduleById(booking.scheduleId),
                            onClick = { onOpenTicket(booking.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun startOfTicketDay(timestamp: Long): Long =
    java.util.Calendar.getInstance().run {
        timeInMillis = timestamp
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        timeInMillis
    }
