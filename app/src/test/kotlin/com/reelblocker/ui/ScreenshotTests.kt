package com.reelblocker.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reelblocker.security.GuardScreen
import com.reelblocker.security.GuardTarget
import com.reelblocker.tracking.LiveUsage
import com.reelblocker.tracking.MonitoredApp
import com.reelblocker.tracking.TrackingState
import com.reelblocker.ui.dashboard.DashboardScreen
import com.reelblocker.ui.theme.AppBackground
import com.reelblocker.ui.theme.ReelBlockerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders key screens to real PNG files via Robolectric's native graphics mode - no emulator or
 * device needed. These exist so the UI (in particular the Cosmic Glass / Liquid Silver theming)
 * can be reviewed as images rather than requiring a phone install for every visual change.
 *
 * Output goes to [OUTPUT_DIR], outside the build directory so a `clean` doesn't wipe the one copy
 * meant for human eyes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val widthPx = 1080
    private val heightPx = 2340

    private fun capture(name: String, darkTheme: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        // Text field cursor blink (and similar) animations never settle to idle under Robolectric's
        // synthetic clock, so drive frames manually instead of waiting for idle.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val uiMode = if (darkTheme) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO
            val config = android.content.res.Configuration(ApplicationProvider.getApplicationContext<android.content.Context>().resources.configuration)
            config.uiMode = (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or uiMode
            val context = ApplicationProvider.getApplicationContext<android.content.Context>().createConfigurationContext(config)
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides context,
                androidx.compose.ui.platform.LocalConfiguration provides config,
            ) {
                ReelBlockerTheme {
                    content()
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        // Draw the real decor View straight into a bitmap, bypassing compose-ui-test's
        // captureToImage() (which waits on a real PixelCopy/redraw callback that never fires
        // under Robolectric here) - a plain software draw pass works without it. Use the size
        // Compose's own activity attachment already laid out, rather than forcing a second
        // measure/layout pass (which left stale double-drawn content from the first pass).
        val view: View = composeTestRule.activity.window.decorView
        val width = view.width.takeIf { it > 0 } ?: widthPx
        val height = view.height.takeIf { it > 0 } ?: heightPx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        File(OUTPUT_DIR).mkdirs()
        File(OUTPUT_DIR, "$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun `dashboard screen - dark (Cosmic Glass)`() {
        seedSampleUsage()
        capture("dashboard_dark", darkTheme = true) {
            AppBackground { Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) { DashboardScreen() } }
        }
    }

    @Test
    fun `dashboard screen - light (Liquid Silver)`() {
        seedSampleUsage()
        capture("dashboard_light", darkTheme = false) {
            AppBackground { Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) { DashboardScreen() } }
        }
    }

    @Test
    fun `guard pin screen - dark (Cosmic Glass)`() {
        capture("guard_pin_dark", darkTheme = true) {
            AppBackground {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    GuardScreen(
                        target = GuardTarget.UNINSTALL,
                        onVerify = { false },
                        onRecover = { _, _ -> false },
                        onSuccess = {},
                        onCancel = {},
                    )
                }
            }
        }
    }

    @Test
    fun `guard pin screen - light (Liquid Silver)`() {
        capture("guard_pin_light", darkTheme = false) {
            AppBackground {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    GuardScreen(
                        target = GuardTarget.UNINSTALL,
                        onVerify = { false },
                        onRecover = { _, _ -> false },
                        onSuccess = {},
                        onCancel = {},
                    )
                }
            }
        }
    }

    private fun seedSampleUsage() {
        TrackingState.update(
            LiveUsage(
                sessionApp = MonitoredApp.INSTAGRAM,
                sessionMillis = 4 * 60_000L,
                dayTotalsMillis = mapOf(
                    MonitoredApp.INSTAGRAM to 42 * 60_000L,
                    MonitoredApp.FACEBOOK to 11 * 60_000L,
                    MonitoredApp.YOUTUBE to 67 * 60_000L,
                ),
            ),
        )
    }

    companion object {
        // Overridable so CI/local runs don't need to agree on a path; defaults into build/ so a
        // `clean` naturally sweeps it.
        val OUTPUT_DIR: String = System.getenv("REEL_BLOCKER_SCREENSHOT_DIR") ?: "build/screenshots"
    }
}
