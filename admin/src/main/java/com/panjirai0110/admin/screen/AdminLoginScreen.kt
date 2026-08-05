package com.panjirai0110.admin.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.panjirai0110.admin.viewmodel.AdminViewModel
import com.panjirai0110.shared.ui.theme.*

@Composable
fun AdminLoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    BackHandler(enabled = viewModel.authLoading) { }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GowinLightGray)
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AdminLightPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛠️",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "GO-WIN Admin",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = GowinDark
            )

            Text(
                text = "Masuk untuk mengelola jadwal perjalanan",
                style = MaterialTheme.typography.bodyMedium,
                color = GowinGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearAuthError()
                },
                label = { Text("Email Admin") },
                placeholder = { Text("admin@gowin.id") },
                singleLine = true,
                enabled = !viewModel.authLoading,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearAuthError()
                },
                label = { Text("Kata Sandi") },
                placeholder = { Text("Masukkan kata sandi") },
                singleLine = true,
                enabled = !viewModel.authLoading,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            viewModel.authError?.let {
                Text(
                    text = it,
                    color = GowinRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login(email, password, onLoginSuccess) },
                enabled = !viewModel.authLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminLightPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (viewModel.authLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "MASUK SEBAGAI ADMIN",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
