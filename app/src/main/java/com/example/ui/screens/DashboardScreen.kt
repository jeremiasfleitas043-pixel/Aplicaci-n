package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.system.Formatters
import com.example.ui.MainUiState
import com.example.ui.components.MetricStatCard
import com.example.ui.components.NeuralStorageOrb
import com.example.ui.components.UsageAccessBanner
import com.example.ui.theme.LocalExtendedColors

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onAnalyzeApps: () -> Unit,
    onOpenMultiClean: () -> Unit,
    onOpenCacheManager: () -> Unit,
    onOpenClearData: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenHistory: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onRefresh: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val summary = uiState.storageSummary
    val hasUsageAccess = uiState.compatibilityInfo?.hasUsageAccess ?: false

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CLEAN MASTER AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Análisis real y seguro de almacenamiento",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("dashboard_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualizar análisis",
                            tint = ext.emeraldAccent
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(visible = uiState.isScanning) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { uiState.scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ext.emeraldAccent
                        )
                        Text(
                            text = "Analizando aplicaciones y volúmenes del dispositivo...",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.emeraldAccent
                        )
                    }
                }
            }

            // Hero Storage Orb + Main Action Buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    if (isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            NeuralStorageOrb(
                                usedPercentage = summary?.usedPercentage ?: 0,
                                primaryMetricText = summary?.let { Formatters.formatBytes(it.availableBytes) } ?: "...",
                                secondaryLabelText = "Disponible",
                                isScanning = uiState.isScanning
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DashboardHeroActions(
                                    smartRecommendationCount = uiState.smartRecommendedApps.size,
                                    onAnalyzeApps = onAnalyzeApps,
                                    onOpenMultiClean = onOpenMultiClean
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            NeuralStorageOrb(
                                usedPercentage = summary?.usedPercentage ?: 0,
                                primaryMetricText = summary?.let { Formatters.formatBytes(it.availableBytes) } ?: "...",
                                secondaryLabelText = "Disponible",
                                isScanning = uiState.isScanning
                            )
                            DashboardHeroActions(
                                smartRecommendationCount = uiState.smartRecommendedApps.size,
                                onAnalyzeApps = onAnalyzeApps,
                                onOpenMultiClean = onOpenMultiClean
                            )
                        }
                    }
                }
            }

            if (!hasUsageAccess) {
                item {
                    UsageAccessBanner(onGrantClick = onGrantUsageAccess)
                }
            }

            // Real Storage & App Stats Grid (Total, Ocupado, Disponible, Apps detectadas, Seleccionadas)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Estado del dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "Almacenamiento total",
                            value = summary?.let { Formatters.formatBytes(it.totalBytes) } ?: "No disponible",
                            subtitle = "Capacidad interna",
                            icon = Icons.Rounded.Storage,
                            accentColor = ext.blueAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Almacenamiento ocupado",
                            value = summary?.let { Formatters.formatBytes(it.usedBytes) } ?: "No disponible",
                            subtitle = "${summary?.usedPercentage ?: 0}% en uso",
                            icon = Icons.Rounded.Memory,
                            accentColor = ext.warning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "Disponible",
                            value = summary?.let { Formatters.formatBytes(it.availableBytes) } ?: "No disponible",
                            subtitle = "Espacio libre real",
                            icon = Icons.Rounded.CleaningServices,
                            accentColor = ext.emeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Apps detectadas",
                            value = "${uiState.allApps.size}",
                            subtitle = "${uiState.selectedPackages.size} seleccionada(s)",
                            icon = Icons.Rounded.Apps,
                            accentColor = ext.cyanAccent,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenApps
                        )
                    }
                }
            }

            // Quick Modules Row (Caché, Borrar Datos, Selección, Cola)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Caché",
                        value = "Gestor",
                        subtitle = "Propia: ${Formatters.formatBytes(summary?.ownCacheBytes ?: 0L)}",
                        icon = Icons.Rounded.DeleteSweep,
                        accentColor = ext.emeraldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCacheManager
                    )
                    MetricStatCard(
                        title = "Borrar datos",
                        value = "Modo seguro",
                        subtitle = "Con confirmación",
                        icon = Icons.Rounded.Layers,
                        accentColor = ext.danger,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenClearData
                    )
                }
            }

            // Selected count pill + Smart Clean entry
            item {
                Card(
                    onClick = onAnalyzeApps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_smart_clean_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.emeraldAccent.copy(alpha = 0.38f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ext.emeraldAccent.copy(alpha = 0.16f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Limpieza inteligente",
                                tint = ext.emeraldAccent,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Limpieza inteligente",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.smartRecommendedApps.isNotEmpty()) {
                                    "${uiState.smartRecommendedApps.size} app(s) recomendadas para revisar (sin borrado automático)"
                                } else {
                                    "Analiza aplicaciones y recibe recomendaciones seguras"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent Cleaning History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Historial reciente",
                            tint = ext.blueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Historial de limpiezas reciente",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Text("Ver todo")
                    }
                }
            }

            if (uiState.recentHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Aún no hay limpiezas registradas. Toda operación real o verificada se guardará aquí localmente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentHistory, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${entry.operationType} • ${Formatters.formatDateTime(entry.timestampMillis)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = entry.status,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (entry.status == "Completada") ext.success else ext.warning
                                )
                                Text(
                                    text = entry.recoveredBytes?.let { Formatters.formatBytes(it) } ?: "No verificable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DashboardHeroActions(
    smartRecommendationCount: Int,
    onAnalyzeApps: () -> Unit,
    onOpenMultiClean: () -> Unit
) {
    val ext = LocalExtendedColors.current
    if (smartRecommendationCount > 0) {
        Text(
            text = "$smartRecommendationCount aplicación(es) sugerida(s) para revisión segura",
            style = MaterialTheme.typography.bodyMedium,
            color = ext.emeraldAccent
        )
    } else {
        Text(
            text = "Almacenamiento medido en tiempo real desde Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onAnalyzeApps,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp)
                .testTag("analyze_apps_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Analizar apps", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledTonalButton(
            onClick = onOpenMultiClean,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp)
                .testTag("multi_clean_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = ext.blueAccent.copy(alpha = 0.2f),
                contentColor = ext.blueAccent
            )
        ) {
            Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Limpieza múltiple", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
