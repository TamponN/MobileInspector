package com.bestplus.mobileinspector.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.bestplus.mobileinspector.ui.theme.InspectorTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Экран настроек.
 * Повторяет C# SettingsPage.xaml.cs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("Настройки сохранены")
            viewModel.clearSaved()
        }
    }

    SettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddressChange = viewModel::onAddressChange,
        onDatabaseChange = viewModel::onDatabaseChange,
        onSslToggle = viewModel::onSslToggle,
        onGuidChange = viewModel::onGuidChange,
        onSyncIntervalChange = viewModel::onSyncIntervalChange,
        onSave = viewModel::save,
        onLogout = viewModel::logout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onDatabaseChange: (String) -> Unit = {},
    onSslToggle: (Boolean) -> Unit = {},
    onGuidChange: (String) -> Unit = {},
    onSyncIntervalChange: (Int) -> Unit = {},
    onSave: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Сервер 1С", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.address,
                onValueChange = onAddressChange,
                label = { Text("Адрес сервера (IP:порт)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.database,
                onValueChange = onDatabaseChange,
                label = { Text("Имя базы данных") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(checked = state.useSsl, onCheckedChange = onSslToggle)
                Text("Использовать SSL (HTTPS)")
            }

            OutlinedTextField(
                value = state.guid,
                onValueChange = onGuidChange,
                label = { Text("UUID устройства") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Синхронизация", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.syncInterval.toString(),
                onValueChange = { onSyncIntervalChange(it.toIntOrNull() ?: 10) },
                label = { Text("Интервал синхронизации (мин)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("Сохранить")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Выйти из аккаунта")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ──────────────────────────── Previews ─────────────────────────────────────

@Preview(showBackground = true, name = "Settings — пустые")
@Composable
private fun SettingsPreview() {
    InspectorTheme {
        SettingsContent(state = SettingsUiState())
    }
}

@Preview(showBackground = true, name = "Settings — заполнено")
@Composable
private fun SettingsFilledPreview() {
    InspectorTheme {
        SettingsContent(
            state = SettingsUiState(
                address = "192.168.1.100:8080",
                database = "MobileInspector",
                useSsl = false,
                guid = "550e8400-e29b-41d4-a716-446655440000",
                syncInterval = 15,
            ),
        )
    }
}
