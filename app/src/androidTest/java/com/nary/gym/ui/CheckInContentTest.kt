package com.nary.gym.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.data.local.GymLocation
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.ui.screens.CheckIn.CHECK_IN_LIST_TAG
import com.nary.gym.ui.screens.CheckIn.CheckInContent
import com.nary.gym.ui.theme.NaryToDoTheme
import com.nary.gym.utils.formatClockTime
import com.nary.gym.utils.formatDayLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class CheckInContentTest {

    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneOffset.UTC
    private val now = 1_790_000_000_000L
    private val oneDay = Duration.ofDays(1).toMillis()
    private val today = GymVisit(
        id = "a",
        checkInAt = now - Duration.ofHours(3).toMillis(),
        checkOutAt = now - Duration.ofHours(2).toMillis(),
        source = VisitSource.AUTO,
    )
    private val yesterday = GymVisit(
        id = "b",
        checkInAt = today.checkInAt - oneDay,
        checkOutAt = today.checkOutAt!! - oneDay,
        source = VisitSource.MANUAL,
    )

    private fun show(
        visits: List<GymVisit> = listOf(today, yesterday),
        openVisit: GymVisit? = null,
        onCheckIn: () -> Unit = {},
        onCheckOut: () -> Unit = {},
        onDeleteVisit: (String) -> Unit = {},
        fontScale: Float = 1f,
    ) = rule.setContent {
        WithFontScale(fontScale) { NaryToDoTheme {
            CheckInContent(
                visits = visits,
                openVisit = openVisit,
                gym = GymLocation(12.97, 77.59, 150f),
                autoCheckInEnabled = false,
                isLocating = false,
                now = now,
                zone = zone,
                contentPadding = PaddingValues(),
                onCheckIn = onCheckIn,
                onCheckOut = onCheckOut,
                onUseCurrentLocation = {},
                onRadiusSelected = {},
                onEnableAutoCheckIn = {},
                onDisableAutoCheckIn = {},
                onDeleteVisit = onDeleteVisit,
            )
        } }
    }

    private fun scrollTo(matcher: SemanticsMatcher) =
        rule.onNodeWithTag(CHECK_IN_LIST_TAG).performScrollToNode(matcher)

    @Test
    fun statusCard_atDoubleFontSize_doesNotBreakWords() {
        show(fontScale = 2f)
        listOf("Not at the gym", "Check in yourself, or let auto check-in do it.").forEach { text ->
            assertEquals(text, emptyList<String>(), rule.onNodeWithText(text).textLayout().linesBrokenInsideAWord())
        }
    }

    @Test
    fun liveCard_atDoubleFontSize_doesNotBreakWords() {
        val open = GymVisit(id = "o", checkInAt = now - Duration.ofMinutes(38).toMillis(), source = VisitSource.AUTO)
        show(openVisit = open, fontScale = 2f)
        listOf("Checked in", "Since ${formatClockTime(open.checkInAt, zone)} · 38m").forEach { text ->
            assertEquals(text, emptyList<String>(), rule.onNodeWithText(text).textLayout().linesBrokenInsideAWord())
        }
    }

    @Test
    fun notCheckedIn_offersCheckInNow() {
        var checkedIn = false
        show(onCheckIn = { checkedIn = true })
        rule.onNodeWithText("Checked in").assertDoesNotExist()
        rule.onNodeWithText("Check in now").performClick()
        assertTrue(checkedIn)
    }

    @Test
    fun openVisit_showsLiveCard_andChecksOut() {
        var checkedOut = false
        val open = GymVisit(id = "o", checkInAt = now - Duration.ofMinutes(38).toMillis(), source = VisitSource.AUTO)
        show(openVisit = open, onCheckOut = { checkedOut = true })
        rule.onNodeWithText("Check in now").assertDoesNotExist()
        rule.onNodeWithText("Since ${formatClockTime(open.checkInAt, zone)} · 38m").assertExists()
        rule.onNodeWithText("Check out").performClick()
        assertTrue(checkedOut)
    }

    @Test
    fun history_isGroupedByDay() {
        show()
        val todayLabel = formatDayLabel(today.checkInAt, zone)
        val yesterdayLabel = formatDayLabel(yesterday.checkInAt, zone)
        scrollTo(hasText(todayLabel))
        rule.onNodeWithText(todayLabel).assertExists()
        scrollTo(hasText(yesterdayLabel))
        rule.onNodeWithText(yesterdayLabel).assertExists()
        rule.onNodeWithText("Manual").assertExists()
    }

    @Test
    fun deleteVisit_asksFirst_cancelKeeps_confirmDeletes() {
        val deleted = mutableListOf<String>()
        show(visits = listOf(today), onDeleteVisit = { deleted += it })
        val deleteIcon = hasContentDescription("Delete visit on ${formatDayLabel(today.checkInAt, zone)}")
        scrollTo(deleteIcon)

        rule.onNode(deleteIcon).performClick()
        rule.onNodeWithText("Delete visit?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(emptyList<String>(), deleted)

        rule.onNode(deleteIcon).performClick()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("a"), deleted)
    }
}
