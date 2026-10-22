package com.nary.gym.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import java.io.File
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.screens.Workouts.WorkoutEditor
import com.nary.gym.ui.theme.NaryToDoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutEditorTest {

    @get:Rule
    val rule = createComposeRule()

    private val squat = WorkOuts(id = "1", workOutName = "Squat", workOutType = WorkOutType.LEG, workOutImage = "")

    private fun edit(existingNames: Set<String> = emptySet()) {
        rule.setContent {
            NaryToDoTheme {
                WorkoutEditor(workout = squat, onSave = {}, onCancel = {}, existingNames = existingNames)
            }
        }
    }

    @Test
    fun add_withNameButNoPhoto_cannotSave() {
        rule.setContent {
            NaryToDoTheme {
                WorkoutEditor(workout = null, onSave = {}, onCancel = {}, initialType = WorkOutType.BACK)
            }
        }
        rule.onNodeWithText("Delete workout").assertDoesNotExist()
        rule.onNodeWithText("Exercise name").performTextInput("Deadlift")
        rule.onNodeWithText("Save workout").assertIsNotEnabled()
    }

    @Test
    fun shortName_showsErrorAndBlocksSave() {
        edit()
        rule.onNodeWithText("Squat").performTextReplacement("Sq")
        rule.onNodeWithText("At least 3 characters").assertExists()
        rule.onNodeWithText("Save workout").assertIsNotEnabled()
    }

    @Test
    fun duplicateName_showsErrorAndBlocksSave() {
        edit(existingNames = setOf("Lunge"))
        rule.onNodeWithText("Squat").performTextReplacement("Lunge")
        rule.onNodeWithText("You already have a workout with this name").assertExists()
        rule.onNodeWithText("Save workout").assertIsNotEnabled()
    }

    @Test
    fun edit_choosingMuscleGroupChangesSavedType() {
        var saved: WorkOuts? = null
        rule.setContent {
            NaryToDoTheme { WorkoutEditor(workout = squat, onSave = { saved = it }, onCancel = {}) }
        }
        rule.onNodeWithText("Muscle group").performClick() // opens the dropdown
        rule.onNodeWithText("Shoulder").performClick()
        rule.onNodeWithText("Save workout").performClick()
        assertEquals(squat.copy(workOutType = WorkOutType.SHOULDER), saved)
    }

    @Test
    fun edit_deleteAsksFirst_cancelKeeps_confirmDeletes() {
        val deleted = mutableListOf<WorkOuts>()
        rule.setContent {
            NaryToDoTheme {
                WorkoutEditor(workout = squat, onSave = {}, onCancel = {}, onDelete = { deleted += it })
            }
        }
        rule.onNodeWithText("Delete workout").performClick()
        rule.onNodeWithText("Delete workout?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(emptyList<WorkOuts>(), deleted)

        rule.onNodeWithText("Delete workout").performClick()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf(squat), deleted)
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val photoDir = File(context.filesDir, "workouts")
    private val photosBefore = photoDir.listFiles().orEmpty().toSet()
    private val png = File(context.cacheDir, "pick.png").apply {
        outputStream().use { Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private val fakePicker: (onPicked: (Uri) -> Unit) -> Unit = { onPicked -> onPicked(Uri.fromFile(png)) }

    private fun newPhotos() = photoDir.listFiles().orEmpty().toSet() - photosBefore

    private fun pickAndWait() {
        rule.onNodeWithText(if (newPhotos().isEmpty()) "Add photo" else "Change photo").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Change photo").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun cleanUpPhotos() {
        newPhotos().forEach { it.delete() }
        png.delete()
    }

    @Test
    fun add_withPhoto_savesWithInitialType_andKeepsThePhoto() {
        var saved: WorkOuts? = null
        rule.setContent {
            NaryToDoTheme {
                WorkoutEditor(
                    workout = null, onSave = { saved = it }, onCancel = {},
                    initialType = WorkOutType.BACK, pickPhoto = fakePicker,
                )
            }
        }
        rule.onNodeWithText("Exercise name").performTextInput("Deadlift")
        pickAndWait()
        rule.onNodeWithText("Save workout").assertIsEnabled().performClick()
        assertEquals("Deadlift", saved?.workOutName)
        assertEquals(WorkOutType.BACK, saved?.workOutType)
        assertEquals(setOf(File(saved!!.workOutImage)), newPhotos())
    }

    @Test
    fun pickingAgain_deletesThePreviousPhoto() {
        var saved: WorkOuts? = null
        rule.setContent {
            NaryToDoTheme { WorkoutEditor(workout = squat, onSave = { saved = it }, onCancel = {}, pickPhoto = fakePicker) }
        }
        pickAndWait()
        val first = newPhotos().single()
        rule.onNodeWithText("Change photo").performClick()
        rule.waitUntil(5_000) { newPhotos().isNotEmpty() && first !in newPhotos() }
        rule.waitForIdle()
        rule.onNodeWithText("Save workout").performClick()
        assertEquals(setOf(File(saved!!.workOutImage)), newPhotos())
    }

    @Test
    fun closingWithoutSaving_deletesThePickedPhoto() {
        var open by mutableStateOf(true)
        rule.setContent {
            NaryToDoTheme {
                if (open) WorkoutEditor(workout = squat, onSave = {}, onCancel = {}, pickPhoto = fakePicker)
            }
        }
        pickAndWait()
        assertEquals(1, newPhotos().size)
        open = false
        rule.waitForIdle()
        assertEquals(emptySet<File>(), newPhotos())
    }
}
