package com.reelblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.reelblocker.permissions.PermissionChecks
import com.reelblocker.security.PinManager
import com.reelblocker.tracking.TrackingService
import com.reelblocker.ui.dashboard.DashboardScreen
import com.reelblocker.ui.history.HistoryScreen
import com.reelblocker.ui.onboarding.OnboardingScreen
import com.reelblocker.ui.onboarding.onboardingSteps
import com.reelblocker.ui.settings.SettingsScreen
import androidx.compose.material3.MaterialTheme
import com.reelblocker.ui.theme.AppBackground
import com.reelblocker.ui.theme.ReelBlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReelBlockerTheme {
                AppBackground {
                    Surface(color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
                        RootScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun RootScreen() {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val setupComplete = refreshTrigger.let {
        onboardingSteps.all { step -> step.isGranted(context) } && pinManager.isPinSet()
    }

    if (!setupComplete) {
        OnboardingScreen(
            refreshTrigger = refreshTrigger,
            onAllPermissionsGranted = { refreshTrigger++ },
            onPinConfigured = {
                TrackingService.start(context)
                refreshTrigger++
            },
        )
    } else {
        MainTabs(refreshTrigger)
    }
}

private enum class Tab(val label: String) { DASHBOARD("Today"), HISTORY("History"), SETTINGS("Settings") }

@Composable
private fun MainTabs(refreshTrigger: Int) {
    var tab by remember { mutableStateOf(Tab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.DASHBOARD,
                    onClick = { tab = Tab.DASHBOARD },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(Tab.DASHBOARD.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.HISTORY,
                    onClick = { tab = Tab.HISTORY },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text(Tab.HISTORY.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS,
                    onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(Tab.SETTINGS.label) },
                )
            }
        },
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            when (tab) {
                Tab.DASHBOARD -> DashboardScreen()
                Tab.HISTORY -> HistoryScreen()
                Tab.SETTINGS -> SettingsScreen(refreshTrigger)
            }
        }
    }
}
