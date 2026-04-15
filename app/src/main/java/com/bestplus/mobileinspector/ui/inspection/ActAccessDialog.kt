package com.bestplus.mobileinspector.ui.inspection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bestplus.mobileinspector.domain.model.ActAccess

private val agreeOptions = listOf("Да", "Нет")

/**
 * Нижний лист «Акт допуска».
 * Повторяет C# ActAccess.xaml.cs — фиксация допуска к прибору учёта.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActAccessDialog(
    initial: ActAccess?,
    onDismiss: () -> Unit,
    onSave: (ActAccess) -> Unit,
) {
    val existing = initial ?: ActAccess()

    var roomAccess by remember { mutableStateOf(existing.roomAccess) }
    var agreeAccess by remember { mutableStateOf(existing.agreeAccess) }
    var causeNoAgree by remember { mutableStateOf(existing.causeNoAgreeAccess) }
    var unauthorizedPersons by remember { mutableStateOf(existing.unauthorizedPersonsAccess) }
    var isCreateAct by remember { mutableStateOf(existing.isCreateActAccess) }

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
                text = "Акт допуска",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            OutlinedTextField(
                value = roomAccess,
                onValueChange = { roomAccess = it },
                label = { Text("Помещение") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            DropdownField(
                label = "Согласие на допуск",
                value = agreeAccess,
                options = agreeOptions,
                onSelected = { agreeAccess = it },
            )
            OutlinedTextField(
                value = causeNoAgree,
                onValueChange = { causeNoAgree = it },
                label = { Text("Причина отказа в допуске") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
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
                                roomAccess = roomAccess,
                                agreeAccess = agreeAccess,
                                causeNoAgreeAccess = causeNoAgree,
                                unauthorizedPersonsAccess = unauthorizedPersons,
                                isCreateActAccess = isCreateAct,
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
