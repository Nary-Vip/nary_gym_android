package com.example.narytodo.ui.screens

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.narytodo.data.room_db.TaskItem
import com.example.narytodo.ui.theme.grey
import com.example.narytodo.viewModel.TaskViewModel

@Composable
fun ToDoScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.allTasks.collectAsState()
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }

    var showEditorDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showEditorDialog = true
                }, shape = RoundedCornerShape(20.dp),
                containerColor = grey,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Button")
                Text("New Tasks", modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),

            ) {

            Text(
                text = "Nary Tasks",
                modifier = Modifier.padding(top = 32.dp),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text("${tasks.filter { !it.isDone }.size} remaining today", color = Color.Gray)

            Spacer(modifier = Modifier.size(20.dp))

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Tasks created.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        TaskItemUi(item = task, onEditClick = {
                            taskToEdit = task
                            showEditorDialog = true
                        }, onDeleteClick = {
                            viewModel.deleteTask(task)
                        }, onCheckChange = { checked ->
                            viewModel.updateTask(task.copy(isDone = checked))
                        })
                    }
                }
            }
        }
    }

    if(showEditorDialog){
        TaskEditorDialog(task= taskToEdit,onSave = { name->
            if(taskToEdit == null){
                viewModel.addTask(TaskItem(title = name, isDone = false))
            } else {
                taskToEdit?.let {currTask->
                    viewModel.updateTask(currTask.copy(title = name))
                }
            }
            taskToEdit = null
            showEditorDialog = false
        }, onCancel = {showEditorDialog = false})
    }

}