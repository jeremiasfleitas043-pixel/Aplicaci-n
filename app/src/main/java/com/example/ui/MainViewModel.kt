package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CleaningHistoryEntity
import com.example.data.local.CleaningHistoryRepository
import com.example.data.local.SettingsPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.system.DeviceStorageRepository
import com.example.data.system.Formatters
import com.example.domain.model.ActiveGuideSession
import com.example.domain.model.AppFilterOption
import com.example.domain.model.AppSortOption
import com.example.domain.model.CleanQueueItem
import com.example.domain.model.CompatibilityInfo
import com.example.domain.model.GuideSource
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.OperationStatus
import com.example.domain.model.QueueAction
import com.example.domain.model.StorageSummary
import com.example.domain.model.VerificationPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class MainUiState(
    val isInitialLoading: Boolean = true,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val storageSummary: StorageSummary? = null,
    val allApps: List<InstalledAppInfo> = emptyList(),
    val filteredApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val cacheSelectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOption: AppSortOption = AppSortOption.NAME_ASC,
    val filterOption: AppFilterOption = AppFilterOption.ALL,
    val inspectedApp: InstalledAppInfo? = null,
    val clearDataCandidate: InstalledAppInfo? = null,
    val showSmartCleanSheet: Boolean = false,
    val smartRecommendedApps: List<InstalledAppInfo> = emptyList(),
    val multiCleanQueue: List<CleanQueueItem> = emptyList(),
    val isQueueActive: Boolean = false,
    val activeGuideSession: ActiveGuideSession? = null,
    val pendingClearDataQueueConfirm: CleanQueueItem? = null,
    val verificationPrompt: VerificationPrompt? = null,
    val compatibilityInfo: CompatibilityInfo? = null,
    val userSettings: UserSettings = UserSettings(),
    val historyList: List<CleaningHistoryEntity> = emptyList(),
    val recentHistory: List<CleaningHistoryEntity> = emptyList(),
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val storageRepository = DeviceStorageRepository(application)
    private val settingsRepository = SettingsPreferencesRepository(application)
    private val historyRepository = CleaningHistoryRepository(
        AppDatabase.getInstance(application).cleaningHistoryDao()
    )

    private val internalState = MutableStateFlow(MainUiState())
    private var lastKnownUsageAccess = storageRepository.hasUsageStatsPermission()

    val uiState: StateFlow<MainUiState> = combine(
        internalState,
        settingsRepository.settingsFlow,
        historyRepository.allHistory,
        historyRepository.recentHistory
    ) { state, settings, allHistory, recentHistory ->
        state.copy(
            userSettings = settings,
            historyList = allHistory,
            recentHistory = recentHistory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(compatibilityInfo = storageRepository.getCompatibilityInfo())
    )

    init {
        refreshAll(runSmartScanAnimation = false)
    }

    fun refreshAll(runSmartScanAnimation: Boolean = true) {
        viewModelScope.launch {
            internalState.update {
                it.copy(
                    isScanning = true,
                    scanProgress = if (runSmartScanAnimation) 0.15f else 0.5f,
                    compatibilityInfo = storageRepository.getCompatibilityInfo()
                )
            }
            if (runSmartScanAnimation) {
                delay(180)
                internalState.update { it.copy(scanProgress = 0.45f) }
            }

            val summary = storageRepository.getDeviceStorageSummary()
            val loadedApps = storageRepository.loadInstalledApps()

            if (runSmartScanAnimation) {
                internalState.update { it.copy(scanProgress = 0.85f) }
                delay(140)
            }

            internalState.update { current ->
                val validSelected = current.selectedPackages.intersect(loadedApps.map { it.packageName }.toSet())
                val validCacheSelected = current.cacheSelectedPackages.intersect(loadedApps.map { it.packageName }.toSet())
                val syncedApps = loadedApps.map { it.copy(isSelected = it.packageName in validSelected) }
                val recommended = syncedApps
                    .filter { it.smartRecommendation != null }
                    .sortedByDescending { it.cacheBytes ?: it.totalBytes ?: 0L }

                current.copy(
                    isInitialLoading = false,
                    isScanning = false,
                    scanProgress = 1f,
                    storageSummary = summary,
                    allApps = syncedApps,
                    filteredApps = applyFilterAndSort(
                        apps = syncedApps,
                        query = current.searchQuery,
                        sort = current.sortOption,
                        filter = current.filterOption
                    ),
                    selectedPackages = validSelected,
                    cacheSelectedPackages = validCacheSelected,
                    smartRecommendedApps = recommended,
                    compatibilityInfo = storageRepository.getCompatibilityInfo(),
                    inspectedApp = current.inspectedApp?.let { prev ->
                        syncedApps.find { it.packageName == prev.packageName }
                    }
                )
            }
        }
    }

    fun onAppResumed() {
        val currentUsageAccess = storageRepository.hasUsageStatsPermission()
        val accessChanged = currentUsageAccess != lastKnownUsageAccess
        lastKnownUsageAccess = currentUsageAccess

        if (accessChanged) {
            refreshAll(runSmartScanAnimation = false)
        } else {
            internalState.update {
                it.copy(compatibilityInfo = storageRepository.getCompatibilityInfo())
            }
        }

        val session = internalState.value.activeGuideSession
        if (session != null && session.launchedSettings) {
            verifyReturnedGuideSession(session)
        }
    }

    fun analyzeAndOpenSmartClean() {
        viewModelScope.launch {
            refreshAll(runSmartScanAnimation = true)
            internalState.update { it.copy(showSmartCleanSheet = true) }
        }
    }

    fun dismissSmartCleanSheet() {
        internalState.update { it.copy(showSmartCleanSheet = false) }
    }

    fun updateSearchQuery(query: String) {
        internalState.update { current ->
            current.copy(
                searchQuery = query,
                filteredApps = applyFilterAndSort(
                    apps = current.allApps,
                    query = query,
                    sort = current.sortOption,
                    filter = current.filterOption
                )
            )
        }
    }

    fun updateSortOption(option: AppSortOption) {
        internalState.update { current ->
            current.copy(
                sortOption = option,
                filteredApps = applyFilterAndSort(
                    apps = current.allApps,
                    query = current.searchQuery,
                    sort = option,
                    filter = current.filterOption
                )
            )
        }
    }

    fun updateFilterOption(option: AppFilterOption) {
        internalState.update { current ->
            current.copy(
                filterOption = option,
                filteredApps = applyFilterAndSort(
                    apps = current.allApps,
                    query = current.searchQuery,
                    sort = current.sortOption,
                    filter = option
                )
            )
        }
    }

    fun toggleAppSelection(packageName: String) {
        internalState.update { current ->
            val newSelection = current.selectedPackages.toMutableSet().apply {
                if (!add(packageName)) remove(packageName)
            }
            val updatedApps = current.allApps.map {
                it.copy(isSelected = it.packageName in newSelection)
            }
            current.copy(
                selectedPackages = newSelection,
                allApps = updatedApps,
                filteredApps = applyFilterAndSort(
                    apps = updatedApps,
                    query = current.searchQuery,
                    sort = current.sortOption,
                    filter = current.filterOption
                )
            )
        }
    }

    fun selectAllFilteredApps() {
        internalState.update { current ->
            val newSelection = current.selectedPackages + current.filteredApps.map { it.packageName }
            val updatedApps = current.allApps.map { it.copy(isSelected = it.packageName in newSelection) }
            current.copy(
                selectedPackages = newSelection,
                allApps = updatedApps,
                filteredApps = applyFilterAndSort(
                    apps = updatedApps,
                    query = current.searchQuery,
                    sort = current.sortOption,
                    filter = current.filterOption
                )
            )
        }
    }

    fun deselectAllApps() {
        internalState.update { current ->
            val updatedApps = current.allApps.map { it.copy(isSelected = false) }
            current.copy(
                selectedPackages = emptySet(),
                allApps = updatedApps,
                filteredApps = applyFilterAndSort(
                    apps = updatedApps,
                    query = current.searchQuery,
                    sort = current.sortOption,
                    filter = current.filterOption
                )
            )
        }
    }

    fun toggleCacheAppSelection(packageName: String) {
        internalState.update { current ->
            val next = current.cacheSelectedPackages.toMutableSet().apply {
                if (!add(packageName)) remove(packageName)
            }
            current.copy(cacheSelectedPackages = next)
        }
    }

    fun selectAllCacheApps() {
        internalState.update { current ->
            val all = current.allApps.map { it.packageName }.toSet()
            current.copy(cacheSelectedPackages = all)
        }
    }

    fun deselectAllCacheApps() {
        internalState.update { it.copy(cacheSelectedPackages = emptySet()) }
    }

    fun inspectApp(app: InstalledAppInfo?) {
        internalState.update { it.copy(inspectedApp = app) }
    }

    fun clearOwnCacheNow() {
        viewModelScope.launch {
            val ownBefore = storageRepository.calculateOwnAppCacheBytes()
            val deleted = storageRepository.clearOwnAppCache()
            val summary = storageRepository.getDeviceStorageSummary()
            historyRepository.recordEntry(
                appName = "CLEAN MASTER AI",
                packageName = getApplication<Application>().packageName,
                operationType = "Limpieza de caché propia",
                status = OperationStatus.COMPLETED.label,
                recoveredBytes = deleted,
                notes = if (ownBefore == 0L) "La caché interna ya estaba vacía" else "Limpieza directa oficial"
            )
            internalState.update {
                it.copy(
                    storageSummary = summary,
                    snackbarMessage = "Caché propia limpiada: ${Formatters.formatBytes(deleted)} liberados"
                )
            }
        }
    }

    fun startCacheGuidedQueue(packages: Set<String>, launchIntent: (Intent) -> Boolean) {
        val selectedApps = internalState.value.allApps.filter { it.packageName in packages }
        if (selectedApps.isEmpty()) {
            showSnackbar("Selecciona al menos una aplicación para limpiar caché")
            return
        }
        val ownPkg = getApplication<Application>().packageName
        val hasOwn = selectedApps.any { it.packageName == ownPkg }
        if (hasOwn) {
            clearOwnCacheNow()
        }
        val externalApps = selectedApps.filter { it.packageName != ownPkg }
        if (externalApps.isEmpty()) return

        val first = externalApps.first()
        val session = ActiveGuideSession(
            source = GuideSource.CACHE_MANAGER,
            packageName = first.packageName,
            appName = first.appName,
            action = QueueAction.CLEAR_CACHE,
            cacheBeforeBytes = first.cacheBytes,
            dataBeforeBytes = first.dataBytes,
            stepIndex = 0,
            totalSteps = externalApps.size,
            launchedSettings = false
        )
        internalState.update { it.copy(activeGuideSession = session) }
        launchCurrentGuideSettings(launchIntent)
    }

    fun requestClearDataForApp(app: InstalledAppInfo) {
        if (internalState.value.userSettings.confirmClearData || app.isProtectedApp) {
            internalState.update { it.copy(clearDataCandidate = app) }
        } else {
            internalState.update { it.copy(clearDataCandidate = app) }
        }
    }

    fun dismissClearDataDialog() {
        internalState.update { it.copy(clearDataCandidate = null) }
    }

    fun confirmClearDataAndOpenSettings(app: InstalledAppInfo, launchIntent: (Intent) -> Boolean) {
        val session = ActiveGuideSession(
            source = GuideSource.CLEAR_DATA_MANAGER,
            packageName = app.packageName,
            appName = app.appName,
            action = QueueAction.CLEAR_DATA,
            cacheBeforeBytes = app.cacheBytes,
            dataBeforeBytes = app.dataBytes,
            stepIndex = 0,
            totalSteps = 1,
            launchedSettings = false
        )
        internalState.update {
            it.copy(
                clearDataCandidate = null,
                activeGuideSession = session
            )
        }
        launchCurrentGuideSettings(launchIntent)
    }

    fun addAppsToMultiQueue(apps: List<InstalledAppInfo>, defaultAction: QueueAction) {
        internalState.update { current ->
            val existingByPkg = current.multiCleanQueue.associateBy { it.packageName }.toMutableMap()
            apps.forEach { app ->
                val safeAction = if (defaultAction == QueueAction.CLEAR_DATA && app.isProtectedApp) {
                    QueueAction.CLEAR_CACHE
                } else {
                    defaultAction
                }
                existingByPkg[app.packageName] = CleanQueueItem(
                    packageName = app.packageName,
                    appName = app.appName,
                    isProtectedApp = app.isProtectedApp,
                    protectionReason = app.protectionReason,
                    action = safeAction,
                    status = OperationStatus.PENDING,
                    cacheBeforeBytes = app.cacheBytes,
                    dataBeforeBytes = app.dataBytes
                )
            }
            current.copy(
                multiCleanQueue = existingByPkg.values.toList(),
                snackbarMessage = "${apps.size} aplicación(es) agregada(s) a Limpieza múltiple"
            )
        }
    }

    fun enqueueSmartRecommendations() {
        val safeApps = internalState.value.smartRecommendedApps.filter {
            it.smartRecommendation?.safeToQueueCache == true
        }
        if (safeApps.isEmpty()) {
            showSnackbar("No hay cachés recomendadas para agregar en este momento")
            return
        }
        addAppsToMultiQueue(safeApps, QueueAction.CLEAR_CACHE)
        internalState.update { it.copy(showSmartCleanSheet = false) }
    }

    fun updateQueueItemAction(packageName: String, action: QueueAction) {
        internalState.update { current ->
            val updated = current.multiCleanQueue.map { item ->
                if (item.packageName == packageName) {
                    item.copy(
                        action = action,
                        status = if (action == QueueAction.DO_NOT_MODIFY) {
                            OperationStatus.PENDING
                        } else {
                            OperationStatus.PENDING
                        },
                        statusDetail = ""
                    )
                } else {
                    item
                }
            }
            current.copy(multiCleanQueue = updated)
        }
    }

    fun removeQueueItem(packageName: String) {
        internalState.update { current ->
            current.copy(multiCleanQueue = current.multiCleanQueue.filterNot { it.packageName == packageName })
        }
    }

    fun clearMultiQueue() {
        internalState.update {
            it.copy(
                multiCleanQueue = emptyList(),
                isQueueActive = false,
                activeGuideSession = null
            )
        }
    }

    fun startMultiCleanQueue(launchIntent: (Intent) -> Boolean) {
        viewModelScope.launch {
            val ownPkg = getApplication<Application>().packageName
            val queue = internalState.value.multiCleanQueue
            if (queue.isEmpty()) {
                showSnackbar("Agrega aplicaciones a la cola antes de iniciar")
                return@launch
            }

            internalState.update { it.copy(isQueueActive = true) }

            // 1. Procesar caché propia directamente si está en cola
            val updatedList = queue.map { item ->
                if (item.action == QueueAction.DO_NOT_MODIFY) {
                    item.copy(statusDetail = "Excluida por el usuario")
                } else if (item.packageName == ownPkg && item.action == QueueAction.CLEAR_CACHE) {
                    val freed = storageRepository.clearOwnAppCache()
                    historyRepository.recordEntry(
                        appName = item.appName,
                        packageName = item.packageName,
                        operationType = item.action.label,
                        status = OperationStatus.COMPLETED.label,
                        recoveredBytes = freed,
                        notes = "Limpieza interna verificada"
                    )
                    item.copy(
                        status = OperationStatus.COMPLETED,
                        recoveredBytes = freed,
                        statusDetail = "Caché propia eliminada (${Formatters.formatBytes(freed)})"
                    )
                } else {
                    item
                }
            }

            internalState.update { it.copy(multiCleanQueue = updatedList) }
            advanceMultiCleanQueue(launchIntent)
        }
    }

    fun advanceMultiCleanQueue(launchIntent: (Intent) -> Boolean) {
        val queue = internalState.value.multiCleanQueue
        val actionable = queue.filter {
            it.action != QueueAction.DO_NOT_MODIFY &&
                (it.status == OperationStatus.PENDING || it.status == OperationStatus.IN_PROGRESS)
        }

        if (actionable.isEmpty()) {
            internalState.update {
                it.copy(
                    isQueueActive = false,
                    activeGuideSession = null,
                    snackbarMessage = "Cola de limpieza procesada"
                )
            }
            refreshAll(runSmartScanAnimation = false)
            return
        }

        val next = actionable.first()
        val totalActionable = queue.count { it.action != QueueAction.DO_NOT_MODIFY }
        val completedOrDone = queue.count {
            it.action != QueueAction.DO_NOT_MODIFY &&
                it.status != OperationStatus.PENDING &&
                it.status != OperationStatus.IN_PROGRESS
        }

        if (next.action == QueueAction.CLEAR_DATA) {
            internalState.update { current ->
                current.copy(
                    multiCleanQueue = current.multiCleanQueue.map {
                        if (it.packageName == next.packageName) {
                            it.copy(
                                status = OperationStatus.REQUIRES_CONFIRMATION,
                                statusDetail = "Requiere confirmación explícita antes de abrir Ajustes"
                            )
                        } else {
                            it
                        }
                    },
                    pendingClearDataQueueConfirm = next
                )
            }
            return
        }

        val session = ActiveGuideSession(
            source = GuideSource.MULTI_CLEAN_QUEUE,
            packageName = next.packageName,
            appName = next.appName,
            action = next.action,
            cacheBeforeBytes = next.cacheBeforeBytes,
            dataBeforeBytes = next.dataBeforeBytes,
            stepIndex = completedOrDone,
            totalSteps = totalActionable,
            launchedSettings = false
        )

        internalState.update { current ->
            current.copy(
                activeGuideSession = session,
                multiCleanQueue = current.multiCleanQueue.map {
                    if (it.packageName == next.packageName) {
                        it.copy(
                            status = OperationStatus.IN_PROGRESS,
                            statusDetail = "Abriendo pantalla oficial de Android..."
                        )
                    } else {
                        it
                    }
                }
            )
        }
        launchCurrentGuideSettings(launchIntent)
    }

    fun confirmQueueClearDataItem(item: CleanQueueItem, launchIntent: (Intent) -> Boolean) {
        val queue = internalState.value.multiCleanQueue
        val totalActionable = queue.count { it.action != QueueAction.DO_NOT_MODIFY }
        val done = queue.count {
            it.action != QueueAction.DO_NOT_MODIFY &&
                it.status != OperationStatus.PENDING &&
                it.status != OperationStatus.IN_PROGRESS
        }
        val session = ActiveGuideSession(
            source = GuideSource.MULTI_CLEAN_QUEUE,
            packageName = item.packageName,
            appName = item.appName,
            action = QueueAction.CLEAR_DATA,
            cacheBeforeBytes = item.cacheBeforeBytes,
            dataBeforeBytes = item.dataBeforeBytes,
            stepIndex = done,
            totalSteps = totalActionable,
            launchedSettings = false
        )
        internalState.update {
            it.copy(
                pendingClearDataQueueConfirm = null,
                activeGuideSession = session
            )
        }
        launchCurrentGuideSettings(launchIntent)
    }

    fun skipQueueClearDataItem(item: CleanQueueItem, launchIntent: (Intent) -> Boolean) {
        viewModelScope.launch {
            historyRepository.recordEntry(
                appName = item.appName,
                packageName = item.packageName,
                operationType = item.action.label,
                status = OperationStatus.NOT_ALLOWED.label,
                recoveredBytes = null,
                notes = "Omitido por el usuario durante confirmación de seguridad"
            )
            internalState.update { current ->
                current.copy(
                    pendingClearDataQueueConfirm = null,
                    multiCleanQueue = current.multiCleanQueue.map {
                        if (it.packageName == item.packageName) {
                            it.copy(
                                status = OperationStatus.NOT_ALLOWED,
                                statusDetail = "Omitida por seguridad del usuario"
                            )
                        } else {
                            it
                        }
                    }
                )
            }
            advanceMultiCleanQueue(launchIntent)
        }
    }

    fun launchCurrentGuideSettings(launchIntent: (Intent) -> Boolean) {
        val session = internalState.value.activeGuideSession ?: return
        val intent = storageRepository.buildAppInfoIntent(session.packageName)
        val opened = launchIntent(intent)
        if (opened) {
            internalState.update { current ->
                current.copy(
                    activeGuideSession = session.copy(launchedSettings = true),
                    multiCleanQueue = current.multiCleanQueue.map {
                        if (session.source == GuideSource.MULTI_CLEAN_QUEUE && it.packageName == session.packageName) {
                            it.copy(
                                status = OperationStatus.REQUIRES_CONFIRMATION,
                                statusDetail = "Esperando confirmación en Ajustes del sistema"
                            )
                        } else {
                            it
                        }
                    }
                )
            }
        } else {
            viewModelScope.launch {
                historyRepository.recordEntry(
                    appName = session.appName,
                    packageName = session.packageName,
                    operationType = session.action.label,
                    status = OperationStatus.ERROR.label,
                    recoveredBytes = null,
                    notes = "No se pudo abrir la pantalla oficial de Ajustes"
                )
                internalState.update { current ->
                    current.copy(
                        activeGuideSession = null,
                        multiCleanQueue = current.multiCleanQueue.map {
                            if (session.source == GuideSource.MULTI_CLEAN_QUEUE && it.packageName == session.packageName) {
                                it.copy(
                                    status = OperationStatus.ERROR,
                                    statusDetail = "El sistema no permitió abrir Ajustes"
                                )
                            } else {
                                it
                            }
                        },
                        snackbarMessage = "No se pudo abrir Información de ${session.appName}"
                    )
                }
            }
        }
    }

    private fun verifyReturnedGuideSession(session: ActiveGuideSession) {
        viewModelScope.launch {
            val measurement = storageRepository.measureSingleAppSize(session.packageName)
            val hasUsageAccess = storageRepository.hasUsageStatsPermission()

            val (beforeMetric, afterMetric) = when (session.action) {
                QueueAction.CLEAR_CACHE -> session.cacheBeforeBytes to measurement.cacheBytes
                QueueAction.CLEAR_DATA -> {
                    val beforeCombined = if (session.dataBeforeBytes != null && session.cacheBeforeBytes != null) {
                        session.dataBeforeBytes + session.cacheBeforeBytes
                    } else {
                        session.dataBeforeBytes
                    }
                    val afterCombined = if (measurement.dataBytes != null && measurement.cacheBytes != null) {
                        measurement.dataBytes + measurement.cacheBytes
                    } else {
                        measurement.dataBytes
                    }
                    beforeCombined to afterCombined
                }
                QueueAction.DO_NOT_MODIFY -> null to null
            }

            if (hasUsageAccess && beforeMetric != null && afterMetric != null && afterMetric < beforeMetric) {
                val recovered = beforeMetric - afterMetric
                historyRepository.recordEntry(
                    appName = session.appName,
                    packageName = session.packageName,
                    operationType = session.action.label,
                    status = OperationStatus.COMPLETED.label,
                    recoveredBytes = recovered,
                    notes = "Verificado mediante StorageStatsManager"
                )
                internalState.update { current ->
                    current.copy(
                        multiCleanQueue = current.multiCleanQueue.map {
                            if (session.source == GuideSource.MULTI_CLEAN_QUEUE && it.packageName == session.packageName) {
                                it.copy(
                                    status = OperationStatus.COMPLETED,
                                    recoveredBytes = recovered,
                                    statusDetail = "Verificado: ${Formatters.formatBytes(recovered)} liberados"
                                )
                            } else {
                                it
                            }
                        },
                        snackbarMessage = "${session.appName}: ${Formatters.formatBytes(recovered)} liberados (verificado)"
                    )
                }
                moveToNextCacheOrFinishSession(session)
                refreshAll(runSmartScanAnimation = false)
            } else {
                val detailMsg = if (!hasUsageAccess) {
                    "Se abrió la pantalla de Ajustes de ${session.appName}, pero sin Acceso a datos de uso Android no permite comprobar si el tamaño cambió. Por transparencia, no se marcará como completada."
                } else {
                    "Al regresar de Ajustes de ${session.appName}, el tamaño medido sigue en ${Formatters.formatBytesOrUnavailable(afterMetric)} (antes: ${Formatters.formatBytesOrUnavailable(beforeMetric)}). Puedes volver a intentarlo o continuar."
                }

                internalState.update { current ->
                    current.copy(
                        activeGuideSession = session.copy(launchedSettings = false),
                        multiCleanQueue = current.multiCleanQueue.map {
                            if (session.source == GuideSource.MULTI_CLEAN_QUEUE && it.packageName == session.packageName) {
                                it.copy(
                                    status = OperationStatus.REQUIRES_CONFIRMATION,
                                    statusDetail = "Sin reducción verificada"
                                )
                            } else {
                                it
                            }
                        },
                        verificationPrompt = VerificationPrompt(
                            packageName = session.packageName,
                            appName = session.appName,
                            action = session.action,
                            source = session.source,
                            hasUsageStatsAccess = hasUsageAccess,
                            previousBytes = beforeMetric,
                            currentBytes = afterMetric,
                            detailMessage = detailMsg
                        )
                    )
                }
            }
        }
    }

    private fun moveToNextCacheOrFinishSession(completedSession: ActiveGuideSession) {
        when (completedSession.source) {
            GuideSource.CLEAR_DATA_MANAGER -> {
                internalState.update { it.copy(activeGuideSession = null) }
            }
            GuideSource.CACHE_MANAGER -> {
                val ownPkg = getApplication<Application>().packageName
                val remaining = internalState.value.allApps
                    .filter { it.packageName in internalState.value.cacheSelectedPackages && it.packageName != ownPkg }
                val nextIndex = completedSession.stepIndex + 1
                if (nextIndex < remaining.size) {
                    val nextApp = remaining[nextIndex]
                    internalState.update {
                        it.copy(
                            activeGuideSession = ActiveGuideSession(
                                source = GuideSource.CACHE_MANAGER,
                                packageName = nextApp.packageName,
                                appName = nextApp.appName,
                                action = QueueAction.CLEAR_CACHE,
                                cacheBeforeBytes = nextApp.cacheBytes,
                                dataBeforeBytes = nextApp.dataBytes,
                                stepIndex = nextIndex,
                                totalSteps = remaining.size,
                                launchedSettings = false
                            )
                        )
                    }
                } else {
                    internalState.update { it.copy(activeGuideSession = null) }
                }
            }
            GuideSource.MULTI_CLEAN_QUEUE -> {
                internalState.update { it.copy(activeGuideSession = null) }
            }
        }
    }

    fun dismissVerificationAndAdvance(launchIntent: (Intent) -> Boolean) {
        val prompt = internalState.value.verificationPrompt ?: return
        val session = internalState.value.activeGuideSession
        viewModelScope.launch {
            historyRepository.recordEntry(
                appName = prompt.appName,
                packageName = prompt.packageName,
                operationType = prompt.action.label,
                status = OperationStatus.REQUIRES_CONFIRMATION.label,
                recoveredBytes = null,
                notes = if (prompt.hasUsageStatsAccess) {
                    "Ajustes abiertos sin cambio de tamaño detectado"
                } else {
                    "Ajustes abiertos (verificación de tamaño no disponible)"
                }
            )
        }
        internalState.update { current ->
            current.copy(
                verificationPrompt = null,
                multiCleanQueue = current.multiCleanQueue.map {
                    if (prompt.source == GuideSource.MULTI_CLEAN_QUEUE && it.packageName == prompt.packageName) {
                        it.copy(
                            status = OperationStatus.REQUIRES_CONFIRMATION,
                            statusDetail = "Sin verificación de reducción"
                        )
                    } else {
                        it
                    }
                }
            )
        }
        if (session != null) {
            if (session.source == GuideSource.MULTI_CLEAN_QUEUE) {
                internalState.update { it.copy(activeGuideSession = null) }
                advanceMultiCleanQueue(launchIntent)
            } else {
                moveToNextCacheOrFinishSession(session)
            }
        }
    }

    fun retryVerificationSettings(launchIntent: (Intent) -> Boolean) {
        internalState.update { it.copy(verificationPrompt = null) }
        launchCurrentGuideSettings(launchIntent)
    }

    fun cancelActiveGuideSession() {
        internalState.update {
            it.copy(
                activeGuideSession = null,
                verificationPrompt = null,
                isQueueActive = false
            )
        }
    }

    fun recordSystemCacheIntentOpened() {
        viewModelScope.launch {
            historyRepository.recordEntry(
                appName = "Sistema Android",
                packageName = "android",
                operationType = "Asistente oficial CLEAR_APP_CACHE",
                status = OperationStatus.REQUIRES_CONFIRMATION.label,
                recoveredBytes = null,
                notes = "Confirmación delegada al sistema operativo Android"
            )
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(enabled)
        }
    }

    fun setConfirmClearData(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setConfirmClearData(enabled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
            showSnackbar("Historial de limpiezas borrado")
        }
    }

    fun showSnackbar(message: String) {
        internalState.update { it.copy(snackbarMessage = message) }
    }

    fun consumeSnackbar() {
        internalState.update { it.copy(snackbarMessage = null) }
    }

    private fun applyFilterAndSort(
        apps: List<InstalledAppInfo>,
        query: String,
        sort: AppSortOption,
        filter: AppFilterOption
    ): List<InstalledAppInfo> {
        val trimmed = query.trim().lowercase(Locale("es", "ES"))
        val filtered = apps.filter { app ->
            val matchesQuery = trimmed.isEmpty() ||
                app.appName.lowercase(Locale("es", "ES")).contains(trimmed) ||
                app.packageName.lowercase(Locale.ROOT).contains(trimmed)

            val matchesFilter = when (filter) {
                AppFilterOption.ALL -> true
                AppFilterOption.USER_ONLY -> !app.isSystemApp
                AppFilterOption.PROTECTED -> app.isProtectedApp
                AppFilterOption.WITH_CACHE -> (app.cacheBytes ?: 0L) > 0L
            }
            matchesQuery && matchesFilter
        }

        return when (sort) {
            AppSortOption.NAME_ASC -> filtered.sortedBy { it.appName.lowercase(Locale("es", "ES")) }
            AppSortOption.NAME_DESC -> filtered.sortedByDescending { it.appName.lowercase(Locale("es", "ES")) }
            AppSortOption.SIZE_DESC -> filtered.sortedByDescending { it.totalBytes ?: -1L }
            AppSortOption.CACHE_DESC -> filtered.sortedByDescending { it.cacheBytes ?: -1L }
            AppSortOption.SELECTED_FIRST -> filtered.sortedWith(
                compareByDescending<InstalledAppInfo> { it.isSelected }
                    .thenBy { it.appName.lowercase(Locale("es", "ES")) }
            )
        }
    }
}
