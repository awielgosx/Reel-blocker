package com.reelblocker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reelblocker.tracking.TrackingState
import com.reelblocker.ui.theme.ReelBlockerPanel
import com.reelblocker.util.DurationFormat

@Composable
fun DashboardScreen() {
    val usage by TrackingState.live.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Today", style = MaterialTheme.typography.headlineSmall)

        ReelBlockerPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Combined", style = MaterialTheme.typography.labelLarge)
                Text(
                    DurationFormat.format(usage.combinedDayMillis),
                    style = MaterialTheme.typography.displaySmall,
                )
            }
        }

        ReelBlockerPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Current session", style = MaterialTheme.typography.labelLarge)
                Text(
                    usage.sessionApp?.let { app ->
                        "${app.displayName}: ${DurationFormat.format(usage.sessionMillis)}"
                    } ?: "Not currently in a tracked app",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        ReelBlockerPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Per app today", style = MaterialTheme.typography.labelLarge)
                com.reelblocker.tracking.MonitoredApp.entries.forEach { app ->
                    val millis = usage.dayTotalsMillis[app] ?: 0L
                    Text("${app.displayName}: ${DurationFormat.format(millis)}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
