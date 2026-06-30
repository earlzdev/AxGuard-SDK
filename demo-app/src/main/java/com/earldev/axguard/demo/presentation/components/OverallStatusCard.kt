package com.earldev.axguard.demo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earldev.axguard.demo.domain.model.ScanSummary
import com.earldev.axguard.demo.presentation.theme.StatusVisual
import com.earldev.axguard.demo.presentation.theme.statusColors

/** Hero card summarizing the whole scan: verdict, headline, and per-outcome counts. */
@Composable
fun OverallStatusCard(
    summary: ScanSummary,
    modifier: Modifier = Modifier,
) {
    val visual = MaterialTheme.statusColors.forVerdict(summary.verdict)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = visual.container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(visual.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = verdictIcon(summary.verdict),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = verdictTitle(summary.verdict),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = visual.onContainer,
                    )
                    Text(
                        text = verdictSubtitle(summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = visual.onContainer,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CountTile(
                    count = summary.secure,
                    label = "Secure",
                    visual = MaterialTheme.statusColors.secure,
                    modifier = Modifier.weight(1f),
                )
                CountTile(
                    count = summary.threats,
                    label = "Threats",
                    visual = MaterialTheme.statusColors.threat,
                    modifier = Modifier.weight(1f),
                )
                CountTile(
                    count = summary.inconclusive,
                    label = "Inconclusive",
                    visual = MaterialTheme.statusColors.warning,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CountTile(
    count: Int,
    label: String,
    visual: StatusVisual,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = visual.accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun verdictIcon(verdict: ScanSummary.Verdict): ImageVector = when (verdict) {
    ScanSummary.Verdict.PROTECTED -> Icons.Filled.GppGood
    ScanSummary.Verdict.AT_RISK -> Icons.Filled.GppBad
    ScanSummary.Verdict.INCONCLUSIVE -> Icons.Filled.GppMaybe
}

private fun verdictTitle(verdict: ScanSummary.Verdict): String = when (verdict) {
    ScanSummary.Verdict.PROTECTED -> "Device protected"
    ScanSummary.Verdict.AT_RISK -> "Threats detected"
    ScanSummary.Verdict.INCONCLUSIVE -> "Review recommended"
}

private fun verdictSubtitle(summary: ScanSummary): String = when (summary.verdict) {
    ScanSummary.Verdict.PROTECTED ->
        "All ${summary.total} checks passed."
    ScanSummary.Verdict.AT_RISK ->
        "${summary.threats} of ${summary.total} checks found a threat."
    ScanSummary.Verdict.INCONCLUSIVE ->
        "${summary.inconclusive} of ${summary.total} checks were inconclusive."
}
