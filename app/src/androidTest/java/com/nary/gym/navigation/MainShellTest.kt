package com.nary.gym.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.ui.theme.NaryToDoTheme
import com.nary.gym.ui.WithFontScale
import com.nary.gym.ui.textLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainShellTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun setShell(requestedTab: Tab? = null, onAvatarClick: () -> Unit = {}, fontScale: Float = 1f) {
        rule.setContent {
            WithFontScale(fontScale) { NaryToDoTheme {
                MainShell(
                    tabNavController = rememberNavController(),
                    requestedTab = requestedTab,
                    onRequestedTabHandled = {},
                    avatarName = "Naresh",
                    avatarImageUrl = null,
                    onAvatarClick = onAvatarClick,
                ) { tab, _ -> Text("content-${tab.name}") }
            } }
        }
    }

    private fun navItem(label: String) = rule.onNode(hasText(label) and hasClickAction())

    private fun pressBack() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    @Test
    fun tabLabels_stayOnOneLine_atDoubleFontSize() {
        setShell(fontScale = 2f)
        Tab.entries.forEach { tab ->
            assertEquals(tab.label, 1, rule.onNode(hasText(tab.label) and hasAnyAncestor(hasClickAction()), useUnmergedTree = true).textLayout().lineCount)
        }
    }

    @Test
    fun showsThreeLabelledTabs_andStartsOnToday() {
        setShell()
        navItem("Today").assertIsSelected()
        navItem("Workouts").assertExists()
        navItem("Check-ins").assertExists()
        rule.onNodeWithText("content-TODAY").assertExists()
    }

    @Test
    fun selectingWorkouts_thenBack_returnsToToday() {
        setShell()
        navItem("Workouts").performClick()
        rule.onNodeWithText("content-WORKOUTS").assertExists()
        pressBack()
        rule.onNodeWithText("content-TODAY").assertExists()
        navItem("Today").assertIsSelected()
    }

    @Test
    fun reselectingTab_doesNotStack_andBackReturnsToToday() {
        setShell()
        navItem("Workouts").performClick()
        navItem("Workouts").performClick()
        navItem("Check-ins").performClick()
        navItem("Check-ins").performClick()
        pressBack()
        rule.onNodeWithText("content-TODAY").assertExists()
    }

    @Test
    fun avatar_invokesProfileCallback() {
        var opened = false
        setShell(onAvatarClick = { opened = true })
        rule.onNodeWithContentDescription("Profile").performClick()
        assertTrue(opened)
    }

    @Test
    fun requestedTab_isOpened() {
        setShell(requestedTab = Tab.CHECK_INS)
        rule.onNodeWithText("content-CHECK_INS").assertExists()
        navItem("Check-ins").assertIsSelected()
    }

    @Test
    fun requestedTab_arrivingAfterShellIsShown_isOpened() {
        var requested by mutableStateOf<Tab?>(null)
        rule.setContent {
            NaryToDoTheme {
                MainShell(
                    tabNavController = rememberNavController(),
                    requestedTab = requested,
                    onRequestedTabHandled = { requested = null },
                    avatarName = "Naresh",
                    avatarImageUrl = null,
                    onAvatarClick = {},
                ) { tab, _ -> Text("content-${tab.name}") }
            }
        }
        rule.onNodeWithText("content-TODAY").assertExists()
        rule.runOnIdle { requested = Tab.CHECK_INS }
        rule.onNodeWithText("content-CHECK_INS").assertExists()
        rule.runOnIdle { requested = Tab.WORKOUTS }
        rule.onNodeWithText("content-WORKOUTS").assertExists()
    }
}
