package com.example.narytodo.ui.screens.Workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.narytodo.data.room_db.TaskItem
import com.example.narytodo.ui.common_widgets.ProfileField
import com.example.narytodo.ui.theme.grey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(task: TaskItem?, onSave: (String)-> Unit, onCancel: ()-> Unit) {
    var taskName by remember { mutableStateOf(task?.title ?: "") }
    ModalBottomSheet(
        onDismissRequest = {
            onCancel()
        },
        containerColor = Color.White,

        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(if(task == null) "Create New Task" else "Update Task", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(10.dp))
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Task Name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = grey,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.size(16.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {onSave(taskName.trim())}, modifier = Modifier.size(140.dp, 50.dp),
                    enabled = taskName.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Save Task", fontSize = 16.sp)
                }
            }
        }
    }
}