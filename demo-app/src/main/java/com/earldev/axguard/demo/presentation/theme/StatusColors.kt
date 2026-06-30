package com.earldev.axguard.demo.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.earldev.axguard.demo.domain.model.CheckStatus
import com.earldev.axguard.demo.domain.model.ScanSummary

/** A semantic status color triplet: an accent plus a matching container/on-container pair. */
@Immutable
data class StatusVisual(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

/**
 * Extended, semantic palette layered on top of the Material scheme. Status colors carry
 * meaning the base scheme can't (secure / threat / warning / neutral) and are provided per
 * theme through [LocalStatusColors].
 */
@Immutable
data class StatusColors(
    val secure: StatusVisual,
    val threat: StatusVisual,
    val warning: StatusVisual,
    val neutral: StatusVisual,
) {
    fun forStatus(status: CheckStatus): StatusVisual = when (status) {
        CheckStatus.SECURE -> secure
        CheckStatus.THREAT -> threat
        CheckStatus.TIMEOUT, CheckStatus.ERROR -> warning
        CheckStatus.UNSUPPORTED -> neutral
    }

    fun forVerdict(verdict: ScanSummary.Verdict): StatusVisual = when (verdict) {
        ScanSummary.Verdict.PROTECTED -> secure
        ScanSummary.Verdict.AT_RISK -> threat
        ScanSummary.Verdict.INCONCLUSIVE -> warning
    }
}

internal val LightStatusColors = StatusColors(
    secure = StatusVisual(Color(0xFF1B873F), Color(0xFFD7F2DF), Color(0xFF072711)),
    threat = StatusVisual(Color(0xFFC62828), Color(0xFFFBDCDC), Color(0xFF410E0E)),
    warning = StatusVisual(Color(0xFFB26A00), Color(0xFFFCE9CC), Color(0xFF3D2500)),
    neutral = StatusVisual(Color(0xFF5B6472), Color(0xFFE6E9F0), Color(0xFF232830)),
)

internal val DarkStatusColors = StatusColors(
    secure = StatusVisual(Color(0xFF6FD693), Color(0xFF123625), Color(0xFFBCF2CF)),
    threat = StatusVisual(Color(0xFFFF9088), Color(0xFF4A1918), Color(0xFFFFD7D3)),
    warning = StatusVisual(Color(0xFFFFC163), Color(0xFF41300E), Color(0xFFFFE5BE)),
    neutral = StatusVisual(Color(0xFFB0B8C6), Color(0xFF272C35), Color(0xFFD8DDE6)),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Ergonomic accessor mirroring [MaterialTheme]. */
val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
