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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.UserSettings
import com.example.domain.model.CompatibilityInfo
import com.example.ui.theme.LocalExtendedColors

@Composable
fun SettingsScreen(
    userSettings: UserSettings,
    compatibilityInfo: CompatibilityInfo?,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleConfirmClearData: (Boolean) -> Unit,
    onOpenUsageAccessSettings: () -> Unit
) {
    val ext = LocalExtendedColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Apariencia y seguridad
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = ext.emeraldAccent)
                                Column {
                                    Text("Tema oscuro", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (userSettings.isDarkTheme) "Oscuro tecnológico activo" else "Tema claro activo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = userSettings.isDarkTheme,
                                onCheckedChange = onToggleDarkTheme,
                                modifier = Modifier.testTag("dark_theme_switch")
                            )
                        }

                        HorizontalDivider(color = ext.cardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = ext.blueAccent)
                                Column {
                                    Text("Idioma", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Español (ES) — Nativo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ext.blueAccent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "ES",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ext.blueAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = ext.cardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = null, tint = ext.warning)
                                Column {
                                    Text("Confirmación de borrado de datos", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Exige diálogo de advertencia antes de abrir Ajustes para borrar datos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = userSettings.confirmClearData,
                                onCheckedChange = onToggleConfirmClearData,
                                modifier = Modifier.testTag("confirm_clear_data_switch")
                            )
                        }
                    }
                }
            }

            // Información de permisos
            item {
                val hasUsageAccess = compatibilityInfo?.hasUsageAccess == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = ext.emeraldAccent)
                            Text("Información de permisos", style = MaterialTheme.typography.titleMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PACKAGE_USAGE_STATS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (hasUsageAccess) ext.success.copy(alpha = 0.18f) else ext.warning.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = if (hasUsageAccess) "Concedido" else "No concedido",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (hasUsageAccess) ext.success else ext.warning,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.usage_access_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onOpenUsageAccessSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_usage_access_btn")
                        ) {
                            Text(if (hasUsageAccess) "Revisar acceso en Ajustes" else "Otorgar Acceso a datos de uso")
                        }

                        HorizontalDivider(color = ext.cardBorder)

                        Text(
                            text = "Visibilidad de aplicaciones (<queries> vs QUERY_ALL_PACKAGES)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ext.blueAccent
                        )
                        Text(
                            text = stringResource(R.string.query_packages_explanation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.query_all_packages_restriction),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Información de compatibilidad
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Devices, contentDescription = null, tint = ext.blueAccent)
                            Text("Compatibilidad del dispositivo", style = MaterialTheme.typography.titleMedium)
                        }

                        if (compatibilityInfo != null) {
                            CompatRow("Sistema", "Android ${compatibilityInfo.androidVersion} (API ${compatibilityInfo.sdkInt})")
                            CompatRow("Fabricante / Modelo", "${compatibilityInfo.manufacturer} ${compatibilityInfo.model}")
                            CompatRow("StorageStatsManager", if (compatibilityInfo.hasStorageStatsApi) "Compatible (Android 8.0+)" else "No disponible (< Android 8.0)")
                            CompatRow("Asistente CLEAR_APP_CACHE", if (compatibilityInfo.supportsSystemClearCacheIntent) "Soportado por el sistema" else "Alternativa oficial por Ajustes")

                            HorizontalDivider(color = ext.cardBorder)
                            Text(
                                text = "Guía adaptada para ${compatibilityInfo.oemGuidanceTitle}:",
                                style = MaterialTheme.typography.labelLarge,
                                color = ext.emeraldAccent
                            )
                            compatibilityInfo.oemGuidanceSteps.forEachIndexed { idx, step ->
                                Text(
                                    text = "${idx + 1}. $step",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Compatible con interfaces personalizadas de Samsung, Xiaomi, Motorola, ZTE, Nubia, Tecno, Infinix y Android estándar sin root ni servicios de accesibilidad.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Acerca de CLEAN MASTER AI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = ext.emeraldAccent)
                            Text(
                                text = "Acerca de CLEAN MASTER AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        CompatRow("Paquete", BuildConfig.APPLICATION_ID)
                        CompatRow("Versión", BuildConfig.VERSION_NAME)
                        Text(
                            text = "Procesamiento 100% local mediante APIs oficiales de Android (PackageManager, StorageStatsManager, StorageManager y DataStore/Room). Sin servidores externos, sin root, sin ADB ni automatizaciones ocultas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CompatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
