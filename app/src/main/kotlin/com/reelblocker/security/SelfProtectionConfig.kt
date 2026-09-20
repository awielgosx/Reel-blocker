package com.reelblocker.security

/**
 * Master switch for Device Admin + the accessibility self-protection guard.
 *
 * Keep this false for as long as we're actively iterating: a guard that can intercept
 * updates/uninstalls is exactly the wrong thing to have armed while builds change daily. When
 * [ENABLED] is false, Device Admin is dropped from onboarding entirely (nothing to grant, nothing
 * to strand you on) and [GuardAccessibilityService] never intercepts anything even if
 * Accessibility itself is on (which it still needs to be, for the reel-blocking work in
 * Milestone 2).
 *
 * Flip this to true only once the app is otherwise stable and you're deliberately choosing to
 * lock it down for real day-to-day use - not by default, not silently.
 */
object SelfProtectionConfig {
    const val ENABLED = false
}
