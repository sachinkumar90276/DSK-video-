package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRegistrationFormValidator
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

private enum class AdminPortalTab { LOGIN, FORGOT_PASSWORD, ACTIVATE }

@Composable
fun AdminLoginScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onAdminSignIn: (email: String, password: String) -> Unit,
    onSendReset: (email: String) -> Unit,
    onActivateWithSecret: (fullName: String, email: String, password: String, secretCode: String) -> Unit,
    onOpenAdminRegister: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AdminPortalTab.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var secretCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }
    var resetFeedback by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DskPitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .minimumInteractiveComponentSize()
                .testTag("admin_login_back")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DskWhite)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 56.dp)
                .widthIn(max = 500.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = DskNeonViolet,
                            modifier = Modifier.size(34.dp)
                        )
                        Column {
                            Text("Admin Portal", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Server-verified admin_roles access only",
                                style = MaterialTheme.typography.labelMedium,
                                color = DskMuted
                            )
                        }
                    }

                    TabRow(
                        selectedTabIndex = activeTab.ordinal,
                        containerColor = DskObsidian,
                        contentColor = DskWhite
                    ) {
                        Tab(
                            selected = activeTab == AdminPortalTab.LOGIN,
                            onClick = {
                                activeTab = AdminPortalTab.LOGIN
                                resetFeedback = null
                                onClearError()
                            },
                            text = { Text("Login") },
                            modifier = Modifier.testTag("admin_tab_login")
                        )
                        Tab(
                            selected = activeTab == AdminPortalTab.FORGOT_PASSWORD,
                            onClick = {
                                activeTab = AdminPortalTab.FORGOT_PASSWORD
                                resetFeedback = null
                                onClearError()
                            },
                            text = { Text("Reset") },
                            modifier = Modifier.testTag("admin_tab_forgot_password")
                        )
                        Tab(
                            selected = activeTab == AdminPortalTab.ACTIVATE,
                            onClick = {
                                activeTab = AdminPortalTab.ACTIVATE
                                resetFeedback = null
                                onClearError()
                            },
                            text = { Text("Activate") },
                            modifier = Modifier.testTag("admin_tab_activate")
                        )
                    }

                    if (activeTab == AdminPortalTab.ACTIVATE) {
                        Text(
                            "Already verified your email? Sign in with your Admin Registration Secret Code to activate server-side Admin privileges.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DskSilver
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_activate_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Admin Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_email_input")
                    )

                    if (activeTab != AdminPortalTab.FORGOT_PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Admin Password") },
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
                                .testTag("admin_password_input")
                        )
                    }

                    if (activeTab == AdminPortalTab.ACTIVATE) {
                        OutlinedTextField(
                            value = secretCode,
                            onValueChange = { secretCode = it },
                            label = { Text("Admin Registration Secret Code") },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { secretVisible = !secretVisible }) {
                                    Icon(
                                        imageVector = if (secretVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle Secret Code Visibility"
                                    )
                                }
                            },
                            visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_activate_secret_input")
                        )
                    }

                    if (activeTab == AdminPortalTab.LOGIN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    activeTab = AdminPortalTab.FORGOT_PASSWORD
                                    onClearError()
                                },
                                modifier = Modifier.testTag("admin_forgot_password_button")
                            ) {
                                Text("Forgot Password?", color = DskNeonViolet)
                            }
                        }
                    }

                    if (!resetFeedback.isNullOrBlank()) {
                        Surface(
                            color = DskSuccess.copy(alpha = 0.16f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = resetFeedback!!,
                                color = DskSuccess,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
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
                                AdminPortalTab.LOGIN -> onAdminSignIn(email, password)
                                AdminPortalTab.FORGOT_PASSWORD -> {
                                    onSendReset(email)
                                    if (email.isNotBlank()) {
                                        resetFeedback = "If an authorized account exists for ${email.trim()}, reset instructions have been sent."
                                    }
                                }
                                AdminPortalTab.ACTIVATE -> onActivateWithSecret(fullName, email, password, secretCode)
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_login_submit")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DskWhite, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = when (activeTab) {
                                    AdminPortalTab.LOGIN -> "Sign In as Administrator"
                                    AdminPortalTab.FORGOT_PASSWORD -> "Send Admin Reset Link"
                                    AdminPortalTab.ACTIVATE -> "Verify Secret & Activate Admin"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    onClearError()
                    onOpenAdminRegister()
                },
                border = DskGradients.GlassBorderStroke,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("open_admin_register_button")
            ) {
                Icon(Icons.Filled.HowToReg, contentDescription = null, tint = DskRubyGlow)
                Spacer(Modifier.width(8.dp))
                Text("Authorized Admin Registration", color = DskWhite, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AdminRegisterScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onRegisterAdmin: (
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        secretCode: String,
        onVerificationPending: (String) -> Unit
    ) -> Unit,
    onOpenAdminLogin: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var secretCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var secretVisible by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    var verificationMessage by remember { mutableStateOf<String?>(null) }
    var showFirstAdminGuide by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DskPitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .minimumInteractiveComponentSize()
                .testTag("admin_register_back")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DskWhite)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 56.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (verificationMessage != null) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.MarkEmailRead,
                            contentDescription = "Email Verification Required",
                            tint = DskNeonViolet,
                            modifier = Modifier.size(38.dp)
                        )
                        Text("Confirm Email & Activate Admin", style = MaterialTheme.typography.headlineSmall)
                        Text(verificationMessage!!, style = MaterialTheme.typography.bodyMedium, color = DskSilver)
                        Button(
                            onClick = onOpenAdminLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("admin_reg_go_to_login")
                        ) {
                            Text("Continue to Admin Login", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = DskRubyGlow,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text("Admin Registration", style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    "Authorized personnel with server-issued Secret Code only",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DskMuted
                                )
                            }
                        }

                        Surface(
                            color = DskRoyalPurple.copy(alpha = 0.16f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = DskNeonViolet, modifier = Modifier.size(18.dp))
                                Text(
                                    "Privileges are assigned strictly server-side in admin_roles after bcrypt secret verification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DskSilver
                                )
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; localValidationError = null; onClearError() },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_reg_full_name_input")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; localValidationError = null; onClearError() },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_reg_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; localValidationError = null; onClearError() },
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
                                .testTag("admin_reg_password_input")
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; localValidationError = null; onClearError() },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle Confirm Password Visibility"
                                    )
                                }
                            },
                            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_reg_confirm_password_input")
                        )

                        OutlinedTextField(
                            value = secretCode,
                            onValueChange = { secretCode = it; localValidationError = null; onClearError() },
                            label = { Text("Admin Registration Secret Code") },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { secretVisible = !secretVisible }) {
                                    Icon(
                                        imageVector = if (secretVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle Secret Code Visibility"
                                    )
                                }
                            },
                            visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_reg_secret_code_input")
                        )

                        val displayedError = localValidationError ?: errorMessage
                        if (!displayedError.isNullOrBlank()) {
                            Surface(
                                color = DskError.copy(alpha = 0.16f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = displayedError,
                                    color = DskError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val validationErr = AdminRegistrationFormValidator.validate(
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    confirmPassword = confirmPassword,
                                    secretCode = secretCode
                                )
                                if (validationErr != null) {
                                    localValidationError = validationErr
                                } else {
                                    onRegisterAdmin(
                                        fullName,
                                        email,
                                        password,
                                        confirmPassword,
                                        secretCode
                                    ) { pendingMsg ->
                                        verificationMessage = pendingMsg
                                    }
                                }
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_register_submit")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DskWhite, strokeWidth = 2.dp)
                            } else {
                                Text("Register & Verify Admin Access", fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = {
                                onClearError()
                                onOpenAdminLogin()
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("admin_reg_back_to_login")
                        ) {
                            Text("Already an Admin? Sign In", color = DskNeonViolet)
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showFirstAdminGuide = !showFirstAdminGuide },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("first_admin_setup_guide_toggle")
                    ) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, tint = DskSilver)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (showFirstAdminGuide) "Hide First-Admin Server Setup Guide" else "First-Admin Server Setup Process",
                            color = DskSilver
                        )
                    }
                    AnimatedVisibility(visible = showFirstAdminGuide) {
                        Text(
                            "1. Run supabase_schema.sql in Supabase SQL Editor.\n" +
                                "2. Insert a bcrypt-hashed invite secret into public.admin_registration_invites (or insert the first super_admin row into public.admin_roles server-side).\n" +
                                "3. Passwords are handled exclusively by Supabase Auth and Service Role Keys are never bundled in the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DskMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAccessDeniedScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DskPitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .testTag("admin_access_denied_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.GppBad, contentDescription = "Unauthorized", tint = DskError, modifier = Modifier.size(48.dp))
                Text("Admin Access Required", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Standard user accounts cannot access protected DSK VIDEO Admin routes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DskSilver
                )
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_access_denied_return")
                ) {
                    Text("Return")
                }
            }
        }
    }
}
