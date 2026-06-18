package com.nary.gym.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.nary.gym.MainActivity
import com.nary.gym.utils.WorkoutCycle

class WorkoutWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        
        val workoutTitle = try {
            val workoutCycle = WorkoutCycle(context)
            workoutCycle.getCurrentDayWorkout().title.uppercase()
        } catch (e: Exception) {
            Log.e("WorkoutWidget", "Error getting workout", e)
            "ERROR"
        }

        provideContent {
            Box(modifier = GlanceModifier.background(Color.Black).cornerRadius(10.dp)) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "TODAY'S WORKOUT", style = TextStyle(color = ColorProvider(Color.White, Color.White), textAlign = TextAlign.Center))
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    Text(text = workoutTitle, style = TextStyle(color = ColorProvider(Color.White,
                        Color.White), fontWeight = FontWeight.Bold
                    ))
                    Spacer(modifier = GlanceModifier.height(22.dp))
                    Button(
                        modifier = GlanceModifier.fillMaxWidth().background(Color.Gray),
                        text = "VIEW",
                        style = TextStyle(color = ColorProvider(Color.White, Color.White)),
                        onClick = actionRunCallback<CompleteWorkoutAction>()
                    )
                }
            }
        }
    }
}

class CompleteWorkoutAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("source", "notification_yes")
        }
        context.startActivity(intent)
    }
}

class WorkoutOfTheDayReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkoutWidget()
}
