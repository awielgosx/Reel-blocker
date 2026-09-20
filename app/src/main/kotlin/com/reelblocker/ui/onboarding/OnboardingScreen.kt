package com.reelblocker.ui.onboarding

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.reelblocker.permissions.PermissionChecks
import com.reelblocker.security.PinManager
import com.reelblocker.security.ReelBlockerDeviceAdminReceiver

data class OnboardingStep(
    val title: String,
    val explanation: String,
    val isGranted: (android.content.Context) -> Boolean,
    val action: (android.content.Context) -> Intent?,
)

val onboardingSteps = listOf(
    OnboardingStep(
        title = "Usage access",
        explanation = "Lets the app see how long Instagram/Facebook/YouTube are on screen, so it can show your timer. Find \"Reel Blocker\" in the list and turn it on.",
        isGranted = { PermissionChecks.hasUsageAccess(it) },
        action = { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) },
    ),
    OnboardingStep(
        title = "Notifications",
        explanation = "Needed to show the always-visible session/day timer in the status bar.",
        isGranted = { PermissionChecks.hasNotificationPermission(it) },
        action = { null }, // handled via runtime permission launcher below
    ),
    OnboardingStep(
        title = "Battery optimization",
        explanation = "Exempts Reel Blocker so Android doesn't kill the timer/blocking service in the background.",
        isGranted = { PermissionChecks.isIgnoringBatteryOptimizations(it) },
        action = { Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${it.packageName}")) },
    ),
    OnboardingStep(
        title = "Accessibility service",
        explanation = "Lets Reel Blocker protect its own settings from being switched off by accident. Since this app isn't from the Play Store, Android may grey the toggle out at first — tap the app's name, then the ⋮ menu, then \"Allow restricted settings\", then come back here and turn it on.",
        isGranted = { PermissionChecks.isAccessibilityGuardEnabled(it) },
        action = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
    ),
    OnboardingStep(
        title = "Device admin",
        explanation = "The other half of protecting Reel Blocker from an impulsive uninstall.",
        isGranted = { PermissionChecks.isDeviceAdminActive(it) },
        action = {
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ReelBlockerDeviceAdminReceiver.componentName(it))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Protects Reel Blocker from being disabled without your PIN.",
                )
            }
        },
    ),
)

@Composable
fun OnboardingScreen(refreshTrigger: Int, onAllPermissionsGranted: () -> Unit, onPinConfigured: () -> Unit) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val allGranted = onboardingSteps.all { it.isGranted(context) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Set up Reel Blocker", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A few permissions are required for the timer and self-protection to work.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        com.reelblocker.ui.theme.ReelBlockerPanel(modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(onboardingSteps) { step ->
                val granted = step.isGranted(context)
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Column {
                        Text(step.title, style = MaterialTheme.typography.titleMedium)
                        Text(step.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                    if (granted) {
                        Icon(Icons.Filled.Check, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = {
                                if (step.title == "Notifications") {
                                    if (Build.VERSION.SDK_INT >= 33) {
                                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    step.action(context)?.let { context.startActivity(it) }
                                }
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        ) { Text("Grant") }
                    }
                }
            }

            item {
                if (allGranted) {
                    PinSetupSection(pinManager = pinManager, onDone = onPinConfigured)
                }
            }
        }
        }
    }
}

@Composable
private fun PinSetupSection(pinManager: PinManager, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Set your PIN", style = MaterialTheme.typography.titleMedium)
        Text(
            "This PIN gates opening Instagram/Facebook/YouTube, and protects Reel Blocker itself from being disabled.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) },
            label = { Text("4-digit PIN") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 4) confirmPin = it.filter(Char::isDigit) },
            label = { Text("Confirm PIN") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 8.dp),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp)) }
        Button(
            onClick = {
                if (pin.length != 4) {
                    error = "PIN must be 4 digits"
                } else if (pin != confirmPin) {
                    error = "PINs don't match"
                } else {
                    pinManager.setPin(pin)
                    pinManager.ensureRecoveryCode()
                    onDone()
                }
            },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("Finish setup") }
    }
}
