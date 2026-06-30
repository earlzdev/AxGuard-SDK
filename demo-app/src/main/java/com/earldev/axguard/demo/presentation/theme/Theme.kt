package com.earldev.axguard.demo.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * App theme. Uses a hand-tuned brand palette (dynamic color is intentionally off so the
 * demo presents a consistent identity) and provides the extended [StatusColors] alongside
 * the Material scheme.
 *
 * @param darkTheme whether to render the dark palette.
 */
@Composable
fun AxGuardTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
