package com.nary.gym.widget

import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasRunCallbackClickAction
import androidx.glance.testing.unit.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nary.gym.core.WorkOutType
import com.nary.gym.utils.rotationDay
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutWidgetContentTest {

    @Test
    fun notDone_showsTodayAndMarkComplete() = runGlanceAppWidgetUnitTest {
        provideComposable { WorkoutWidgetContent(rotationDay(WorkOutType.LEG, completedToday = false)) }
        onNode(hasText("TODAY")).assertExists()
        onNode(hasText("Leg Day")).assertExists()
        onNode(hasText("Mark complete")).assert(hasRunCallbackClickAction<CompleteWorkoutAction>())
        onNode(hasText("Done ✓")).assertDoesNotExist()
    }

    @Test
    fun done_showsDoneAndNextUp_withoutButton() = runGlanceAppWidgetUnitTest {
        provideComposable { WorkoutWidgetContent(rotationDay(WorkOutType.CHEST, completedToday = true)) }
        onNode(hasText("Done ✓")).assertExists()
        onNode(hasText("Next: Chest")).assertExists()
        onNode(hasText("Mark complete")).assertDoesNotExist()
    }
}
