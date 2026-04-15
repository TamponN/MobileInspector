package com.bestplus.mobileinspector.ui.camera

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestplus.mobileinspector.domain.repository.RouteRepository
import com.bestplus.mobileinspector.service.TextRecognitionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CameraUiState(
    val isProcessing: Boolean = false,
    val recognizedText: String = "",
    val error: String? = null,
    val photoSaved: Boolean = false,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val routeUuid: String = checkNotNull(savedStateHandle["routeUuid"])
    val subscriberUuid: String = checkNotNull(savedStateHandle["subscriberUuid"])
    val deviceKey: String = checkNotNull(savedStateHandle["deviceKey"])
    val scaleKey: String = checkNotNull(savedStateHandle["scaleKey"])
    val testimonyKey: String = checkNotNull(savedStateHandle["testimonyKey"])

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /** После захвата фото: запускаем OCR и сохраняем результат */
    fun onPhotoCaptured(photoFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val recognized = TextRecognitionHelper.recognizeTestimony(context, photoFile)
                _uiState.update { it.copy(recognizedText = recognized) }

                // Для actcheck — только OCR, без сохранения в репозиторий
                if (scaleKey != "actcheck") {
                    routeRepository.updateTestimony(
                        routeUuid = routeUuid,
                        subscriberUuid = subscriberUuid,
                        deviceKey = deviceKey,
                        scaleKey = scaleKey,
                        testimonyKey = testimonyKey,
                        currentValue = recognized,
                        picturePath = photoFile.absolutePath,
                    )
                }
                _uiState.update { it.copy(isProcessing = false, photoSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, error = "Ошибка распознавания: ${e.message}")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
