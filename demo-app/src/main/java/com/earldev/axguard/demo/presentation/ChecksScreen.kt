package com.earldev.axguard.demo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.earldev.axguard.demo.presentation.components.CheckDetailSheet
import com.earldev.axguard.demo.presentation.components.CheckListItem
import com.earldev.axguard.demo.presentation.components.IntroHeader
import com.earldev.axguard.demo.presentation.components.OverallStatusCard
import com.earldev.axguard.demo.presentation.models.ChecksUiState

/**
 * The whole demo is one screen: a persistent header and check list that move through
 * idle → scanning → results states in place, with a single Run action pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecksScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChecksViewModel = viewModel(factory = ChecksViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val topBarScroll = TopAppBarDefaults.enterAlwaysScrollBehavior()

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(topBarScroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("AxGuard", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme",
                        )
                    }
                },
                scrollBehavior = topBarScroll,
            )
        },
        bottomBar = {
            // Hidden while scanning so the loader stands alone.
            if (!uiState.isRunning) {
                RunBar(hasRun = uiState.hasRun, onRun = viewModel::runChecks)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            ChecksList(
                uiState = uiState,
                contentPadding = innerPadding,
                onCheckClick = viewModel::selectCheck,
            )
        }
    }

    uiState.selectedRow?.let { selected ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        CheckDetailSheet(
            row = selected,
            sheetState = sheetState,
            onDismiss = viewModel::dismissDetail,
        )
    }
}

@Composable
private fun ChecksList(
    uiState: ChecksUiState,
    contentPadding: PaddingValues,
    onCheckClick: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            if (uiState.summary != null) {
                OverallStatusCard(uiState.summary, Modifier.padding(bottom = 6.dp))
            } else {
                IntroHeader(Modifier.padding(bottom = 6.dp))
            }
        }
        items(uiState.rows, key = { it.id }) { row ->
            CheckListItem(
                row = row,
                onClick = { onCheckClick(row.id) },
            )
        }
    }
}

@Composable
private fun RunBar(
    hasRun: Boolean,
    onRun: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Button(
            onClick = onRun,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
        ) {
            Icon(
                imageVector = if (hasRun) Icons.Filled.Refresh else Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (hasRun) "Re-run checks" else "Run checks",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
