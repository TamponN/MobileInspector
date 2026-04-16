package com.bestplus.mobileinspector.ui.inspection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestplus.mobileinspector.domain.model.Address
import com.bestplus.mobileinspector.domain.model.InfoSubscriber
import com.bestplus.mobileinspector.domain.model.MeteringDevice
import com.bestplus.mobileinspector.domain.model.Scale
import com.bestplus.mobileinspector.domain.model.Subscriber
import com.bestplus.mobileinspector.domain.model.Testimony
import com.bestplus.mobileinspector.ui.theme.InspectorTheme

/**
 * Экран осмотра абонента.
 * Повторяет C# PageControler.xaml.cs:
 *   - Информация об абоненте / адресе
 *   - Список приборов учёта со шкалами
 *   - Ввод показаний
 *   - Кнопка "Выполнить"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(
    routeUuid: String,
    subscriberUuid: String,
    onBack: () -> Unit,
    onCameraClick: (deviceIndex: String, scaleIndex: String, testimonyIndex: String) -> Unit = { _, _, _ -> },
    actCheckOcrResult: String = "",
    onActCheckOcrConsumed: () -> Unit = {},
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val subscriber by viewModel.subscriber.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = SnackbarHostState()
    LaunchedEffect(uiState.savedMessage) {
        uiState.savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(actCheckOcrResult) {
        if (actCheckOcrResult.isNotEmpty()) {
            viewModel.applyOcrToActCheckDraft(actCheckOcrResult)
            onActCheckOcrConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subscriber?.displayName ?: "Загрузка…") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val sub = subscriber
            if (sub != null && sub.statusTask != "Выполнена") {
                ExtendedFloatingActionButton(
                    onClick = viewModel::markCompleted,
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text("Выполнить") },
                )
            }
        },
    ) { padding ->
        val sub = subscriber
        if (sub == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Информация об абоненте
            item {
                SubscriberInfoCard(sub)
            }

            // Приборы учёта
            itemsIndexed(sub.meteringDevices, key = { idx, dev -> dev.uuidDevice.ifBlank { "device_$idx" } }) { deviceIdx, device ->
                DeviceCard(
                    device = device,
                    autoShowActCheck = device.key == uiState.actCheckPendingDeviceKey,
                    actCheckDraft = if (device.key == uiState.actCheckPendingDeviceKey) uiState.actCheckDraft else null,
                    onTestimonyChange = { scaleIdx, testimonyIdx, value ->
                        viewModel.updateTestimony(deviceIdx, scaleIdx, testimonyIdx, value)
                    },
                    onActCheckSave = { actCheck ->
                        viewModel.saveActCheck(device.key, actCheck)
                        viewModel.consumeActCheckDraft()
                    },
                    onActAccessSave = { actAccess ->
                        viewModel.saveActAccess(device.key, actAccess)
                    },
                    onCameraClick = { scaleIdx, testimonyIdx ->
                        onCameraClick(deviceIdx.toString(), scaleIdx.toString(), testimonyIdx.toString())
                    },
                    onActCheckCameraClick = { draft ->
                        viewModel.setActCheckDraftForCamera(device.key, draft)
                        onCameraClick(deviceIdx.toString(), "actcheck", "fact")
                    },
                )
            }

            // Пустое пространство для FAB
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SubscriberInfoCard(subscriber: com.bestplus.mobileinspector.domain.model.InfoSubscriber) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = subscriber.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text("Адрес: ${subscriber.address.fullAddress}", style = MaterialTheme.typography.bodyMedium)
            Text("Л/С: ${subscriber.subscriber.personalAccount}", style = MaterialTheme.typography.bodySmall)
            if (subscriber.subscriber.phone.isNotBlank()) {
                Text("Тел: ${subscriber.subscriber.phone}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: MeteringDevice,
    autoShowActCheck: Boolean = false,
    actCheckDraft: com.bestplus.mobileinspector.domain.model.ActCheck? = null,
    onTestimonyChange: (scaleIndex: Int, testimonyIndex: Int, value: String) -> Unit,
    onActCheckSave: (com.bestplus.mobileinspector.domain.model.ActCheck) -> Unit = {},
    onActAccessSave: (com.bestplus.mobileinspector.domain.model.ActAccess) -> Unit = {},
    onCameraClick: (scaleIndex: Int, testimonyIndex: Int) -> Unit = { _, _ -> },
    onActCheckCameraClick: (com.bestplus.mobileinspector.domain.model.ActCheck) -> Unit = {},
) {
    var showActCheck by remember { mutableStateOf(false) }
    var showActAccess by remember { mutableStateOf(false) }

    // Автоматически открываем диалог после возврата с OCR-камеры
    LaunchedEffect(autoShowActCheck) {
        if (autoShowActCheck) showActCheck = true
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${device.nameService} — ${device.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Зав. № ${device.factoryNumber}",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(8.dp))

            device.scales.forEachIndexed { scaleIdx, scale ->
                ScaleSection(
                    scale = scale,
                    onTestimonyChange = { testimonyIdx, value ->
                        onTestimonyChange(scaleIdx, testimonyIdx, value)
                    },
                    onCameraClick = { testimonyIdx ->
                        onCameraClick(scaleIdx, testimonyIdx)
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Кнопки актов
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showActCheck = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (device.actCheck != null) "✓ Акт проверки" else "Акт проверки",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                OutlinedButton(
                    onClick = { showActAccess = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (device.actAccess != null) "✓ Акт допуска" else "Акт допуска",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }

    if (showActCheck) {
        ActCheckDialog(
            initial = actCheckDraft ?: device.actCheck,
            onDismiss = { showActCheck = false },
            onSave = { onActCheckSave(it) },
            onCameraForFactTestimony = { draft ->
                showActCheck = false
                onActCheckCameraClick(draft)
            },
        )
    }
    if (showActAccess) {
        ActAccessDialog(
            initial = device.actAccess,
            onDismiss = { showActAccess = false },
            onSave = { onActAccessSave(it) },
        )
    }
}

@Composable
private fun ScaleSection(
    scale: Scale,
    onTestimonyChange: (testimonyIndex: Int, value: String) -> Unit,
    onCameraClick: (testimonyIndex: Int) -> Unit = {},
) {
    Column {
        if (scale.nameScale.isNotBlank()) {
            Text(
                text = "${scale.nameScale} (${scale.unitType})",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        scale.testimonies.forEachIndexed { tIdx, testimony ->
            TestimonyRow(
                testimony = testimony,
                onValueChange = { value -> onTestimonyChange(tIdx, value) },
                onCameraClick = { onCameraClick(tIdx) },
            )
        }
    }
}

@Composable
private fun TestimonyRow(
    testimony: Testimony,
    onValueChange: (String) -> Unit,
    onCameraClick: () -> Unit = {},
) {
    var textValue by remember(testimony.key, testimony.currentTestimony) {
        mutableStateOf(testimony.currentTestimony)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = testimony.nameTariff.ifBlank { "Тариф ${testimony.index}" },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Пред: ${testimony.previousTestimony}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                onValueChange(newValue)
            },
            modifier = Modifier.width(110.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text("Показание") },
        )

        IconButton(onClick = onCameraClick) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Сфотографировать показание",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

private val previewInfoSubscriber = InfoSubscriber(
    uuid = "s1",
    subscriber = Subscriber(
        key = "k1", fullName = "Иванов Иван Иванович", personalAccount = "123456789",
        uuidAccount = "a1", typeSubscriber = "Физ.лицо", building = "",
        phone = "+7 999 123-45-67", passport = "", dateOfPassport = "",
    ),
    address = Address(
        key = "ad1", value = "", country = "Россия", region = "МО",
        city = "Москва", area = "", street = "Ленина", sideStreet = "",
        house = "10", body = "", apartment = "5",
    ),
    meteringDevices = listOf(
        MeteringDevice(
            key = "dev1", name = "Счётчик холодной воды",
            nameService = "Холодное водоснабжение",
            number = "001", counterBrand = "МТК", factoryNumber = "СВ-12345",
            countingPointKey = "", dateTimePlomb = "2024-01-01", numberPlomb = "A001",
            typeDevice = "Водосчётчик", uuidDevice = "ud1",
            scales = listOf(
                Scale(
                    key = "sc1", name = "Шкала 1", nameScale = "", unitType = "м³",
                    uuidScaleDevices = "usc1",
                    testimonies = listOf(
                        Testimony(
                            key = "t1", idCountingPointKey = "cp1", idScale = "sc1",
                            uuidTariffZone = "tz1", index = 0, nameScale = "",
                            nameTariff = "День", previousTestimony = "1234.56",
                            currentTestimony = "", dateTimePrevious = "01.03.2026",
                        ),
                    ),
                ),
            ),
        ),
    ),
    planDate = "2026-04-12", statusTask = "Открыт", uuidAccount = "a1",
)

@Preview(showBackground = true, name = "SubscriberInfoCard")
@Composable
private fun SubscriberInfoCardPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            SubscriberInfoCard(subscriber = previewInfoSubscriber)
        }
    }
}

@Preview(showBackground = true, name = "DeviceCard")
@Composable
private fun DeviceCardPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            DeviceCard(
                device = previewInfoSubscriber.meteringDevices.first(),
                onTestimonyChange = { _, _, _ -> },
            )
        }
    }
}

@Preview(showBackground = true, name = "TestimonyRow — пусто")
@Composable
private fun TestimonyRowEmptyPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            TestimonyRow(
                testimony = previewInfoSubscriber.meteringDevices.first().scales.first().testimonies.first(),
                onValueChange = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "TestimonyRow — заполнено")
@Composable
private fun TestimonyRowFilledPreview() {
    InspectorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            TestimonyRow(
                testimony = previewInfoSubscriber.meteringDevices.first().scales.first().testimonies.first()
                    .copy(currentTestimony = "1265.80"),
                onValueChange = {},
            )
        }
    }
}
