package com.earldev.axguard.demo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.earldev.axguard.demo.domain.SecurityChecker
import com.earldev.axguard.demo.domain.catalog.CheckCatalog
import com.earldev.axguard.demo.domain.model.ScanSummary
import com.earldev.axguard.demo.presentation.models.CheckRow
import com.earldev.axguard.demo.presentation.models.ChecksUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the checks-screen state and runs scans through the [SecurityChecker].
 *
 * @property checker security checker
 */
class ChecksViewModel(
    private val checker: SecurityChecker,
) : ViewModel() {

    private val _uiState: MutableStateFlow<ChecksUiState> = MutableStateFlow(
        ChecksUiState(
            rows = CheckCatalog.all.map {
                CheckRow(
                    metadata = it,
                    result = null,
                )
            }
        ),
    )
    val uiState: StateFlow<ChecksUiState> = _uiState.asStateFlow()

    fun runChecks() {
        if (_uiState.value.isRunning) return

        _uiState.update { state ->
            state.copy(
                isRunning = true,
                summary = null,
                rows = state.rows.map { it.copy(result = null) }.sortedBy { it.id },
            )
        }
        viewModelScope.launch {
            try {
                delay(SCAN_MIN_VISIBLE_MS)
                val results = checker.runAllChecks().associateBy { it.id }
                _uiState.update { state ->
                    state.copy(
                        isRunning = false,
                        hasRun = true,
                        rows = state.rows
                            .map { it.copy(result = results[it.id]) }
                            .sortedWith(
                                compareBy(
                                    { it.status?.sortPriority ?: Int.MAX_VALUE },
                                    { it.id },
                                ),
                            ),
                        summary = ScanSummary.from(results.values.toList()),
                    )
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                failWith("The scan was interrupted before it finished.")
            } catch (invalid: IllegalArgumentException) {
                failWith(invalid.message ?: "The scan configuration was rejected.")
            }
        }
    }

    fun selectCheck(id: Int) {
        _uiState.update { it.copy(selectedCheckId = id) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(selectedCheckId = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun failWith(message: String) {
        _uiState.update { it.copy(isRunning = false, errorMessage = message) }
    }

    companion object {
        /**
         * Minimum time the scanning state stays on screen so the loader stays visible.
         */
        private const val SCAN_MIN_VISIBLE_MS = 1_000L

        /**
         * Wires the checker from the application context; the demo has no DI framework.
         */
        val Factory = viewModelFactory {
            initializer {
                ChecksViewModel(SecurityChecker(this[APPLICATION_KEY]!!))
            }
        }
    }
}
