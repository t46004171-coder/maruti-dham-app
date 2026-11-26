package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UiState
import com.example.data.model.getAppStrings
import com.example.data.repository.AuthHelper
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.MarutiRepository
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FoodDeliveryScreen
import com.example.ui.screens.FoodOrderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarutiSplashScreen
import com.example.ui.screens.MyRequestsScreen
import com.example.ui.screens.RoomsAndFreshenUpScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.MarutiDhamTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DefaultCatalogData
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_FOOD_ORDER = "food_order"
const val ROUTE_ROOMS_FRESHEN_UP = "rooms_freshen_up"
const val ROUTE_FOOD_DELIVERY = "food_delivery"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_MY_REQUESTS = "my_requests"
const val ROUTE_ADMIN_PANEL = "admin_panel"

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

// Exactly 5 bottom navigation tabs with exact requested titles
private val FIVE_BOTTOM_TABS = listOf(
    BottomTab(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    BottomTab(ROUTE_FOOD_ORDER, "Food Order", Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu, "tab_food_order"),
    BottomTab(ROUTE_ROOMS_FRESHEN_UP, "Rooms & Freshen Up", Icons.Filled.Hotel, Icons.Outlined.Hotel, "tab_rooms_freshen_up"),
    BottomTab(ROUTE_FOOD_DELIVERY, "Food Delivery", Icons.Filled.DeliveryDining, Icons.Outlined.DeliveryDining, "tab_food_delivery"),
    BottomTab(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MarutiDhamRoot()
        }
    }
}

@Composable
fun MarutiDhamRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val languagePrefs = remember { LanguagePreferencesRepository(context) }
    val languageCode by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = "en")
    val strings = remember(languageCode) { getAppStrings(languageCode) }

    var showSplash by remember { mutableStateOf(true) }
    var loginMode by rememberSaveable { mutableStateOf("SELECTOR") }
    var ownerDeniedMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val currentUser by AuthHelper.authStateFlow(Firebase.auth)
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val isOwnerRequestedDenied = currentUser != null &&
        loginMode == "OWNER" &&
        !AuthHelper.isOwnerEmail(currentUser)

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null && ownerDeniedMessage == null) {
            loginMode = "SELECTOR"
        }
    }

    LaunchedEffect(isOwnerRequestedDenied, currentUser?.uid) {
        if (isOwnerRequestedDenied) {
            ownerDeniedMessage = if (strings.isHindi) {
                "अस्वीकृत: यह Google खाता स्वामी / व्यवस्थापक पैनल के लिए अधिकृत नहीं है।"
            } else {
                "Access Denied: This Google account is not authorized for Owner / Admin access."
            }
            AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
        }
    }

    MarutiDhamTheme(isHindi = languageCode == "hi") {
        if (showSplash) {
            MarutiSplashScreen(
                strings = strings,
                onFinished = { showSplash = false }
            )
        } else {
            val user = currentUser
            if (user == null || isOwnerRequestedDenied) {
                AuthScreen(
                    strings = strings,
                    languageCode = languageCode,
                    loginMode = loginMode,
                    accessDeniedMessage = ownerDeniedMessage,
                    onLoginModeChange = { nextMode ->
                        loginMode = nextMode
                        if (nextMode != "OWNER") ownerDeniedMessage = null
                    },
                    onClearAccessDenied = { ownerDeniedMessage = null },
                    onLanguageChange = { code ->
                        scope.launch { languagePrefs.setLanguage(code) }
                    },
                    onAuthSuccess = { mode ->
                        loginMode = mode
                    }
                )
            } else {
                val verifiedOwner = AuthHelper.isOwnerEmail(user)
                val startRoute = if (loginMode == "OWNER" && verifiedOwner) ROUTE_ADMIN_PANEL else ROUTE_HOME
                key(user.uid) {
                    AuthenticatedMarutiApp(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "Devotee",
                        isBootstrappedOwner = verifiedOwner,
                        initialRoute = startRoute,
                        onSignedOut = {
                            loginMode = "SELECTOR"
                            ownerDeniedMessage = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedMarutiApp(
    uid: String,
    email: String,
    displayName: String,
    isBootstrappedOwner: Boolean,
    initialRoute: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val repo = MarutiRepository(db)
                val langRepo = LanguagePreferencesRepository(app)
                MainViewModel(
                    repository = repo,
                    languagePrefs = langRepo,
                    currentUserId = uid,
                    currentUserEmail = email,
                    currentUserDisplayName = displayName,
                    isBootstrappedOwner = isBootstrappedOwner
                )
            }
        }
    )

    val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
    val strings = remember(languageCode) { getAppStrings(languageCode) }
    val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val shopState by viewModel.shopSettings.collectAsStateWithLifecycle()
    val siteConfigState by viewModel.siteConfig.collectAsStateWithLifecycle()
    val activeThemeConfig by viewModel.activeThemeConfig.collectAsStateWithLifecycle()
    val isThemePreviewActive by viewModel.isThemePreviewActive.collectAsStateWithLifecycle()
    val shopEvaluation by viewModel.shopEvaluation.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcements.collectAsStateWithLifecycle()
    val productsState by viewModel.products.collectAsStateWithLifecycle()
    val roomsState by viewModel.roomConfigs.collectAsStateWithLifecycle()
    val freshenState by viewModel.freshenUpConfigs.collectAsStateWithLifecycle()
    val deliveryState by viewModel.deliverySettings.collectAsStateWithLifecycle()
    val myRequestsState by viewModel.myRequests.collectAsStateWithLifecycle()
    val adminRequestsState by viewModel.allAdminRequests.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val customerDirectory by viewModel.customerDirectory.collectAsStateWithLifecycle()
    val reportPeriod by viewModel.reportPeriod.collectAsStateWithLifecycle()
    val reportStartDateMillis by viewModel.reportStartDateMillis.collectAsStateWithLifecycle()
    val reportEndDateMillis by viewModel.reportEndDateMillis.collectAsStateWithLifecycle()
    val salesReport by viewModel.salesReport.collectAsStateWithLifecycle()
    val ownerPrivateSettings by viewModel.ownerPrivateSettings.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val adminOrderBadges by viewModel.adminOrderBadges.collectAsStateWithLifecycle()
    val lowStockAlerts by viewModel.lowStockAlerts.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    val foodCart by viewModel.foodCart.collectAsStateWithLifecycle()
    val deliveryCart by viewModel.deliveryCart.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val submittedRequest by viewModel.submittedRequest.collectAsStateWithLifecycle()
    val recentOrderSuccess by viewModel.recentOrderSuccess.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val shopSettings = (shopState as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
    val siteConfig = (siteConfigState as? UiState.Success)?.data ?: DefaultCatalogData.defaultSiteConfig
    val deliverySettings = (deliveryState as? UiState.Success)?.data ?: DefaultCatalogData.defaultDeliverySettings

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showPrimaryNav = currentDestination?.route in FIVE_BOTTOM_TABS.map { it.route }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun performSignOut() {
        AuthHelper.signOut(credentialManager = credentialManager, scope = scope) {
            onSignedOut()
        }
    }

    MarutiDhamTheme(
        isHindi = languageCode == "hi",
        themeConfig = activeThemeConfig
    ) {
        val palette = LocalMarutiPalette.current
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = palette.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showPrimaryNav && !useRail) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = palette.primary.copy(alpha = 0.38f)
                            )
                            NavigationBar(
                                containerColor = palette.surface,
                                contentColor = palette.accent,
                                tonalElevation = 8.dp
                            ) {
                                FIVE_BOTTOM_TABS.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (selected) 1.12f else 1f,
                                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                                        label = "bottom_nav_scale_${tab.route}"
                                    )
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        alwaysShowLabel = true,
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label,
                                                modifier = Modifier.scale(iconScale)
                                            )
                                        },
                                        label = {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = tab.label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = if (selected) 11.sp else 10.sp,
                                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(22.dp)
                                                        .height(2.5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (selected) palette.accent else Color.Transparent)
                                                )
                                            }
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = palette.background,
                                            selectedTextColor = palette.accent,
                                            indicatorColor = palette.accent,
                                            unselectedIconColor = palette.textSecondary.copy(alpha = 0.72f),
                                            unselectedTextColor = palette.textSecondary.copy(alpha = 0.72f)
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
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
                    if (showPrimaryNav && useRail) {
                        NavigationRail(
                            containerColor = palette.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            FIVE_BOTTOM_TABS.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.12f else 1f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                                    label = "rail_nav_scale_${tab.route}"
                                )
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    alwaysShowLabel = true,
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            modifier = Modifier.scale(iconScale)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = palette.background,
                                        selectedTextColor = palette.accent,
                                        indicatorColor = palette.accent,
                                        unselectedIconColor = palette.textSecondary.copy(alpha = 0.72f),
                                        unselectedTextColor = palette.textSecondary.copy(alpha = 0.72f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = initialRoute,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                shopState = shopState,
                                siteConfig = siteConfig,
                                shopEvaluation = shopEvaluation,
                                announcementsState = announcementsState,
                                strings = strings,
                                isOwner = isOwner,
                                pendingNewOrdersCount = adminOrderBadges.totalPendingNewOrders,
                                hasNewlyArrivedOrders = adminOrderBadges.hasNewlyArrived,
                                onNavigateToFoodOrder = { navigateToTab(ROUTE_FOOD_ORDER) },
                                onNavigateToRooms = { navigateToTab(ROUTE_ROOMS_FRESHEN_UP) },
                                onNavigateToDelivery = { navigateToTab(ROUTE_FOOD_DELIVERY) },
                                onNavigateToMyRequests = { navController.navigate(ROUTE_MY_REQUESTS) },
                                onNavigateToAdmin = { navController.navigate(ROUTE_ADMIN_PANEL) }
                            )
                        }

                        composable(ROUTE_FOOD_ORDER) {
                            FoodOrderScreen(
                                productsState = productsState,
                                shopEvaluation = shopEvaluation,
                                shopSettings = shopSettings,
                                deliverySettings = deliverySettings,
                                siteConfigCategories = siteConfig.categories,
                                cart = foodCart,
                                userProfile = userProfile,
                                defaultUserName = displayName,
                                defaultUserEmail = email,
                                isSubmitting = isSubmitting,
                                recentSuccessOrder = recentOrderSuccess,
                                strings = strings,
                                onUpdateQuantity = viewModel::updateFoodCartQuantity,
                                onRegisterCustomer = viewModel::registerCustomer,
                                onSubmitRequest = viewModel::submitFoodOrderRequest,
                                onOpenMyRequests = { navController.navigate(ROUTE_MY_REQUESTS) }
                            )
                        }

                        composable(ROUTE_ROOMS_FRESHEN_UP) {
                            RoomsAndFreshenUpScreen(
                                roomConfigsState = roomsState,
                                freshenUpConfigsState = freshenState,
                                userProfile = userProfile,
                                defaultUserName = displayName,
                                isSubmitting = isSubmitting,
                                recentSuccessOrder = recentOrderSuccess,
                                strings = strings,
                                onSubmitRoomRequest = viewModel::submitRoomRequest,
                                onSubmitFreshenUpRequest = viewModel::submitFreshenUpRequest
                            )
                        }

                        composable(ROUTE_FOOD_DELIVERY) {
                            FoodDeliveryScreen(
                                productsState = productsState,
                                deliverySettingsState = deliveryState,
                                shopEvaluation = shopEvaluation,
                                shopSettings = shopSettings,
                                deliveryCart = deliveryCart,
                                userProfile = userProfile,
                                defaultUserName = displayName,
                                defaultUserEmail = email,
                                isSubmitting = isSubmitting,
                                recentSuccessOrder = recentOrderSuccess,
                                strings = strings,
                                onUpdateQuantity = viewModel::updateDeliveryCartQuantity,
                                onRegisterCustomer = viewModel::registerCustomer,
                                onSubmitDeliveryRequest = viewModel::submitFoodDeliveryRequest
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                shopSettingsState = shopState,
                                deliverySettingsState = deliveryState,
                                userProfile = userProfile,
                                languageCode = languageCode,
                                userName = userProfile?.displayName?.ifBlank { displayName } ?: displayName,
                                userEmail = email,
                                isOwner = isOwner,
                                pendingNewOrdersCount = adminOrderBadges.totalPendingNewOrders,
                                hasNewlyArrivedOrders = adminOrderBadges.hasNewlyArrived,
                                isSubmitting = isSubmitting,
                                strings = strings,
                                onLanguageChange = viewModel::setLanguage,
                                onRegisterCustomer = viewModel::registerCustomer,
                                onRequestAccountDeletion = viewModel::requestAccountDeletion,
                                onNavigateToMyRequests = { navController.navigate(ROUTE_MY_REQUESTS) },
                                onNavigateToAdmin = { navController.navigate(ROUTE_ADMIN_PANEL) }
                            )
                        }

                        composable(ROUTE_MY_REQUESTS) {
                            MyRequestsScreen(
                                requestsState = myRequestsState,
                                shopSettingsState = shopState,
                                strings = strings,
                                onLoadMoreOrders = viewModel::loadMoreCustomerOrders,
                                onMarkWhatsAppShared = viewModel::markManualWhatsAppShared,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ADMIN_PANEL) {
                            AdminScreen(
                                isOwner = isOwner,
                                ownerEmail = email,
                                dashboardStats = dashboardStats,
                                customerDirectory = customerDirectory,
                                salesReport = salesReport,
                                reportPeriod = reportPeriod,
                                reportStartDateMillis = reportStartDateMillis,
                                reportEndDateMillis = reportEndDateMillis,
                                requestsState = adminRequestsState,
                                shopSettingsState = shopState,
                                siteConfigState = siteConfigState,
                                isThemePreviewActive = isThemePreviewActive,
                                ownerPrivateSettings = ownerPrivateSettings,
                                shopEvaluation = shopEvaluation,
                                announcementsState = announcementsState,
                                productsState = productsState,
                                roomConfigsState = roomsState,
                                freshenUpConfigsState = freshenState,
                                deliverySettingsState = deliveryState,
                                activityLogs = activityLogs,
                                notifications = notifications,
                                orderBadges = adminOrderBadges,
                                lowStockAlerts = lowStockAlerts,
                                aiState = aiState,
                                strings = strings,
                                onSelectReportPeriod = viewModel::setReportPeriod,
                                onSelectReportDateRange = viewModel::setReportCustomDateRange,
                                onAcknowledgeNewOrders = viewModel::acknowledgeNewOrders,
                                onAcknowledgeLowStockAlerts = viewModel::acknowledgeLowStockAlerts,
                                onUpdateOrderStatus = viewModel::adminUpdateOrderStatus,
                                onVerifyPayment = viewModel::adminVerifyPayment,
                                onRetryWhatsApp = viewModel::adminRetryWhatsApp,
                                onMarkWhatsAppShared = viewModel::markManualWhatsAppShared,
                                onLoadMoreAdminOrders = viewModel::loadMoreAdminOrders,
                                onLoadMoreCustomers = viewModel::loadMoreCustomers,
                                onSaveShop = viewModel::adminSaveShopSettings,
                                onSaveSiteConfigDraft = viewModel::adminSaveSiteConfigDraft,
                                onPublishSiteConfig = viewModel::adminPublishSiteConfig,
                                onToggleThemePreview = viewModel::toggleThemePreview,
                                onUpdateCategories = viewModel::adminUpdateCategories,
                                onSaveOwnerPrivateSettings = viewModel::adminSaveOwnerPrivateSettings,
                                onSaveAnnouncement = viewModel::adminSaveAnnouncement,
                                onDeleteAnnouncement = viewModel::adminDeleteAnnouncement,
                                onSaveProduct = viewModel::adminSaveProduct,
                                onDeleteProduct = viewModel::adminDeleteProduct,
                                onSaveRoom = viewModel::adminSaveRoomConfig,
                                onSaveFreshen = viewModel::adminSaveFreshenUpConfig,
                                onSaveDelivery = viewModel::adminSaveDeliverySettings,
                                onMarkNotificationRead = viewModel::markNotificationRead,
                                onRunAiCommand = viewModel::runAiCommand,
                                onConfirmAiAction = viewModel::confirmPendingAiAction,
                                onDismissAiProposal = viewModel::dismissAiProposal,
                                onSignOut = ::performSignOut,
                                onNavigateBack = {
                                    if (!navController.popBackStack()) {
                                        navigateToTab(ROUTE_HOME)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            submittedRequest?.let { req ->
                DigitalReceiptDialog(
                    request = req,
                    shopSettings = shopSettings,
                    strings = strings,
                    isNewSubmission = true,
                    targetWhatsAppOverride = if (isOwner) ownerPrivateSettings.ownerWhatsAppNumber else shopSettings.whatsappNumber,
                    onMarkWhatsAppShared = viewModel::markManualWhatsAppShared,
                    onDismiss = viewModel::dismissSubmittedRequest
                )
            }
        }
    }
}
