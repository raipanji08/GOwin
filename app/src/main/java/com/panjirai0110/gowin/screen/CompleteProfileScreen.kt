package com.panjirai0110.gowin.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panjirai0110.gowin.components.GowinProfileAvatar
import com.panjirai0110.gowin.ui.component.GowinActionCard
import com.panjirai0110.gowin.ui.component.GowinPrimaryActionButton
import com.panjirai0110.gowin.ui.component.GowinSecondaryActionButton
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.UserGender
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    viewModel: MainViewModel,
    isOnboarding: Boolean,
    onBack: () -> Unit,
    onFinished: () -> Unit
) {
    val profile = viewModel.userProfile
    var name by rememberSaveable(profile?.userId) {
        mutableStateOf(profile?.name.orEmpty())
    }
    var phone by rememberSaveable(profile?.userId) {
        mutableStateOf(profile?.phone.orEmpty())
    }
    var gender by rememberSaveable(profile?.userId) {
        mutableStateOf(profile?.gender.orEmpty())
    }
    val email = profile?.email?.takeIf(String::isNotBlank)
        ?: viewModel.currentUserEmail.orEmpty()

    BackHandler(enabled = isOnboarding || viewModel.profileSaving) {
        if (!viewModel.profileSaving) {
            viewModel.skipProfileOnboarding(onFinished)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isOnboarding) "Lengkapi Profil" else "Edit Profil",
                        style = MaterialTheme.typography.titleMedium,
                        color = GowinDark,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (!isOnboarding) {
                        IconButton(
                            onClick = onBack,
                            enabled = !viewModel.profileSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Kembali",
                                tint = GowinDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isOnboarding) {
                        TextButton(
                            onClick = { viewModel.skipProfileOnboarding(onFinished) },
                            enabled = !viewModel.profileSaving
                        ) {
                            Text(
                                text = "Lewati",
                                color = GowinBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GowinProfileAvatar(
                gender = gender,
                modifier = Modifier.size(112.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isOnboarding) {
                    "Biar perjalanan makin personal"
                } else {
                    "Perbarui informasi profil Anda"
                },
                color = GowinDark,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = if (isOnboarding) {
                    "Isi data berikut atau lewati dulu. Anda dapat mengubahnya kapan saja."
                } else {
                    "Data ini digunakan untuk menampilkan identitas perjalanan Anda."
                },
                color = GowinGray,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            GowinActionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AuthFieldLabel(text = "Nama lengkap")
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it.take(80)
                            viewModel.clearProfileFeedback()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        placeholder = { Text("Masukkan nama lengkap") },
                        singleLine = true,
                        enabled = !viewModel.profileSaving,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = authTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthFieldLabel(text = "Nomor ponsel")
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it.filter(Char::isDigit).take(13)
                            viewModel.clearProfileFeedback()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        placeholder = { Text("08xxxxxxxxxx") },
                        enabled = !viewModel.profileSaving,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = authTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthFieldLabel(text = "Email akun")
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        readOnly = true,
                        placeholder = { Text("Email akun") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = authTextFieldColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GowinActionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pilih avatar profil",
                        color = GowinDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Avatar dibuat otomatis dari pilihan gender dan dapat diubah nanti.",
                        color = GowinGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GenderAvatarOption(
                            label = "Laki-laki",
                            gender = UserGender.Male,
                            selected = gender == UserGender.Male,
                            onClick = {
                                gender = UserGender.Male
                                viewModel.clearProfileFeedback()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GenderAvatarOption(
                            label = "Perempuan",
                            gender = UserGender.Female,
                            selected = gender == UserGender.Female,
                            onClick = {
                                gender = UserGender.Female
                                viewModel.clearProfileFeedback()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            viewModel.profileError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = GowinRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            GowinPrimaryActionButton(
                text = if (isOnboarding) "SIMPAN PROFIL" else "SIMPAN PERUBAHAN",
                onClick = {
                    viewModel.saveProfile(
                        name = name,
                        phone = phone,
                        gender = gender,
                        requirePhone = isOnboarding,
                        completeOnboarding = isOnboarding,
                        onSuccess = onFinished
                    )
                },
                enabled = !viewModel.profileSaving
            )
            if (isOnboarding) {
                Spacer(modifier = Modifier.height(10.dp))
                GowinSecondaryActionButton(
                    text = "LEWATI UNTUK SEKARANG",
                    onClick = { viewModel.skipProfileOnboarding(onFinished) },
                    enabled = !viewModel.profileSaving
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GenderAvatarOption(
    label: String,
    gender: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFF4F8FF) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (selected) GowinBlue else GowinBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GowinProfileAvatar(gender = gender, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = if (selected) GowinBlue else GowinDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
