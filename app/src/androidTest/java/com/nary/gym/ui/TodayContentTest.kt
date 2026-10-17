package com.nary.gym.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.screens.Today.TodayContent
import com.nary.gym.ui.screens.Today.TODAY_LIST_TAG
import com.nary.gym.ui.theme.NaryToDoTheme
import com.nary.gym.viewModel.TodayUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class TodayContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val now = 1_790_000_000_000L
    private val squat = WorkOuts(id = "1", workOutName = "Squat", workOutType = WorkOutType.LEG, workOutImage = "")
    private val base = TodayUiState(
        dayType = WorkOutType.LEG, dayNumber = 1, dayCount = 4, completedToday = false,
        nextUp = WorkOutType.CHEST, exercises = listOf(squat), weekVisits = 3,
        weekMillis = Duration.ofMinutes(260).toMillis(), openVisit = null, autoCheckInReady = true,
    )

    private fun show(
        state: TodayUiState,
        onMarkComplete: () -> Unit = {},
        onCheckOut: () -> Unit = {},
        onSetUp: () -> Unit = {},
        onAdd: () -> Unit = {},
    ) = rule.setContent {
        NaryToDoTheme {
            TodayContent(
                state = state, now = now, contentPadding = PaddingValues(),
                onMarkComplete = onMarkComplete, onCheckOut = onCheckOut, onSetUpCheckIn = onSetUp,
                onExerciseClick = {}, onAddWorkout = onAdd, zone = ZoneOffset.UTC,
            )
        }
    }

    private fun scrollTo(text: String) = rule.onNodeWithTag(TODAY_LIST_TAG).performScrollToNode(hasText(text))

    @Test
    fun notCompleted_showsDayAndMarkComplete() {
        var completed = false
        show(base, onMarkComplete = { completed = true })
        rule.onNodeWithText("Leg Day").assertExists()
        rule.onNodeWithText("Work hard babe").assertExists()
        rule.onNodeWithText("Mark as complete").performClick()
        assertTrue(completed)
    }

    @Test
    fun completed_showsDoneAndNextUp_withoutButton() {
        show(base.copy(completedToday = true))
        rule.onNodeWithText("Done for today 💪").assertExists()
        rule.onNodeWithText("Next up: Chest").assertExists()
        rule.onNodeWithText("Mark as complete").assertDoesNotExist()
    }

    @Test
    fun showsWeeklyStats() {
        show(base)
        rule.onNodeWithText("3").assertExists()
        rule.onNodeWithText("4h 20m").assertExists()
    }

    @Test
    fun openVisit_showsLiveCard_andCheckOutCallsBack() {
        var checkedOut = false
        val visit = GymVisit(checkInAt = now - Duration.ofMinutes(38).toMillis(), source = VisitSource.AUTO)
        show(base.copy(openVisit = visit), onCheckOut = { checkedOut = true })
        scrollTo("Check out")
        rule.onNodeWithText("Checked in").assertExists()
        rule.onNodeWithText("Check out").performClick()
        assertTrue(checkedOut)
    }

    @Test
    fun autoCheckInNotReady_showsSetUpLink() {
        var opened = false
        show(base.copy(autoCheckInReady = false), onSetUp = { opened = true })
        scrollTo("Set up auto check-in")
        rule.onNodeWithText("Set up auto check-in").performClick()
        assertTrue(opened)
    }

    @Test
    fun noExercises_offersToAddOne() {
        var add = false
        show(base.copy(exercises = emptyList()), onAdd = { add = true })
        scrollTo("Add a Leg workout")
        rule.onNodeWithText("Add a Leg workout").performClick()
        assertTrue(add)
    }
}
