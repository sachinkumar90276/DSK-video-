package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.supabase.SupabaseConfig
import com.example.ui.components.DskBrandMark
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthState

private enum class AuthTab { LOGIN, REGISTER, FORGOT_PASSWORD }

@Composable
fun AuthScreen(
    authState: AuthState,
    isBusy: Boolean,
    errorMessage: String?,
    onSignIn: (email: String, password: String, rememberSession: Boolean) -> Unit,
    onSignUp: (email: String, password: String, username: String) -> Unit,
    onSendReset: (email: String) -> Unit,
    onResendVerification: (email: String) -> Unit,
    onBackToSignIn: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onClearError: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var rememberSession by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DskPitchBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_splash_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DskBrandMark(compact = false)
            Text(
                text = "Entertainment Without Limits",
                style = MaterialTheme.typography.bodyLarge,
                color = DskSilver
            )

            if (!SupabaseConfig.isConfigured) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Backend Key Setup Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = DskWarning
                        )
                        Text(
                            text = "Connected Project: ${SupabaseConfig.baseUrl}\nAdd SUPABASE_ANON_KEY in the AI Studio Secrets panel to enable live authentication & streaming.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DskSilver
                        )
                    }
                }
            }

            if (authState is AuthState.VerificationPending) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailRead,
                            contentDescription = "Verify Email",
                            tint = DskNeonViolet,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Verify Your Email",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = authState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DskSilver
                        )
                        Button(
                            onClick = { onResendVerification(authState.email) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resend_verification_button")
                        ) {
                            Text("Resend Verification Email")
                        }
                        OutlinedButton(
                            onClick = onBackToSignIn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Sign In", color = DskWhite)
                        }
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = activeTab.ordinal,
                            containerColor = DskObsidian,
                            contentColor = DskWhite
                        ) {
                            Tab(
                                selected = activeTab == AuthTab.LOGIN,
                                onClick = { activeTab = AuthTab.LOGIN; onClearError() },
                                text = { Text("Sign In") },
                                modifier = Modifier.testTag("auth_tab_login")
                            )
                            Tab(
                                selected = activeTab == AuthTab.REGISTER,
                                onClick = { activeTab = AuthTab.REGISTER; onClearError() },
                                text = { Text("Register") },
                                modifier = Modifier.testTag("auth_tab_register")
                            )
                            Tab(
                                selected = activeTab == AuthTab.FORGOT_PASSWORD,
                                onClick = { activeTab = AuthTab.FORGOT_PASSWORD; onClearError() },
                                text = { Text("Reset") },
                                modifier = Modifier.testTag("auth_tab_reset")
                            )
                        }

                        if (activeTab == AuthTab.REGISTER) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Full Name / Username") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_input")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        if (activeTab != AuthTab.FORGOT_PASSWORD) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle Password Visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input")
                            )
                        }

                        if (activeTab == AuthTab.LOGIN) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = rememberSession,
                                        onCheckedChange = { rememberSession = it },
                                        modifier = Modifier.testTag("remember_session_checkbox")
                                    )
                                    Text("Remember Session", style = MaterialTheme.typography.bodyMedium)
                                }
                                TextButton(onClick = { activeTab = AuthTab.FORGOT_PASSWORD }) {
                                    Text("Forgot Password?", color = DskNeonViolet)
                                }
                            }
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Surface(
                                color = DskError.copy(alpha = 0.16f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = DskError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                when (activeTab) {
                                    AuthTab.LOGIN -> onSignIn(email, password, rememberSession)
                                    AuthTab.REGISTER -> onSignUp(email, password, username)
                                    AuthTab.FORGOT_PASSWORD -> onSendReset(email)
                                }
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = DskWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = when (activeTab) {
                                        AuthTab.LOGIN -> "Sign In to Watch"
                                        AuthTab.REGISTER -> "Create Free Account"
                                        AuthTab.FORGOT_PASSWORD -> "Send Reset Link"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenAdminLogin,
                border = DskGradients.GlassBorderStroke,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_admin_login_button")
            ) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = DskNeonViolet)
                Spacer(Modifier.width(8.dp))
                Text("Separate Admin Portal Login", color = DskWhite)
            }

            Text(
                text = "100% Free OTT Streaming • No Subscriptions • No Hidden Fees",
                style = MaterialTheme.typography.labelMedium,
                color = DskMuted
            )
        }
    }
}
