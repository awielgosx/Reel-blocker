package com.reelblocker.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.reelblocker.ui.theme.AppBackground
import com.reelblocker.ui.theme.ReelBlockerTheme

class GuardActivity : ComponentActivity() {

    private lateinit var pinManager: PinManager
    private lateinit var suppression: GuardSuppression
    private lateinit var target: GuardTarget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinManager = PinManager(this)
        suppression = GuardSuppression(this)
        target = GuardTarget.entries.getOrElse(intent.getIntExtra(EXTRA_TARGET, 0)) { GuardTarget.APP_SETTINGS }

        setContent {
            ReelBlockerTheme {
                AppBackground {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
                    GuardScreen(
                        target = target,
                        onVerify = { pin -> pinManager.verifyPin(pin) },
                        onRecover = { code, newPin -> pinManager.resetPinWithRecoveryCode(code, newPin) },
                        onSuccess = {
                            suppression.suppressFor()
                            startActivity(intentFor(target))
                            finish()
                        },
                        onCancel = { finish() },
                    )
                }
                }
            }
        }
    }

    private fun intentFor(target: GuardTarget): Intent = when (target) {
        GuardTarget.ACCESSIBILITY_SETTINGS -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        GuardTarget.DEVICE_ADMIN_SETTINGS -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        GuardTarget.UNINSTALL -> Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        GuardTarget.APP_SETTINGS -> Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName"),
        )
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    companion object {
        private const val EXTRA_TARGET = "target"

        fun launch(context: Context, target: GuardTarget) {
            val intent = Intent(context, GuardActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_TARGET, target.ordinal)
            context.startActivity(intent)
        }
    }
}

@Composable
private fun GuardScreen(
    target: GuardTarget,
    onVerify: (String) -> Boolean,
    onRecover: (code: String, newPin: String) -> Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var recoveryCode by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Enter your PIN to continue", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = descriptionFor(target),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        if (!showRecovery) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) },
                label = { Text("4-digit PIN") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
            )
            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

            Button(
                onClick = {
                    if (onVerify(pin)) onSuccess() else error = "Wrong PIN"
                },
                modifier = Modifier.padding(top = 16.dp),
                enabled = pin.length == 4,
            ) { Text("Confirm") }

            TextButton(onClick = { showRecovery = true; error = null }) { Text("Forgot PIN? Use recovery code") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        } else {
            OutlinedTextField(
                value = recoveryCode,
                onValueChange = { recoveryCode = it },
                label = { Text("Recovery code") },
            )
            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 4) newPin = it.filter(Char::isDigit) },
                label = { Text("New 4-digit PIN") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(top = 8.dp),
            )
            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

            Button(
                onClick = {
                    if (newPin.length == 4 && onRecover(recoveryCode, newPin)) onSuccess()
                    else error = "That recovery code doesn't match"
                },
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("Reset PIN and continue") }

            TextButton(onClick = { showRecovery = false; error = null }) { Text("Back") }
        }
    }
}

private fun descriptionFor(target: GuardTarget): String = when (target) {
    GuardTarget.ACCESSIBILITY_SETTINGS -> "This protects Reel Blocker from being switched off by accident."
    GuardTarget.DEVICE_ADMIN_SETTINGS -> "This protects Reel Blocker from being switched off by accident."
    GuardTarget.UNINSTALL -> "This confirms you really want to uninstall Reel Blocker."
    GuardTarget.APP_SETTINGS -> "This protects Reel Blocker's settings from being changed by accident."
}
