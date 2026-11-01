package com.ciyuan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Pink = Color(0xFFFB7299)
val PinkSoft = Color(0xFFFFF1F5)
val Ink = Color(0xFF18191C)
val Ink2 = Color(0xFF61666D)
val Ink3 = Color(0xFF9499A0)
val Bg = Color(0xFFF6F7F8)
val Line = Color(0xFFE9EBEE)
val Blue = Color(0xFF00AEEC)

private val Colors = lightColorScheme(
    primary = Pink,
    onPrimary = Color.White,
    primaryContainer = PinkSoft,
    onPrimaryContainer = Pink,
    secondary = Blue,
    background = Bg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F2F3),
    onSurfaceVariant = Ink2,
    outline = Line,
    outlineVariant = Line
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink),
    bodyLarge = TextStyle(fontSize = 15.sp, color = Ink),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Ink2),
    bodySmall = TextStyle(fontSize = 12.sp, color = Ink3),
    labelSmall = TextStyle(fontSize = 11.sp, color = Ink3)
)

@Composable
fun CiyuanTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme()
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
