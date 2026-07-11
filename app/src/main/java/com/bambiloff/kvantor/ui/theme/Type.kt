package com.bambiloff.kvantor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bambiloff.kvantor.R

val Rubik = FontFamily(
    Font(R.font.rubik_regular, FontWeight.Normal),
    Font(R.font.rubik_italic, FontWeight.Normal)
)

private fun rubikStyle(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Int = size + 6
) = TextStyle(
    fontFamily = Rubik,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayMedium = rubikStyle(size = 44, weight = FontWeight.Bold, lineHeight = 50),
    headlineLarge = rubikStyle(size = 32, weight = FontWeight.Bold, lineHeight = 38),
    headlineMedium = rubikStyle(size = 28, weight = FontWeight.Bold, lineHeight = 34),
    headlineSmall = rubikStyle(size = 24, weight = FontWeight.SemiBold, lineHeight = 30),
    titleLarge = rubikStyle(size = 22, weight = FontWeight.Bold, lineHeight = 28),
    titleMedium = rubikStyle(size = 18, weight = FontWeight.SemiBold, lineHeight = 24),
    titleSmall = rubikStyle(size = 16, weight = FontWeight.SemiBold, lineHeight = 22),
    bodyLarge = rubikStyle(size = 16, lineHeight = 24),
    bodyMedium = rubikStyle(size = 14, lineHeight = 21),
    bodySmall = rubikStyle(size = 12, lineHeight = 18),
    labelLarge = rubikStyle(size = 14, weight = FontWeight.SemiBold, lineHeight = 18),
    labelMedium = rubikStyle(size = 12, weight = FontWeight.SemiBold, lineHeight = 16),
    labelSmall = rubikStyle(size = 11, weight = FontWeight.Medium, lineHeight = 14)
)
