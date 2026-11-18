package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CleaningHistoryEntity
import com.example.data.system.Formatters
import com.example.ui.theme.LocalExtendedColors

@Composable
fun HistoryScreen(
    historyList: List<CleaningHistoryEntity>,
    onClearHistory: () -> Unit
) {
    val ext = LocalExtendedColors.current
    var showConfirmDelete by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historial de limpiezas",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${historyList.size} registro(s) guardado(s) localmente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showConfirmDelete = true },
                        modifier = Modifier.testTag("clear_history_button"),
                        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.danger)
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Borrar")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (historyList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Sin historial",
                            tint = ext.blueAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No hay operaciones registradas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aquí verás la fecha, aplicación, operación, estado real y espacio recuperado verificado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { it.id }) { item ->
                        val statusColor = when (item.status) {
                            "Completada" -> ext.success
                            "Requiere confirmación" -> ext.warning
                            "Error", "No permitida" -> ext.danger
                            else -> ext.blueAccent
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = statusColor.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = item.status,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${item.operationType} • ${Formatters.formatDateTime(item.timestampMillis)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Recuperado: ${item.recoveredBytes?.let { Formatters.formatBytes(it) } ?: "No verificable"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (item.recoveredBytes != null && item.recoveredBytes > 0L) ext.emeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (item.notes.isNotBlank()) {
                                    Text(
                                        text = item.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showConfirmDelete) {
            AlertDialog(
                onDismissRequest = { showConfirmDelete = false },
                title = { Text("Borrar historial local") },
                text = { Text("Se eliminarán todos los registros guardados de limpiezas anteriores. Esta acción no afecta a tus aplicaciones.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearHistory()
                            showConfirmDelete = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ext.danger),
                        modifier = Modifier.testTag("confirm_clear_history_btn")
                    ) {
                        Text("Borrar historial")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDelete = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
