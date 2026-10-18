package com.nary.gym.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.nary.gym.ui.theme.NaryToDoTheme

class RecordingHaptics : HapticFeedback {
    val performed = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        performed += hapticFeedbackType
    }
}

@Composable
fun NaryToDoThemeWithHaptics(haptics: HapticFeedback, content: @Composable () -> Unit) {
    NaryToDoTheme { CompositionLocalProvider(LocalHapticFeedback provides haptics, content = content) }
}
