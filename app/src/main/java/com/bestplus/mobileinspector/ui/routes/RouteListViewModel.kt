package com.bestplus.mobileinspector.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestplus.mobileinspector.domain.model.RouteSheet
import com.bestplus.mobileinspector.domain.model.SyncStatus
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteListUiState(
    val isSyncing: Boolean = false,
    val syncError: String? = null,
)

@HiltViewModel
class RouteListViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
) : ViewModel() {

    val routeSheets: StateFlow<List<RouteSheet>> =
        routeRepository.observeRouteSheets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(RouteListUiState())
    val uiState: StateFlow<RouteListUiState> = _uiState.asStateFlow()

    fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            val result = routeRepository.sync()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncError = when (result) {
                        SyncStatus.SUCCESS -> null
                        SyncStatus.ERROR_NO_INTERNET -> "Нет подключения к интернету"
                        SyncStatus.ERROR_NO_SERVER -> "Сервер 1С недоступен"
                        SyncStatus.ERROR_UUID -> "Неверный UUID"
                        else -> null
                    },
                )
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(syncError = null) }
}
