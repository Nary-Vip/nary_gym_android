package com.nary.gym.ui.screens.Workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nary.gym.core.WorkOutType
import com.nary.gym.ui.screens.WorkoutCard
import com.nary.gym.ui.theme.grey
import com.nary.gym.viewModel.WorkOutViewModel

@Composable
fun IndividualDayWorkout(
    workOutType: WorkOutType,
    viewModel: WorkOutViewModel,
    innerPadding: PaddingValues,
    isCompletedToday: Boolean,
    onComplete: () -> Unit,
) {
    val workOuts by viewModel.allWorkouts.collectAsState()
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(10.dp))
        Text("${workOutType.title} Day", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(4.dp))
        Text("Work hard babe", fontSize = 14.sp, fontWeight = FontWeight.W300, color = grey)
        Spacer(modifier = Modifier.size(20.dp))
        Button(
            onClick = {
                onComplete()
            },
            enabled = !isCompletedToday,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                disabledContainerColor = grey
            ), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(height = 50.dp).fillMaxWidth()
        ) {
            Text(
                if (isCompletedToday) "Completed for Today" else "Mark As Complete",
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.size(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            workOuts.filter { it.workOutType == workOutType }.forEach { workOut ->
                item {
                    WorkoutCard(
                        workout = workOut
                    )
                }
            }
        }
    }
}