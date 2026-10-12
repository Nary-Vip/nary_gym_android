package com.nary.gym.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.ui.components.NaryPasswordField
import com.nary.gym.ui.components.NaryTextField
import com.nary.gym.ui.theme.NaryToDoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NaryTextFieldsTest {

    @get:Rule
    val rule = createComposeRule()

    /** Matches what the field displays; hasText would also match the raw (unmasked) input. */
    private fun displays(text: String) = SemanticsMatcher("displays '$text'") {
        it.config.getOrNull(SemanticsProperties.EditableText)?.text == text
    }

    @Test
    fun error_isShownUnderTheField() {
        rule.setContent {
            NaryToDoTheme {
                NaryTextField("nary@", {}, "Email", Icons.Filled.Email, error = "Enter a valid email")
            }
        }
        rule.onNodeWithText("Enter a valid email").assertIsDisplayed()
    }

    @Test
    fun passwordEye_revealsAndHidesThePassword() {
        rule.setContent {
            var visible by remember { mutableStateOf(false) }
            NaryToDoTheme {
                NaryPasswordField("secret1", {}, "Password", visible, onToggleVisible = { visible = !visible })
            }
        }
        rule.onNode(displays("•••••••")).assertExists() // masked as dots
        rule.onNodeWithContentDescription("Show password").performClick()
        rule.onNode(displays("secret1")).assertExists()
        rule.onNodeWithContentDescription("Hide password").performClick()
        rule.onNode(displays("•••••••")).assertExists()
    }
}
