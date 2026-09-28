package com.nary.gym.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nary.gym.R

@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: FontWeight) = Font(
    resId = R.font.space_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val SpaceGrotesk = FontFamily(
    spaceGrotesk(FontWeight.Normal),
    spaceGrotesk(FontWeight.Medium),
    spaceGrotesk(FontWeight.Bold),
)

private val m3 = Typography()

private fun TextStyle.display() = copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
private fun TextStyle.title() = copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium)
private fun TextStyle.body() = copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal)

val NaryTypography = Typography(
    displayLarge = m3.displayLarge.display(),
    displayMedium = m3.displayMedium.display(),
    displaySmall = m3.displaySmall.display(),
    headlineLarge = m3.headlineLarge.display(),
    headlineMedium = m3.headlineMedium.display(),
    headlineSmall = m3.headlineSmall.display(),
    titleLarge = m3.titleLarge.title(),
    titleMedium = m3.titleMedium.title(),
    titleSmall = m3.titleSmall.title(),
    bodyLarge = m3.bodyLarge.body(),
    bodyMedium = m3.bodyMedium.body(),
    bodySmall = m3.bodySmall.body(),
    labelLarge = m3.labelLarge.title(),
    labelMedium = m3.labelMedium.title(),
    labelSmall = m3.labelSmall.title(),
)
