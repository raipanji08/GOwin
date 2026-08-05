package com.panjirai0110.gowin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panjirai0110.gowin.components.GowinProfileAvatar
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinRed
import kotlinx.coroutines.delay

private enum class ProfileDialog {
    None,
    Payment,
    Help,
    About
}

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    userName: String,
    userEmail: String,
    onEditProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    var dialog by remember { mutableStateOf(ProfileDialog.None) }
    val profile = viewModel.userProfile
    val displayName = profile?.name?.takeIf(String::isNotBlank)
        ?: userName.ifBlank { "Profil" }
    val displayEmail = profile?.email?.takeIf(String::isNotBlank)
        ?: userEmail.ifBlank { "Email belum tersedia" }
    val displayPhone = profile?.phone?.takeIf(String::isNotBlank)
        ?: "Nomor ponsel belum ditambahkan"
    val profileMessage = viewModel.profileMessage

    LaunchedEffect(profileMessage) {
        if (profileMessage != null) {
            delay(3_500)
            if (viewModel.profileMessage == profileMessage) {
                viewModel.dismissProfileMessage()
            }
        }
    }

    when (dialog) {
        ProfileDialog.Payment -> {
            InformationDialog(
                title = "Metode Pembayaran",
                body = "GO-WIN mendukung QRIS, E-Wallet, Virtual Account, dan transfer bank. Pembayaran transfer bank akan diverifikasi oleh admin.",
                onDismiss = { dialog = ProfileDialog.None }
            )
        }

        ProfileDialog.Help -> {
            InformationDialog(
                title = "Bantuan & FAQ",
                body = "Pilih jadwal, kursi, lalu metode pembayaran. Tiket terbit setelah pembayaran berstatus berhasil. Untuk transfer manual, tunggu verifikasi admin. Jika kursi tidak tersedia, pilih kursi lain.",
                onDismiss = { dialog = ProfileDialog.None }
            )
        }

        ProfileDialog.About -> {
            InformationDialog(
                title = "Tentang GO-WIN",
                body = "GO-WIN adalah aplikasi pemesanan travel Bandung–Garut dengan pemilihan kursi real-time, pembayaran aman, dan tiket digital.",
                onDismiss = { dialog = ProfileDialog.None }
            )
        }

        ProfileDialog.None -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            GowinBlue,
                            Color(0xFF3470ED),
                            Color(0xFF4B80F0)
                        )
                    )
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GowinProfileAvatar(
                    gender = profile?.gender.orEmpty(),
                    modifier = Modifier.size(104.dp)
                )
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        displayPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        displayEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            ProfileMenuItem(
                icon = Icons.Outlined.Edit,
                text = "Edit Profil",
                onClick = onEditProfile
            )
            ProfileDivider()
            ProfileMenuItem(
                icon = Icons.Outlined.History,
                text = "Riwayat Perjalanan",
                onClick = onNavigateToHistory
            )
            ProfileDivider()
            ProfileMenuItem(
                icon = Icons.Outlined.AccountBalanceWallet,
                text = "Metode Pembayaran",
                onClick = { dialog = ProfileDialog.Payment }
            )
            ProfileDivider()
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                text = "Bantuan & FAQ",
                onClick = { dialog = ProfileDialog.Help }
            )
            ProfileDivider()
            ProfileMenuItem(
                icon = Icons.Outlined.Info,
                text = "Tentang Aplikasi",
                onClick = { dialog = ProfileDialog.About }
            )
            ProfileDivider()
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                text = "Keluar",
                contentColor = GowinRed,
                showArrow = false,
                onClick = onSignOut
            )
        }

        profileMessage?.let {
            Text(
                text = it,
                color = GowinGreen,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clickable { viewModel.dismissProfileMessage() }
                    .padding(16.dp)
            )
        }
        if (viewModel.profileLoading) {
            CircularProgressIndicator(
                color = GowinBlue,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(20.dp)
            )
        }
    }
}

@Composable
private fun InformationDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body, color = GowinGray) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GowinBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Mengerti")
            }
        }
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    contentColor: Color = GowinDark,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GowinGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = GowinBorder
    )
}
