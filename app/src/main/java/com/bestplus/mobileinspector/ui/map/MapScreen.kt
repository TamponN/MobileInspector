package com.bestplus.mobileinspector.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestplus.mobileinspector.ui.theme.StatusGreen
import com.bestplus.mobileinspector.ui.theme.StatusYellow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Карта маршрутного листа с маркерами абонентов.
 * Реализует функциональность C# PageMap.xaml.cs:
 *   - Отображение абонентов на карте Google Maps
 *   - Цветные маркеры: зелёный — выполнено, жёлтый — открыт, серый — закрыт
 *   - Клик по маркеру показывает инфо-карточку абонента
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onSubscriberClick: (subscriberUuid: String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val markers by viewModel.markers.collectAsStateWithLifecycle()
    val routeName by viewModel.routeName.collectAsStateWithLifecycle()

    // Default center: Moscow; will move to first marker when available
    val defaultPosition = LatLng(55.7558, 37.6173)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    // Move camera to first marker when markers appear
    LaunchedEffect(markers) {
        if (markers.isNotEmpty()) {
            val first = markers.first()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(first.position.latitude, first.position.longitude),
                    14f,
                ),
            )
        }
    }

    var selectedMarker by remember { mutableStateOf<SubscriberMarker?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routeName.ifBlank { "Карта маршрута" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (markers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Нет абонентов с геолокацией",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "GPS-координаты отсутствуют в маршрутном листе",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true),
                ) {
                    markers.forEach { marker ->
                        val latLng = LatLng(marker.position.latitude, marker.position.longitude)
                        val hue = when (marker.statusTask) {
                            "Выполнена" -> BitmapDescriptorFactory.HUE_GREEN
                            "Закрыт" -> BitmapDescriptorFactory.HUE_AZURE
                            else -> BitmapDescriptorFactory.HUE_YELLOW
                        }
                        Marker(
                            state = MarkerState(position = latLng),
                            title = marker.displayName,
                            snippet = marker.address,
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                selectedMarker = marker
                                false
                            },
                        )
                    }
                }

                // Selected subscriber bottom card
                selectedMarker?.let { sub ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(sub.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(sub.address, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val statusColor = when (sub.statusTask) {
                                    "Выполнена" -> StatusGreen
                                    else -> StatusYellow
                                }
                                Text(
                                    text = sub.statusTask,
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                TextButton(onClick = { onSubscriberClick(sub.uuid) }) {
                                    Text("Открыть")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
