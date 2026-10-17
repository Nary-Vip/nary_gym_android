package com.nary.gym.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.data.remote.dto.UserResponse
import com.nary.gym.ui.screens.Profile.CONFIRM_LOGOUT_TAG
import com.nary.gym.ui.screens.Profile.LOGOUT_BUTTON_TAG
import com.nary.gym.ui.screens.Profile.ProfileContent
import com.nary.gym.ui.theme.NaryToDoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val nary = UserResponse(id = "u1", email = "nary@example.com", phone = "9876543210", name = "Nary", profile = null)

    @Composable
    private fun Holder(
        user: UserResponse?,
        onSave: (name: String, phone: String) -> Unit = { _, _ -> },
        onLogout: () -> Unit = {},
    ) {
        var name by remember { mutableStateOf(user?.name.orEmpty()) }
        var phone by remember { mutableStateOf(user?.phone.orEmpty()) }
        var reminders by remember { mutableStateOf(false) }
        NaryToDoTheme {
            ProfileContent(
                user = user,
                name = name,
                phone = phone,
                onNameChange = { name = it },
                onPhoneChange = { phone = it },
                pickedImage = null,
                isSaving = false,
                isLoggingOut = false,
                notificationsEnabled = reminders,
                reminderLabel = "7:00 AM",
                contentPadding = PaddingValues(),
                onPickImage = {},
                onSave = { onSave(name, phone) },
                onRemindersToggled = { reminders = it },
                onReminderTimeClick = {},
                onLogout = onLogout,
            )
        }
    }

    @Test
    fun loading_showsLoading_andFieldsCannotBeEdited() {
        rule.setContent { Holder(user = null) }
        rule.onNodeWithText("Loading your profile…").assertExists()
        runCatching { rule.onNodeWithText("Full name").performTextInput("x") }
        rule.onNodeWithText("Save changes").assertDoesNotExist()
    }

    @Test
    fun loaded_saveAppearsOnlyAfterAnEdit_andSavesIt() {
        var saved: Pair<String, String>? = null
        rule.setContent { Holder(user = nary, onSave = { n, p -> saved = n to p }) }
        rule.onNodeWithText("Loading your profile…").assertDoesNotExist()
        rule.onNodeWithText("Save changes").assertDoesNotExist()

        rule.onNodeWithText("Full name").performTextReplacement("Naresh")
        rule.onNodeWithText("Save changes").performScrollTo().performClick()
        assertEquals("Naresh" to "9876543210", saved)
    }

    @Test
    fun reminderTimeRow_appearsOnlyWhenRemindersAreOn() {
        rule.setContent { Holder(user = nary) }
        rule.onNodeWithText("Reminder time").assertDoesNotExist()
        rule.onNode(isToggleable()).performScrollTo().performClick()
        rule.onNodeWithText("Reminder time").assertExists()
        rule.onNodeWithText("Every day at 7:00 AM").assertExists()
    }

    @Test
    fun logout_asksFirst_cancelKeeps_confirmLogsOut() {
        var logouts = 0
        rule.setContent { Holder(user = nary, onLogout = { logouts++ }) }

        rule.onNodeWithTag(LOGOUT_BUTTON_TAG).performScrollTo().performClick()
        rule.onNodeWithText("Log out?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(0, logouts)

        rule.onNodeWithTag(LOGOUT_BUTTON_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(CONFIRM_LOGOUT_TAG).performClick()
        assertEquals(1, logouts)
    }
}
