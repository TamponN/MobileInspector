package com.bestplus.mobileinspector.ui.inspection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestplus.mobileinspector.domain.model.ActAccess
import com.bestplus.mobileinspector.domain.model.ActCheck
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

data class InspectionUiState(
    val isSaving: Boolean = false,
    val savedMessage: String? = null,
    val actCheckPendingDeviceKey: String? = null,
    val actCheckDraft: ActCheck? = null,
)

@HiltViewModel
class InspectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    private val routeUuid: String = checkNotNull(savedStateHandle["routeUuid"])
    private val subscriberUuid: String = checkNotNull(savedStateHandle["subscriberUuid"])

    val subscriber: StateFlow<InfoSubscriber?> =
        routeRepository.observeRouteSheets()
            .map { sheets ->
                sheets
                    .find { it.uuidDocument == routeUuid }
                    ?.subscribers
                    ?.find { it.uuid == subscriberUuid }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState.asStateFlow()

    fun updateTestimony(
        deviceKey: String,
        scaleKey: String,
        testimonyKey: String,
        value: String,
        picturePath: String? = null,
    ) {
        viewModelScope.launch {
            routeRepository.updateTestimony(
                routeUuid = routeUuid,
                subscriberUuid = subscriberUuid,
                deviceKey = deviceKey,
                scaleKey = scaleKey,
                testimonyKey = testimonyKey,
                currentValue = value,
                picturePath = picturePath,
            )
        }
    }

    fun saveActCheck(deviceKey: String, actCheck: ActCheck) {
        viewModelScope.launch {
            routeRepository.saveActCheck(routeUuid, subscriberUuid, deviceKey, actCheck)
        }
    }

    fun saveActAccess(deviceKey: String, actAccess: ActAccess) {
        viewModelScope.launch {
            routeRepository.saveActAccess(routeUuid, subscriberUuid, deviceKey, actAccess)
        }
    }

    fun markCompleted() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            routeRepository.markSubscriberCompleted(routeUuid, subscriberUuid)
            _uiState.update { it.copy(isSaving = false, savedMessage = "Абонент отмечен как выполненный") }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(savedMessage = null) }

    /** Сохранить черновик перед переходом на камеру для ACT CHECK */
    fun setActCheckDraftForCamera(deviceKey: String, draft: ActCheck) {
        _uiState.update { it.copy(actCheckPendingDeviceKey = deviceKey, actCheckDraft = draft) }
    }

    /** Применить OCR-результат к черновику (factTestimony) */
    fun applyOcrToActCheckDraft(ocrText: String) {
        _uiState.update { state ->
            state.copy(actCheckDraft = state.actCheckDraft?.copy(factTestimony = ocrText))
        }
    }

    /** Сбросить черновик после сохранения/закрытия диалога */
    fun consumeActCheckDraft() {
        _uiState.update { it.copy(actCheckPendingDeviceKey = null, actCheckDraft = null) }
    }
}
