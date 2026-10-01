package com.reelblocker.security

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Milestone 1 scope: this service exists only to protect Reel Blocker's own settings from being
 * turned off on impulse. It watches for the system Settings app / package installer showing a
 * screen that mentions this app, backs out of it immediately, and hands off to [GuardActivity] to
 * require the PIN before letting the user back in - but only when [GuardScreenClassifier] (unit
 * tested separately) says the screen is actually a disable/uninstall attempt, not just a list that
 * mentions the app or an unrelated install/update dialog. It does not read or act on content in
 * Instagram/Facebook/YouTube in this build - that's Milestone 2.
 */
class GuardAccessibilityService : AccessibilityService() {

    private var lastTriggerAtMillis = 0L
    private val pinManager: PinManager by lazy { PinManager(this) }
    private val suppression: GuardSuppression by lazy { GuardSuppression(this) }
    private val appLabel: String by lazy { packageManager.getApplicationLabel(applicationInfo).toString() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = GuardScreenClassifier.WATCHED_PACKAGES.toTypedArray()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Master switch - see SelfProtectionConfig. Off during active development on purpose.
        if (!SelfProtectionConfig.ENABLED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName !in GuardScreenClassifier.WATCHED_PACKAGES) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerAtMillis < DEBOUNCE_MILLIS) return

        val root = rootInActiveWindow ?: return
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = packageName,
            pinIsSet = pinManager.isPinSet(),
            isSuppressed = suppression.isSuppressed(),
            screenMentions = { needle -> nodeMentions(root, needle) },
        )

        val target = (decision as? GuardDecision.Trigger)?.target ?: return

        lastTriggerAtMillis = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        GuardActivity.launch(this, target)
    }

    override fun onInterrupt() {}

    private fun nodeMentions(node: AccessibilityNodeInfo, needle: String, depth: Int = 0): Boolean {
        if (depth > MAX_SCAN_DEPTH) return false
        val text = node.text?.toString()
        val description = node.contentDescription?.toString()
        if (text?.contains(needle, ignoreCase = true) == true) return true
        if (description?.contains(needle, ignoreCase = true) == true) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (nodeMentions(child, needle, depth + 1)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 1500L
        private const val MAX_SCAN_DEPTH = 40
    }
}

enum class GuardTarget {
    ACCESSIBILITY_SETTINGS,
    DEVICE_ADMIN_SETTINGS,
    UNINSTALL,
    APP_SETTINGS,
}
