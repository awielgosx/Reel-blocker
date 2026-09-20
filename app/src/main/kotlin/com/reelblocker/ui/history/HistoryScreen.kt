package com.reelblocker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reelblocker.data.DayUsage
import com.reelblocker.data.UsageRepository
import com.reelblocker.tracking.DayBoundary
import com.reelblocker.ui.theme.ReelBlockerPanel
import com.reelblocker.util.DurationFormat
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val repository = remember { UsageRepository(context) }
    var days by remember { mutableStateOf<List<DayUsage>>(emptyList()) }

    LaunchedEffect(Unit) {
        val today = DayBoundary.currentAppDay()
        days = repository.getRange(today.minusDays(6), today)
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Last 7 days", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Weekly/monthly trend detail is planned for a later pass.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        ReelBlockerPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                if (days.isNotEmpty()) {
                    WeekBarChart(days, modifier = Modifier.fillMaxWidth().height(160.dp))
                }
                Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    days.forEach { day ->
                        val label = day.day.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                        Text("$label — ${DurationFormat.format(day.totalMillis)}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBarChart(days: List<DayUsage>, modifier: Modifier = Modifier) {
    val maxMillis = (days.maxOfOrNull { it.totalMillis } ?: 0L).coerceAtLeast(60_000L)
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val barCount = days.size.coerceAtLeast(1)
        val gap = size.width * 0.02f
        val barWidth = (size.width - gap * (barCount + 1)) / barCount
        days.forEachIndexed { index, day ->
            val heightFraction = (day.totalMillis.toFloat() / maxMillis).coerceIn(0f, 1f)
            val barHeight = size.height * heightFraction
            val left = gap + index * (barWidth + gap)
            drawRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}
