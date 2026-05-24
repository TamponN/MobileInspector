package com.bestplus.mobileinspector.ui.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.bestplus.mobileinspector.domain.model.Address
import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import com.bestplus.mobileinspector.domain.model.Subscriber
import com.bestplus.mobileinspector.ui.theme.InspectorTheme
import com.bestplus.mobileinspector.ui.theme.StatusGreen
import com.bestplus.mobileinspector.ui.theme.StatusRed
import com.bestplus.mobileinspector.ui.theme.StatusYellow

/**
 * Список абонентов в маршрутном листе.
 * Повторяет C# PageLists.xaml.cs → ListView subscribers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberListScreen(
    routeUuid: String,
    onSubscriberClick: (String) -> Unit,
    onBack: () -> Unit,
    onMapClick: () -> Unit = {},
    viewModel: SubscriberListViewModel = hiltViewModel(),
) {
    val subscribers by viewModel.subscribers.collectAsStateWithLifecycle()
    val routeName by viewModel.routeName.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routeName.ifBlank { "Абоненты" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onMapClick) {
                        Icon(Icons.Default.Map, contentDescription = "Карта маршрута")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(subscribers, key = { idx, s -> s.uuid.ifBlank { "sub_$idx" } }) { _, sub ->
                SubscriberCard(
                    subscriber = sub,
                    onClick = { onSubscriberClick(sub.uuid) },
                )
            }
        }
        }
    }
}

@Composable
private fun SubscriberCard(subscriber: InfoSubscriber, onClick: () -> Unit) {
    val statusColor = when (subscriber.statusTask) {
        "Выполнена" -> StatusGreen
        "Закрыт" -> StatusRed
        else -> StatusYellow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = subscriber.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    color = statusColor,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = subscriber.statusTask,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = subscriber.address.shortAddress,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Л/С: ${subscriber.subscriber.personalAccount}",
                style = MaterialTheme.typography.bodySmall,
            )

            if (subscriber.unreadDeviceCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Не снято показаний: ${subscriber.unreadDeviceCount} прибор(ов)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

private fun makeFakeSub(id: String, name: String, status: String, unread: Int = 0) =
    InfoSubscriber(
        uuid = id,
        subscriber = Subscriber(
            key = id, fullName = name, personalAccount = "12345$id",
            uuidAccount = "a$id", typeSubscriber = "Физ.лицо", building = "",
            phone = "+7 999 000-00-00", passport = "", dateOfPassport = "",
        ),
        address = Address(
            key = "ad$id", value = "", country = "Россия", region = "МО",
            city = "Москва", area = "", street = "Ленина", sideStreet = "",
            house = "10", body = "", apartment = id,
        ),
        meteringDevices = List(unread) {
            com.bestplus.mobileinspector.domain.model.MeteringDevice(
                key = "d$it", name = "Счётчик", nameService = "ХВС",
                number = "$it", counterBrand = "", factoryNumber = "SN-$it",
                countingPointKey = "", dateTimePlomb = "", numberPlomb = "",
                typeDevice = "", uuidDevice = "ud$it",
                scales = listOf(
                    com.bestplus.mobileinspector.domain.model.Scale(
                        key = "sc$it", name = "", nameScale = "",
                        unitType = "м³", uuidScaleDevices = "",
                        testimonies = listOf(
                            com.bestplus.mobileinspector.domain.model.Testimony(
                                key = "t$it", idCountingPointKey = "", idScale = "",
                                uuidTariffZone = "", index = 0, nameScale = "",
                                nameTariff = "", previousTestimony = "100.00",
                                currentTestimony = "0", dateTimePrevious = "",
                            )
                        ),
                    )
                ),
            )
        },
        planDate = "2026-04-12", statusTask = status, uuidAccount = "a$id",
    )

@Preview(showBackground = true, name = "SubscriberCard — Открыт")
@Composable
private fun SubscriberCardOpenPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SubscriberCard(
                subscriber = makeFakeSub("1", "Иванов Иван Иванович", "Открыт", unread = 2),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "SubscriberCard — Выполнена")
@Composable
private fun SubscriberCardDonePreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SubscriberCard(
                subscriber = makeFakeSub("5", "Петрова Мария Алексеевна", "Выполнена"),
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "SubscriberCard — Закрыт")
@Composable
private fun SubscriberCardClosedPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SubscriberCard(
                subscriber = makeFakeSub("9", "Сидоров Пётр Семёнович", "Закрыт"),
                onClick = {},
            )
        }
    }
}
