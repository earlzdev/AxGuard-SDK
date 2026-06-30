package com.earldev.axguard.demo.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Brand (navy blue, seeded from the launcher icon #0F1525) ────────────────────
private val Navy = Color(0xFF0F1525) // the icon color itself
private val Blue40 = Color(0xFF263A5E)
private val Blue80 = Color(0xFFAFC6FF)
private val Blue90 = Color(0xFFD9E2FF)

internal val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Navy,
    secondary = Color(0xFF545F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E2F5),
    onSecondaryContainer = Color(0xFF111C2B),
    background = Color(0xFFF8FAFF),
    onBackground = Navy,
    surface = Color(0xFFF8FAFF),
    onSurface = Navy,
    surfaceVariant = Color(0xFFDFE2EC),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF737780),
    outlineVariant = Color(0xFFC3C6CF),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

internal val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF14264A),
    primaryContainer = Color(0xFF2C3E62),
    onPrimaryContainer = Blue90,
    secondary = Color(0xFFBCC7DC),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD8E2F5),
    background = Navy, // tie the dark theme back to the icon
    onBackground = Color(0xFFE2E6EE),
    surface = Color(0xFF141B2C),
    onSurface = Color(0xFFE2E6EE),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF2A2F3A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
