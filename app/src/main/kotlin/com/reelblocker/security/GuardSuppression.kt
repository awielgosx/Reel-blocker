package com.reelblocker.security

import android.content.Context

/**
 * Short grace window after a successful PIN entry so completing the action the user just
 * unlocked (finishing an uninstall, toggling off accessibility, etc.) doesn't immediately
 * re-trigger [GuardAccessibilityService] on the very next screen it causes to appear.
 */
class GuardSuppression(context: Context) {
    private val prefs = context.getSharedPreferences("reelblocker_guard_state", Context.MODE_PRIVATE)

    fun suppressFor(durationMillis: Long = DEFAULT_DURATION_MILLIS) {
        prefs.edit().putLong(KEY_UNTIL, System.currentTimeMillis() + durationMillis).apply()
    }

    fun isSuppressed(): Boolean = System.currentTimeMillis() < prefs.getLong(KEY_UNTIL, 0L)

    companion object {
        private const val KEY_UNTIL = "suppressed_until"
        const val DEFAULT_DURATION_MILLIS = 3 * 60 * 1000L // 3 minutes
    }
}
