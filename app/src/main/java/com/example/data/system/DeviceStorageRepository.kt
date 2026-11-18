package com.example.data.system

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.example.domain.model.CompatibilityInfo
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.StorageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class AppSizeMeasurement(
    val appBytes: Long?,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val totalBytes: Long?
)

class DeviceStorageRepository(private val context: Context) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    suspend fun getDeviceStorageSummary(): StorageSummary = withContext(Dispatchers.IO) {
        val ownCache = calculateOwnAppCacheBytes()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val statsManager = context.getSystemService(StorageStatsManager::class.java)
                if (statsManager != null) {
                    val total = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                    val free = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                    val used = (total - free).coerceAtLeast(0L)
                    return@withContext StorageSummary(
                        totalBytes = total,
                        usedBytes = used,
                        availableBytes = free,
                        usedPercentage = Formatters.formatPercent(used, total),
                        ownCacheBytes = ownCache
                    )
                }
            } catch (_: Exception) {
                // Fallback a StatFs si el fabricante o perfil restringe StorageStatsManager
            }
        }

        val statFs = StatFs(Environment.getDataDirectory().path)
        val total = statFs.totalBytes
        val available = statFs.availableBytes
        val used = (total - available).coerceAtLeast(0L)
        StorageSummary(
            totalBytes = total,
            usedBytes = used,
            availableBytes = available,
            usedPercentage = Formatters.formatPercent(used, total),
            ownCacheBytes = ownCache
        )
    }

    fun calculateOwnAppCacheBytes(): Long {
        var total = 0L
        context.cacheDir?.let { total += folderSize(it) }
        context.codeCacheDir?.let { total += folderSize(it) }
        context.externalCacheDir?.let { total += folderSize(it) }
        return total
    }

    suspend fun clearOwnAppCache(): Long = withContext(Dispatchers.IO) {
        val before = calculateOwnAppCacheBytes()
        context.cacheDir?.let { deleteChildren(it) }
        context.codeCacheDir?.let { deleteChildren(it) }
        context.externalCacheDir?.let { deleteChildren(it) }
        val after = calculateOwnAppCacheBytes()
        (before - after).coerceAtLeast(0L)
    }

    suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val hasUsageAccess = hasUsageStatsPermission()
        val usageMap = if (hasUsageAccess) queryRecentUsageMap() else emptyMap()
        val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasUsageAccess) {
            context.getSystemService(StorageStatsManager::class.java)
        } else {
            null
        }

        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launchIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
        }

        val seenPackages = LinkedHashSet<String>()
        val result = ArrayList<InstalledAppInfo>(resolveInfos.size)

        for (resolveInfo in resolveInfos) {
            val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
            val packageName = appInfo.packageName ?: continue
            if (!seenPackages.add(packageName)) continue

            val label = try {
                pm.getApplicationLabel(appInfo).toString().ifBlank { packageName }
            } catch (_: Exception) {
                packageName
            }

            val versionName = try {
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }
                pkgInfo.versionName ?: "1.0"
            } catch (_: Exception) {
                "No disponible"
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

            val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                    @Suppress("DEPRECATION") ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
            } else {
                @Suppress("DEPRECATION") ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
            }

            val protectionReason = SmartCleanAnalyzer.detectProtectionReason(
                packageName = packageName,
                isGameCategory = isGame,
                isSystemApp = isSystem
            )

            val sizeMeasurement = queryAppSizeInternal(
                storageStatsManager = storageStatsManager,
                appInfo = appInfo,
                hasUsageAccess = hasUsageAccess
            )

            val info = InstalledAppInfo(
                packageName = packageName,
                appName = label,
                versionName = versionName,
                isSystemApp = isSystem,
                isProtectedApp = protectionReason != null,
                protectionReason = protectionReason,
                appBytes = sizeMeasurement.appBytes,
                dataBytes = sizeMeasurement.dataBytes,
                cacheBytes = sizeMeasurement.cacheBytes,
                totalBytes = sizeMeasurement.totalBytes,
                lastTimeUsedMillis = usageMap[packageName],
                isSelected = false,
                smartRecommendation = null
            )

            result.add(
                info.copy(smartRecommendation = SmartCleanAnalyzer.evaluateApp(info))
            )
        }

        result.sortBy { it.appName.lowercase(Locale("es", "ES")) }
        result
    }

    suspend fun measureSingleAppSize(packageName: String): AppSizeMeasurement = withContext(Dispatchers.IO) {
        val hasUsageAccess = hasUsageStatsPermission()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || (!hasUsageAccess && packageName != context.packageName)) {
            return@withContext AppSizeMeasurement(null, null, null, null)
        }
        val pm = context.packageManager
        val appInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        } catch (_: Exception) {
            return@withContext AppSizeMeasurement(null, null, null, null)
        }
        val statsManager = context.getSystemService(StorageStatsManager::class.java)
        queryAppSizeInternal(statsManager, appInfo, hasUsageAccess)
    }

    private fun queryAppSizeInternal(
        storageStatsManager: StorageStatsManager?,
        appInfo: ApplicationInfo,
        hasUsageAccess: Boolean
    ): AppSizeMeasurement {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || storageStatsManager == null) {
            return AppSizeMeasurement(null, null, null, null)
        }
        if (!hasUsageAccess && appInfo.packageName != context.packageName) {
            return AppSizeMeasurement(null, null, null, null)
        }
        return try {
            queryStatsApi26(storageStatsManager, appInfo)
        } catch (_: SecurityException) {
            AppSizeMeasurement(null, null, null, null)
        } catch (_: Exception) {
            AppSizeMeasurement(null, null, null, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun queryStatsApi26(
        storageStatsManager: StorageStatsManager,
        appInfo: ApplicationInfo
    ): AppSizeMeasurement {
        val stats = storageStatsManager.queryStatsForPackage(
            appInfo.storageUuid,
            appInfo.packageName,
            Process.myUserHandle()
        )
        val appBytes = stats.appBytes
        val dataBytes = stats.dataBytes
        val cacheBytes = stats.cacheBytes
        return AppSizeMeasurement(
            appBytes = appBytes,
            dataBytes = dataBytes,
            cacheBytes = cacheBytes,
            totalBytes = appBytes + dataBytes + cacheBytes
        )
    }

    private fun queryRecentUsageMap(): Map<String, Long> {
        return try {
            val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()
            val end = System.currentTimeMillis()
            val start = end - (60L * 24L * 60L * 60L * 1000L)
            val stats = usageManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, start, end)
            val map = HashMap<String, Long>()
            stats?.forEach { entry ->
                val existing = map[entry.packageName] ?: 0L
                if (entry.lastTimeUsed > existing) {
                    map[entry.packageName] = entry.lastTimeUsed
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun loadAppIconBitmap(packageName: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (_: Exception) {
            null
        }
    }

    fun buildAppInfoIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildUsageAccessIntent(): Intent {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val packageSpecific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (packageSpecific.resolveActivity(pm) != null) {
                return packageSpecific
            }
        }
        val generic = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (generic.resolveActivity(pm) != null) {
            return generic
        }
        return Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildSystemClearCacheIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val intent = Intent(StorageManager.ACTION_CLEAR_APP_CACHE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) intent else null
    }

    fun getCompatibilityInfo(): CompatibilityInfo {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val upperMfr = manufacturer.uppercase(Locale.ROOT)
        val hasStorageStats = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        val supportsSystemClear = buildSystemClearCacheIntent() != null

        val (title, steps) = when {
            upperMfr.contains("SAMSUNG") -> "Samsung (One UI)" to listOf(
                "En Información de la aplicación, toca 'Almacenamiento'.",
                "Usa 'Eliminar caché' en la esquina inferior derecha.",
                "Para datos, toca 'Eliminar datos' y confirma solo si estás seguro."
            )
            upperMfr.contains("XIAOMI") || upperMfr.contains("REDMI") || upperMfr.contains("POCO") ->
                "Xiaomi / Redmi / POCO (MIUI / HyperOS)" to listOf(
                    "En Información de la aplicación, toca 'Almacenamiento' o 'Limpiar datos'.",
                    "Selecciona 'Limpiar caché' (evita 'Borrar todos los datos' salvo que lo hayas confirmado).",
                    "Si MIUI bloquea el acceso directo, abre Ajustes > Aplicaciones > Administrar aplicaciones."
                )
            upperMfr.contains("MOTOROLA") -> "Motorola (MyUX / Hello UI)" to listOf(
                "Toca 'Almacenamiento y caché' dentro de Información de la aplicación.",
                "Pulsa 'Borrar caché' para liberar archivos temporales de forma segura."
            )
            upperMfr.contains("ZTE") || upperMfr.contains("NUBIA") -> "ZTE / Nubia (MyOS)" to listOf(
                "Abre 'Almacenamiento y caché' o 'Almacenamiento' según la versión de MyOS.",
                "Selecciona 'Borrar caché' y regresa con el botón Atrás a CLEAN MASTER AI."
            )
            upperMfr.contains("TECNO") || upperMfr.contains("INFINIX") -> "Tecno / Infinix (HiOS / XOS)" to listOf(
                "Entra en 'Almacenamiento y caché' dentro de Detalles de la app.",
                "Toca 'Borrar caché' y vuelve a CLEAN MASTER AI para verificar el espacio."
            )
            else -> "Android Estándar ($manufacturer)" to listOf(
                "En la pantalla oficial de la app, busca 'Almacenamiento y caché' o 'Almacenamiento'.",
                "Toca 'Borrar caché' para eliminar archivos temporales sin perder cuentas ni sesiones.",
                "Regresa a CLEAN MASTER AI para comprobar si el sistema actualizó el tamaño."
            )
        }

        return CompatibilityInfo(
            androidVersion = Build.VERSION.RELEASE ?: "Desconocida",
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = manufacturer.ifBlank { "Genérico" },
            model = model.ifBlank { "Dispositivo Android" },
            hasStorageStatsApi = hasStorageStats,
            hasUsageAccess = hasUsageStatsPermission(),
            supportsSystemClearCacheIntent = supportsSystemClear,
            oemGuidanceTitle = title,
            oemGuidanceSteps = steps
        )
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) folderSize(file) else file.length()
        }
        return size
    }

    private fun deleteChildren(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                deleteChildren(file)
                file.delete()
            } else {
                file.delete()
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
