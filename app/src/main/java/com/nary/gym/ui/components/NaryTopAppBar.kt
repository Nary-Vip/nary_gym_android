package com.nary.gym.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaryTopAppBar(
    title: String,
    avatarName: String?,
    avatarImageUrl: String?,
    onAvatarClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        actions = {
            IconButton(
                onClick = onAvatarClick,
                modifier = Modifier.semantics { contentDescription = "Profile" },
            ) {
                NaryAvatar(name = avatarName, imageUrl = avatarImageUrl)
            }
        },
    )
}
