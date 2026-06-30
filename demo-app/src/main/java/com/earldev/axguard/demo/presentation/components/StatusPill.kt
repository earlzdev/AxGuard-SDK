package com.earldev.axguard.demo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.earldev.axguard.demo.domain.model.CheckStatus
import com.earldev.axguard.demo.presentation.theme.statusIcon
import com.earldev.axguard.demo.presentation.theme.statusLabel
import com.earldev.axguard.demo.presentation.theme.statusColors

/** Compact, color-coded chip stating a check's [status]. */
@Composable
fun StatusPill(
    status: CheckStatus,
    modifier: Modifier = Modifier,
) {
    val visual = MaterialTheme.statusColors.forStatus(status)
    Pill(
        icon = statusIcon(status),
        label = statusLabel(status),
        container = visual.container,
        onContainer = visual.onContainer,
        modifier = modifier,
    )
}

/** Neutral chip shown for a check that has not been scanned yet. */
@Composable
fun PendingPill(modifier: Modifier = Modifier) {
    val visual = MaterialTheme.statusColors.neutral
    Pill(
        icon = Icons.Filled.RadioButtonUnchecked,
        label = "NOT RUN",
        container = visual.container,
        onContainer = visual.onContainer,
        modifier = modifier,
    )
}

@Composable
private fun Pill(
    icon: ImageVector,
    label: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier
                .padding(end = 5.dp)
                .size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
        )
    }
}
