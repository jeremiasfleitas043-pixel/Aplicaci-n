package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.QueueAction
import com.example.ui.MainViewModel
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.ClearDataWarningDialog
import com.example.ui.components.SmartCleanSheet
import com.example.ui.components.VerificationResultDialog
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.CleanerHubScreen
import com.example.ui.screens.CleanerSubTab
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CleanMasterTheme

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_APPS = "apps"
const val ROUTE_CLEANER = "cleaner"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavDestinationItem(ROUTE_DASHBOARD, "Inicio", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    NavDestinationItem(ROUTE_APPS, "Apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    NavDestinationItem(ROUTE_CLEANER, "Limpiar", Icons.Rounded.CleaningServices, Icons.Outlined.CleaningServices),
    NavDestinationItem(ROUTE_HISTORY, "Historial", Icons.Rounded.History, Icons.Outlined.History),
    NavDestinationItem(ROUTE_SETTINGS, "Ajustes", Icons.Rounded.Settings, Icons.Outlined.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanMasterRoot()
        }
    }
}

@Composable
fun CleanMasterRoot(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var cleanerSubTab by rememberSaveable { mutableStateOf(CleanerSubTab.CACHE) }

    val safeLaunchIntent: (Intent) -> Boolean = remember(context) {
        { intent -> launchIntentSafely(context, intent) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CleanMasterTheme(darkTheme = uiState.userSettings.isDarkTheme) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 680.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("rail_${item.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    uiState = uiState,
                                    onAnalyzeApps = { viewModel.analyzeAndOpenSmartClean() },
                                    onOpenMultiClean = {
                                        cleanerSubTab = CleanerSubTab.MULTI_QUEUE
                                        navigateTo(ROUTE_CLEANER)
                                    },
                                    onOpenCacheManager = {
                                        cleanerSubTab = CleanerSubTab.CACHE
                                        navigateTo(ROUTE_CLEANER)
                                    },
                                    onOpenClearData = {
                                        cleanerSubTab = CleanerSubTab.CLEAR_DATA
                                        navigateTo(ROUTE_CLEANER)
                                    },
                                    onOpenApps = { navigateTo(ROUTE_APPS) },
                                    onOpenHistory = { navigateTo(ROUTE_HISTORY) },
                                    onGrantUsageAccess = {
                                        if (!safeLaunchIntent(viewModel.storageRepository.buildUsageAccessIntent())) {
                                            viewModel.showSnackbar("No se pudo abrir la pantalla de acceso de uso")
                                        }
                                    },
                                    onRefresh = { viewModel.refreshAll(runSmartScanAnimation = true) }
                                )
                            }

                            composable(ROUTE_APPS) {
                                AppManagerScreen(
                                    uiState = uiState,
                                    repository = viewModel.storageRepository,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onSortChange = viewModel::updateSortOption,
                                    onFilterChange = viewModel::updateFilterOption,
                                    onSelectAll = viewModel::selectAllFilteredApps,
                                    onDeselectAll = viewModel::deselectAllApps,
                                    onToggleSelection = viewModel::toggleAppSelection,
                                    onInspectApp = viewModel::inspectApp,
                                    onSendSelectedToCache = {
                                        val selected = uiState.selectedPackages
                                        selected.forEach { pkg ->
                                            if (pkg !in uiState.cacheSelectedPackages) {
                                                viewModel.toggleCacheAppSelection(pkg)
                                            }
                                        }
                                        cleanerSubTab = CleanerSubTab.CACHE
                                        navigateTo(ROUTE_CLEANER)
                                    },
                                    onSendSelectedToMultiQueue = {
                                        val selectedApps = uiState.allApps.filter { it.packageName in uiState.selectedPackages }
                                        viewModel.addAppsToMultiQueue(selectedApps, QueueAction.CLEAR_CACHE)
                                        cleanerSubTab = CleanerSubTab.MULTI_QUEUE
                                        navigateTo(ROUTE_CLEANER)
                                    },
                                    onGrantUsageAccess = {
                                        safeLaunchIntent(viewModel.storageRepository.buildUsageAccessIntent())
                                    }
                                )
                            }

                            composable(ROUTE_CLEANER) {
                                CleanerHubScreen(
                                    selectedTab = cleanerSubTab,
                                    onSelectTab = { cleanerSubTab = it },
                                    uiState = uiState,
                                    repository = viewModel.storageRepository,
                                    onClearOwnCache = viewModel::clearOwnCacheNow,
                                    onLaunchSystemClearCache = {
                                        val intent = viewModel.storageRepository.buildSystemClearCacheIntent()
                                        if (intent != null && safeLaunchIntent(intent)) {
                                            viewModel.recordSystemCacheIntentOpened()
                                        } else {
                                            viewModel.showSnackbar("El dispositivo no admite el asistente directo; usa la lista de abajo")
                                        }
                                    },
                                    onToggleCacheApp = viewModel::toggleCacheAppSelection,
                                    onSelectAllCacheApps = viewModel::selectAllCacheApps,
                                    onDeselectAllCacheApps = viewModel::deselectAllCacheApps,
                                    onStartCacheCleaning = {
                                        viewModel.startCacheGuidedQueue(uiState.cacheSelectedPackages, safeLaunchIntent)
                                    },
                                    onRequestClearData = viewModel::requestClearDataForApp,
                                    onUpdateQueueAction = viewModel::updateQueueItemAction,
                                    onRemoveQueueItem = viewModel::removeQueueItem,
                                    onClearQueue = viewModel::clearMultiQueue,
                                    onStartMultiClean = {
                                        viewModel.startMultiCleanQueue(safeLaunchIntent)
                                    },
                                    onOpenGuideSettings = {
                                        viewModel.launchCurrentGuideSettings(safeLaunchIntent)
                                    },
                                    onCancelGuide = viewModel::cancelActiveGuideSession,
                                    onGoToAppsPicker = { navigateTo(ROUTE_APPS) }
                                )
                            }

                            composable(ROUTE_HISTORY) {
                                HistoryScreen(
                                    historyList = uiState.historyList,
                                    onClearHistory = viewModel::clearHistory
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                SettingsScreen(
                                    userSettings = uiState.userSettings,
                                    compatibilityInfo = uiState.compatibilityInfo,
                                    onToggleDarkTheme = viewModel::setDarkTheme,
                                    onToggleConfirmClearData = viewModel::setConfirmClearData,
                                    onOpenUsageAccessSettings = {
                                        safeLaunchIntent(viewModel.storageRepository.buildUsageAccessIntent())
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheets & Safety Dialogs
        uiState.inspectedApp?.let { app ->
            AppDetailBottomSheet(
                app = app,
                repository = viewModel.storageRepository,
                onDismiss = { viewModel.inspectApp(null) },
                onQueueCache = { target ->
                    viewModel.addAppsToMultiQueue(listOf(target), QueueAction.CLEAR_CACHE)
                    cleanerSubTab = CleanerSubTab.MULTI_QUEUE
                    navigateTo(ROUTE_CLEANER)
                },
                onRequestClearData = { target ->
                    viewModel.requestClearDataForApp(target)
                },
                onOpenSystemSettings = { pkg ->
                    safeLaunchIntent(viewModel.storageRepository.buildAppInfoIntent(pkg))
                }
            )
        }

        if (uiState.showSmartCleanSheet) {
            SmartCleanSheet(
                recommendedApps = uiState.smartRecommendedApps,
                hasUsageAccess = uiState.compatibilityInfo?.hasUsageAccess == true,
                isScanning = uiState.isScanning,
                scanProgress = uiState.scanProgress,
                repository = viewModel.storageRepository,
                onDismiss = viewModel::dismissSmartCleanSheet,
                onEnqueueSafeCaches = {
                    viewModel.enqueueSmartRecommendations()
                    cleanerSubTab = CleanerSubTab.MULTI_QUEUE
                    navigateTo(ROUTE_CLEANER)
                },
                onGrantUsageAccess = {
                    safeLaunchIntent(viewModel.storageRepository.buildUsageAccessIntent())
                },
                onInspectApp = { app ->
                    viewModel.dismissSmartCleanSheet()
                    viewModel.inspectApp(app)
                }
            )
        }

        uiState.clearDataCandidate?.let { candidate ->
            ClearDataWarningDialog(
                appName = candidate.appName,
                protectionReason = candidate.protectionReason,
                onConfirm = {
                    viewModel.confirmClearDataAndOpenSettings(candidate, safeLaunchIntent)
                },
                onDismiss = viewModel::dismissClearDataDialog
            )
        }

        uiState.pendingClearDataQueueConfirm?.let { queueItem ->
            ClearDataWarningDialog(
                appName = queueItem.appName,
                protectionReason = queueItem.protectionReason,
                onConfirm = {
                    viewModel.confirmQueueClearDataItem(queueItem, safeLaunchIntent)
                },
                onDismiss = {
                    viewModel.skipQueueClearDataItem(queueItem, safeLaunchIntent)
                }
            )
        }

        uiState.verificationPrompt?.let { prompt ->
            VerificationResultDialog(
                prompt = prompt,
                onRetry = { viewModel.retryVerificationSettings(safeLaunchIntent) },
                onContinueNext = { viewModel.dismissVerificationAndAdvance(safeLaunchIntent) },
                onCancelSession = viewModel::cancelActiveGuideSession
            )
        }
    }
}

private fun launchIntentSafely(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
