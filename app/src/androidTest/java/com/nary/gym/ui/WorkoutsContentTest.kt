package com.nary.gym.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.SyncState
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.screens.Workouts.WorkoutsContent
import com.nary.gym.ui.theme.NaryToDoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutsContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val squat = WorkOuts(id = "1", workOutName = "Squat", workOutType = WorkOutType.LEG, workOutImage = "")
    private val lunge = WorkOuts(id = "2", workOutName = "Lunge", workOutType = WorkOutType.LEG, workOutImage = "")
    private val bench = WorkOuts(id = "3", workOutName = "Bench press", workOutType = WorkOutType.CHEST, workOutImage = "")
    private val deleted = mutableListOf<WorkOuts>()
    private val haptics = RecordingHaptics()
    private var failDeletes = false
    private val recordDelete: (WorkOuts, () -> Unit) -> Unit = { workout, onFailed ->
        deleted += workout
        if (failDeletes) onFailed()
    }

    private fun show(workouts: List<WorkOuts> = listOf(squat, lunge, bench), isLoading: Boolean = false) {
        rule.setContent {
            NaryToDoThemeWithHaptics(haptics) {
                WorkoutsContent(
                    workouts, PaddingValues(), onAdd = {}, onEdit = {}, onDelete = recordDelete, isLoading = isLoading,
                )
            }
        }
    }

    private fun swipeAway(name: String) {
        rule.onNodeWithText(name).performTouchInput { swipeLeft() }
    }

    @Test
    fun groupsByMuscle_andFilterChipsNarrowTheList() {
        show()
        rule.onNodeWithText("LEG · 2").assertExists()
        rule.onNodeWithText("CHEST · 1").assertExists()
        rule.onNodeWithTag("filter_CHEST").performClick()
        rule.onNodeWithText("Bench press").assertExists()
        rule.onNodeWithText("Squat").assertDoesNotExist()
    }

    @Test
    fun swipe_thenUndo_restoresRow_andNothingIsDeleted() {
        show()
        rule.mainClock.autoAdvance = false
        swipeAway("Squat")
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Deleted Squat").assertExists()
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(10_000)
        rule.onNodeWithText("Squat").assertExists()
        assertEquals(emptyList<WorkOuts>(), deleted)
    }

    @Test
    fun swipe_thenTimeout_deletesExactlyOnce() {
        show()
        rule.mainClock.autoAdvance = false
        swipeAway("Squat")
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Squat").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(6_000) 
        assertEquals(listOf(squat), deleted)
    }

    @Test
    fun swipeSecond_whilePending_commitsFirst() {
        show()
        rule.mainClock.autoAdvance = false
        swipeAway("Squat")
        rule.mainClock.advanceTimeBy(1_000)
        swipeAway("Lunge")
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(listOf(squat), deleted) 
        rule.onNodeWithText("Deleted Lunge").assertExists()
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(10_000)
        assertEquals(listOf(squat), deleted) 
    }

    @Test
    fun leavingWithPendingDelete_commitsIt() {
        var visible by mutableStateOf(true)
        rule.setContent {
            NaryToDoThemeWithHaptics(haptics) {
                if (visible) WorkoutsContent(listOf(squat, bench), PaddingValues(), {}, {}, recordDelete)
            }
        }
        rule.mainClock.autoAdvance = false
        swipeAway("Squat")
        rule.mainClock.advanceTimeBy(1_000)
        visible = false
        rule.mainClock.advanceTimeBy(500)
        assertEquals(listOf(squat), deleted)
    }

    @Test
    fun loading_withNoWorkoutsYet_showsLoadingInsteadOfEmptyState() {
        show(workouts = emptyList(), isLoading = true)
        rule.onNodeWithText("Loading workouts…").assertExists()
        rule.onNodeWithText("No workouts yet").assertDoesNotExist()
    }

    @Test
    fun failedDelete_bringsTheRowBack() {
        failDeletes = true
        show()
        rule.mainClock.autoAdvance = false
        swipeAway("Squat")
        rule.mainClock.advanceTimeBy(1_000)
        rule.mainClock.advanceTimeBy(6_000)
        assertEquals(listOf(squat), deleted)
        rule.onNodeWithText("Squat").assertExists()
    }

    @Test
    fun rows_offerDeleteAsAnAccessibilityAction_insteadOfAHiddenIcon() {
        show()
        rule.onNodeWithContentDescription("Delete Squat").assertDoesNotExist()
        val actions = rule.onNode(hasText("Squat") and hasClickAction())
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val delete = actions.single { it.label == "Delete" }
        rule.runOnIdle { delete.action() }
        rule.onNodeWithText("Deleted Squat").assertExists()
    }

    @Test
    fun swipePastThreshold_givesContextClick() {
        show()
        swipeAway("Squat")
        assertTrue(HapticFeedbackType.ContextClick in haptics.performed)
    }

    @Test
    fun pendingRow_showsWaitingToSync() {
        show(listOf(squat.copy(syncState = SyncState.PENDING_UPDATE)))
        rule.onNodeWithText("Leg · Waiting to sync").assertExists()
    }

    @Test
    fun rejectedRow_showsCouldntSyncReason() {
        show(listOf(squat.copy(syncState = SyncState.PENDING_CREATE, syncError = "Name too short")))
        rule.onNodeWithText("Leg · Couldn't sync: Name too short").assertExists()
    }

    @Test
    fun syncedRow_showsNoSyncLabel() {
        show(listOf(squat))
        rule.onNodeWithText("Waiting to sync", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Couldn't sync", substring = true).assertDoesNotExist()
    }
}
