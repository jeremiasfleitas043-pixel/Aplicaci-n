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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.system.DeviceStorageRepository
import com.example.data.system.Formatters
import com.example.domain.model.GuideSource
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.QueueAction
import com.example.ui.MainUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.GuidedOperationCard
import com.example.ui.components.ProtectedAppBadge
import com.example.ui.theme.LocalExtendedColors

enum class CleanerSubTab(val label: String) {
    CACHE("Caché"),
    CLEAR_DATA("Borrar datos"),
    MULTI_QUEUE("Múltiple")
}

@Composable
fun CleanerHubScreen(
    selectedTab: CleanerSubTab,
    onSelectTab: (CleanerSubTab) -> Unit,
    uiState: MainUiState,
    repository: DeviceStorageRepository,
    onClearOwnCache: () -> Unit,
    onLaunchSystemClearCache: () -> Unit,
    onToggleCacheApp: (String) -> Unit,
    onSelectAllCacheApps: () -> Unit,
    onDeselectAllCacheApps: () -> Unit,
    onStartCacheCleaning: () -> Unit,
    onRequestClearData: (InstalledAppInfo) -> Unit,
    onUpdateQueueAction: (String, QueueAction) -> Unit,
    onRemoveQueueItem: (String) -> Unit,
    onClearQueue: () -> Unit,
    onStartMultiClean: () -> Unit,
    onOpenGuideSettings: () -> Unit,
    onCancelGuide: () -> Unit,
    onGoToAppsPicker: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (selectedTab) {
                    CleanerSubTab.CACHE -> "Administrador de caché"
                    CleanerSubTab.CLEAR_DATA -> "BORRAR DATOS"
                    CleanerSubTab.MULTI_QUEUE -> "Limpieza múltiple"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                CleanerSubTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index, CleanerSubTab.entries.size),
                        modifier = Modifier.testTag("cleaner_tab_${tab.name.lowercase()}")
                    ) {
                        Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                CleanerSubTab.CACHE -> CacheManagerTab(
                    uiState = uiState,
                    repository = repository,
                    onClearOwnCache = onClearOwnCache,
                    onLaunchSystemClearCache = onLaunchSystemClearCache,
                    onToggleApp = onToggleCacheApp,
                    onSelectAll = onSelectAllCacheApps,
                    onDeselectAll = onDeselectAllCacheApps,
                    onStartCacheCleaning = onStartCacheCleaning,
                    onOpenGuideSettings = onOpenGuideSettings,
                    onCancelGuide = onCancelGuide
                )
                CleanerSubTab.CLEAR_DATA -> ClearDataTab(
                    uiState = uiState,
                    repository = repository,
                    onRequestClearData = onRequestClearData,
                    onOpenGuideSettings = onOpenGuideSettings,
                    onCancelGuide = onCancelGuide
                )
                CleanerSubTab.MULTI_QUEUE -> MultiCleanQueueTab(
                    queue = uiState.multiCleanQueue,
                    activeSession = uiState.activeGuideSession,
                    compatibilityInfo = uiState.compatibilityInfo,
                    repository = repository,
                    onUpdateAction = onUpdateQueueAction,
                    onRemoveItem = onRemoveQueueItem,
                    onClearQueue = onClearQueue,
                    onStartCleaning = onStartMultiClean,
                    onOpenGuideSettings = onOpenGuideSettings,
                    onCancelGuide = onCancelGuide,
                    onGoToAppsPicker = onGoToAppsPicker
                )
            }
        }
    }
}

@Composable
private fun CacheManagerTab(
    uiState: MainUiState,
    repository: DeviceStorageRepository,
    onClearOwnCache: () -> Unit,
    onLaunchSystemClearCache: () -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onStartCacheCleaning: () -> Unit,
    onOpenGuideSettings: () -> Unit,
    onCancelGuide: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val supportsSystemClear = uiState.compatibilityInfo?.supportsSystemClearCacheIntent == true
    val ownCache = uiState.storageSummary?.ownCacheBytes ?: 0L

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val active = uiState.activeGuideSession
            if (active != null && active.source == GuideSource.CACHE_MANAGER) {
                item {
                    GuidedOperationCard(
                        session = active,
                        compatibilityInfo = uiState.compatibilityInfo,
                        onOpenAppSettings = onOpenGuideSettings,
                        onCancel = onCancelGuide
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.emeraldAccent.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Caché propia de CLEAN MASTER AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Espacio actual: ${Formatters.formatBytes(ownCache)}. Android autoriza limpiarla directamente sin salir de la app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onClearOwnCache,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_own_cache_btn")
                            ) {
                                Text("Limpiar caché propia")
                            }
                            if (supportsSystemClear) {
                                OutlinedButton(
                                    onClick = onLaunchSystemClearCache,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("system_clear_cache_intent_btn")
                                ) {
                                    Text("Asistente Android")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Text(
                        text = "Por seguridad de Android 8.0+, ninguna app externa puede borrar en silencio la caché privada de otra app. Al iniciar, CLEAN MASTER AI abrirá la pantalla oficial de cada app seleccionada y verificará el cambio real al regresar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Todas")
                    }
                    OutlinedButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Rounded.Deselect, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ninguna")
                    }
                }
            }

            items(uiState.allApps, key = { it.packageName }) { app ->
                val checked = app.packageName in uiState.cacheSelectedPackages
                Card(
                    onClick = { onToggleApp(app.packageName) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) ext.emeraldAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, if (checked) ext.emeraldAccent else ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppIconThumbnail(app.packageName, app.appName, repository, size = 42.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Caché: ${Formatters.formatBytesOrUnavailable(app.cacheBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.emeraldAccent
                            )
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(checkedColor = ext.emeraldAccent)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onStartCacheCleaning,
            enabled = uiState.cacheSelectedPackages.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .heightIn(min = 52.dp)
                .testTag("start_cache_cleaning_btn"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.CleaningServices, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Limpiar caché (${uiState.cacheSelectedPackages.size} seleccionada(s))")
        }
    }
}

@Composable
private fun ClearDataTab(
    uiState: MainUiState,
    repository: DeviceStorageRepository,
    onRequestClearData: (InstalledAppInfo) -> Unit,
    onOpenGuideSettings: () -> Unit,
    onCancelGuide: () -> Unit
) {
    val ext = LocalExtendedColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val active = uiState.activeGuideSession
        if (active != null && active.source == GuideSource.CLEAR_DATA_MANAGER) {
            item {
                GuidedOperationCard(
                    session = active,
                    compatibilityInfo = uiState.compatibilityInfo,
                    onOpenAppSettings = onOpenGuideSettings,
                    onCancel = onCancelGuide
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.danger.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Advertencia crítica",
                        tint = ext.danger
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Advertencia de borrado de datos",
                            style = MaterialTheme.typography.titleMedium,
                            color = ext.danger,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.clear_data_warning_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        items(uiState.allApps, key = { it.packageName }) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppIconThumbnail(app.packageName, app.appName, repository, size = 44.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Datos: ${Formatters.formatBytesOrUnavailable(app.dataBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.blueAccent
                        )
                        if (app.protectionReason != null) {
                            ProtectedAppBadge(reason = app.protectionReason)
                        }
                    }
                    OutlinedButton(
                        onClick = { onRequestClearData(app) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("clear_data_btn_${app.packageName}"),
                        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ext.danger)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Borrar")
                    }
                }
            }
        }
    }
}
