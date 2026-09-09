package com.thinkblox.radiantrush

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadiantRushSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun welcomeScreenOpensShellWithoutWalletPopup() {
        composeRule.onNodeWithText("Connect. Quest. Prove.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTestTags.WELCOME_OPEN_RUSH).performScrollTo().performClick()
        composeRule.onNodeWithTag(UiTestTags.NAV_HOME).assertExists()
        composeRule.onNodeWithTag(UiTestTags.NAV_DEMO).assertExists()
    }

    @Test
    fun demoTabIsReachableForJudgeWalkthrough() {
        composeRule.onNodeWithTag(UiTestTags.WELCOME_OPEN_RUSH).performScrollTo().performClick()
        composeRule.onNodeWithTag(UiTestTags.NAV_DEMO).performClick()
        composeRule.onNodeWithTag(UiTestTags.DEMO_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Judge Demo Mode", substring = true).assertIsDisplayed()
    }
}
