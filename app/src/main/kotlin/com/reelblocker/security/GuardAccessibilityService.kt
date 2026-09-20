package com.reelblocker.security

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Milestone 1 scope: this service exists only to protect Reel Blocker's own settings from being
 * turned off on impulse. It watches for the system Settings app / package installer showing a
 * screen that mentions this app (accessibility toggle, device admin deactivation, uninstall),
 * backs out of it immediately, and hands off to [GuardActivity] to require the PIN before letting
 * the user back in. It does not read or act on content in Instagram/Facebook/YouTube in this
 * build - that's Milestone 2.
 */
class GuardAccessibilityService : AccessibilityService() {

    private var lastTriggerAtMillis = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = WATCHED_PACKAGES.toTypedArray()
        }
        serviceInfo = info
    }

    private val pinManager: PinManager by lazy { PinManager(this) }
    private val suppression: GuardSuppression by lazy { GuardSuppression(this) }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Nothing to protect until a PIN actually exists - and without one, verifyPin() can
        // never succeed, which would otherwise strand the user on a PIN screen they can't pass.
        if (!pinManager.isPinSet()) return

        // Grace window right after a verified PIN entry, so finishing the action the user just
        // unlocked doesn't immediately get intercepted again on the very next screen it shows.
        if (suppression.isSuppressed()) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName !in WATCHED_PACKAGES) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerAtMillis < DEBOUNCE_MILLIS) return

        val root = rootInActiveWindow ?: return
        if (!mentionsThisAppSpecifically(root, packageName)) return

        lastTriggerAtMillis = now
        val target = classifyTarget(root)
        performGlobalAction(GLOBAL_ACTION_BACK)
        GuardActivity.launch(this, target)
    }

    override fun onInterrupt() {}

    private val appLabel: String by lazy {
        packageManager.getApplicationLabel(applicationInfo).toString()
    }

    /**
     * The app's name alone isn't enough to trigger - it shows up on plain list screens too
     * (e.g. Settings > Accessibility > Installed apps just lists every service by name, which
     * the user has to pass through to turn the toggle ON in the first place). Require an
     * action/detail keyword alongside the name so this only fires on an actual
     * disable/uninstall/admin screen, not a list that merely mentions the app.
     *
     * The package installer additionally shows this app's *install/update* confirmation, which
     * must never be guarded (that's not a self-disable action) - so for that package, only the
     * literal uninstall confirmation counts, not the broader Settings keyword set.
     */
    private fun mentionsThisAppSpecifically(root: AccessibilityNodeInfo, packageName: String): Boolean {
        if (!nodeMentions(root, appLabel)) return false
        val keywords = if (packageName == "com.android.settings") ACTION_KEYWORDS else listOf("uninstall")
        return keywords.any { nodeMentions(root, it) }
    }

    private fun classifyTarget(root: AccessibilityNodeInfo): GuardTarget {
        return when {
            nodeMentions(root, "Accessibility") -> GuardTarget.ACCESSIBILITY_SETTINGS
            nodeMentions(root, "device admin") || nodeMentions(root, "Device admin") ->
                GuardTarget.DEVICE_ADMIN_SETTINGS
            nodeMentions(root, "Uninstall") -> GuardTarget.UNINSTALL
            else -> GuardTarget.APP_SETTINGS
        }
    }

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
        private val WATCHED_PACKAGES = listOf(
            "com.android.settings",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
        )
        private val ACTION_KEYWORDS = listOf(
            "turn off", "use reel blocker", "uninstall", "remove app", "device admin",
            "deactivate", "app info", "force stop",
        )
    }
}

enum class GuardTarget {
    ACCESSIBILITY_SETTINGS,
    DEVICE_ADMIN_SETTINGS,
    UNINSTALL,
    APP_SETTINGS,
}
