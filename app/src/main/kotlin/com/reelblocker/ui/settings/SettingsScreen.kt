package com.reelblocker.ui.settings

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
import androidx.compose.material3.TextButton
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
import com.reelblocker.security.PinManager
import com.reelblocker.ui.onboarding.onboardingSteps

private enum class SettingsPanel { NONE, CHANGE_PIN, VIEW_RECOVERY }

@Composable
fun SettingsScreen(refreshTrigger: Int) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    var panel by remember { mutableStateOf(SettingsPanel.NONE) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

        when (panel) {
            SettingsPanel.NONE -> SettingsMenu(
                onChangePin = { panel = SettingsPanel.CHANGE_PIN },
                onViewRecovery = { panel = SettingsPanel.VIEW_RECOVERY },
            )
            SettingsPanel.CHANGE_PIN -> ChangePinPanel(pinManager, onDone = { panel = SettingsPanel.NONE })
            SettingsPanel.VIEW_RECOVERY -> ViewRecoveryPanel(pinManager, onDone = { panel = SettingsPanel.NONE })
        }
    }
}

@Composable
private fun SettingsMenu(onChangePin: () -> Unit, onViewRecovery: () -> Unit) {
    val context = LocalContext.current

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text("Security", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Button(onClick = onChangePin, modifier = Modifier.fillMaxWidth()) { Text("Change PIN") }
            Button(onClick = onViewRecovery, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("View recovery code")
            }
            Text(
                "Permissions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
        }
        items(onboardingSteps) { step ->
            val granted = step.isGranted(context)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(step.title, style = MaterialTheme.typography.titleSmall)
                if (granted) {
                    Icon(Icons.Filled.Check, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = { step.action(context)?.let { context.startActivity(it) } }) {
                        Text("Fix")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangePinPanel(pinManager: PinManager, onDone: () -> Unit) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        Text("Change PIN", style = MaterialTheme.typography.titleMedium)
        PinField(value = currentPin, onValueChange = { currentPin = it }, label = "Current PIN")
        PinField(value = newPin, onValueChange = { newPin = it }, label = "New PIN", modifier = Modifier.padding(top = 8.dp))
        PinField(value = confirmPin, onValueChange = { confirmPin = it }, label = "Confirm new PIN", modifier = Modifier.padding(top = 8.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Button(
            onClick = {
                when {
                    !pinManager.verifyPin(currentPin) -> error = "Current PIN is wrong"
                    newPin.length != 4 -> error = "New PIN must be 4 digits"
                    newPin != confirmPin -> error = "New PINs don't match"
                    else -> { pinManager.setPin(newPin); onDone() }
                }
            },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text("Save") }
        TextButton(onClick = onDone) { Text("Cancel") }
    }
}

@Composable
private fun ViewRecoveryPanel(pinManager: PinManager, onDone: () -> Unit) {
    var currentPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var revealedCode by remember { mutableStateOf<String?>(null) }

    Column {
        Text("View recovery code", style = MaterialTheme.typography.titleMedium)
        if (revealedCode == null) {
            Text(
                "Enter your current PIN to view it.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PinField(value = currentPin, onValueChange = { currentPin = it }, label = "Current PIN")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Button(
                onClick = {
                    if (pinManager.verifyPin(currentPin)) {
                        revealedCode = pinManager.ensureRecoveryCode()
                    } else {
                        error = "Wrong PIN"
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
            ) { Text("Reveal") }
        } else {
            Text(revealedCode ?: "", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Save this somewhere safe (e.g. a password manager). Anyone with this code and access to the app can reset your PIN.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        TextButton(onClick = onDone, modifier = Modifier.padding(top = 12.dp)) { Text("Done") }
    }
}

@Composable
private fun PinField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 4) onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier,
    )
}
