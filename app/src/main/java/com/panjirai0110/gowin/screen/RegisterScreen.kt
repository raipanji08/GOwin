package com.panjirai0110.gowin.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Keep system back in sync with the disabled app-bar action while the
    // account creation request is in flight.
    BackHandler(enabled = viewModel.authLoading) { }

    val submitRegister = {
        focusManager.clearFocus()
        if (password != confirmPassword) {
            viewModel.reportAuthError("Password tidak cocok")
        } else {
            viewModel.register(email, password, onRegisterSuccess)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    enabled = !viewModel.authLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Kembali",
                        modifier = Modifier.size(28.dp),
                        tint = GowinDark
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Buat Akun Baru \uD83D\uDCDD",
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = GowinDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Daftar untuk memulai perjalanan",
                color = GowinGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            AuthFieldLabel(text = "Email")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearAuthFeedback()
                },
                placeholder = {
                    Text("Masukkan email", color = GowinGray, fontSize = 14.sp)
                },
                singleLine = true,
                enabled = !viewModel.authLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthFieldLabel(text = "Password")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearAuthFeedback()
                },
                placeholder = {
                    Text("Masukkan password", color = GowinGray, fontSize = 14.sp)
                },
                singleLine = true,
                enabled = !viewModel.authLoading,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !viewModel.authLoading
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Sembunyikan password"
                            } else {
                                "Tampilkan password"
                            },
                            modifier = Modifier.size(20.dp),
                            tint = GowinGray
                        )
                    }
                },
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthFieldLabel(text = "Konfirmasi Password")
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    viewModel.clearAuthFeedback()
                },
                placeholder = {
                    Text("Masukkan ulang password", color = GowinGray, fontSize = 14.sp)
                },
                singleLine = true,
                enabled = !viewModel.authLoading,
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!viewModel.authLoading) submitRegister()
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            confirmPasswordVisible = !confirmPasswordVisible
                        },
                        enabled = !viewModel.authLoading
                    ) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (confirmPasswordVisible) {
                                "Sembunyikan konfirmasi password"
                            } else {
                                "Tampilkan konfirmasi password"
                            },
                            modifier = Modifier.size(20.dp),
                            tint = GowinGray
                        )
                    }
                },
                colors = authTextFieldColors()
            )

            Spacer(modifier = Modifier.height(18.dp))

            AuthFeedback(
                error = viewModel.authError,
                message = viewModel.authMessage
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = submitRegister,
                enabled = !viewModel.authLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GowinBlue,
                    contentColor = Color.White
                )
            ) {
                if (viewModel.authLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "DAFTAR",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            HorizontalDivider(color = com.panjirai0110.shared.ui.theme.GowinBorder)

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sudah punya akun? ",
                    color = GowinGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "Masuk",
                    color = GowinGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        enabled = !viewModel.authLoading
                    ) {
                        onBack()
                    }
                )
            }
        }
    }
}
