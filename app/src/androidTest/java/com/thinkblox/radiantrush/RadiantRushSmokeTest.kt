package com.thinkblox.radiantrush

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadiantRushSmokeTest {
    /**
     * Compose UI Test v2 owns ActivityScenario launch ordering for the app-hosted
     * Compose content. This avoids the intermittent "No compose hierarchies found"
     * failure seen with the deprecated pre-v2 rule on physical devices.
     */
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun hasTag(tag: String): Boolean = runCatching {
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)

    /**
     * Firebase / Activity startup can take a moment on a real phone. Also tolerate
     * Android restoring the already-entered shell instead of assuming Welcome is
     * always the first composed destination.
     */
    private fun enterShell() {
        // The Activity may restore an already-entered shell on a physical device.
        // Do not assume that restored shell is still on Home; the bottom-nav Home tag
        // is the stable signal that the shell itself exists.
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            hasTag(UiTestTags.WELCOME_OPEN_RUSH) || hasTag(UiTestTags.NAV_HOME)
        }

        if (hasTag(UiTestTags.WELCOME_OPEN_RUSH)) {
            composeRule
                .onNodeWithTag(UiTestTags.WELCOME_OPEN_RUSH)
                .assertIsDisplayed()
                .performClick()
        }

        composeRule.waitUntil(timeoutMillis = 10_000L) {
            hasTag(UiTestTags.NAV_HOME)
        }

        // Force a deterministic destination before asserting screen content. This makes
        // the smoke test independent of the user's previously selected tab.
        composeRule.onNodeWithTag(UiTestTags.NAV_HOME).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000L) {
            hasTag(UiTestTags.HOME_SCREEN)
        }
    }

    @Test
    fun welcomeScreenOpensShellWithoutWalletPopup() {
        enterShell()
        composeRule.onNodeWithTag(UiTestTags.HOME_SCREEN).assertIsDisplayed()
    }

    @Test
    fun demoTabIsReachableForJudgeWalkthrough() {
        enterShell()

        composeRule.onNodeWithTag(UiTestTags.NAV_DEMO).performClick()
        composeRule.onNodeWithTag(UiTestTags.DEMO_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Welcome to Radiant Circle", substring = true).assertIsDisplayed()
    }
}
