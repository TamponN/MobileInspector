package com.bestplus.mobileinspector.ui.inspection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bestplus.mobileinspector.domain.model.ActCheck

private val sealOptions = listOf("Не нарушена", "Нарушена")
private val consentOptions = listOf("Согласен", "Не согласен")

/**
 * Нижний лист «Акт проверки ПУ».
 * Повторяет C# ActCheck.xaml.cs — форма проверки прибора учёта.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActCheckDialog(
    initial: ActCheck?,
    onDismiss: () -> Unit,
    onSave: (ActCheck) -> Unit,
    onCameraForFactTestimony: ((ActCheck) -> Unit)? = null,
) {
    val existing = initial ?: ActCheck()

    var roomCheck by remember { mutableStateOf(existing.roomCheck) }
    var sealIntegrity by remember { mutableStateOf(existing.sealIntegrity) }
    var revealed by remember { mutableStateOf(existing.revealed) }
    var reconciliation by remember { mutableStateOf(existing.reconciliation) }
    var consentTestimony by remember { mutableStateOf(existing.consentTestimony) }
    var causeNoAgree by remember { mutableStateOf(existing.causeNoAgreeCheck) }
    var unauthorizedPersons by remember { mutableStateOf(existing.unauthorizedPersonsCheck) }
    var inspectionResult by remember { mutableStateOf(existing.inspectionResult) }
    var factTestimony by remember { mutableStateOf(existing.factTestimony) }
    var isNoAgrees by remember { mutableStateOf(existing.isNoAgreesTestimony) }
    var reasonForDisagreeing by remember { mutableStateOf(existing.reasonForDisagreeing) }
    var testimonyAgrees by remember { mutableStateOf(existing.testimonyAgrees) }
    var isCreateAct by remember { mutableStateOf(existing.isCreateAct) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Акт проверки ПУ",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            OutlinedTextField(
                value = roomCheck,
                onValueChange = { roomCheck = it },
                label = { Text("Помещение") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            DropdownField(
                label = "Целостность пломб",
                value = sealIntegrity,
                options = sealOptions,
                onSelected = { sealIntegrity = it },
            )
            OutlinedTextField(
                value = revealed,
                onValueChange = { revealed = it },
                label = { Text("Выявлено") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = reconciliation,
                onValueChange = { reconciliation = it },
                label = { Text("Сверка показаний") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            // Фактические показания + кнопка камеры
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = factTestimony,
                    onValueChange = { factTestimony = it },
                    label = { Text("Фактические показания") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                if (onCameraForFactTestimony != null) {
                    IconButton(
                        onClick = {
                            onCameraForFactTestimony(
                                existing.copy(
                                    roomCheck = roomCheck,
                                    sealIntegrity = sealIntegrity,
                                    revealed = revealed,
                                    reconciliation = reconciliation,
                                    consentTestimony = consentTestimony,
                                    causeNoAgreeCheck = causeNoAgree,
                                    unauthorizedPersonsCheck = unauthorizedPersons,
                                    inspectionResult = inspectionResult,
                                    factTestimony = factTestimony,
                                    isNoAgreesTestimony = isNoAgrees,
                                    reasonForDisagreeing = reasonForDisagreeing,
                                    testimonyAgrees = testimonyAgrees,
                                    isCreateAct = isCreateAct,
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Сфотографировать показание",
                        )
                    }
                }
            }
            OutlinedTextField(
                value = inspectionResult,
                onValueChange = { inspectionResult = it },
                label = { Text("Результат проверки") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Не согласен с показаниями", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isNoAgrees, onCheckedChange = { isNoAgrees = it })
            }

            if (isNoAgrees) {
                OutlinedTextField(
                    value = reasonForDisagreeing,
                    onValueChange = { reasonForDisagreeing = it },
                    label = { Text("Причина несогласия") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = testimonyAgrees,
                    onValueChange = { testimonyAgrees = it },
                    label = { Text("Показания по мнению абонента") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            DropdownField(
                label = "Согласие с показаниями",
                value = consentTestimony,
                options = consentOptions,
                onSelected = { consentTestimony = it },
            )
            OutlinedTextField(
                value = causeNoAgree,
                onValueChange = { causeNoAgree = it },
                label = { Text("Причина несогласия (акт)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = unauthorizedPersons,
                onValueChange = { unauthorizedPersons = it },
                label = { Text("Посторонние лица") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Создать акт", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isCreateAct, onCheckedChange = { isCreateAct = it })
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Отмена") }
                Button(
                    onClick = {
                        onSave(
                            existing.copy(
                                roomCheck = roomCheck,
                                sealIntegrity = sealIntegrity,
                                revealed = revealed,
                                reconciliation = reconciliation,
                                consentTestimony = consentTestimony,
                                causeNoAgreeCheck = causeNoAgree,
                                unauthorizedPersonsCheck = unauthorizedPersons,
                                inspectionResult = inspectionResult,
                                factTestimony = factTestimony,
                                isNoAgreesTestimony = isNoAgrees,
                                reasonForDisagreeing = reasonForDisagreeing,
                                testimonyAgrees = testimonyAgrees,
                                isCreateAct = isCreateAct,
                            )
                        )
                        onDismiss()
                    },
                ) { Text("Сохранить") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
