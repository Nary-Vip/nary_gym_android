package com.nary.gym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun NaryToDoTheme(
    // Light-only for now: the old screens still use the fixed light palette in Color.kt and
    // would be unreadable in dark mode. Part 2 of the M3 redesign moves every screen onto the
    // tokens and then switches this to isSystemInDarkTheme() (see the redesign spec §6).
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VoltDarkColors else VoltLightColors,
        typography = Typography,
        content = content
    )
}
