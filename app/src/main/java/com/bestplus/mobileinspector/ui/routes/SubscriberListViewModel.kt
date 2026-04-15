package com.bestplus.mobileinspector.ui.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriberListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    private val routeUuid: String = checkNotNull(savedStateHandle["routeUuid"])

    val subscribers: StateFlow<List<InfoSubscriber>> =
        routeRepository.observeRouteSheets()
            .map { sheets ->
                sheets
                    .find { it.uuidDocument == routeUuid }
                    ?.subscribers
                    .orEmpty()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routeName: StateFlow<String> =
        routeRepository.observeRouteSheets()
            .map { sheets -> sheets.find { it.uuidDocument == routeUuid }?.name.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.update { true }
            routeRepository.sync()
            _isRefreshing.update { false }
        }
    }
}
