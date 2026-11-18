package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.system.DeviceStorageRepository
import com.example.domain.model.OperationStatus
import com.example.ui.theme.LocalExtendedColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val iconMemoryCache = object : LinkedHashMap<String, ImageBitmap>(120, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
        return size > 150
    }
}

@Composable
fun AppIconThumbnail(
    packageName: String,
    appName: String,
    repository: DeviceStorageRepository,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val cached = iconMemoryCache[packageName]
    val bitmapState by produceState<ImageBitmap?>(initialValue = cached, key1 = packageName) {
        if (value == null) {
            val loaded: Bitmap? = repository.loadAppIconBitmap(packageName)
            if (loaded != null) {
                val img = loaded.asImageBitmap()
                iconMemoryCache[packageName] = img
                value = img
            }
        }
    }

    val ext = LocalExtendedColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmapState
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Icono de $appName",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = "Icono predeterminado de $appName",
                tint = ext.emeraldAccent,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
fun NeuralStorageOrb(
    usedPercentage: Int,
    primaryMetricText: String,
    secondaryLabelText: String,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (usedPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "storage_orb_progress"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isScanning) 3000 else 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_glow"
    )

    val emerald = ext.emeraldAccent
    val cyan = ext.cyanAccent
    val blue = ext.blueAccent
    val track = ext.orbTrack
    val surfaceBg = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = modifier
            .size(204.dp)
            .testTag("storage_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokePx = 14.dp.toPx()
            val radius = (diameter - strokePx) / 2f

            // Radial ambient glow behind orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        emerald.copy(alpha = 0.16f * glowPulse),
                        blue.copy(alpha = 0.08f * glowPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                radius = radius * 1.05f,
                center = center
            )

            // Inner subtle tech surface
            drawCircle(
                color = surfaceBg.copy(alpha = 0.72f),
                radius = radius - strokePx * 0.85f,
                center = center
            )

            // Particle ring dots inspired by futuristic mesh orb
            val dotCount = 28
            val innerParticleRadius = radius - strokePx * 1.15f
            for (i in 0 until dotCount) {
                val angleRad = ((i * (360f / dotCount)) + rotationAngle) * (PI / 180.0)
                val wobble = sin((i * 2 + rotationAngle * 0.05) * PI / 180.0).toFloat() * 4.dp.toPx()
                val r = innerParticleRadius + wobble
                val px = center.x + (r * cos(angleRad)).toFloat()
                val py = center.y + (r * sin(angleRad)).toFloat()
                val dotColor = if (i % 2 == 0) emerald else cyan
                drawCircle(
                    color = dotColor.copy(alpha = 0.35f + 0.3f * ((i % 4) / 3f)),
                    radius = if (i % 3 == 0) 2.2.dp.toPx() else 1.4.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Outer background track
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active progress arc (Emerald -> Cyan -> Electric Blue)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(emerald, cyan, blue, emerald),
                    center = center
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isScanning) "..." else "$usedPercentage%",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = primaryMetricText,
                style = MaterialTheme.typography.titleMedium,
                color = ext.emeraldAccent
            )
            Text(
                text = secondaryLabelText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val ext = LocalExtendedColors.current
    val shape = RoundedCornerShape(18.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, ext.cardBorder)
        ) {
            MetricCardContent(title, value, subtitle, icon, accentColor)
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, ext.cardBorder)
        ) {
            MetricCardContent(title, value, subtitle, icon, accentColor)
        }
    }
}

@Composable
private fun MetricCardContent(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusBadgeChip(
    status: OperationStatus,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val (bgColor, fgColor, icon) = when (status) {
        OperationStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Rounded.Schedule
        )
        OperationStatus.IN_PROGRESS -> Triple(
            ext.blueAccent.copy(alpha = 0.16f),
            ext.blueAccent,
            Icons.Rounded.HourglassTop
        )
        OperationStatus.REQUIRES_CONFIRMATION -> Triple(
            ext.warning.copy(alpha = 0.18f),
            ext.warning,
            Icons.Rounded.NotificationsActive
        )
        OperationStatus.COMPLETED -> Triple(
            ext.success.copy(alpha = 0.18f),
            ext.success,
            Icons.Rounded.CheckCircle
        )
        OperationStatus.NOT_ALLOWED -> Triple(
            ext.danger.copy(alpha = 0.15f),
            ext.danger,
            Icons.Outlined.Shield
        )
        OperationStatus.ERROR -> Triple(
            ext.danger.copy(alpha = 0.18f),
            ext.danger,
            Icons.Rounded.ErrorOutline
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status.label,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fgColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ProtectedAppBadge(
    reason: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ext.warning.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = "Aplicación protegida",
                tint = ext.warning,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = ext.warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UsageAccessBanner(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ext.blueAccent.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, ext.blueAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = "Permiso de estadísticas de uso",
                    tint = ext.blueAccent
                )
                Text(
                    text = "Acceso especial requerido para medir tamaños exactos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Sin el Acceso a datos de uso, Android oculta los valores de Aplicación, Datos y Caché de otras apps y se mostrarán como \"No disponible\". Actívalo solo si deseas mediciones reales.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onGrantClick,
                    shape = RoundedCornerShape(12.dp),
                    color = ext.blueAccent,
                    modifier = Modifier.testTag("grant_usage_access_button")
                ) {
                    Text(
                        text = "Configurar acceso de uso",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
