package com.panjirai0110.gowin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panjirai0110.gowin.R
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray

private val seatRows = listOf(
    "A1" to "A2",
    "B1" to "B2",
    "B3" to "B4",
    "C1" to "C2",
    "C3" to "C4",
    "D1" to "D2"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    scheduleId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var selectedSeat by rememberSaveable { mutableStateOf<String?>(null) }
    val schedule = viewModel.getScheduleById(scheduleId)

    DisposableEffect(scheduleId, viewModel.selectedTravelDate) {
        viewModel.observeSeats(scheduleId, viewModel.selectedTravelDate)
        onDispose(viewModel::stopObservingSeats)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Pilih Kursi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = GowinDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ChevronLeft,
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
        ) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = schedule?.vehicleName ?: "Hiace Premio",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = GowinDark
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(Color.White, GowinBorder, "Kosong")
                LegendItem(Color(0xFF1F2937), Color.Transparent, "Terisi")
                LegendItem(GowinBlue, Color.Transparent, "Terpilih")
            }
            Spacer(Modifier.height(10.dp))

            when {
                viewModel.seatsLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GowinBlue)
                    }
                }

                viewModel.seatsError != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            viewModel.seatsError.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.gowin_vehicle_cabin),
                            contentDescription = "Ilustrasi interior kendaraan",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 42.dp,
                                    end = 42.dp,
                                    top = 22.dp,
                                    bottom = 18.dp
                                ),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = "Driver",
                                modifier = Modifier.align(Alignment.End),
                                style = MaterialTheme.typography.labelSmall,
                                color = GowinGray
                            )
                            seatRows.forEach { (left, right) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SeatButton(
                                        name = left,
                                        isBooked = left in viewModel.bookedSeats,
                                        isSelected = selectedSeat == left,
                                        onClick = {
                                            selectedSeat =
                                                if (selectedSeat == left) null else left
                                        }
                                    )
                                    SeatButton(
                                        name = right,
                                        isBooked = right in viewModel.bookedSeats,
                                        isSelected = selectedSeat == right,
                                        onClick = {
                                            selectedSeat =
                                                if (selectedSeat == right) null else right
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, GowinBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 14.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kursi yang dipilih",
                            color = GowinGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = selectedSeat ?: "—",
                            color = GowinBlue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = GowinBorder
                    )
                    Button(
                        onClick = { selectedSeat?.let(onContinue) },
                        enabled = selectedSeat != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GowinBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("LANJUT", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun LegendItem(
    fillColor: Color,
    borderColor: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(16.dp),
            shape = RoundedCornerShape(4.dp),
            color = fillColor,
            border = BorderStroke(0.5.dp, borderColor)
        ) {}
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GowinDark
        )
    }
}

@Composable
private fun SeatButton(
    name: String,
    isBooked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fill = when {
        isBooked -> Color(0xFF1F2937)
        isSelected -> GowinBlue
        else -> Color.White
    }
    val content = if (isBooked || isSelected) Color.White else GowinDark

    Surface(
        modifier = Modifier
            .width(64.dp)
            .height(40.dp)
            .clickable(enabled = !isBooked, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = fill,
        border = if (!isBooked && !isSelected) {
            BorderStroke(0.5.dp, GowinBorder)
        } else {
            null
        },
        shadowElevation = if (!isBooked && !isSelected) 1.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}
