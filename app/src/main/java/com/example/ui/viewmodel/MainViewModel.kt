package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLogEntry
import com.example.data.model.AdminOrderBadgeState
import com.example.data.model.Announcement
import com.example.data.model.CustomerRequest
import com.example.data.model.CustomerSummary
import com.example.data.model.DeliverySettings
import com.example.data.model.FreshenUpConfig
import com.example.data.model.FulfillmentType
import com.example.data.model.LowStockAlertSummary
import com.example.data.model.LowStockSeverity
import com.example.data.model.OwnerDashboardStats
import com.example.data.model.OwnerNotification
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ReportPeriod
import com.example.data.model.RequestCategory
import com.example.data.model.RequestLineItem
import com.example.data.model.RequestStatus
import com.example.data.model.RoomConfig
import com.example.data.model.SalesReportSummary
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.data.model.SiteConfig
import com.example.data.model.ThemeConfig
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.LanguagePreferencesRepository
import com.example.data.repository.MarutiAiRepository
import com.example.data.repository.MarutiRepository
import com.example.util.DefaultCatalogData
import com.example.util.DomainCalculators
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartKey(
    val productId: String,
    val variantLabel: String,
    val unitPrice: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: MarutiRepository,
    private val languagePrefs: LanguagePreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    private val isBootstrappedOwner: Boolean,
    aiRepository: MarutiAiRepository = MarutiAiRepository()
) : ViewModel() {

    private val aiCoordinator = MarutiAiCoordinator(aiRepository, repository)

    private companion object {
        const val TAG = "MarutiMainVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun <T> Flow<T>.toUiStateFlow(initial: UiState<T>): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing Firestore stream", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load data"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = initial
            )

    val languageCode: StateFlow<String> = languagePrefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), "en")

    private val adminDocExists: StateFlow<Boolean> = repository.observeIsAdminDoc(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val isOwner: StateFlow<Boolean> = adminDocExists
        .map { docExists -> isBootstrappedOwner || docExists }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), isBootstrappedOwner)

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val shopSettings: StateFlow<UiState<ShopSettings>> = repository.observeShopSettings()
        .toUiStateFlow(UiState.Success(DefaultCatalogData.defaultShopSettings))

    val siteConfig: StateFlow<UiState<SiteConfig>> = repository.observeSiteConfig()
        .toUiStateFlow(UiState.Success(DefaultCatalogData.defaultSiteConfig))

    private val _isThemePreviewActive = MutableStateFlow(false)
    val isThemePreviewActive: StateFlow<Boolean> = _isThemePreviewActive.asStateFlow()

    val activeThemeConfig: StateFlow<ThemeConfig> = combine(siteConfig, isOwner, _isThemePreviewActive) { siteState, owner, preview ->
        val site = (siteState as? UiState.Success)?.data ?: DefaultCatalogData.defaultSiteConfig
        if (owner && preview) site.draftTheme else site.publishedTheme
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        DefaultCatalogData.defaultSiteConfig.publishedTheme
    )

    val shopEvaluation: StateFlow<ShopOpenEvaluation> = shopSettings
        .map { state ->
            val shop = (state as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
            DomainCalculators.evaluateShopOpenStatus(shop)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            DomainCalculators.evaluateShopOpenStatus(DefaultCatalogData.defaultShopSettings)
        )

    val announcements: StateFlow<UiState<List<Announcement>>> = repository.observeAnnouncements()
        .toUiStateFlow(UiState.Loading)

    val products: StateFlow<UiState<List<Product>>> = repository.observeProducts()
        .toUiStateFlow(UiState.Success(DefaultCatalogData.initialProducts))

    val roomConfigs: StateFlow<UiState<List<RoomConfig>>> = repository.observeRoomConfigs()
        .toUiStateFlow(UiState.Success(DefaultCatalogData.initialRoomConfigs))

    val freshenUpConfigs: StateFlow<UiState<List<FreshenUpConfig>>> = repository.observeFreshenUpConfigs()
        .toUiStateFlow(UiState.Success(DefaultCatalogData.initialFreshenUpConfigs))

    val deliverySettings: StateFlow<UiState<DeliverySettings>> = repository.observeDeliverySettings()
        .toUiStateFlow(UiState.Success(DefaultCatalogData.defaultDeliverySettings))

    // Pagination limits
    private val _customerPageLimit = MutableStateFlow(30L)
    val myRequests: StateFlow<UiState<List<CustomerRequest>>> = _customerPageLimit
        .flatMapLatest { limit -> repository.observeCustomerRequests(currentUserId, limit) }
        .toUiStateFlow(UiState.Loading)

    private val _adminPageLimit = MutableStateFlow(60L)
    val allAdminRequests: StateFlow<UiState<List<CustomerRequest>>> = combine(isOwner, _adminPageLimit) { owner, limit ->
        owner to limit
    }.flatMapLatest { (owner, limit) ->
        if (owner) repository.observeAllRequestsForAdmin(limit)
        else flowOf(emptyList())
    }.toUiStateFlow(UiState.Loading)

    private val _adminUsersPageLimit = MutableStateFlow(50L)
    val allAdminUsers: StateFlow<UiState<List<UserProfile>>> = combine(isOwner, _adminUsersPageLimit) { owner, limit ->
        owner to limit
    }.flatMapLatest { (owner, limit) ->
        if (owner) repository.observeAllUsersForAdmin(limit)
        else flowOf(emptyList())
    }.toUiStateFlow(UiState.Success(emptyList()))

    val dashboardStats: StateFlow<OwnerDashboardStats> = combine(allAdminRequests, allAdminUsers) { reqState, userState ->
        val reqs = (reqState as? UiState.Success)?.data.orEmpty()
        val users = (userState as? UiState.Success)?.data.orEmpty()
        DomainCalculators.computeOwnerDashboardStats(reqs, users)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OwnerDashboardStats())

    private var hasInitializedAdminSnapshot = false
    private val knownAdminRequestIds = mutableSetOf<String>()
    private val _newlyArrivedOrderIds = MutableStateFlow<Set<String>>(emptySet())
    private val _latestNewOrderSummary = MutableStateFlow<String?>(null)

    private var hasInitializedLowStockSnapshot = false
    private val knownFlaggedLowStockIds = mutableSetOf<String>()
    private val _newlyFlaggedLowStockIds = MutableStateFlow<Set<String>>(emptySet())

    val adminOrderBadges: StateFlow<AdminOrderBadgeState> = combine(
        allAdminRequests,
        _newlyArrivedOrderIds,
        _latestNewOrderSummary
    ) { reqState, newIds, latestSummary ->
        val reqs = (reqState as? UiState.Success)?.data.orEmpty()
        DomainCalculators.computeAdminOrderBadgeCounts(
            requests = reqs,
            newlyArrivedOrderIds = newIds,
            latestNewOrderSummary = latestSummary
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        AdminOrderBadgeState()
    )

    val lowStockAlerts: StateFlow<LowStockAlertSummary> = combine(
        products,
        _newlyFlaggedLowStockIds
    ) { productsState, newlyFlaggedIds ->
        val items = (productsState as? UiState.Success)?.data.orEmpty()
        DomainCalculators.evaluateLowStockAlerts(
            products = items,
            newlyFlaggedProductIds = newlyFlaggedIds
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        LowStockAlertSummary()
    )

    val customerDirectory: StateFlow<List<CustomerSummary>> = combine(allAdminUsers, allAdminRequests) { userState, reqState ->
        val users = (userState as? UiState.Success)?.data.orEmpty()
        val reqs = (reqState as? UiState.Success)?.data.orEmpty()
        DomainCalculators.buildCustomerDirectory(users, reqs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _reportPeriod = MutableStateFlow(ReportPeriod.DAILY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    private val _reportStartDateMillis = MutableStateFlow<Long?>(null)
    val reportStartDateMillis: StateFlow<Long?> = _reportStartDateMillis.asStateFlow()

    private val _reportEndDateMillis = MutableStateFlow<Long?>(null)
    val reportEndDateMillis: StateFlow<Long?> = _reportEndDateMillis.asStateFlow()

    val salesReport: StateFlow<SalesReportSummary> = combine(
        allAdminRequests,
        _reportPeriod,
        _reportStartDateMillis,
        _reportEndDateMillis
    ) { reqState, period, startMillis, endMillis ->
        val reqs = (reqState as? UiState.Success)?.data.orEmpty()
        DomainCalculators.generateSalesReport(
            requests = reqs,
            period = period,
            customStartDateMillis = startMillis,
            customEndDateMillis = endMillis
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        DomainCalculators.generateSalesReport(emptyList(), ReportPeriod.DAILY)
    )

    val ownerPrivateSettings: StateFlow<OwnerPrivateSettings> = isOwner
        .flatMapLatest { owner ->
            if (owner) repository.observeOwnerPrivateSettings().catch { e ->
                if (e is CancellationException) throw e
                emit(DefaultCatalogData.defaultOwnerPrivateSettings)
            }
            else flowOf(OwnerPrivateSettings(ownerWhatsAppNumber = "", whatsappWebhookUrl = ""))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            OwnerPrivateSettings(ownerWhatsAppNumber = "", whatsappWebhookUrl = "")
        )

    val activityLogs: StateFlow<List<ActivityLogEntry>> = isOwner
        .flatMapLatest { owner ->
            if (owner) repository.observeActivityLogs().catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val notifications: StateFlow<List<OwnerNotification>> = isOwner
        .flatMapLatest { owner ->
            if (owner) repository.observeNotifications().catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _aiState = MutableStateFlow<MarutiAiUiState>(MarutiAiUiState.Idle)
    val aiState: StateFlow<MarutiAiUiState> = _aiState.asStateFlow()

    // Carts
    private val _foodCart = MutableStateFlow<Map<CartKey, Long>>(emptyMap())
    val foodCart: StateFlow<Map<CartKey, Long>> = _foodCart.asStateFlow()

    private val _deliveryCart = MutableStateFlow<Map<CartKey, Long>>(emptyMap())
    val deliveryCart: StateFlow<Map<CartKey, Long>> = _deliveryCart.asStateFlow()

    // Submission & Feedback state
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    private var lastSubmitTimestampMs: Long = 0L

    private val _submittedRequest = MutableStateFlow<CustomerRequest?>(null)
    val submittedRequest: StateFlow<CustomerRequest?> = _submittedRequest.asStateFlow()

    private val _recentOrderSuccess = MutableStateFlow<CustomerRequest?>(null)
    val recentOrderSuccess: StateFlow<CustomerRequest?> = _recentOrderSuccess.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val lang = languageCode.value
            repository.ensureUserProfile(
                uid = currentUserId,
                displayName = currentUserDisplayName,
                email = currentUserEmail,
                preferredLanguage = lang
            )
            if (isBootstrappedOwner) {
                repository.verifyAndBootstrapOwner()
            }
        }
        viewModelScope.launch {
            isOwner.collect { owner ->
                if (owner) {
                    repository.seedInitialDataIfOwner()
                } else {
                    hasInitializedAdminSnapshot = false
                    knownAdminRequestIds.clear()
                    _newlyArrivedOrderIds.value = emptySet()
                    _latestNewOrderSummary.value = null
                    hasInitializedLowStockSnapshot = false
                    knownFlaggedLowStockIds.clear()
                    _newlyFlaggedLowStockIds.value = emptySet()
                }
            }
        }
        viewModelScope.launch {
            products.collect { prodState ->
                if (!isOwner.value) return@collect
                val list = (prodState as? UiState.Success)?.data ?: return@collect
                val evaluated = DomainCalculators.evaluateLowStockAlerts(list)
                val currentFlaggedIds = evaluated.flaggedItems.mapNotNull { it.product.id.takeIf(String::isNotBlank) }.toSet()
                if (!hasInitializedLowStockSnapshot) {
                    knownFlaggedLowStockIds.addAll(currentFlaggedIds)
                    hasInitializedLowStockSnapshot = true
                    return@collect
                }
                val newlyFlagged = DomainCalculators.detectNewlyFlaggedLowStockProducts(list, knownFlaggedLowStockIds)
                knownFlaggedLowStockIds.clear()
                knownFlaggedLowStockIds.addAll(currentFlaggedIds)
                if (newlyFlagged.isNotEmpty()) {
                    _newlyFlaggedLowStockIds.update { it + newlyFlagged.mapNotNull { item -> item.product.id.takeIf(String::isNotBlank) } }
                    val first = newlyFlagged.first()
                    _snackbarMessage.value = if (newlyFlagged.size == 1) {
                        if (first.severity == LowStockSeverity.OUT_OF_STOCK) {
                            "OUT OF STOCK ALERT: ${first.product.name} reached 0 qty!"
                        } else {
                            "LOW STOCK ALERT: ${first.product.name} (${first.stockQuantity} left, threshold ${first.threshold})"
                        }
                    } else {
                        "LOW STOCK ALERT: ${newlyFlagged.size} menu items reached critical stock threshold!"
                    }
                }
            }
        }
        viewModelScope.launch {
            allAdminRequests.collect { reqState ->
                if (!isOwner.value) return@collect
                val list = (reqState as? UiState.Success)?.data ?: return@collect
                val incomingIds = list.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()
                if (!hasInitializedAdminSnapshot) {
                    knownAdminRequestIds.addAll(incomingIds)
                    hasInitializedAdminSnapshot = true
                    return@collect
                }
                val newPendingOrders = DomainCalculators.detectNewlyArrivedPendingOrders(
                    incomingRequests = list,
                    previouslyKnownIds = knownAdminRequestIds
                )
                knownAdminRequestIds.addAll(incomingIds)
                if (newPendingOrders.isNotEmpty()) {
                    val newest = newPendingOrders.first()
                    val orderLabel = newest.orderNumber.ifBlank { newest.id.take(8) }
                    val summary = "$orderLabel • ${newest.customerName.ifBlank { "Customer" }} (₹${newest.grandTotal})"
                    _newlyArrivedOrderIds.update { it + newPendingOrders.map { req -> req.id } }
                    _latestNewOrderSummary.value = summary
                    _snackbarMessage.value = if (newPendingOrders.size == 1) {
                        "New order received: $summary"
                    } else {
                        "${newPendingOrders.size} new orders received! Latest: $summary"
                    }
                }
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            languagePrefs.setLanguage(code)
            repository.updateLanguageOnly(uid = currentUserId, preferredLanguage = code)
        }
    }

    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun setReportCustomDateRange(startMillis: Long?, endMillis: Long?) {
        _reportStartDateMillis.value = startMillis
        _reportEndDateMillis.value = endMillis
        if (startMillis != null || endMillis != null) {
            _reportPeriod.value = ReportPeriod.CUSTOM_RANGE
        } else if (_reportPeriod.value == ReportPeriod.CUSTOM_RANGE) {
            _reportPeriod.value = ReportPeriod.DAILY
        }
    }

    fun loadMoreCustomerOrders() {
        _customerPageLimit.update { (it + 30L).coerceAtMost(300L) }
    }

    fun loadMoreAdminOrders() {
        _adminPageLimit.update { (it + 60L).coerceAtMost(500L) }
    }

    fun loadMoreCustomers() {
        _adminUsersPageLimit.update { (it + 50L).coerceAtMost(500L) }
    }

    fun toggleThemePreview() {
        if (!isOwner.value) return
        _isThemePreviewActive.update { !it }
    }

    fun acknowledgeNewOrders(category: RequestCategory? = null) {
        if (category == null) {
            _newlyArrivedOrderIds.value = emptySet()
            _latestNewOrderSummary.value = null
            return
        }
        val reqs = (allAdminRequests.value as? UiState.Success)?.data.orEmpty()
        val idsInCategory = reqs
            .filter { RequestCategory.fromRaw(it.category) == category }
            .mapNotNull { it.id.takeIf(String::isNotBlank) }
            .toSet()
        if (idsInCategory.isNotEmpty()) {
            _newlyArrivedOrderIds.update { current -> current - idsInCategory }
            if (_newlyArrivedOrderIds.value.isEmpty()) {
                _latestNewOrderSummary.value = null
            }
        }
    }

    fun acknowledgeLowStockAlerts() {
        _newlyFlaggedLowStockIds.value = emptySet()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun dismissSubmittedRequest() {
        _submittedRequest.value = null
    }

    fun requestAccountDeletion() {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.requestAccountDeletion(currentUserId)
                .onSuccess {
                    _snackbarMessage.value = "Account deletion request recorded for owner review."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to record deletion request."
                }
            _isSubmitting.value = false
        }
    }

    fun registerCustomer(
        displayName: String,
        mobile: String,
        email: String,
        preferredFulfillment: FulfillmentType,
        defaultAddress: String,
        defaultLocality: String,
        onCompleted: () -> Unit = {}
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.registerCustomerProfile(
                uid = currentUserId,
                displayName = displayName,
                mobile = mobile,
                email = email,
                preferredFulfillment = preferredFulfillment,
                defaultAddress = defaultAddress,
                defaultLocality = defaultLocality,
                preferredLanguage = languageCode.value
            )
            _isSubmitting.value = false
            result.onSuccess {
                _snackbarMessage.value = "Customer profile saved!"
                onCompleted()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Registration failed."
            }
        }
    }

    fun updateFoodCartQuantity(product: Product, variantLabel: String, unitPrice: Long, delta: Long) {
        if (delta > 0L && !shopEvaluation.value.isOpen) {
            _snackbarMessage.value = "SHOP CLOSED: Ordering is disabled while shop is closed."
            return
        }
        val currentQty = _foodCart.value[CartKey(product.id, variantLabel, unitPrice)] ?: 0L
        if (delta > 0L && product.trackStock && currentQty + delta > product.stockQuantity) {
            _snackbarMessage.value = "Only ${product.stockQuantity} in stock for ${product.name}."
            return
        }
        if (delta > 0L && _recentOrderSuccess.value != null) {
            _recentOrderSuccess.value = null
        }
        updateCart(_foodCart, product.id, variantLabel, unitPrice, delta)
    }

    fun updateDeliveryCartQuantity(product: Product, variantLabel: String, unitPrice: Long, delta: Long) {
        if (delta > 0L && !shopEvaluation.value.isOpen) {
            _snackbarMessage.value = "SHOP CLOSED: Ordering is disabled while shop is closed."
            return
        }
        val currentQty = _deliveryCart.value[CartKey(product.id, variantLabel, unitPrice)] ?: 0L
        if (delta > 0L && product.trackStock && currentQty + delta > product.stockQuantity) {
            _snackbarMessage.value = "Only ${product.stockQuantity} in stock for ${product.name}."
            return
        }
        if (delta > 0L && _recentOrderSuccess.value != null) {
            _recentOrderSuccess.value = null
        }
        updateCart(_deliveryCart, product.id, variantLabel, unitPrice, delta)
    }

    private fun updateCart(
        target: MutableStateFlow<Map<CartKey, Long>>,
        productId: String,
        variantLabel: String,
        unitPrice: Long,
        delta: Long
    ) {
        val key = CartKey(productId, variantLabel, unitPrice)
        target.update { current ->
            val next = current.toMutableMap()
            val nextQty = (current[key] ?: 0L) + delta
            if (nextQty <= 0L) next.remove(key) else next[key] = nextQty.coerceAtMost(99L)
            next
        }
    }

    fun clearFoodCart() {
        _foodCart.value = emptyMap()
    }

    fun clearDeliveryCart() {
        _deliveryCart.value = emptyMap()
    }

    fun buildCartLineItems(cart: Map<CartKey, Long>, allProducts: List<Product>): List<RequestLineItem> {
        val productMap = allProducts.associateBy { it.id }
        return cart.entries.mapNotNull { (key, qty) ->
            if (qty <= 0L) return@mapNotNull null
            val product = productMap[key.productId]
            val name = product?.name ?: key.productId
            RequestLineItem(
                name = name,
                variantLabel = key.variantLabel,
                unitPrice = key.unitPrice,
                quantity = qty,
                lineTotal = key.unitPrice * qty
            )
        }
    }

    fun submitFoodOrderRequest(
        customerName: String,
        contactPhone: String,
        expectedArrivalTime: String,
        fulfillmentType: FulfillmentType,
        deliveryLocality: String?,
        deliveryAddress: String?,
        paymentMethod: PaymentMethod,
        upiTransactionRef: String?,
        notes: String,
        allProducts: List<Product>,
        deliveryConfig: DeliverySettings
    ) {
        if (!shopEvaluation.value.isOpen) {
            _snackbarMessage.value = "SHOP CLOSED: Cannot place orders when shop is closed."
            return
        }
        val lineItems = buildCartLineItems(_foodCart.value, allProducts)
        if (lineItems.isEmpty()) {
            _snackbarMessage.value = "Please select at least one food item."
            return
        }
        val foodTotal = DomainCalculators.calculateFoodTotal(lineItems)

        if (fulfillmentType == FulfillmentType.PICKUP && !deliveryConfig.pickupEnabled) {
            _snackbarMessage.value = "Pickup orders are currently paused by the owner."
            return
        }

        if (fulfillmentType == FulfillmentType.DELIVERY) {
            val validation = DomainCalculators.validateDeliveryArea(
                address = deliveryAddress ?: "",
                selectedLocality = deliveryLocality ?: "",
                settings = deliveryConfig
            )
            if (!validation.isValid) {
                _snackbarMessage.value = validation.reason ?: "Delivery is restricted to Khatu Shyam Ji."
                return
            }
            if (foodTotal < deliveryConfig.minimumOrderAmount) {
                _snackbarMessage.value = "Minimum delivery order is ₹${deliveryConfig.minimumOrderAmount}."
                return
            }
        }

        val deliveryCharge = if (fulfillmentType == FulfillmentType.DELIVERY) deliveryConfig.deliveryCharge.coerceAtLeast(0L) else 0L
        val packagingCharge = deliveryConfig.packagingCharge.coerceAtLeast(0L)
        val grandTotal = DomainCalculators.calculateGrandTotal(
            foodTotal = foodTotal,
            roomTotal = 0L,
            freshenUpTotal = 0L,
            deliveryCharge = deliveryCharge,
            packagingCharge = packagingCharge
        )

        val category = if (fulfillmentType == FulfillmentType.DELIVERY) {
            RequestCategory.FOOD_DELIVERY_REQUEST.name
        } else {
            RequestCategory.FOOD_REQUEST.name
        }

        submitRequestInternal(
            CustomerRequest(
                userId = currentUserId,
                category = category,
                customerName = customerName,
                contactPhone = contactPhone,
                customerEmail = userProfile.value?.email?.ifBlank { currentUserEmail } ?: currentUserEmail,
                items = lineItems,
                foodTotal = foodTotal,
                roomTotal = 0L,
                freshenUpTotal = 0L,
                deliveryCharge = deliveryCharge,
                packagingCharge = packagingCharge,
                grandTotal = grandTotal,
                expectedTime = expectedArrivalTime,
                fulfillmentType = fulfillmentType.name,
                paymentMethod = paymentMethod.name,
                upiTransactionRef = upiTransactionRef?.takeIf { it.isNotBlank() },
                deliveryAddress = if (fulfillmentType == FulfillmentType.DELIVERY) deliveryAddress else null,
                deliveryLocality = if (fulfillmentType == FulfillmentType.DELIVERY) deliveryLocality else null,
                notes = notes.ifBlank { null }
            ),
            onSuccess = { _foodCart.value = emptyMap() }
        )
    }

    fun submitRoomRequest(
        roomConfig: RoomConfig,
        durationHours: Int,
        numberOfRooms: Int,
        numberOfPeople: Int,
        expectedArrivalTime: String,
        customerName: String,
        contactPhone: String,
        notes: String
    ) {
        if (!roomConfig.available || roomConfig.statusLabel != "AVAILABLE") {
            _snackbarMessage.value = "${roomConfig.title} is currently ${roomConfig.statusLabel.lowercase().replace('_', ' ')}."
            return
        }
        val unitRate = DomainCalculators.resolveRoomRateForDuration(roomConfig, durationHours)
        val roomTotal = DomainCalculators.calculateRoomTotal(unitRate, numberOfRooms)
        val rateConfigured = unitRate != null
        val summaryItem = RequestLineItem(
            name = "${roomConfig.title} ($durationHours hr)",
            variantLabel = if (rateConfigured) "₹$unitRate / room" else "Owner rate TBD",
            unitPrice = unitRate ?: 0L,
            quantity = numberOfRooms.toLong(),
            lineTotal = roomTotal
        )
        submitRequestInternal(
            CustomerRequest(
                userId = currentUserId,
                category = RequestCategory.ROOM_REQUEST.name,
                customerName = customerName,
                contactPhone = contactPhone,
                customerEmail = currentUserEmail,
                items = listOf(summaryItem),
                foodTotal = 0L,
                roomTotal = roomTotal,
                freshenUpTotal = 0L,
                grandTotal = roomTotal,
                expectedTime = expectedArrivalTime,
                fulfillmentType = FulfillmentType.PICKUP.name,
                paymentMethod = PaymentMethod.CASH.name,
                roomType = roomConfig.roomType,
                numberOfRooms = numberOfRooms.toLong(),
                numberOfPeople = numberOfPeople.toLong(),
                roomDurationHours = durationHours.toLong(),
                roomRateConfigured = rateConfigured,
                notes = notes.ifBlank { null }
            )
        )
    }

    fun submitFreshenUpRequest(
        config: FreshenUpConfig,
        numberOfPeople: Int,
        expectedArrivalTime: String,
        customerName: String,
        contactPhone: String,
        notes: String
    ) {
        if (!config.available) {
            _snackbarMessage.value = "${config.title} is currently unavailable."
            return
        }
        val total = DomainCalculators.calculateFreshenUpTotal(config.pricePerPerson, numberOfPeople)
        val lineItem = RequestLineItem(
            name = config.title,
            variantLabel = "Per Person",
            unitPrice = config.pricePerPerson,
            quantity = numberOfPeople.toLong(),
            lineTotal = total
        )
        submitRequestInternal(
            CustomerRequest(
                userId = currentUserId,
                category = RequestCategory.FRESHEN_UP_REQUEST.name,
                customerName = customerName,
                contactPhone = contactPhone,
                customerEmail = currentUserEmail,
                items = listOf(lineItem),
                foodTotal = 0L,
                roomTotal = 0L,
                freshenUpTotal = total,
                grandTotal = total,
                expectedTime = expectedArrivalTime,
                fulfillmentType = FulfillmentType.PICKUP.name,
                paymentMethod = PaymentMethod.CASH.name,
                numberOfPeople = numberOfPeople.toLong(),
                freshenUpOption = config.optionKey,
                notes = notes.ifBlank { null }
            )
        )
    }

    fun submitFoodDeliveryRequest(
        customerName: String,
        contactPhone: String,
        deliveryLocality: String,
        deliveryAddress: String,
        requestedDeliveryTime: String,
        paymentMethod: PaymentMethod,
        upiTransactionRef: String?,
        notes: String,
        allProducts: List<Product>,
        deliveryConfig: DeliverySettings
    ) {
        if (!shopEvaluation.value.isOpen) {
            _snackbarMessage.value = "SHOP CLOSED: Delivery ordering is disabled while shop is closed."
            return
        }
        val lineItems = buildCartLineItems(_deliveryCart.value, allProducts)
        if (lineItems.isEmpty()) {
            _snackbarMessage.value = "Please select at least one food item for delivery."
            return
        }
        val foodTotal = DomainCalculators.calculateFoodTotal(lineItems)
        if (foodTotal < deliveryConfig.minimumOrderAmount) {
            _snackbarMessage.value = "Minimum order for delivery is ₹${deliveryConfig.minimumOrderAmount}."
            return
        }
        val validation = DomainCalculators.validateDeliveryArea(deliveryAddress, deliveryLocality, deliveryConfig)
        if (!validation.isValid) {
            _snackbarMessage.value = validation.reason ?: "Address outside delivery area."
            return
        }
        val deliveryCharge = deliveryConfig.deliveryCharge.coerceAtLeast(0L)
        val packagingCharge = deliveryConfig.packagingCharge.coerceAtLeast(0L)
        val grandTotal = DomainCalculators.calculateGrandTotal(
            foodTotal = foodTotal,
            roomTotal = 0L,
            freshenUpTotal = 0L,
            deliveryCharge = deliveryCharge,
            packagingCharge = packagingCharge
        )
        submitRequestInternal(
            CustomerRequest(
                userId = currentUserId,
                category = RequestCategory.FOOD_DELIVERY_REQUEST.name,
                customerName = customerName,
                contactPhone = contactPhone,
                customerEmail = currentUserEmail,
                items = lineItems,
                foodTotal = foodTotal,
                roomTotal = 0L,
                freshenUpTotal = 0L,
                deliveryCharge = deliveryCharge,
                packagingCharge = packagingCharge,
                grandTotal = grandTotal,
                expectedTime = requestedDeliveryTime,
                fulfillmentType = FulfillmentType.DELIVERY.name,
                paymentMethod = paymentMethod.name,
                upiTransactionRef = upiTransactionRef?.takeIf { it.isNotBlank() },
                deliveryAddress = deliveryAddress,
                deliveryLocality = deliveryLocality,
                notes = notes.ifBlank { null }
            ),
            onSuccess = { _deliveryCart.value = emptyMap() }
        )
    }

    private fun submitRequestInternal(
        request: CustomerRequest,
        onSuccess: () -> Unit = {}
    ) {
        if (_isSubmitting.value) return
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastSubmitTimestampMs < 3000L) {
            _snackbarMessage.value = "Please wait a moment before submitting again."
            return
        }
        if (request.customerName.isBlank()) {
            _snackbarMessage.value = "Please enter customer name."
            return
        }
        if (!DomainCalculators.isValidMobileNumber(request.contactPhone)) {
            _snackbarMessage.value = "Please enter a valid 10-digit mobile number."
            return
        }
        if (request.expectedTime.isBlank()) {
            _snackbarMessage.value = "Please enter expected arrival or delivery time."
            return
        }

        val shop = (shopSettings.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
        val enforcedPaymentMethod = if (shop.upiEnabled && request.paymentMethod == PaymentMethod.UPI.name) {
            PaymentMethod.UPI.name
        } else {
            PaymentMethod.CASH.name
        }
        val enforcedRequest = request.copy(
            paymentMethod = enforcedPaymentMethod,
            upiTransactionRef = if (enforcedPaymentMethod == PaymentMethod.UPI.name) request.upiTransactionRef else null
        )

        viewModelScope.launch {
            _isSubmitting.value = true
            lastSubmitTimestampMs = System.currentTimeMillis()
            val existingProfile = userProfile.value
            if (existingProfile == null || !existingProfile.registrationCompleted) {
                val regRes = repository.registerCustomerProfile(
                    uid = currentUserId,
                    displayName = enforcedRequest.customerName,
                    mobile = enforcedRequest.contactPhone,
                    email = existingProfile?.email?.ifBlank { currentUserEmail } ?: currentUserEmail,
                    preferredFulfillment = FulfillmentType.fromRaw(enforcedRequest.fulfillmentType),
                    defaultAddress = enforcedRequest.deliveryAddress ?: existingProfile?.defaultAddress ?: "",
                    defaultLocality = enforcedRequest.deliveryLocality ?: existingProfile?.defaultLocality ?: "Khatu Shyam Ji",
                    preferredLanguage = languageCode.value
                )
                if (regRes.isFailure) {
                    _isSubmitting.value = false
                    _snackbarMessage.value = regRes.exceptionOrNull()?.localizedMessage ?: "Customer registration failed."
                    return@launch
                }
            }

            val result = repository.submitCustomerRequest(enforcedRequest, shop)
            _isSubmitting.value = false
            result.onSuccess { saved ->
                onSuccess()
                _recentOrderSuccess.value = saved
                viewModelScope.launch {
                    delay(950L)
                    _submittedRequest.value = saved
                    delay(3500L)
                    if (_recentOrderSuccess.value?.id == saved.id) {
                        _recentOrderSuccess.value = null
                    }
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to submit order."
            }
        }
    }

    fun markManualWhatsAppShared(requestId: String) {
        viewModelScope.launch {
            repository.markOrderManualWhatsAppShared(requestId)
        }
    }

    // --- Owner Admin Actions ---

    fun adminSaveShopSettings(settings: ShopSettings) {
        if (!isOwner.value) return
        val prev = (shopSettings.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.saveShopSettings(settings)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Updated Shop & Payment Settings",
                        targetType = "shop_settings",
                        targetId = "main",
                        previousValue = "mode=${prev?.shopControlMode}, open=${prev?.shopOpen}, upi=${prev?.upiEnabled}, cash=${prev?.cashEnabled}",
                        newValue = "mode=${settings.shopControlMode}, open=${settings.shopOpen}, upi=${settings.upiEnabled}, cash=${settings.cashEnabled}"
                    )
                    _snackbarMessage.value = "Shop & payment settings updated."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update shop settings." }
        }
    }

    fun adminSaveSiteConfigDraft(config: SiteConfig) {
        if (!isOwner.value) return
        val prev = (siteConfig.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.saveSiteConfig(config)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Saved Theme & Layout Draft",
                        targetType = "site_config",
                        targetId = "main",
                        previousValue = "draft=${prev?.draftTheme?.preset}, cats=${prev?.categories?.size}",
                        newValue = "draft=${config.draftTheme.preset}, cats=${config.categories.size}"
                    )
                    _snackbarMessage.value = "Theme draft saved! Toggle Live Preview to inspect before publishing."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save theme draft." }
        }
    }

    fun adminPublishSiteConfig(config: SiteConfig) {
        if (!isOwner.value) return
        val prev = (siteConfig.value as? UiState.Success)?.data
        val publishedConfig = config.copy(publishedTheme = config.draftTheme)
        viewModelScope.launch {
            repository.saveSiteConfig(publishedConfig)
                .onSuccess {
                    _isThemePreviewActive.value = false
                    repository.logOwnerActivity(
                        action = "Published Website & App Theme",
                        targetType = "site_config",
                        targetId = "main",
                        previousValue = "published=${prev?.publishedTheme?.preset}",
                        newValue = "published=${publishedConfig.publishedTheme.preset} (${publishedConfig.publishedTheme.primaryColorHex})"
                    )
                    _snackbarMessage.value = "Theme published live to all customers & website!"
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to publish theme." }
        }
    }

    fun adminUpdateCategories(categories: List<String>) {
        if (!isOwner.value) return
        val current = (siteConfig.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultSiteConfig
        val cleaned = categories.map { it.trim().take(50) }.filter { it.isNotBlank() }.distinct().take(20)
        if (cleaned.isEmpty()) {
            _snackbarMessage.value = "Keep at least one category."
            return
        }
        viewModelScope.launch {
            repository.saveSiteConfig(current.copy(categories = cleaned))
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Updated Menu Categories",
                        targetType = "site_config",
                        targetId = "main",
                        previousValue = current.categories.joinToString(),
                        newValue = cleaned.joinToString()
                    )
                    _snackbarMessage.value = "Menu categories updated."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update categories." }
        }
    }

    fun adminSaveOwnerPrivateSettings(settings: OwnerPrivateSettings) {
        if (!isOwner.value) return
        val prev = ownerPrivateSettings.value
        viewModelScope.launch {
            repository.saveOwnerPrivateSettings(settings)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Updated Owner Private Notification Settings",
                        targetType = "owner_private_settings",
                        targetId = "main",
                        previousValue = "whatsappConfigured=${prev.ownerWhatsAppNumber.isNotBlank()}, webhook=${prev.whatsappWebhookUrl.isNotBlank()}",
                        newValue = "whatsappConfigured=${settings.ownerWhatsAppNumber.isNotBlank()}, webhook=${settings.whatsappWebhookUrl.isNotBlank()}"
                    )
                    _snackbarMessage.value = "Owner notification settings saved."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save private settings." }
        }
    }

    fun adminSaveAnnouncement(announcement: Announcement) {
        if (!isOwner.value) return
        viewModelScope.launch {
            repository.saveAnnouncement(announcement)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Saved Announcement",
                        targetType = "announcements",
                        targetId = announcement.id.ifBlank { "new" },
                        previousValue = "",
                        newValue = "${announcement.title} (active=${announcement.active}, special=${announcement.dailySpecial})"
                    )
                    _snackbarMessage.value = "Announcement saved."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save announcement." }
        }
    }

    fun adminDeleteAnnouncement(id: String) {
        if (!isOwner.value) return
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Deleted Announcement",
                        targetType = "announcements",
                        targetId = id,
                        previousValue = id,
                        newValue = "deleted"
                    )
                    _snackbarMessage.value = "Announcement deleted."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete announcement." }
        }
    }

    fun adminSaveProduct(product: Product) {
        if (!isOwner.value) return
        val prev = (products.value as? UiState.Success)?.data?.firstOrNull { it.id == product.id }
        viewModelScope.launch {
            repository.saveProduct(product)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = if (prev == null) "Added Menu Item: ${product.name}" else "Updated Menu Item: ${product.name}",
                        targetType = "products",
                        targetId = product.id.ifBlank { product.name },
                        previousValue = prev?.let {
                            "avail=${it.available}, stock=${it.stockQuantity}, prices=${it.priceOptions.joinToString { p -> "${p.label}:₹${p.price}" }}"
                        } ?: "new",
                        newValue = "avail=${product.available}, stock=${product.stockQuantity}, prices=${product.priceOptions.joinToString { p -> "${p.label}:₹${p.price}" }}"
                    )
                    _snackbarMessage.value = "Menu item saved."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to save product." }
        }
    }

    fun adminDeleteProduct(productId: String) {
        if (!isOwner.value) return
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Deleted Menu Item",
                        targetType = "products",
                        targetId = productId,
                        previousValue = productId,
                        newValue = "deleted"
                    )
                    _snackbarMessage.value = "Menu item removed."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to delete product." }
        }
    }

    fun adminSaveRoomConfig(config: RoomConfig) {
        if (!isOwner.value) return
        val prev = (roomConfigs.value as? UiState.Success)?.data?.firstOrNull { it.roomType == config.roomType }
        viewModelScope.launch {
            repository.saveRoomConfig(config)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Updated Room Config: ${config.title}",
                        targetType = "room_configs",
                        targetId = config.id.ifBlank { config.roomType },
                        previousValue = "status=${prev?.statusLabel}, 6h=₹${prev?.rate6Hours}, 12h=₹${prev?.rate12Hours}",
                        newValue = "status=${config.statusLabel}, 6h=₹${config.rate6Hours}, 12h=₹${config.rate12Hours}"
                    )
                    _snackbarMessage.value = "${config.title} updated."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update room settings." }
        }
    }

    fun adminSaveFreshenUpConfig(config: FreshenUpConfig) {
        if (!isOwner.value) return
        val prev = (freshenUpConfigs.value as? UiState.Success)?.data?.firstOrNull { it.optionKey == config.optionKey }
        viewModelScope.launch {
            repository.saveFreshenUpConfig(config)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Updated Freshen Up: ${config.optionKey}",
                        targetType = "freshen_up_configs",
                        targetId = config.id.ifBlank { config.optionKey },
                        previousValue = "₹${prev?.pricePerPerson}, avail=${prev?.available}",
                        newValue = "₹${config.pricePerPerson}, avail=${config.available}"
                    )
                    _snackbarMessage.value = "${config.title} updated."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update freshen-up option." }
        }
    }

    fun adminSaveDeliverySettings(settings: DeliverySettings) {
        if (!isOwner.value) return
        val prev = (deliverySettings.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.saveDeliverySettings(settings)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Updated Delivery & Charge Settings",
                        targetType = "delivery_settings",
                        targetId = "main",
                        previousValue = "delivery=${prev?.deliveryEnabled}, pickup=${prev?.pickupEnabled}, delCharge=₹${prev?.deliveryCharge}, packCharge=₹${prev?.packagingCharge}",
                        newValue = "delivery=${settings.deliveryEnabled}, pickup=${settings.pickupEnabled}, delCharge=₹${settings.deliveryCharge}, packCharge=₹${settings.packagingCharge}"
                    )
                    _snackbarMessage.value = "Delivery settings updated."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update delivery settings." }
        }
    }

    fun adminUpdateOrderStatus(requestId: String, status: RequestStatus, adminNote: String?) {
        if (!isOwner.value) return
        val prev = (allAdminRequests.value as? UiState.Success)?.data?.firstOrNull { it.id == requestId }
        val currentProducts = (products.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            repository.updateRequestStatusAndPayment(requestId, newStatus = status, adminNote = adminNote)
                .onSuccess {
                    val stockUpdates = DomainCalculators.computeStockDeductionsOnOrderApproval(
                        previousStatus = prev?.status,
                        newStatus = status,
                        order = prev,
                        products = currentProducts
                    )
                    stockUpdates.forEach { updatedProduct ->
                        repository.saveProduct(updatedProduct)
                    }
                    repository.logOwnerActivity(
                        action = "Order ${prev?.orderNumber ?: requestId} Status -> ${status.name}",
                        targetType = "requests",
                        targetId = requestId,
                        previousValue = prev?.status ?: "",
                        newValue = status.name
                    )
                    _snackbarMessage.value = "Order updated to ${status.name}."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update order status." }
        }
    }

    fun adminVerifyPayment(requestId: String, paymentStatus: PaymentStatus, adminNote: String?) {
        if (!isOwner.value) return
        val prev = (allAdminRequests.value as? UiState.Success)?.data?.firstOrNull { it.id == requestId }
        viewModelScope.launch {
            repository.updateRequestStatusAndPayment(requestId, newPaymentStatus = paymentStatus, adminNote = adminNote)
                .onSuccess {
                    repository.logOwnerActivity(
                        action = "Order ${prev?.orderNumber ?: requestId} Payment -> ${paymentStatus.name}",
                        targetType = "requests",
                        targetId = requestId,
                        previousValue = prev?.paymentStatus ?: "",
                        newValue = paymentStatus.name
                    )
                    _snackbarMessage.value = "Payment status marked as ${paymentStatus.name}."
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to update payment status." }
        }
    }

    fun adminRetryWhatsApp(requestId: String) {
        if (!isOwner.value) return
        val shop = (shopSettings.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
        val webhook = ownerPrivateSettings.value.whatsappWebhookUrl.ifBlank { shop.whatsappWebhookUrl }
        viewModelScope.launch {
            repository.retryWhatsAppViaCloudFunction(requestId, webhook)
                .onSuccess { msg -> _snackbarMessage.value = msg }
                .onFailure { err -> _snackbarMessage.value = err.localizedMessage ?: "WhatsApp retry failed." }
        }
    }

    fun markNotificationRead(notificationId: String) {
        if (!isOwner.value) return
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun runAiCommand(prompt: String) {
        if (!isOwner.value) return
        val cleaned = prompt.trim()
        if (cleaned.isBlank()) {
            _aiState.value = MarutiAiUiState.Error("Please enter a command in Hindi or English.")
            return
        }
        val shop = (shopSettings.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
        val delivery = (deliverySettings.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultDeliverySettings
        val productList = (products.value as? UiState.Success)?.data ?: emptyList()
        val stats = dashboardStats.value

        viewModelScope.launch {
            _aiState.value = MarutiAiUiState.Loading(cleaned)
            _aiState.value = aiCoordinator.interpret(cleaned, shop, delivery, productList, stats)
        }
    }

    fun confirmPendingAiAction() {
        if (!isOwner.value) return
        val proposal = _aiState.value as? MarutiAiUiState.Proposal ?: return
        val shop = (shopSettings.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
        val delivery = (deliverySettings.value as? UiState.Success)?.data ?: DefaultCatalogData.defaultDeliverySettings
        val productList = (products.value as? UiState.Success)?.data ?: emptyList()

        viewModelScope.launch {
            _aiState.value = MarutiAiUiState.Loading(proposal.prompt)
            aiCoordinator.applyConfirmedAction(proposal.action, shop, delivery, productList)
                .onSuccess { message ->
                    _aiState.value = MarutiAiUiState.ResultMessage(proposal.prompt, message)
                    _snackbarMessage.value = message
                }
                .onFailure { err ->
                    _aiState.value = MarutiAiUiState.Error(err.localizedMessage ?: "Failed to apply AI action.")
                }
        }
    }

    fun dismissAiProposal() {
        _aiState.value = MarutiAiUiState.Idle
    }
}
