package com.example.domain.model

data class StorageSummary(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usedPercentage: Int,
    val ownCacheBytes: Long
)

enum class AppSortOption(val label: String) {
    NAME_ASC("Nombre (A-Z)"),
    NAME_DESC("Nombre (Z-A)"),
    SIZE_DESC("Mayor tamaño"),
    CACHE_DESC("Mayor caché"),
    SELECTED_FIRST("Seleccionadas")
}

enum class AppFilterOption(val label: String) {
    ALL("Todas"),
    USER_ONLY("Usuario"),
    PROTECTED("Protegidas"),
    WITH_CACHE("Con caché")
}

data class SmartRecommendation(
    val reason: String,
    val categoryLabel: String,
    val safeToQueueCache: Boolean
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isProtectedApp: Boolean,
    val protectionReason: String?,
    val appBytes: Long?,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val totalBytes: Long?,
    val lastTimeUsedMillis: Long?,
    val isSelected: Boolean = false,
    val smartRecommendation: SmartRecommendation? = null
)

enum class QueueAction(val label: String) {
    CLEAR_CACHE("Limpiar caché"),
    CLEAR_DATA("Borrar datos"),
    DO_NOT_MODIFY("Sin modificar")
}

enum class OperationStatus(val label: String) {
    PENDING("Pendiente"),
    IN_PROGRESS("En proceso"),
    REQUIRES_CONFIRMATION("Requiere confirmación"),
    COMPLETED("Completada"),
    NOT_ALLOWED("No permitida"),
    ERROR("Error")
}

data class CleanQueueItem(
    val packageName: String,
    val appName: String,
    val isProtectedApp: Boolean,
    val protectionReason: String?,
    val action: QueueAction = QueueAction.CLEAR_CACHE,
    val status: OperationStatus = OperationStatus.PENDING,
    val cacheBeforeBytes: Long? = null,
    val dataBeforeBytes: Long? = null,
    val recoveredBytes: Long? = null,
    val statusDetail: String = ""
)

data class ActiveGuideSession(
    val source: GuideSource,
    val packageName: String,
    val appName: String,
    val action: QueueAction,
    val cacheBeforeBytes: Long?,
    val dataBeforeBytes: Long?,
    val stepIndex: Int,
    val totalSteps: Int,
    val launchedSettings: Boolean = false
)

enum class GuideSource {
    CACHE_MANAGER,
    CLEAR_DATA_MANAGER,
    MULTI_CLEAN_QUEUE
}

data class VerificationPrompt(
    val packageName: String,
    val appName: String,
    val action: QueueAction,
    val source: GuideSource,
    val hasUsageStatsAccess: Boolean,
    val previousBytes: Long?,
    val currentBytes: Long?,
    val detailMessage: String
)

data class CompatibilityInfo(
    val androidVersion: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val hasStorageStatsApi: Boolean,
    val hasUsageAccess: Boolean,
    val supportsSystemClearCacheIntent: Boolean,
    val oemGuidanceTitle: String,
    val oemGuidanceSteps: List<String>
)
