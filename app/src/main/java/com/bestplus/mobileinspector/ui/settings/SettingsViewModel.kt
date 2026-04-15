package com.bestplus.mobileinspector.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestplus.mobileinspector.domain.model.ServerSettings
import com.bestplus.mobileinspector.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val address: String = "",
    val database: String = "",
    val useSsl: Boolean = false,
    val guid: String = "",
    val syncInterval: Int = 10,
    val isSaved: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getServerSettings()
            val interval = settingsRepository.getSyncIntervalMinutes()
            _state.update {
                it.copy(
                    address = settings.address,
                    database = settings.databaseName,
                    useSsl = settings.useSsl,
                    guid = settings.guid,
                    syncInterval = interval,
                )
            }
        }
    }

    fun onAddressChange(v: String) = _state.update { it.copy(address = v) }
    fun onDatabaseChange(v: String) = _state.update { it.copy(database = v) }
    fun onSslToggle(v: Boolean) = _state.update { it.copy(useSsl = v) }
    fun onGuidChange(v: String) = _state.update { it.copy(guid = v) }
    fun onSyncIntervalChange(v: Int) = _state.update { it.copy(syncInterval = v) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            settingsRepository.saveServerSettings(
                ServerSettings(
                    address = s.address.trim(),
                    databaseName = s.database.trim(),
                    useSsl = s.useSsl,
                    guid = s.guid.trim(),
                ),
            )
            settingsRepository.setSyncIntervalMinutes(s.syncInterval)
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsRepository.clearUserSession()
        }
    }

    fun clearSaved() = _state.update { it.copy(isSaved = false) }
}
