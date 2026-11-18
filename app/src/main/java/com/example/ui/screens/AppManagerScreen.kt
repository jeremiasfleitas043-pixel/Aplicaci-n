package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.system.DeviceStorageRepository
import com.example.data.system.Formatters
import com.example.domain.model.AppFilterOption
import com.example.domain.model.AppSortOption
import com.example.domain.model.InstalledAppInfo
import com.example.ui.MainUiState
import com.example.ui.components.AppIconThumbnail
import com.example.ui.components.ProtectedAppBadge
import com.example.ui.components.UsageAccessBanner
import com.example.ui.theme.LocalExtendedColors

@Composable
fun AppManagerScreen(
    uiState: MainUiState,
    repository: DeviceStorageRepository,
    onSearchChange: (String) -> Unit,
    onSortChange: (AppSortOption) -> Unit,
    onFilterChange: (AppFilterOption) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onInspectApp: (InstalledAppInfo) -> Unit,
    onSendSelectedToCache: () -> Unit,
    onSendSelectedToMultiQueue: () -> Unit,
    onGrantUsageAccess: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val hasUsageAccess = uiState.compatibilityInfo?.hasUsageAccess ?: false
    var showSortMenu by remember { mutableStateOf(false) }

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
                        text = "Administrador de aplicaciones",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.filteredApps.size} mostradas • ${uiState.selectedPackages.size} seleccionada(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("app_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Ordenar aplicaciones",
                            tint = ext.blueAccent
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        AppSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSortChange(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input"),
                placeholder = { Text("Buscar por nombre o paquete...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppFilterOption.entries) { filter ->
                    FilterChip(
                        selected = uiState.filterOption == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ext.emeraldAccent.copy(alpha = 0.2f),
                            selectedLabelColor = ext.emeraldAccent
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("select_all_apps_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Seleccionar todas", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onDeselectAll,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .testTag("deselect_all_apps_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Deselect, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Deseleccionar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = if (uiState.selectedPackages.isNotEmpty()) 96.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasUsageAccess) {
                    item {
                        UsageAccessBanner(onGrantClick = onGrantUsageAccess)
                    }
                }

                if (uiState.filteredApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckBox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No se encontraron aplicaciones con ese criterio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                        AppRowCard(
                            app = app,
                            repository = repository,
                            onClick = { onInspectApp(app) },
                            onToggle = { onToggleSelection(app.packageName) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.selectedPackages.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, ext.emeraldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.widthIn(max = 640.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSendSelectedToCache,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("apps_send_to_cache_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Limpiar caché (${uiState.selectedPackages.size})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onSendSelectedToMultiQueue,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("apps_send_to_queue_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ext.blueAccent)
                    ) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("A cola múltiple", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRowCard(
    app: InstalledAppInfo,
    repository: DeviceStorageRepository,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_card_${app.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isSelected) {
                ext.emeraldAccent.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (app.isSelected) ext.emeraldAccent else ext.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIconThumbnail(
                packageName = app.packageName,
                appName = app.appName,
                repository = repository,
                size = 46.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Total: ${Formatters.formatBytesOrUnavailable(app.totalBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.blueAccent
                    )
                    Text(
                        text = "Caché: ${Formatters.formatBytesOrUnavailable(app.cacheBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.emeraldAccent
                    )
                }
                if (app.protectionReason != null) {
                    ProtectedAppBadge(reason = app.protectionReason)
                }
            }

            Checkbox(
                checked = app.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("app_checkbox_${app.packageName}"),
                colors = CheckboxDefaults.colors(checkedColor = ext.emeraldAccent)
            )
        }
    }
}
