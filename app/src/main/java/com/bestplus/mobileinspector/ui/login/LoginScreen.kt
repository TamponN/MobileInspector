package com.bestplus.mobileinspector.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.bestplus.mobileinspector.ui.theme.InspectorTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Экран входа / настройки подключения.
 * Повторяет C# LoginPage.xaml.cs:
 *   - Поля: адрес, база, SSL, GUID, логин, пароль
 *   - Кнопка "Подключиться"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LoginContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onAddressChange = viewModel::onAddressChange,
        onDatabaseChange = viewModel::onDatabaseChange,
        onSslToggle = viewModel::onSslToggle,
        onGuidChange = viewModel::onGuidChange,
        onLoginChange = viewModel::onLoginChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    state: LoginUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAddressChange: (String) -> Unit = {},
    onDatabaseChange: (String) -> Unit = {},
    onSslToggle: (Boolean) -> Unit = {},
    onGuidChange: (String) -> Unit = {},
    onLoginChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onLoginClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Обходчик — Подключение") })
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
            Spacer(Modifier.height(16.dp))

            Text("Настройки сервера 1С", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.address,
                onValueChange = onAddressChange,
                label = { Text("Адрес сервера (IP:порт)") },
                placeholder = { Text("192.168.1.100:8080") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = state.database,
                onValueChange = onDatabaseChange,
                label = { Text("Имя базы данных") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Авторизация", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.login,
                onValueChange = onLoginChange,
                label = { Text("Логин") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Подключиться")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, name = "Login — пустые поля")
@Composable
private fun LoginPreview() {
    InspectorTheme {
        LoginContent(state = LoginUiState())
    }
}

@Preview(showBackground = true, name = "Login — заполнено, загрузка")
@Composable
private fun LoginLoadingPreview() {
    InspectorTheme {
        LoginContent(
            state = LoginUiState(
                address = "192.168.1.100:8080",
                database = "MobileInspector",
                guid = "550e8400-e29b-41d4-a716-446655440000",
                login = "obhodchik",
                isLoading = true,
            ),
        )
    }
}
