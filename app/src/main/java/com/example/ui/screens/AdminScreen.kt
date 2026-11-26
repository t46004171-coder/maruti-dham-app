package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityLogEntry
import com.example.data.model.AdminOrderBadgeState
import com.example.data.model.Announcement
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.CustomerSummary
import com.example.data.model.DeliverySettings
import com.example.data.model.FreshenUpConfig
import com.example.data.model.LowStockAlertSummary
import com.example.data.model.OwnerDashboardStats
import com.example.data.model.OwnerNotification
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ReportPeriod
import com.example.data.model.RequestCategory
import com.example.data.model.RequestStatus
import com.example.data.model.RoomConfig
import com.example.data.model.SalesReportSummary
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.data.model.SiteConfig
import com.example.data.model.UiState
import com.example.ui.components.GoldOutlineCard
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.viewmodel.MarutiAiUiState
import com.example.util.DefaultCatalogData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    isOwner: Boolean,
    ownerEmail: String,
    dashboardStats: OwnerDashboardStats,
    customerDirectory: List<CustomerSummary>,
    salesReport: SalesReportSummary,
    reportPeriod: ReportPeriod,
    reportStartDateMillis: Long?,
    reportEndDateMillis: Long?,
    requestsState: UiState<List<CustomerRequest>>,
    shopSettingsState: UiState<ShopSettings>,
    siteConfigState: UiState<SiteConfig>,
    isThemePreviewActive: Boolean,
    ownerPrivateSettings: OwnerPrivateSettings,
    shopEvaluation: ShopOpenEvaluation,
    announcementsState: UiState<List<Announcement>>,
    productsState: UiState<List<Product>>,
    roomConfigsState: UiState<List<RoomConfig>>,
    freshenUpConfigsState: UiState<List<FreshenUpConfig>>,
    deliverySettingsState: UiState<DeliverySettings>,
    activityLogs: List<ActivityLogEntry>,
    notifications: List<OwnerNotification>,
    orderBadges: AdminOrderBadgeState = AdminOrderBadgeState(),
    lowStockAlerts: LowStockAlertSummary = LowStockAlertSummary(),
    aiState: MarutiAiUiState,
    strings: AppStrings,
    onSelectReportPeriod: (ReportPeriod) -> Unit,
    onSelectReportDateRange: (Long?, Long?) -> Unit,
    onAcknowledgeNewOrders: (RequestCategory?) -> Unit = {},
    onAcknowledgeLowStockAlerts: () -> Unit = {},
    onUpdateOrderStatus: (String, RequestStatus, String?) -> Unit,
    onVerifyPayment: (String, PaymentStatus, String?) -> Unit,
    onRetryWhatsApp: (String) -> Unit,
    onMarkWhatsAppShared: (String) -> Unit,
    onLoadMoreAdminOrders: () -> Unit,
    onLoadMoreCustomers: () -> Unit,
    onSaveShop: (ShopSettings) -> Unit,
    onSaveSiteConfigDraft: (SiteConfig) -> Unit,
    onPublishSiteConfig: (SiteConfig) -> Unit,
    onToggleThemePreview: () -> Unit,
    onUpdateCategories: (List<String>) -> Unit,
    onSaveOwnerPrivateSettings: (OwnerPrivateSettings) -> Unit,
    onSaveAnnouncement: (Announcement) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveRoom: (RoomConfig) -> Unit,
    onSaveFreshen: (FreshenUpConfig) -> Unit,
    onSaveDelivery: (DeliverySettings) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onRunAiCommand: (String) -> Unit,
    onConfirmAiAction: () -> Unit,
    onDismissAiProposal: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val palette = LocalMarutiPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val unreadCount = remember(notifications) { notifications.count { !it.read } }

    val pulseTransition = rememberInfiniteTransition(label = "admin_live_order_pulse")
    val livePulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "admin_live_dot_alpha"
    )

    fun jumpToFirstPendingQueue() {
        val targetTab = when {
            orderBadges.pendingShopOrders > 0 -> 1
            orderBadges.pendingDeliveryOrders > 0 -> 2
            orderBadges.pendingRoomBookings > 0 -> 3
            orderBadges.pendingFreshenUp > 0 -> 4
            else -> 1
        }
        selectedTab = targetTab
        onAcknowledgeNewOrders(null)
    }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.ownerAdminPanel, color = palette.accent) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.primary)
                    }
                },
                actions = {
                    if (isOwner) {
                        if (orderBadges.totalPendingNewOrders > 0 || orderBadges.hasNewlyArrived) {
                            val badgeHighlightColor = if (orderBadges.hasNewlyArrived) StatusClosedRed else StatusPendingAmber
                            Surface(
                                onClick = ::jumpToFirstPendingQueue,
                                shape = RoundedCornerShape(999.dp),
                                color = badgeHighlightColor.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, badgeHighlightColor),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .padding(end = 4.dp)
                                    .testTag("admin_live_new_orders_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .alpha(if (orderBadges.hasNewlyArrived) livePulseAlpha else 1f)
                                            .clip(CircleShape)
                                            .background(badgeHighlightColor)
                                    )
                                    Text(
                                        text = if (orderBadges.hasNewlyArrived) {
                                            "${orderBadges.totalPendingNewOrders} New"
                                        } else {
                                            "${orderBadges.totalPendingNewOrders} Pending"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeHighlightColor
                                    )
                                }
                            }
                        }
                        if (lowStockAlerts.hasAlerts) {
                            val stockBadgeColor = if (lowStockAlerts.outOfStockCount > 0) StatusClosedRed else StatusPendingAmber
                            Surface(
                                onClick = {
                                    selectedTab = 7
                                    onAcknowledgeLowStockAlerts()
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = stockBadgeColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, stockBadgeColor),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .padding(end = 4.dp)
                                    .testTag("admin_low_stock_topbar_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Low Stock Alerts",
                                        tint = stockBadgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${lowStockAlerts.totalFlaggedCount} Low Stock",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = stockBadgeColor
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { selectedTab = 14 },
                            modifier = Modifier.testTag("admin_notifications_bell")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Owner alerts", tint = palette.primary)
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("admin_topbar_logout")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Owner Sign Out", tint = StatusClosedRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface)
            )
        }
    ) { padding ->
        if (!isOwner) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoldOutlineCard {
                    Text("Owner Access Only", style = MaterialTheme.typography.titleMedium, color = StatusClosedRed)
                    Text(
                        text = "Your account does not have owner permissions. Only the verified shop owner account can access the Admin Dashboard, backed by Cloud Firestore security rules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                }
            }
            return@Scaffold
        }

        val tabs = listOf(
            "Overview",
            "Shop Orders",
            "Delivery Orders",
            "Room Bookings",
            "Freshen Up",
            "Customers",
            "Menu & Categories",
            "Stock & Prices",
            "Design & Theme",
            "Shop & Payment",
            "Rooms & Delivery",
            "Reports & WhatsApp",
            "Privacy & Security",
            "MARUTI AI",
            "Activity & Alerts"
        )

        val requests = (requestsState as? UiState.Success)?.data ?: emptyList()
        val shop = (shopSettingsState as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
        val siteConfig = (siteConfigState as? UiState.Success)?.data ?: DefaultCatalogData.defaultSiteConfig
        val announcements = (announcementsState as? UiState.Success)?.data ?: emptyList()
        val products = (productsState as? UiState.Success)?.data ?: DefaultCatalogData.initialProducts
        val rooms = (roomConfigsState as? UiState.Success)?.data ?: DefaultCatalogData.initialRoomConfigs
        val freshen = (freshenUpConfigsState as? UiState.Success)?.data ?: DefaultCatalogData.initialFreshenUpConfigs
        val delivery = (deliverySettingsState as? UiState.Success)?.data ?: DefaultCatalogData.defaultDeliverySettings

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isThemePreviewActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusOpenGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE THEME PREVIEW MODE ACTIVE (Unpublished Draft)",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusOpenGreen,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onToggleThemePreview) {
                        Text("Exit Preview", color = palette.accent)
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = palette.card,
                contentColor = palette.primary,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val tabCategory = when (index) {
                        1 -> RequestCategory.FOOD_REQUEST
                        2 -> RequestCategory.FOOD_DELIVERY_REQUEST
                        3 -> RequestCategory.ROOM_REQUEST
                        4 -> RequestCategory.FRESHEN_UP_REQUEST
                        else -> null
                    }
                    val badgeCount = when (index) {
                        1 -> orderBadges.pendingShopOrders
                        2 -> orderBadges.pendingDeliveryOrders
                        3 -> orderBadges.pendingRoomBookings
                        4 -> orderBadges.pendingFreshenUp
                        7 -> lowStockAlerts.totalFlaggedCount
                        14 -> unreadCount
                        else -> 0
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (tabCategory != null) {
                                onAcknowledgeNewOrders(tabCategory)
                            }
                            if (index == 7) {
                                onAcknowledgeLowStockAlerts()
                            }
                        },
                        modifier = Modifier.testTag("admin_tab_$index"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) palette.accent else palette.textSecondary
                                )
                                if (badgeCount > 0) {
                                    val badgeColor = when {
                                        index == 7 && lowStockAlerts.outOfStockCount > 0 -> StatusClosedRed
                                        index == 7 -> StatusPendingAmber
                                        orderBadges.hasNewlyArrived && index in 1..4 -> StatusClosedRed
                                        else -> StatusPendingAmber
                                    }
                                    Badge(
                                        containerColor = badgeColor,
                                        contentColor = Color.Black,
                                        modifier = Modifier.testTag("admin_tab_badge_$index")
                                    ) {
                                        Text(
                                            text = badgeCount.coerceAtMost(99).toString(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = lowStockAlerts.hasNewlyFlagged) {
                val bannerColor = if (lowStockAlerts.outOfStockCount > 0) StatusClosedRed else StatusPendingAmber
                Surface(
                    color = bannerColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, bannerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("admin_low_stock_alert_banner"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Low Stock Alert",
                                tint = bannerColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "LOW STOCK ALERT: ${lowStockAlerts.totalFlaggedCount} item(s) at critical level",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = bannerColor
                                )
                                val previewNames = lowStockAlerts.flaggedItems.take(3).joinToString(", ") {
                                    "${it.product.name} (${it.stockQuantity} left)"
                                }
                                if (previewNames.isNotBlank()) {
                                    Text(
                                        text = previewNames,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    selectedTab = 7
                                    onAcknowledgeLowStockAlerts()
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Restock", color = palette.accent, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = onAcknowledgeLowStockAlerts,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Dismiss", color = palette.textSecondary)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = orderBadges.hasNewlyArrived) {
                Surface(
                    color = StatusPendingAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, StatusPendingAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("admin_realtime_order_banner"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(livePulseAlpha)
                                    .clip(CircleShape)
                                    .background(StatusOpenGreen)
                            )
                            Column {
                                Text(
                                    text = "LIVE: ${orderBadges.newlyArrivedOrderIds.size} New Order Received!",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.accent
                                )
                                orderBadges.latestNewOrderSummary?.let { summary ->
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = ::jumpToFirstPendingQueue,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("View", color = palette.accent, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { onAcknowledgeNewOrders(null) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Dismiss", color = palette.textSecondary)
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewAndCustomersSection(
                    stats = dashboardStats,
                    customers = customerDirectory,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    lowStockAlerts = lowStockAlerts,
                    onQuickRestockProduct = onSaveProduct,
                    onJumpToStockTab = {
                        selectedTab = 7
                        onAcknowledgeLowStockAlerts()
                    },
                    onMarkWhatsAppShared = onMarkWhatsAppShared,
                    onLoadMoreCustomers = onLoadMoreCustomers,
                    showKpis = true,
                    showCustomerDirectory = false
                )
                1 -> AdminCategoryOrdersSection(
                    sectionTitle = "Shop & Counter Orders",
                    sectionSubtitle = "Dine-in & Counter Pickup food orders only (separated from Khatu delivery).",
                    emptyMessage = "No shop/counter food orders yet.",
                    categoryFilter = RequestCategory.FOOD_REQUEST,
                    allRequests = requests,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    onUpdateStatus = onUpdateOrderStatus,
                    onVerifyPayment = onVerifyPayment,
                    onMarkWhatsAppShared = onMarkWhatsAppShared,
                    onLoadMoreOrders = onLoadMoreAdminOrders,
                    searchTestTag = "admin_shop_order_search"
                )
                2 -> AdminCategoryOrdersSection(
                    sectionTitle = "Khatu Shyam Ji Delivery Orders",
                    sectionSubtitle = "Local Khatu delivery orders with Dharamshala/locality address & charges.",
                    emptyMessage = "No Khatu delivery orders yet.",
                    categoryFilter = RequestCategory.FOOD_DELIVERY_REQUEST,
                    allRequests = requests,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    onUpdateStatus = onUpdateOrderStatus,
                    onVerifyPayment = onVerifyPayment,
                    onMarkWhatsAppShared = onMarkWhatsAppShared,
                    onLoadMoreOrders = onLoadMoreAdminOrders,
                    searchTestTag = "admin_delivery_order_search"
                )
                3 -> AdminCategoryOrdersSection(
                    sectionTitle = "Room Booking Requests",
                    sectionSubtitle = "AC & Non-AC rest room bookings requested by devotees.",
                    emptyMessage = "No room booking requests yet.",
                    categoryFilter = RequestCategory.ROOM_REQUEST,
                    allRequests = requests,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    onUpdateStatus = onUpdateOrderStatus,
                    onVerifyPayment = onVerifyPayment,
                    onMarkWhatsAppShared = onMarkWhatsAppShared,
                    onLoadMoreOrders = onLoadMoreAdminOrders,
                    searchTestTag = "admin_room_order_search"
                )
                4 -> AdminCategoryOrdersSection(
                    sectionTitle = "Freshen-Up Requests",
                    sectionSubtitle = "Only Fresh & Bath + Fresh standalone service requests.",
                    emptyMessage = "No freshen-up requests yet.",
                    categoryFilter = RequestCategory.FRESHEN_UP_REQUEST,
                    allRequests = requests,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    onUpdateStatus = onUpdateOrderStatus,
                    onVerifyPayment = onVerifyPayment,
                    onMarkWhatsAppShared = onMarkWhatsAppShared,
                    onLoadMoreOrders = onLoadMoreAdminOrders,
                    searchTestTag = "admin_freshen_order_search"
                )
                5 -> AdminOverviewAndCustomersSection(
                    stats = dashboardStats,
                    customers = customerDirectory,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    onMarkWhatsAppShared = onMarkWhatsAppShared,
                    onLoadMoreCustomers = onLoadMoreCustomers,
                    showKpis = false,
                    showCustomerDirectory = true
                )
                6 -> AdminMenuAndPriceSection(
                    products = products,
                    categories = siteConfig.categories,
                    onUpdateCategories = onUpdateCategories,
                    onSaveProduct = onSaveProduct,
                    onDeleteProduct = onDeleteProduct
                )
                7 -> AdminStockSection(
                    products = products,
                    lowStockAlerts = lowStockAlerts,
                    onSaveProduct = onSaveProduct
                )
                8 -> AdminThemeCustomizerSection(
                    siteConfig = siteConfig,
                    shopName = shop.shopName,
                    isPreviewActive = isThemePreviewActive,
                    onTogglePreview = onToggleThemePreview,
                    onSaveDraft = onSaveSiteConfigDraft,
                    onPublishTheme = onPublishSiteConfig
                )
                9 -> AdminShopAndNoticesSection(
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    shopEvaluation = shopEvaluation,
                    announcements = announcements,
                    onSaveShop = onSaveShop,
                    onSaveOwnerPrivateSettings = onSaveOwnerPrivateSettings,
                    onSaveAnnouncement = onSaveAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement
                )
                10 -> AdminRoomsAndDeliverySection(
                    rooms = rooms,
                    freshenConfigs = freshen,
                    deliverySettings = delivery,
                    onSaveRoom = onSaveRoom,
                    onSaveFreshen = onSaveFreshen,
                    onSaveDelivery = onSaveDelivery
                )
                11 -> AdminReportsAndWhatsAppSection(
                    requests = requests,
                    report = salesReport,
                    selectedPeriod = reportPeriod,
                    reportStartDateMillis = reportStartDateMillis,
                    reportEndDateMillis = reportEndDateMillis,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    strings = strings,
                    onSelectPeriod = onSelectReportPeriod,
                    onSelectCustomDateRange = onSelectReportDateRange,
                    onRetryWhatsApp = onRetryWhatsApp,
                    onMarkWhatsAppShared = onMarkWhatsAppShared
                )
                12 -> AdminPrivacyAndSecuritySection(
                    ownerEmail = ownerEmail,
                    shop = shop,
                    ownerPrivateSettings = ownerPrivateSettings,
                    customers = customerDirectory,
                    onSaveShop = onSaveShop,
                    onSaveOwnerPrivateSettings = onSaveOwnerPrivateSettings,
                    onSignOut = onSignOut
                )
                13 -> AdminMarutiAiSection(
                    aiState = aiState,
                    onRunCommand = onRunAiCommand,
                    onConfirmAction = onConfirmAiAction,
                    onDismissProposal = onDismissAiProposal
                )
                14 -> AdminActivityLogSection(
                    notifications = notifications,
                    activityLogs = activityLogs,
                    onMarkNotificationRead = onMarkNotificationRead
                )
            }
        }
    }
}
