package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.system.DeviceStorageRepository
import com.example.data.system.Formatters
import com.example.domain.model.ActiveGuideSession
import com.example.domain.model.CleanQueueItem
import com.example.domain.model.CompatibilityInfo
import com.example.domain.model.GuideSource
import com.example.domain.model.QueueAction
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.GuidedOperationCard
import com.example.ui.components.ProtectedAppBadge
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.LocalExtendedColors

@Composable
fun MultiCleanQueueTab(
    queue: List<CleanQueueItem>,
    activeSession: ActiveGuideSession?,
    compatibilityInfo: CompatibilityInfo?,
    repository: DeviceStorageRepository,
    onUpdateAction: (String, QueueAction) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearQueue: () -> Unit,
    onStartCleaning: () -> Unit,
    onOpenGuideSettings: () -> Unit,
    onCancelGuide: () -> Unit,
    onGoToAppsPicker: () -> Unit
) {
    val ext = LocalExtendedColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeSession != null && activeSession.source == GuideSource.MULTI_CLEAN_QUEUE) {
                item {
                    GuidedOperationCard(
                        session = activeSession,
                        compatibilityInfo = compatibilityInfo,
                        onOpenAppSettings = onOpenGuideSettings,
                        onCancel = onCancelGuide
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.blueAccent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ext.blueAccent.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cola de operaciones (${queue.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Asigna Limpiar caché, Borrar datos o Sin modificar por app. Solo se marcan Completada tras verificación real.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onGoToAppsPicker) {
                            Icon(Icons.Rounded.Add, contentDescription = "Agregar apps", tint = ext.emeraldAccent)
                        }
                    }
                }
            }

            if (queue.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAddCheck,
                                contentDescription = null,
                                tint = ext.emeraldAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "La cola de limpieza está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Selecciona aplicaciones en la pestaña Apps o usa Limpieza inteligente para agregar recomendaciones seguras.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onGoToAppsPicker,
                                modifier = Modifier.testTag("queue_go_to_apps_btn")
                            ) {
                                Text("Elegir aplicaciones")
                            }
                        }
                    }
                }
            } else {
                items(queue, key = { it.packageName }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AppIconThumbnail(
                                    packageName = item.packageName,
                                    appName = item.appName,
                                    repository = repository,
                                    size = 40.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    StatusBadgeChip(status = item.status)
                                }
                                IconButton(
                                    onClick = { onRemoveItem(item.packageName) },
                                    modifier = Modifier.testTag("queue_remove_${item.packageName}")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Quitar de cola")
                                }
                            }

                            if (item.protectionReason != null) {
                                ProtectedAppBadge(reason = item.protectionReason)
                            }

                            if (item.statusDetail.isNotBlank()) {
                                Text(
                                    text = item.statusDetail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            item.recoveredBytes?.let { bytes ->
                                Text(
                                    text = "Recuperado verificado: ${Formatters.formatBytes(bytes)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ext.success
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QueueAction.entries.forEach { action ->
                                    val selected = item.action == action
                                    val chipColor = when (action) {
                                        QueueAction.CLEAR_CACHE -> ext.emeraldAccent
                                        QueueAction.CLEAR_DATA -> ext.danger
                                        QueueAction.DO_NOT_MODIFY -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onUpdateAction(item.packageName, action) },
                                        label = {
                                            Text(
                                                text = action.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                            selectedLabelColor = chipColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (queue.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClearQueue,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("clear_queue_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Vaciar cola")
                }
                Button(
                    onClick = onStartCleaning,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("start_multi_clean_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("INICIAR LIMPIEZA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
