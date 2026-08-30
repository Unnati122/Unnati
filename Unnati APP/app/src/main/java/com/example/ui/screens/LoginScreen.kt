package com.example.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.auth.BiometricAuthManager
import com.example.auth.BiometricStatus
import com.example.ui.theme.UxBorder
import com.example.ui.theme.UxErrorRed
import com.example.ui.theme.UxLightSurface
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeBorder
import com.example.ui.theme.UxOrangeDark
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxOrangeLighter
import com.example.ui.theme.UxPrimaryText
import com.example.ui.theme.UxSecondaryText
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxSurfaceContainer
import com.example.ui.theme.UxSurfaceLowest
import com.example.ui.theme.UxWhite
import com.example.viewmodel.TimeAgentViewModel

private fun Context.findActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun LoginScreen(
    viewModel: TimeAgentViewModel,
    onLoginSuccess: () -> Unit,
    onBackToLanding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var workerIdInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoNotice by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isBiometricAuthenticating by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val biometricStatus = remember(context) { BiometricAuthManager.getBiometricStatus(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Proceed with login anyway
        viewModel.login(
            workerId = workerIdInput,
            pin = pinInput,
            lat = null,
            lon = null,
            onSuccess = {
                context.getSharedPreferences("unnati_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("first_login_done", true).apply()
                isLoading = false
                onLoginSuccess()
            },
            onError = { msg ->
                isLoading = false
                errorMessage = msg
            }
        )
    }

    fun performLogin() {
        focusManager.clearFocus()
        errorMessage = null
        infoNotice = null
        isLoading = true

        viewModel.login(
            workerId = workerIdInput,
            pin = pinInput,
            lat = null,
            lon = null,
            onSuccess = {
                context.getSharedPreferences("unnati_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("first_login_done", true).apply()
                isLoading = false
                onLoginSuccess()
            },
            onError = { msg ->
                isLoading = false
                errorMessage = msg
            }
        )
    }

    fun triggerBiometricAuth() {
        focusManager.clearFocus()
        errorMessage = null
        infoNotice = null

        val prefs = context.getSharedPreferences("unnati_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("first_login_done", false)) {
            infoNotice = "Please sign in with your PIN for your first login to enable biometrics."
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to launch biometrics on this window. Please use PIN."
            return
        }

        when (biometricStatus) {
            BiometricStatus.NOT_ENROLLED -> {
                infoNotice = "No biometrics registered on this device. Please use PIN or register in Android Settings."
                return
            }
            BiometricStatus.NO_HARDWARE, BiometricStatus.HARDWARE_UNAVAILABLE -> {
                infoNotice = "Biometric sensor unavailable. Please sign in with your PIN."
                return
            }
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> {
                infoNotice = "Biometric security update required. Please sign in with your PIN."
                return
            }
            BiometricStatus.AVAILABLE, BiometricStatus.UNSUPPORTED -> {
                // Proceed with authentication
            }
        }

        isBiometricAuthenticating = true
        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Unnati Biometric Login",
            subtitle = "Scan fingerprint or face for $workerIdInput",
            description = "Quickly verify your operator identity for immediate field access",
            negativeButtonText = "Use PIN Instead",
            onSuccess = {
                isBiometricAuthenticating = false
                viewModel.loginWithBiometrics(
                    workerId = workerIdInput,
                    onSuccess = {
                        onLoginSuccess()
                    },
                    onError = { err ->
                        errorMessage = err
                    }
                )
            },
            onError = { errorCode, errString ->
                isBiometricAuthenticating = false
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    errorMessage = errString.toString()
                }
            },
            onFailed = {
                isBiometricAuthenticating = false
                errorMessage = "Biometric recognition unverified. Please try again or enter your PIN."
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UxWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Nav / Back
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToLanding,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to landing",
                    tint = UxPrimaryText
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UxOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = UxOrange,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "UNNATI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = UxPrimaryText,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Login Form
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Security emblem with Biometric badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(UxOrangeLight)
                        .border(1.dp, UxOrangeBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security login",
                        tint = UxOrange,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometrics Ready",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Time Agent",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = UxPrimaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sign in to submit your field progress update.",
                fontSize = 13.5.sp,
                color = UxSecondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))



            // 1. Biometric Fast Unlock Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = UxOrange),
                        onClick = { triggerBiometricAuth() }
                    )
                    .testTag("biometric_login_button"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF7F2),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFD4BE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(UxOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBiometricAuthenticating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric scan",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Biometric Fast Unlock",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "FAST PASS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tap to verify with Fingerprint or Face ID",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider: OR SIGN IN WITH PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text(
                    text = "OR SIGN IN WITH PIN",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Worker ID Input Field
            OutlinedTextField(
                value = workerIdInput,
                onValueChange = {
                    workerIdInput = it
                    errorMessage = null
                    infoNotice = null
                },
                label = { Text("Worker ID") },
                placeholder = { Text("e.g. WK-10245") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Worker ID",
                        tint = UxOrange
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("worker_id_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UxOrange,
                    unfocusedBorderColor = UxBorder,
                    focusedLabelColor = UxOrange,
                    cursorColor = UxOrange,
                    focusedContainerColor = UxWhite,
                    unfocusedContainerColor = UxLightSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // PIN / Password Input Field
            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    pinInput = it
                    errorMessage = null
                    infoNotice = null
                },
                label = { Text("PIN") },
                placeholder = { Text("4-digit security PIN") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN",
                        tint = UxOrange
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { pinVisible = !pinVisible }) {
                        Icon(
                            imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (pinVisible) "Hide PIN" else "Show PIN",
                            tint = UxSecondaryText
                        )
                    }
                },
                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UxOrange,
                    unfocusedBorderColor = UxBorder,
                    focusedLabelColor = UxOrange,
                    cursorColor = UxOrange,
                    focusedContainerColor = UxWhite,
                    unfocusedContainerColor = UxLightSurface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { performLogin() })
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = UxErrorRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (infoNotice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = infoNotice ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFFD97706),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sign In Button
            Button(
                onClick = { performLogin() },
                enabled = !isLoading && workerIdInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_in_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UxOrange,
                    contentColor = UxWhite
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = UxWhite,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In with PIN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom Restricted Access Disclaimer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "UX4G Biometric Security Standard",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Authorized personnel only • Fingerprint & Face Unlock enabled",
                fontSize = 10.5.sp,
                color = UxSecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
