package com.nary.gym.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nary.gym.MainActivity
import com.nary.gym.utils.RotationDay
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.utils.rotationDay
import com.nary.gym.utils.rotationDayUpdates

class WorkoutWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cycle = WorkoutCycle(context)
        provideContent {
            val updates = remember { cycle.rotationDayUpdates() }
            val day by updates.collectAsState(initial = cycle.rotationDay())
            GlanceTheme(colors = VoltWidgetColors) { WorkoutWidgetContent(day) }
        }
    }
}

@Composable
fun WorkoutWidgetContent(day: RotationDay) {
    val onContainer = GlanceTheme.colors.onPrimaryContainer
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (day.completedToday) {
            Text("Done ✓", style = TextStyle(color = onContainer, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            Text("Next: ${day.nextUp.title}", style = TextStyle(color = onContainer, fontSize = 14.sp))
        } else {
            Text("TODAY", style = TextStyle(color = onContainer, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Text("${day.dayType.title} Day", style = TextStyle(color = onContainer, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(12.dp))
            Button(
                text = "Mark complete",
                onClick = actionRunCallback<CompleteWorkoutAction>(),
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = onContainer,
                    contentColor = GlanceTheme.colors.primaryContainer,
                ),
            )
        }
    }
}

class CompleteWorkoutAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WorkoutCycle(context).moveToNextDayWorkOut()
        WorkoutWidget().updateAll(context)
    }
}

class WorkoutOfTheDayReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkoutWidget()
}
