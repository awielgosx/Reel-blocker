package com.reelblocker.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the three real incidents this classifier exists to prevent:
 *  1. Firing on the plain Accessibility > Installed apps LIST screen (stranded onboarding).
 *  2. Firing with no PIN set yet (every PIN looked "wrong" because there was nothing to compare).
 *  3. Firing on the package installer's INSTALL/UPDATE dialog, not just an actual uninstall.
 */
class GuardScreenClassifierTest {

    private val appLabel = "Reel Blocker"
    private val settingsPkg = GuardScreenClassifier.SETTINGS_PACKAGE
    private val installerPkg = "com.google.android.packageinstaller"

    private fun screenWith(vararg phrases: String): (String) -> Boolean =
        { needle -> phrases.any { it.contains(needle, ignoreCase = true) } }

    @Test
    fun `does nothing when no PIN has been set yet, regardless of screen content`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = settingsPkg,
            pinIsSet = false,
            isSuppressed = false,
            screenMentions = screenWith("Reel Blocker", "Uninstall"),
        )
        assertEquals(GuardDecision.DoNothing, decision)
    }

    @Test
    fun `does nothing while suppressed after a verified PIN entry`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = settingsPkg,
            pinIsSet = true,
            isSuppressed = true,
            screenMentions = screenWith("Reel Blocker", "Uninstall"),
        )
        assertEquals(GuardDecision.DoNothing, decision)
    }

    @Test
    fun `does nothing on an unwatched package`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = "com.instagram.android",
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Reel Blocker", "Uninstall"),
        )
        assertEquals(GuardDecision.DoNothing, decision)
    }

    @Test
    fun `does NOT fire on the plain Accessibility installed-apps list screen`() {
        // This is exactly what stranded onboarding: a list of service names with no action context.
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = settingsPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Access Dots", "AppBlock", "Reel Blocker", "Live Transcribe"),
        )
        assertEquals(GuardDecision.DoNothing, decision)
    }

    @Test
    fun `fires on the real accessibility toggle-off screen`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = settingsPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Use Reel Blocker", "Accessibility", "Turn off Reel Blocker"),
        )
        assertEquals(GuardDecision.Trigger(GuardTarget.ACCESSIBILITY_SETTINGS), decision)
    }

    @Test
    fun `fires on the device admin deactivation screen`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = settingsPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Reel Blocker", "Deactivate this device admin app?"),
        )
        assertEquals(GuardDecision.Trigger(GuardTarget.DEVICE_ADMIN_SETTINGS), decision)
    }

    @Test
    fun `fires on the app info page which offers Force stop and Uninstall`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = settingsPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Reel Blocker", "App info", "Force stop", "Uninstall"),
        )
        assertEquals(GuardDecision.Trigger(GuardTarget.UNINSTALL), decision)
    }

    @Test
    fun `fires on the installer's real uninstall confirmation`() {
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = installerPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Do you want to uninstall Reel Blocker?"),
        )
        assertEquals(GuardDecision.Trigger(GuardTarget.UNINSTALL), decision)
    }

    @Test
    fun `does NOT fire on the installer's install-update dialog`() {
        // The actual bug: updating the app got blocked because this matched too broadly.
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = installerPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Do you want to install this update to Reel Blocker?"),
        )
        assertEquals(GuardDecision.DoNothing, decision)
    }

    @Test
    fun `does NOT fire on the installer package just because Settings-style keywords appear`() {
        // e.g. a permission-change warning mentioning "device admin" during an update - must not
        // be treated the same as an actual Settings admin-deactivation screen.
        val decision = GuardScreenClassifier.classify(
            appLabel = appLabel,
            packageName = installerPkg,
            pinIsSet = true,
            isSuppressed = false,
            screenMentions = screenWith("Reel Blocker", "has device admin access"),
        )
        assertEquals(GuardDecision.DoNothing, decision)
    }

    @Test
    fun `all watched packages are exactly the expected four`() {
        assertTrue(GuardScreenClassifier.WATCHED_PACKAGES.containsAll(
            listOf(
                "com.android.settings",
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.google.android.permissioncontroller",
            )
        ))
        assertEquals(4, GuardScreenClassifier.WATCHED_PACKAGES.size)
    }
}
