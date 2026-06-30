package com.earldev.axguard.demo.presentation.models

import androidx.compose.runtime.Immutable
import com.earldev.axguard.demo.domain.model.CheckMetadata
import com.earldev.axguard.demo.domain.model.CheckStatus
import com.earldev.axguard.demo.domain.model.SecurityCheck

/**
 * A row on the checks list. Always has [metadata]; [result] is null until the check has
 * been run, which lets the same screen render "not scanned yet" and "scanned" states in
 * place rather than swapping to a separate results view.
 */
@Immutable
data class CheckRow(
    val metadata: CheckMetadata,
    val result: SecurityCheck?,
) {
    val id: Int get() = metadata.id
    val status: CheckStatus? get() = result?.status
    val headline: String get() = result?.statusHeadline ?: "Not scanned yet"
}
