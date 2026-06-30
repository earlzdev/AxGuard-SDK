package com.earldev.axguard.demo.presentation.models

import androidx.compose.runtime.Immutable
import com.earldev.axguard.demo.domain.model.ScanSummary

/** Everything the checks screen renders, as a single immutable state object. */
@Immutable
data class ChecksUiState(
    /** One row per check; present from the start, filled with results after a scan. */
    val rows: List<CheckRow> = emptyList(),
    val isRunning: Boolean = false,
    val hasRun: Boolean = false,
    val summary: ScanSummary? = null,
    /** Id of the check whose detail sheet is open, or null when none is shown. */
    val selectedCheckId: Int? = null,
    /** Transient error to surface once (e.g. via snackbar), then clear. */
    val errorMessage: String? = null,
) {
    val selectedRow: CheckRow?
        get() = selectedCheckId?.let { id -> rows.firstOrNull { it.id == id } }
}
