package com.panjirai0110.admin.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.panjirai0110.admin.viewmodel.AdminViewModel
import com.panjirai0110.shared.model.Schedule
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.PaymentMethod
import com.panjirai0110.shared.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onSignOut: () -> Unit
) {
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var scheduleToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val manualTransfers = viewModel.bookings.filter {
        it.paymentMethod == PaymentMethod.ManualTransfer &&
            it.status == BookingStatus.PendingVerification
    }
    val operationMessage = viewModel.operationMessage

    BackHandler(enabled = viewModel.operationInProgress) { }

    LaunchedEffect(operationMessage) {
        if (operationMessage != null) {
            delay(3_500)
            if (viewModel.operationMessage == operationMessage) {
                viewModel.clearOperationFeedback()
            }
        }
    }

    scheduleToDelete?.let { scheduleId ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Hapus Jadwal?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text("Jadwal hanya dapat dihapus jika belum mempunyai pemesanan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(scheduleId)
                        scheduleToDelete = null
                    }
                ) {
                    Text("Hapus", color = GowinRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Panel Admin GO-WIN",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = GowinDark
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        enabled = !viewModel.operationInProgress
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Keluar",
                            tint = GowinRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GowinLightGray)
            )
        },
        containerColor = GowinLightGray
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Header Stats Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminLightPrimaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = AdminLightPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Total Pemesanan Aktif",
                                style = MaterialTheme.typography.labelMedium,
                                color = AdminLightOnPrimaryContainer
                            )
                            Text(
                                text = "${viewModel.bookings.count { BookingStatus.isActive(it.status) }} Tiket",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = AdminLightPrimary
                            )
                        }
                    }
                }
            }

            item {
                // Add Schedule Form
                ScheduleForm(
                    from = from,
                    to = to,
                    time = time,
                    price = price,
                    enabled = !viewModel.operationInProgress,
                    onFromChange = {
                        from = it
                        viewModel.clearOperationFeedback()
                    },
                    onToChange = {
                        to = it
                        viewModel.clearOperationFeedback()
                    },
                    onTimeChange = {
                        time = it
                        viewModel.clearOperationFeedback()
                    },
                    onPriceChange = {
                        price = it.filter(Char::isDigit)
                        viewModel.clearOperationFeedback()
                    },
                    onSave = {
                        viewModel.addSchedule(from, to, time, price) {
                            from = ""
                            to = ""
                            time = ""
                            price = ""
                        }
                    }
                )

                viewModel.operationError?.let {
                    Text(
                        text = it,
                        color = GowinRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                viewModel.operationMessage?.let {
                    Text(
                        text = it,
                        color = GowinGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verifikasi Transfer Manual",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = GowinDark
                )
                Text(
                    text = "${manualTransfers.size} transaksi menunggu tindakan",
                    style = MaterialTheme.typography.bodySmall,
                    color = GowinGray
                )
            }

            if (!viewModel.dataLoading && manualTransfers.isEmpty()) {
                item {
                    Text(
                        text = "Tidak ada transfer yang menunggu verifikasi.",
                        color = GowinGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(
                    items = manualTransfers,
                    key = { "manual-${it.id}" }
                ) { booking ->
                    ManualTransferCard(
                        booking = booking,
                        enabled = !viewModel.operationInProgress,
                        onApprove = { viewModel.approveManualTransfer(booking) },
                        onReject = { viewModel.rejectManualTransfer(booking) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daftar Perjalanan Aktif",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GowinDark
                )
            }

            when {
                viewModel.dataLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AdminLightPrimary)
                    }
                }

                viewModel.dataError != null -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.dataError.orEmpty(),
                            color = GowinRed
                        )
                        Button(
                            onClick = viewModel::observeData,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                viewModel.schedules.isEmpty() -> item {
                    Text(
                        text = "Belum ada jadwal aktif.",
                        color = GowinGray,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }

                else -> items(
                    items = viewModel.schedules,
                    key = Schedule::id
                ) { schedule ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${schedule.from} → ${schedule.to}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = GowinDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Jam: ${schedule.time}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GowinGray
                                )
                                Text(
                                    text = "Rp %,d".format(schedule.price).replace(',', '.'),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AdminLightPrimary
                                )
                            }
                            IconButton(
                                onClick = { scheduleToDelete = schedule.id },
                                enabled = !viewModel.operationInProgress
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = GowinRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualTransferCard(
    booking: Booking,
    enabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = booking.userName.ifBlank { booking.userEmail.ifBlank { "—" } },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GowinDark
            )
            Text(
                text = "${booking.routeFrom} → ${booking.routeTo} • Kursi ${booking.seatNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = GowinGray
            )
            Text(
                text = "Rp%,d".format(booking.totalAmount).replace(',', '.'),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AdminLightPrimary
            )
            Text(
                text = "Referensi: ${booking.paymentReference.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = GowinGray
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GowinRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Tolak")
                }
                Button(
                    onClick = onApprove,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GowinGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Setujui")
                }
            }
        }
    }
}

@Composable
private fun ScheduleForm(
    from: String,
    to: String,
    time: String,
    price: String,
    enabled: Boolean,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AdminLightPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tambah Jadwal Baru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GowinDark
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = from,
                onValueChange = onFromChange,
                label = { Text("Kota Asal") },
                placeholder = { Text("Bandung") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = to,
                onValueChange = onToChange,
                label = { Text("Kota Tujuan") },
                placeholder = { Text("Garut") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = time,
                onValueChange = onTimeChange,
                label = { Text("Jam Keberangkatan") },
                placeholder = { Text("09:00") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("Harga (Rp)") },
                placeholder = { Text("75000") },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSave,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminLightPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (enabled) "SIMPAN JADWAL" else "MENYIMPAN...",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
