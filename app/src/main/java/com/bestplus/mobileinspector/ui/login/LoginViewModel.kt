package com.bestplus.mobileinspector.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.bestplus.mobileinspector.domain.model.ServerSettings
import com.bestplus.mobileinspector.domain.model.SyncStatus
import com.bestplus.mobileinspector.domain.model.UserSession
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import com.bestplus.mobileinspector.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val address: String = "",
    val database: String = "",
    val useSsl: Boolean = false,
    val guid: String = "",
    val login: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getServerSettings()
            val session = settingsRepository.getUserSession()
            _state.update {
                it.copy(
                    address = settings.address,
                    database = settings.databaseName,
                    useSsl = settings.useSsl,
                    guid = settings.guid,
                    login = session?.login.orEmpty(),
                )
            }
        }
    }

    fun applyQrData(address: String, database: String, ssl: Boolean, uuid: String) {
        Log.d("LoginViewModel", "applyQrData: address=$address database=$database ssl=$ssl uuid=$uuid")
        _state.update {
            it.copy(address = address, database = database, useSsl = ssl, guid = uuid)
        }
    }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value) }
    fun onDatabaseChange(value: String) = _state.update { it.copy(database = value) }
    fun onSslToggle(value: Boolean) = _state.update { it.copy(useSsl = value) }
    fun onGuidChange(value: String) = _state.update { it.copy(guid = value) }
    fun onLoginChange(value: String) = _state.update { it.copy(login = value) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }

    fun onLoginClick() {
        val s = _state.value
        if (s.address.isBlank() || s.database.isBlank() || s.guid.isBlank()) {
            _state.update { it.copy(errorMessage = "Заполните адрес сервера, базу данных и GUID") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // Сохранить настройки
            settingsRepository.saveServerSettings(
                ServerSettings(
                    address = s.address.trim(),
                    databaseName = s.database.trim(),
                    useSsl = s.useSsl,
                    guid = s.guid.trim(),
                ),
            )
            settingsRepository.saveUserSession(
                UserSession(
                    login = s.login.trim(),
                    token = android.util.Base64.encodeToString(
                        "${s.login.trim()}:${s.password}".toByteArray(),
                        android.util.Base64.NO_WRAP,
                    ),
                    guid = s.guid.trim(),
                ),
            )

            // Попробовать синхронизацию для проверки подключения
            val result = routeRepository.sync()
            _state.update {
                when (result) {
                    SyncStatus.SUCCESS -> it.copy(isLoading = false, isLoggedIn = true)
                    SyncStatus.ERROR_NO_INTERNET -> it.copy(
                        isLoading = false,
                        errorMessage = "Нет подключения к интернету",
                    )
                    SyncStatus.ERROR_NO_SERVER -> it.copy(
                        isLoading = false,
                        errorMessage = "Сервер 1С недоступен. Проверьте адрес и порт",
                    )
                    SyncStatus.ERROR_UUID -> it.copy(
                        isLoading = false,
                        errorMessage = "Неверный UUID. Проверьте настройки",
                    )
                    else -> it.copy(isLoading = false, errorMessage = "Ошибка подключения")
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
