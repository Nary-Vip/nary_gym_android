package com.nary.gym.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.screens.Workouts.WorkoutsContent
import com.nary.gym.ui.theme.NaryToDoTheme
import org.junit.Assert.assertEquals
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

    private fun show(workouts: List<WorkOuts> = listOf(squat, lunge, bench)) {
        rule.setContent {
            NaryToDoTheme {
                WorkoutsContent(workouts, PaddingValues(), onAdd = {}, onEdit = {}, onDelete = { deleted += it })
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
        rule.onNodeWithTag("filter_CHEST").performClick() // "Chest" text also appears in rows
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
        rule.mainClock.advanceTimeBy(6_000) // Short snackbar = 4 s
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
        assertEquals(listOf(squat), deleted)           // first committed immediately
        rule.onNodeWithText("Deleted Lunge").assertExists()
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(10_000)
        assertEquals(listOf(squat), deleted)           // second undone
    }

    @Test
    fun leavingWithPendingDelete_commitsIt() {
        var visible by mutableStateOf(true)
        rule.setContent {
            NaryToDoTheme {
                if (visible) WorkoutsContent(listOf(squat, bench), PaddingValues(), {}, {}, { deleted += it })
            }
        }
        rule.mainClock.autoAdvance = false
        swipeAway("Squat")
        rule.mainClock.advanceTimeBy(1_000)
        visible = false
        rule.mainClock.advanceTimeBy(500)
        assertEquals(listOf(squat), deleted)
    }
}
