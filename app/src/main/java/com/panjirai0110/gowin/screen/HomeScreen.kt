package com.panjirai0110.gowin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panjirai0110.gowin.R
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinLightGray
import com.panjirai0110.shared.ui.theme.GowinRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    userName: String,
    onSelectSchedule: (String) -> Unit,
    onViewAllSchedules: () -> Unit
) {
    val schedules = viewModel.schedules
    var routeReversed by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    val firstSchedule = schedules.firstOrNull()
    val baseFrom = firstSchedule?.from ?: "Bandung"
    val baseTo = firstSchedule?.to ?: "Garut"
    val from = if (routeReversed) baseTo else baseFrom
    val to = if (routeReversed) baseFrom else baseTo
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
    }

    if (showNotifications) {
        val activeCount = viewModel.userBookings.count {
            com.panjirai0110.shared.model.BookingStatus.isActive(it.status)
        }
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notifikasi GO-WIN", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (activeCount > 0) {
                        "Anda memiliki $activeCount tiket atau pembayaran yang masih aktif."
                    } else {
                        "Belum ada notifikasi perjalanan baru."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showDatePicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = viewModel.selectedTravelDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(viewModel::setTravelDate)
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = userName.takeIf(String::isNotBlank)
                        ?.let { "Halo, $it 👋" }
                        ?: "Halo 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GowinDark
                )
                Text(
                    text = "Ke mana tujuan Anda?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GowinGray
                )
            }
            IconButton(onClick = { showNotifications = true }) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = "Notifikasi",
                    tint = GowinDark
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.gowin_home_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Perjalanan Nyaman",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = "$from ⇄ $to",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false,
                    ambientColor = Color(0x22000000),
                    spotColor = Color.Transparent
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, GowinBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    HomeRouteSection(
                        from = from,
                        to = to,
                        modifier = Modifier.padding(end = 48.dp)
                    )
                    Surface(
                        onClick = { routeReversed = !routeReversed },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = GowinLightGray
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = "Tukar rute",
                                tint = GowinDark,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = GowinBorder
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.height(58.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoField(
                        label = "Tanggal",
                        value = dateFormatter.format(Date(viewModel.selectedTravelDate)),
                        icon = {
                            HomeFieldAction(
                                onClick = { showDatePicker = true }
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Pilih tanggal",
                                    tint = GowinDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(GowinBorder)
                    )
                    InfoField(
                        label = "Penumpang",
                        value = "${viewModel.selectedPassengerCount} Orang",
                        icon = {
                            HomeFieldAction {
                                Icon(
                                    Icons.Default.PersonOutline,
                                    contentDescription = "Jumlah penumpang",
                                    tint = GowinDark,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onViewAllSchedules,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GowinBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("CARI TIKET", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Jadwal Hari Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GowinDark
            )
            Text(
                text = "Lihat Semua",
                style = MaterialTheme.typography.labelMedium,
                color = GowinBlue,
                modifier = Modifier.clickable(onClick = onViewAllSchedules)
            )
        }
        Spacer(Modifier.height(10.dp))

        when {
            viewModel.schedulesLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GowinBlue)
                }
            }

            viewModel.schedulesError != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(viewModel.schedulesError.orEmpty(), color = GowinRed)
                    TextButton(onClick = viewModel::observeSchedules) {
                        Text("Coba lagi")
                    }
                }
            }

            schedules.isEmpty() -> {
                Text(
                    "Belum ada jadwal tersedia.",
                    color = GowinGray,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, GowinBorder)
                ) {
                    Column {
                        val visibleSchedules = schedules.take(5)
                        visibleSchedules.forEachIndexed { index, schedule ->
                            ScheduleCard(
                                schedule = schedule,
                                onClick = { onSelectSchedule(schedule.id) }
                            )
                            if (index < visibleSchedules.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    thickness = 1.dp,
                                    color = GowinBorder
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HomeRouteSection(
    from: String,
    to: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(19.dp)
                .height(108.dp)
        ) {
            drawLine(
                color = GowinGreen,
                start = Offset(size.width / 2f, 31.dp.toPx()),
                end = Offset(size.width / 2f, size.height - 31.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = GowinGreen,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 13.dp)
                .size(19.dp)
        )
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = GowinGreen,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 13.dp)
                .size(19.dp)
        )
        Column(
            modifier = Modifier.padding(start = 29.dp)
        ) {
            HomeRouteText(label = "Asal", value = from)
            Spacer(Modifier.height(16.dp))
            HomeRouteText(label = "Tujuan", value = to)
        }
    }
}

@Composable
private fun HomeRouteText(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = GowinGray)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = GowinDark
        )
    }
}

@Composable
private fun HomeFieldAction(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val modifier = Modifier.size(36.dp)
    val shape = RoundedCornerShape(10.dp)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = GowinLightGray
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = GowinLightGray
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
private fun InfoField(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = GowinGray)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GowinDark
                )
            }
            icon()
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: Schedule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            schedule.time.replace(
                Regex("\\s*WIB\\s*$", RegexOption.IGNORE_CASE),
                ""
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GowinDark,
            modifier = Modifier.width(62.dp)
        )
        Text(
            "${schedule.from} → ${schedule.to}",
            style = MaterialTheme.typography.bodySmall,
            color = GowinDark,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatRupiah(schedule.price),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = GowinGreen
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Pilih jadwal",
            tint = GowinGreen,
            modifier = Modifier.size(28.dp)
        )
    }
}

fun formatRupiah(value: Int): String =
    "Rp%,d".format(Locale.forLanguageTag("id-ID"), value).replace(',', '.')
