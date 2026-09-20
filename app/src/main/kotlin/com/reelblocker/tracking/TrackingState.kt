package com.reelblocker.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveUsage(
    val sessionApp: MonitoredApp? = null,
    val sessionMillis: Long = 0,
    val dayTotalsMillis: Map<MonitoredApp, Long> = emptyMap(),
) {
    val combinedDayMillis: Long get() = dayTotalsMillis.values.sum()
}

/** In-process, always-current usage snapshot. Written by [TrackingService], read by the UI and widget. */
object TrackingState {
    private val _live = MutableStateFlow(LiveUsage())
    val live = _live.asStateFlow()

    fun update(value: LiveUsage) {
        _live.value = value
    }
}
