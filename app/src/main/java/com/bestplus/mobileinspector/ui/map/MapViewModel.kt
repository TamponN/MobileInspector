package com.bestplus.mobileinspector.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestplus.mobileinspector.domain.model.GeoPosition
import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubscriberMarker(
    val uuid: String,
    val displayName: String,
    val address: String,
    val statusTask: String,
    val position: GeoPosition,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    routeRepository: RouteRepository,
) : ViewModel() {

    private val routeUuid: String = checkNotNull(savedStateHandle["routeUuid"])

    /** Абоненты с известными GPS-координатами */
    val markers: StateFlow<List<SubscriberMarker>> =
        routeRepository.observeRouteSheets()
            .map { sheets ->
                sheets
                    .find { it.uuidDocument == routeUuid }
                    ?.subscribers
                    ?.mapNotNull { sub -> sub.toMarker() }
                    ?: emptyList()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routeName: StateFlow<String> =
        routeRepository.observeRouteSheets()
            .map { sheets -> sheets.find { it.uuidDocument == routeUuid }?.name ?: "" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private fun InfoSubscriber.toMarker(): SubscriberMarker? {
        val pos = position ?: return null
        return SubscriberMarker(
            uuid = uuid,
            displayName = displayName,
            address = address.fullAddress,
            statusTask = statusTask,
            position = pos,
        )
    }
}
