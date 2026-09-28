package com.nary.gym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun NaryToDoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VoltDarkColors else VoltLightColors,
        typography = NaryTypography,
        shapes = NaryShapes,
        content = content
    )
}
