package com.nary.gym.ui.screens.Workouts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nary.gym.core.WorkOutType
import com.nary.gym.data.room_db.WorkOuts
import com.nary.gym.ui.components.NaryTextField
import com.nary.gym.utils.copyImageToInternalStorage
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditor(
    workout: WorkOuts?,
    onSave: (WorkOuts) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((WorkOuts) -> Unit)? = null,
    initialType: WorkOutType = WorkOutType.CHEST,
    existingNames: Set<String> = emptySet(),
    pickPhoto: ((onPicked: (Uri) -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    var draft by remember {
        mutableStateOf(
            workout ?: WorkOuts(id = "", workOutName = "", workOutType = initialType, workOutImage = "")
        )
    }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var processingPhoto by remember { mutableStateOf(false) }
    var pickedPhoto by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val name = draft.workOutName.trim()
    val nameError = when {
        name.length in 1..2 -> "At least 3 characters"
        name in existingNames -> "You already have a workout with this name"
        else -> null
    }
    val canSave = name.length >= 3 && nameError == null && (workout != null || draft.workOutImage.isNotBlank()) && !processingPhoto
    var confirmDelete by remember { mutableStateOf(false) }

    fun usePickedPhoto(uri: Uri) {
        scope.launch {
            processingPhoto = true
            try {
                copyImageToInternalStorage(context = context, uri = uri)
                    .takeIf { it.isNotBlank() }
                    ?.let { path ->
                        pickedPhoto?.let { File(it).delete() }
                        pickedPhoto = path
                        draft = draft.copy(workOutImage = path)
                    }
            } finally {
                processingPhoto = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { if (!saved) pickedPhoto?.let { File(it).delete() } }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(::usePickedPhoto)
    }
    val choosePhoto = { pickPhoto?.invoke(::usePickedPhoto) ?: imagePicker.launch("image/*") }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (workout == null) "Add workout" else "Edit workout",
                style = MaterialTheme.typography.headlineSmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = draft.workOutImage.ifBlank { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.width(16.dp))
                OutlinedButton(onClick = choosePhoto, enabled = !processingPhoto) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            processingPhoto -> "Preparing photo…"
                            draft.workOutImage.isBlank() -> "Add photo"
                            else -> "Change photo"
                        }
                    )
                }
            }
            NaryTextField(
                value = draft.workOutName,
                onValueChange = { draft = draft.copy(workOutName = it) },
                label = "Exercise name",
                leadingIcon = Icons.Filled.FitnessCenter,
                placeholder = "Chest press",
                error = nameError,
            )
            ExposedDropdownMenuBox(expanded = typeMenuOpen, onExpandedChange = { typeMenuOpen = it }) {
                OutlinedTextField(
                    value = draft.workOutType.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Muscle group") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    WorkOutType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.title) },
                            onClick = {
                                draft = draft.copy(workOutType = type)
                                typeMenuOpen = false
                            },
                        )
                    }
                }
            }
            Button(
                onClick = {
                    saved = true
                    onSave(draft.copy(workOutName = name))
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save workout") }
            if (workout != null && onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Delete workout") }
            }
        }
    }

    if (confirmDelete && workout != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete workout?") },
            text = { Text("\"${workout.workOutName}\" will be permanently deleted. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; onDelete(workout) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
