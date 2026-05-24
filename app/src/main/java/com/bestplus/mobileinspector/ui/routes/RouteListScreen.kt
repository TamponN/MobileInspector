package com.bestplus.mobileinspector.ui.routes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.bestplus.mobileinspector.ui.theme.InspectorTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestplus.mobileinspector.domain.model.RouteSheet
import com.bestplus.mobileinspector.ui.theme.StatusGreen
import com.bestplus.mobileinspector.ui.theme.StatusYellow

/**
 * Список маршрутных листов.
 * Повторяет C# PageLists.xaml.cs — ListView с маршрутными листами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    onRouteClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: RouteListViewModel = hiltViewModel(),
) {
    val routes by viewModel.routeSheets.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    RouteListContent(
        routes = routes,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRouteClick = onRouteClick,
        onSyncClick = viewModel::sync,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteListContent(
    routes: List<RouteSheet>,
    uiState: RouteListUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRouteClick: (String) -> Unit = {},
    onSyncClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Маршрутные листы") },
                actions = {
                    IconButton(onClick = onSyncClick, enabled = !uiState.isSyncing) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Синхронизация")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = onSyncClick,
            modifier = Modifier.padding(padding),
        ) {
        if (routes.isEmpty() && !uiState.isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Нет маршрутных листов", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onSyncClick) {
                        Text("Синхронизировать")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(routes, key = { idx, r -> r.uuidDocument.ifBlank { "route_$idx" } }) { _, route ->
                    RouteSheetCard(route = route, onClick = { onRouteClick(route.uuidDocument) })
                }
            }
        }
        }
    }
}

@Composable
private fun RouteSheetCard(route: RouteSheet, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Дата: ${route.planDateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Выполнено: ${route.countCompletedSubs} / ${route.countSubs}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            RouteProgressDonut(
                completed = route.countCompletedSubs,
                closed = route.countClosedSubs,
                total = route.countSubs,
                size = 52.dp,
            )
        }
    }
}

/**
 * Кольцевая диаграмма прогресса маршрутного листа.
 * Зелёный — выполненные, красный — закрытые (недоступные), серый — оставшиеся.
 */
@Composable
private fun RouteProgressDonut(
    completed: Int,
    closed: Int,
    total: Int,
    size: Dp = 52.dp,
) {
    val completedColor = StatusGreen
    val closedColor = MaterialTheme.colorScheme.error
    val remainingColor = MaterialTheme.colorScheme.surfaceVariant
    val strokeWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }

    Canvas(modifier = Modifier.size(size)) {
        val diameter = this.size.minDimension - strokeWidth
        val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
        val stroke = Stroke(width = strokeWidth)

        val safeTotal = total.coerceAtLeast(1).toFloat()
        val completedAngle = 360f * (completed / safeTotal)
        val closedAngle = 360f * (closed / safeTotal)
        val remainingAngle = 360f - completedAngle - closedAngle

        // Background (remaining)
        drawArc(
            color = remainingColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        // Completed (green)
        if (completedAngle > 0f) {
            drawArc(
                color = completedColor,
                startAngle = -90f,
                sweepAngle = completedAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        // Closed / inaccessible (red)
        if (closedAngle > 0f) {
            drawArc(
                color = closedColor,
                startAngle = -90f + completedAngle,
                sweepAngle = closedAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

private val previewRoutes = listOf(
    RouteSheet(
        key = "1", name = "Маршрутный лист №1", uuidDocument = "uuid-1",
        status = "Открыт", statusTask = "В работе", planDateTime = "12.04.2026",
        organizationKey = "org", country = "Россия", street = "Ленина",
        subscribers = List(8) { i ->
            com.bestplus.mobileinspector.domain.model.InfoSubscriber(
                uuid = "s$i",
                subscriber = com.bestplus.mobileinspector.domain.model.Subscriber(
                    key = "k$i", fullName = "Абонент $i", personalAccount = "$i",
                    uuidAccount = "a$i", typeSubscriber = "", building = "",
                    phone = "", passport = "", dateOfPassport = "",
                ),
                address = com.bestplus.mobileinspector.domain.model.Address(
                    key = "ad$i", value = "", country = "", region = "",
                    city = "", area = "", street = "", sideStreet = "",
                    house = "", body = "", apartment = "",
                ),
                meteringDevices = emptyList(),
                planDate = "",
                statusTask = if (i < 5) "Выполнена" else "Открыт",
                uuidAccount = "a$i",
            )
        },
    ),
    RouteSheet(
        key = "2", name = "Маршрутный лист №2", uuidDocument = "uuid-2",
        status = "Открыт", statusTask = "Открыт", planDateTime = "12.04.2026",
        organizationKey = "org", country = "Россия", street = "",
        subscribers = emptyList(),
    ),
)

@Preview(showBackground = true, name = "RouteSheetCard — в процессе")
@Composable
private fun RouteSheetCardPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            RouteSheetCard(route = previewRoutes.first(), onClick = {})
        }
    }
}

@Preview(showBackground = true, name = "RouteList — со списком")
@Composable
private fun RouteListContentPreview() {
    InspectorTheme {
        RouteListContent(
            routes = previewRoutes,
            uiState = RouteListUiState(isSyncing = false),
        )
    }
}

@Preview(showBackground = true, name = "RouteList — пусто")
@Composable
private fun RouteListEmptyPreview() {
    InspectorTheme {
        RouteListContent(
            routes = emptyList(),
            uiState = RouteListUiState(isSyncing = false),
        )
    }
}
