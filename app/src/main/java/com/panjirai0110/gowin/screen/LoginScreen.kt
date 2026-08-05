package com.panjirai0110.gowin.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.panjirai0110.gowin.R
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val configuredGoogleClientId = stringResource(R.string.google_web_client_id)
    val googleServerClientId = configuredGoogleClientId.takeIf(String::isNotBlank)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var googleRequestInProgress by remember { mutableStateOf(false) }

    val busy = viewModel.authLoading || googleRequestInProgress
    // The top-app-bar action is disabled while a request is active; give the
    // system back gesture the same behaviour so a pending sign-in cannot
    // navigate away and later complete against an outdated screen.
    BackHandler(enabled = busy) { }
    val submitLogin = {
        focusManager.clearFocus()
        viewModel.signInWithEmail(email, password, onLoginSuccess)
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
                IconButton(onClick = onBack, enabled = !busy) {
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
                text = "Selamat Datang! \uD83D\uDC4B",
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = GowinDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Masuk untuk melanjutkan",
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
                enabled = !busy,
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
                enabled = !busy,
                visualTransformation = if (passwordVisible) {
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
                        if (!busy) submitLogin()
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !busy
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

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendPasswordReset(email)
                    },
                    enabled = !busy,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Lupa Password?",
                        color = GowinBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AuthFeedback(
                error = viewModel.authError,
                message = viewModel.authMessage
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = submitLogin,
                enabled = !busy,
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
                        text = "MASUK",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = GowinBorder
                )
                Text(
                    text = "atau",
                    color = GowinGray,
                    modifier = Modifier.padding(horizontal = 14.dp),
                    fontSize = 12.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = GowinBorder
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    val clientId = googleServerClientId ?: return@OutlinedButton
                    googleRequestInProgress = true
                    viewModel.clearAuthFeedback()
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        try {
                            val googleIdOption = GetSignInWithGoogleOption.Builder(
                                serverClientId = clientId
                            ).build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val response = credentialManager.getCredential(context, request)
                            val credential = response.credential
                            if (
                                credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                viewModel.signInWithGoogle(
                                    googleCredential.idToken,
                                    onLoginSuccess
                                )
                            } else {
                                viewModel.reportAuthError(
                                    "Kredensial Google tidak dapat dibaca."
                                )
                            }
                        } catch (_: NoCredentialException) {
                            viewModel.reportAuthError(
                                "Tidak ada akun Google yang tersedia di perangkat."
                            )
                        } catch (_: GetCredentialCancellationException) {
                            viewModel.reportAuthError("Google Login dibatalkan.")
                        } catch (_: GetCredentialException) {
                            viewModel.reportAuthError(
                                "Google Login gagal. Silakan coba lagi."
                            )
                        } catch (_: Exception) {
                            viewModel.reportAuthError(
                                "Respons Google tidak dapat diproses."
                            )
                        } finally {
                            googleRequestInProgress = false
                        }
                    }
                },
                enabled = !busy && googleServerClientId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GowinBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = GowinDark
                )
            ) {
                if (googleRequestInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = GowinBlue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Masuk dengan Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Belum punya akun? ",
                    color = GowinGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "Daftar Sekarang",
                    color = GowinGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !busy) { onRegister() }
                )
            }
        }
    }
}

@Composable
internal fun AuthFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = GowinDark
    )
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GowinDark,
    unfocusedTextColor = GowinDark,
    disabledTextColor = GowinGray,
    focusedBorderColor = GowinBlue,
    unfocusedBorderColor = GowinBorder,
    disabledBorderColor = GowinBorder,
    cursorColor = GowinBlue,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White
)

@Composable
internal fun AuthFeedback(
    error: String?,
    message: String?
) {
    error?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            textAlign = TextAlign.Start,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
    message?.let {
        Text(
            text = it,
            color = GowinBlue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            textAlign = TextAlign.Start,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
