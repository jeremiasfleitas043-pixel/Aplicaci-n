package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.system.DeviceStorageRepository
import com.example.data.system.Formatters
import com.example.domain.model.ActiveGuideSession
import com.example.domain.model.CleanQueueItem
import com.example.domain.model.CompatibilityInfo
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.QueueAction
import com.example.domain.model.VerificationPrompt
import com.example.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: InstalledAppInfo,
    repository: DeviceStorageRepository,
    onDismiss: () -> Unit,
    onQueueCache: (InstalledAppInfo) -> Unit,
    onRequestClearData: (InstalledAppInfo) -> Unit,
    onOpenSystemSettings: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalExtendedColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIconThumbnail(
                    packageName = app.packageName,
                    appName = app.appName,
                    repository = repository,
                    size = 56.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Versión ${app.versionName} • ${Formatters.formatDaysAgo(app.lastTimeUsedMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (app.protectionReason != null) {
                ProtectedAppBadge(reason = "Protegida: ${app.protectionReason}")
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Desglose de almacenamiento (StorageStatsManager)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ext.blueAccent
                    )
                    StorageBreakdownRow("Aplicación", Formatters.formatBytesOrUnavailable(app.appBytes))
                    StorageBreakdownRow("Datos", Formatters.formatBytesOrUnavailable(app.dataBytes))
                    StorageBreakdownRow("Caché", Formatters.formatBytesOrUnavailable(app.cacheBytes))
                    HorizontalDivider(color = ext.cardBorder)
                    StorageBreakdownRow(
                        label = "Almacenamiento total",
                        value = Formatters.formatBytesOrUnavailable(app.totalBytes),
                        bold = true
                    )
                }
            }

            app.smartRecommendation?.let { rec ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.emeraldAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ext.emeraldAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Recomendación inteligente",
                            tint = ext.emeraldAccent
                        )
                        Text(
                            text = rec.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onQueueCache(app)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("sheet_queue_cache_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Limpiar caché")
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRequestClearData(app)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("sheet_clear_data_btn"),
                    border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.danger)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar datos")
                }
            }

            OutlinedButton(
                onClick = { onOpenSystemSettings(app.packageName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("sheet_open_android_info_btn")
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir información de la app en Android")
            }
        }
    }
}

@Composable
private fun StorageBreakdownRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCleanSheet(
    recommendedApps: List<InstalledAppInfo>,
    hasUsageAccess: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    repository: DeviceStorageRepository,
    onDismiss: () -> Unit,
    onEnqueueSafeCaches: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onInspectApp: (InstalledAppInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalExtendedColors.current
    val safeCacheCount = recommendedApps.count { it.smartRecommendation?.safeToQueueCache == true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Limpieza inteligente",
                    tint = ext.emeraldAccent,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Limpieza inteligente",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Análisis local sin borrado automático",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isScanning) {
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ext.emeraldAccent
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ext.emeraldAccent.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, ext.emeraldAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = ext.emeraldAccent)
                    Text(
                        text = "Modo seguro activo: CLEAN MASTER AI solo sugiere qué apps revisar y nunca elimina datos de WhatsApp, juegos ni aplicaciones sin tu confirmación.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!hasUsageAccess) {
                UsageAccessBanner(onGrantClick = onGrantUsageAccess)
            }

            if (recommendedApps.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (hasUsageAccess) {
                                "No se detectaron acumulaciones elevadas de caché o apps inactivas"
                            } else {
                                "Activa el Acceso a datos de uso para detectar cachés elevadas y apps sin uso reciente"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recommendedApps, key = { it.packageName }) { app ->
                        val rec = app.smartRecommendation ?: return@items
                        Card(
                            onClick = { onInspectApp(app) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AppIconThumbnail(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    repository = repository,
                                    size = 42.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ext.blueAccent.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = rec.categoryLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ext.blueAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = rec.reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (app.protectionReason != null) {
                                        ProtectedAppBadge(reason = app.protectionReason)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (safeCacheCount > 0) {
                Button(
                    onClick = onEnqueueSafeCaches,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("smart_clean_enqueue_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir $safeCacheCount cachés seguras a Limpieza múltiple")
                }
            }
        }
    }
}

@Composable
fun ClearDataWarningDialog(
    appName: String,
    protectionReason: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Advertencia de borrado de datos",
                tint = ext.danger,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = "Confirmar borrado de datos: $appName",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.clear_data_warning_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (protectionReason != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ext.danger.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "Aplicación protegida ($protectionReason): " +
                                stringResource(R.string.clear_data_protected_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.danger,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Text(
                    text = "Se abrirá la pantalla oficial de Android para que confirmes manualmente el borrado en el sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ext.danger),
                modifier = Modifier.testTag("confirm_clear_data_dialog_btn")
            ) {
                Text("Entiendo, abrir Ajustes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_clear_data_dialog_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun GuidedOperationCard(
    session: ActiveGuideSession,
    compatibilityInfo: CompatibilityInfo?,
    onOpenAppSettings: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.5.dp, ext.emeraldAccent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paso ${session.stepIndex + 1} de ${session.totalSteps} • ${session.action.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.emeraldAccent
                )
                TextButton(onClick = onCancel) {
                    Text("Detener")
                }
            }
            Text(
                text = session.appName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val steps = compatibilityInfo?.oemGuidanceSteps ?: emptyList()
            steps.forEachIndexed { idx, step ->
                Text(
                    text = "${idx + 1}. $step",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenAppSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("guided_open_settings_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (session.action == QueueAction.CLEAR_DATA) ext.danger else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Abrir pantalla oficial de ${session.appName}")
            }
        }
    }
}

@Composable
fun VerificationResultDialog(
    prompt: VerificationPrompt,
    onRetry: () -> Unit,
    onContinueNext: () -> Unit,
    onCancelSession: () -> Unit
) {
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onContinueNext,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Verificación pendiente",
                tint = ext.warning,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Confirmación pendiente: ${prompt.appName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = prompt.detailMessage,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onContinueNext,
                modifier = Modifier.testTag("verification_continue_btn")
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancelSession) {
                    Text("Cancelar")
                }
                TextButton(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    )
}
