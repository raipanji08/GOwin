package com.panjirai0110.gowin.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.gowin.ui.component.GowinActionCard
import com.panjirai0110.gowin.ui.component.GowinPrimaryActionButton
import com.panjirai0110.shared.model.DEFAULT_ADMIN_FEE
import com.panjirai0110.shared.model.PaymentMethod
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinRed

private data class PaymentOption(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val usesMidtrans: Boolean
)

private val paymentOptions = listOf(
    PaymentOption(
        key = PaymentMethod.MidtransQris,
        title = "QRIS",
        icon = Icons.Default.QrCode2,
        usesMidtrans = true
    ),
    PaymentOption(
        key = PaymentMethod.ManualTransfer,
        title = "Transfer Bank",
        icon = Icons.Default.AccountBalance,
        usesMidtrans = false
    ),
    PaymentOption(
        key = PaymentMethod.MidtransEWallet,
        title = "E-Wallet",
        icon = Icons.Default.Wallet,
        usesMidtrans = true
    ),
    PaymentOption(
        key = PaymentMethod.MidtransVirtualAccount,
        title = "Virtual Account",
        icon = Icons.Default.CreditCard,
        usesMidtrans = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    scheduleId: String,
    seatNumber: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onBookingReady: (bookingId: String, usesMidtrans: Boolean) -> Unit
) {
    val schedule = viewModel.getScheduleById(scheduleId)
    val total = (schedule?.price ?: 0) + DEFAULT_ADMIN_FEE
    var selectedMethod by rememberSaveable {
        mutableStateOf(PaymentMethod.MidtransQris)
    }
    var manualConfirmed by rememberSaveable { mutableStateOf(false) }
    val selectedOption = paymentOptions.first { it.key == selectedMethod }

    BackHandler(enabled = viewModel.bookingInProgress) { }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Pilih Metode Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GowinDark
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !viewModel.bookingInProgress
                    ) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            GowinActionCard(modifier = Modifier.fillMaxWidth()) {
                paymentOptions.forEachIndexed { index, option ->
                    PaymentMethodRow(
                        option = option,
                        selected = option.key == selectedMethod,
                        enabled = !viewModel.bookingInProgress,
                        onSelect = {
                            selectedMethod = option.key
                            manualConfirmed = false
                            viewModel.clearPaymentError()
                        }
                    )

                    if (index < paymentOptions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            thickness = 1.dp,
                            color = GowinBorder
                        )
                    }
                }
            }

            if (!selectedOption.usesMidtrans) {
                Spacer(modifier = Modifier.height(12.dp))

                GowinActionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Konfirmasi Transfer",
                            color = GowinDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Pastikan transfer bank telah dilakukan sebelum melanjutkan.",
                            color = GowinGray,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !viewModel.bookingInProgress
                                ) {
                                    manualConfirmed = !manualConfirmed
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = manualConfirmed,
                                enabled = !viewModel.bookingInProgress,
                                onCheckedChange = { manualConfirmed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GowinBlue,
                                    uncheckedColor = GowinGray
                                )
                            )
                            Text(
                                text = "Saya sudah melakukan transfer",
                                style = MaterialTheme.typography.bodySmall,
                                color = GowinDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            viewModel.paymentError?.let {
                Text(
                    text = it,
                    color = GowinRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            GowinActionCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Total Pembayaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GowinDark,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatRupiah(total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GowinGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GowinPrimaryActionButton(
                        text = "BAYAR SEKARANG",
                        loading = viewModel.bookingInProgress,
                        loadingText = "MEMBUAT PESANAN...",
                        enabled = !viewModel.bookingInProgress &&
                            (selectedOption.usesMidtrans || manualConfirmed),
                        onClick = {
                            if (selectedOption.usesMidtrans) {
                                viewModel.startMidtransPayment(
                                    scheduleId = scheduleId,
                                    seatNumber = seatNumber,
                                    paymentMethod = selectedMethod
                                ) { checkout ->
                                    onBookingReady(checkout.bookingId, true)
                                }
                            } else {
                                viewModel.createManualTransferBooking(
                                    scheduleId = scheduleId,
                                    seatNumber = seatNumber
                                ) { booking ->
                                    onBookingReady(booking.id, false)
                                }
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PaymentMethodRow(
    option: PaymentOption,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = GowinGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = option.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = GowinDark,
            fontWeight = FontWeight.Medium
        )
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = GowinBlue,
                unselectedColor = GowinGray
            )
        )
    }
}
