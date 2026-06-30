package com.earldev.axguard.demo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earldev.axguard.demo.domain.model.CheckSignal
import com.earldev.axguard.demo.domain.model.Evidence
import com.earldev.axguard.demo.presentation.models.CheckRow
import com.earldev.axguard.demo.presentation.theme.checkIcon
import com.earldev.axguard.demo.presentation.theme.statusColors

/** Detail sheet shown when a check row is tapped. Explains the check and, once scanned, its outcome. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckDetailSheet(
    row: CheckRow,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SheetHeader(row)

            DetailSection(title = "What this checks", body = row.metadata.whatItChecks)
            DetailSection(title = "Why it matters", body = row.metadata.whyItMatters)

            row.result?.let { result ->
                if (result.signals.isNotEmpty()) SignalsSection(result.signals)
                if (result.evidence.isNotEmpty()) EvidenceSection(result.evidence)
            }

            RecommendationSection(row.metadata.recommendation)
        }
    }
}

@Composable
private fun SheetHeader(row: CheckRow) {
    val statusColors = MaterialTheme.statusColors
    val visual = row.status?.let(statusColors::forStatus) ?: statusColors.neutral
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(visual.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = checkIcon(row.id),
                contentDescription = null,
                tint = visual.onContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                text = row.metadata.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.metadata.category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(visual.container)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.status != null) StatusPill(status = row.status!!) else PendingPill()
        Text(
            text = if (row.status != null) row.headline else "Run the checks to evaluate this item.",
            style = MaterialTheme.typography.bodyMedium,
            color = visual.onContainer,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(title)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SignalsSection(signals: List<CheckSignal>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val triggered = signals.count { it.triggered }
        SectionLabel("Signals · $triggered of ${signals.size} triggered")
        signals.forEach { SignalRow(it) }
    }
}

@Composable
private fun SignalRow(signal: CheckSignal) {
    val statusColors = MaterialTheme.statusColors
    val tint = if (signal.triggered) statusColors.threat.accent
    else MaterialTheme.colorScheme.outline
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (signal.triggered) Icons.Filled.Circle
            else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(if (signal.triggered) 12.dp else 14.dp),
        )
        Text(
            text = signal.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (signal.triggered) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (signal.triggered) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun EvidenceSection(evidence: List<Evidence>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Evidence")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            evidence.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationSection(recommendation: String) {
    val visual = MaterialTheme.statusColors.neutral
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("Recommendation")
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodyMedium,
            color = visual.onContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(visual.container)
                .padding(14.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
