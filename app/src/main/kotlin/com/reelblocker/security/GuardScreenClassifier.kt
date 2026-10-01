package com.reelblocker.security

/**
 * Pure decision logic for [GuardAccessibilityService] - no Android dependency, so the exact bugs
 * we hit in practice (triggering on a plain list screen, triggering on an install/update dialog,
 * triggering before a PIN exists) can be regression-tested directly without a device.
 *
 * The caller supplies [screenMentions] as a closure over the real accessibility node tree; this
 * class only decides, given what's on screen, whether that counts as "the user is trying to
 * disable/uninstall this app right now".
 */
object GuardScreenClassifier {

    const val SETTINGS_PACKAGE = "com.android.settings"

    val WATCHED_PACKAGES = listOf(
        SETTINGS_PACKAGE,
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
    )

    private val SETTINGS_ACTION_KEYWORDS = listOf(
        "turn off", "use reel blocker", "uninstall", "remove app", "device admin",
        "deactivate", "app info", "force stop",
    )

    /**
     * Outside com.android.settings (i.e. the package installer / permission controller), only
     * the literal uninstall confirmation counts - an install/update dialog must never match here,
     * since installing or updating this app is not a self-disable action.
     */
    private val NON_SETTINGS_ACTION_KEYWORDS = listOf("uninstall")

    fun classify(
        appLabel: String,
        packageName: String,
        pinIsSet: Boolean,
        isSuppressed: Boolean,
        screenMentions: (needle: String) -> Boolean,
    ): GuardDecision {
        if (!pinIsSet) return GuardDecision.DoNothing
        if (isSuppressed) return GuardDecision.DoNothing
        if (packageName !in WATCHED_PACKAGES) return GuardDecision.DoNothing
        if (!screenMentions(appLabel)) return GuardDecision.DoNothing

        val keywords = if (packageName == SETTINGS_PACKAGE) SETTINGS_ACTION_KEYWORDS else NON_SETTINGS_ACTION_KEYWORDS
        if (keywords.none { screenMentions(it) }) return GuardDecision.DoNothing

        val target = when {
            screenMentions("Accessibility") -> GuardTarget.ACCESSIBILITY_SETTINGS
            screenMentions("device admin") || screenMentions("Device admin") -> GuardTarget.DEVICE_ADMIN_SETTINGS
            screenMentions("Uninstall") -> GuardTarget.UNINSTALL
            else -> GuardTarget.APP_SETTINGS
        }
        return GuardDecision.Trigger(target)
    }
}

sealed interface GuardDecision {
    data object DoNothing : GuardDecision
    data class Trigger(val target: GuardTarget) : GuardDecision
}
