package com.nary.gym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.ui.screens.AuthSwitchPrompt
import com.nary.gym.ui.theme.NaryToDoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthSwitchPromptTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun signUp_staysReachable_atDoubleFontSize() {
        var opened = false
        rule.setContent {
            WithFontScale(2f) {
                NaryToDoTheme { AuthSwitchPrompt("Don't have an account?", "Sign up") { opened = true } }
            }
        }
        val signUp = rule.onNode(hasText("Sign up") and hasClickAction())
        signUp.assertIsDisplayed()
        assertEquals(emptyList<String>(), rule.onNode(hasText("Sign up"), useUnmergedTree = true).textLayout().linesBrokenInsideAWord())
        signUp.performClick()
        assertTrue(opened)
    }
}
