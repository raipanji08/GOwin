package com.panjirai0110.gowin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.DEFAULT_ADMIN_FEE
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    scheduleId: String,
    seatNumber: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProceedToPayment: () -> Unit
) {
    val schedule = viewModel.schedules.find { it.id == scheduleId }
    val adminFee = DEFAULT_ADMIN_FEE
    val dateFormat = SimpleDateFormat(
        "dd MMM yyyy",
        Locale.forLanguageTag("id-ID")
    )
    val currentDate = dateFormat.format(Date(viewModel.selectedTravelDate))

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Detail Pemesanan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GowinDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Kembali",
                            tint = GowinDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (schedule == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Jadwal tidak ditemukan", color = GowinRed)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, GowinBorder)
            ) {
                Column {
                    BookingRouteSection(
                        from = schedule.from,
                        to = schedule.to
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = GowinBorder
                    )

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        )
                    ) {
                        DetailRow(label = "Tanggal", value = currentDate)
                        Spacer(Modifier.height(12.dp))
                        DetailRow(
                            label = "Jam",
                            value = schedule.time.replace(
                                Regex("\\s*WIB\\s*$", RegexOption.IGNORE_CASE),
                                ""
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        DetailRow(label = "Kursi", value = seatNumber)
                        Spacer(Modifier.height(12.dp))
                        DetailRow(
                            label = "Harga Tiket",
                            value = formatRupiah(schedule.price)
                        )
                        Spacer(Modifier.height(12.dp))
                        DetailRow(
                            label = "Biaya Admin",
                            value = formatRupiah(adminFee)
                        )
                    }

                    DashedHorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    val totalPrice = schedule.price + adminFee
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Pembayaran",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GowinDark
                        )
                        Text(
                            text = formatRupiah(totalPrice),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GowinGreen
                        )
                    }

                    Button(
                        onClick = onProceedToPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GowinBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "PESAN SEKARANG",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BookingRouteSection(
    from: String,
    to: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BookingRouteMarker()
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.height(80.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = from,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GowinDark
            )
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = GowinGray,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = to,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GowinDark
            )
        }
    }
}

@Composable
private fun BookingRouteMarker() {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(80.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = GowinGreen,
                start = Offset(size.width / 2f, 17.dp.toPx()),
                end = Offset(size.width / 2f, size.height - 17.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                )
            )
        }
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = GowinGreen,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(18.dp)
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = GowinGreen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(18.dp)
        )
    }
}

@Composable
private fun DashedHorizontalDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .height(1.dp)
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = GowinBorder,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(5.dp.toPx(), 4.dp.toPx())
            )
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = GowinGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = GowinDark
        )
    }
}
