package com.nary.gym.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density

@Composable
fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale), content = content)
}

fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
    return results.first()
}

fun TextLayoutResult.linesBrokenInsideAWord(): List<String> {
    val text = layoutInput.text.text
    return (0 until lineCount - 1).mapNotNull { line ->
        val end = getLineEnd(line)
        val brokenWord = end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
        if (brokenWord) text.substring(getLineStart(line), end) else null
    }
}
