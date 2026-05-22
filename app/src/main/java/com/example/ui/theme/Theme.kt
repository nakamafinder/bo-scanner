package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = LavenderPrimary,
    secondary = LavenderSecondary,
    tertiary = LavenderTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    outline = DarkBorder,
    onPrimary = LavenderSecondary,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = LightTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LavenderSecondary,
    secondary = LavenderPrimary,
    tertiary = LavenderTertiary,
    background = LightBackground,
    surface = LightSurface,
    outline = LightBorder,
    onPrimary = Color.White,
    onSecondary = LavenderSecondary,
    onBackground = DarkText,
    onSurface = DarkText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = DarkTextSecondary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Preset signature identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
