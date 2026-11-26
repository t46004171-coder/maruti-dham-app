package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.DeliverySettings
import com.example.data.model.FulfillmentType
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.RequestCategory
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AnimatedSuccessCheckmark
import com.example.ui.components.DevotionalScreenHeader
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.OrderSummarySuccessBanner
import com.example.ui.components.ProductOrServiceImage
import com.example.ui.components.QuantityStepper
import com.example.ui.components.ScrollToTopIndicator
import com.example.ui.components.ShopClosedBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.localDrawableForProductKey
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.viewmodel.CartKey
import com.example.util.DomainCalculators

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodOrderScreen(
    productsState: UiState<List<Product>>,
    shopEvaluation: ShopOpenEvaluation,
    shopSettings: ShopSettings,
    deliverySettings: DeliverySettings,
    siteConfigCategories: List<String> = emptyList(),
    cart: Map<CartKey, Long>,
    userProfile: UserProfile?,
    defaultUserName: String,
    defaultUserEmail: String,
    isSubmitting: Boolean,
    recentSuccessOrder: CustomerRequest? = null,
    strings: AppStrings,
    onUpdateQuantity: (Product, String, Long, Long) -> Unit,
    onRegisterCustomer: (String, String, String, FulfillmentType, String, String, () -> Unit) -> Unit,
    onSubmitRequest: (
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
    ) -> Unit,
    onOpenMyRequests: () -> Unit
) {
    val allProducts = (productsState as? UiState.Success)?.data ?: emptyList()
    val visibleProducts = remember(allProducts) {
        allProducts.filter { it.visible && it.foodOrderEnabled }
    }
    val categories = remember(visibleProducts, siteConfigCategories) {
        listOf("All") + (siteConfigCategories + visibleProducts.map { it.category }).distinct()
    }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts by remember(visibleProducts, selectedCategory, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            visibleProducts.filter { p ->
                val catMatch = selectedCategory == "All" || p.category == selectedCategory
                val textMatch = q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.nameHindi.lowercase().contains(q) ||
                    p.category.lowercase().contains(q)
                catMatch && textMatch
            }
        }
    }

    var customerName by remember { mutableStateOf(defaultUserName) }
    var contactPhone by remember { mutableStateOf("") }
    var arrivalTime by remember { mutableStateOf("") }
    var fulfillmentType by remember { mutableStateOf(FulfillmentType.PICKUP) }
    var deliveryLocality by remember(deliverySettings.allowedLocalities) {
        mutableStateOf(deliverySettings.allowedLocalities.firstOrNull() ?: "Khatu Shyam Ji")
    }
    var deliveryAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var upiRef by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showRegistrationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            if (customerName.isBlank() && profile.displayName.isNotBlank()) customerName = profile.displayName
            if (contactPhone.isBlank() && profile.phone.isNotBlank()) contactPhone = profile.phone
            if (deliveryAddress.isBlank() && profile.defaultAddress.isNotBlank()) deliveryAddress = profile.defaultAddress
            if (profile.defaultLocality.isNotBlank()) deliveryLocality = profile.defaultLocality
            if (profile.registrationCompleted) {
                fulfillmentType = FulfillmentType.fromRaw(profile.preferredFulfillment)
            }
        }
    }

    val selectedLineItems by remember(cart, allProducts) {
        derivedStateOf {
            val productMap = allProducts.associateBy { it.id }
            cart.entries.mapNotNull { (key, qty) ->
                if (qty <= 0L) null else {
                    val p = productMap[key.productId]
                    Triple("${p?.name ?: key.productId} (${key.variantLabel})", qty, key.unitPrice * qty)
                }
            }
        }
    }
    val foodTotal by remember(cart) {
        derivedStateOf { cart.entries.sumOf { (key, qty) -> key.unitPrice * qty } }
    }

    val areaValidation = remember(fulfillmentType, deliveryAddress, deliveryLocality, deliverySettings) {
        if (fulfillmentType == FulfillmentType.DELIVERY) {
            DomainCalculators.validateDeliveryArea(deliveryAddress, deliveryLocality, deliverySettings)
        } else {
            com.example.util.DeliveryValidationResult(true)
        }
    }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RoyalGold,
        unfocusedBorderColor = AntiqueGold.copy(alpha = 0.45f),
        focusedLabelColor = LuminousGold,
        unfocusedLabelColor = MutedSlate,
        focusedTextColor = CreamIvory,
        unfocusedTextColor = CreamIvory
    )

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DevotionalScreenHeader(
            title = strings.foodMenuTitle,
            subtitle = "${shopEvaluation.badgeText} • ${strings.subtitleLocation}"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (!shopEvaluation.isOpen) {
                item {
                    ShopClosedBanner(evaluation = shopEvaluation, strings = strings)
                }
            }

            if (userProfile != null && !userProfile.registrationCompleted) {
                item {
                    GoldOutlineCard(containerColor = NavyElevated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.completeRegistrationTitle, style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                                Text(strings.completeRegistrationSubtitle, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showRegistrationDialog = true },
                                border = BorderStroke(1.dp, RoyalGold),
                                modifier = Modifier.testTag("open_registration_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Register", color = RoyalGold)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(if (strings.isHindi) "मेन्यू खोजें" else "Search menu items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalGold) },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("food_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalGold,
                                    selectedLabelColor = NavyDeep
                                )
                            )
                        }
                    }
                    IconButton(onClick = onOpenMyRequests) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = strings.myRequestsTitle, tint = RoyalGold)
                    }
                }
            }

            if (productsState is UiState.Error) {
                item {
                    GoldOutlineCard {
                        Text(productsState.message, color = StatusClosedRed)
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    GoldOutlineCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = RoyalGold)
                            Text("No menu items match this filter.", color = MutedSlate)
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    FoodProductCard(
                        product = product,
                        cart = cart,
                        shopIsOpen = shopEvaluation.isOpen,
                        strings = strings,
                        onUpdateQuantity = { label, price, delta ->
                            onUpdateQuantity(product, label, price, delta)
                        }
                    )
                }
            }

            // Order Summary, Fulfillment & Checkout Form
            item {
                val showSuccess = recentSuccessOrder != null && (
                    recentSuccessOrder.category == RequestCategory.FOOD_REQUEST.name ||
                        recentSuccessOrder.category == RequestCategory.FOOD_DELIVERY_REQUEST.name
                    )
                val summaryBorderColor by animateColorAsState(
                    targetValue = if (showSuccess) StatusOpenGreen else RoyalGold.copy(alpha = 0.45f),
                    animationSpec = tween(durationMillis = 320),
                    label = "food_summary_border"
                )
                GoldOutlineCard(
                    containerColor = NavyElevated,
                    borderColor = summaryBorderColor
                ) {
                    Text(
                        text = strings.orderSummary,
                        style = MaterialTheme.typography.titleLarge,
                        color = LuminousGold
                    )
                    OrderSummarySuccessBanner(
                        visible = showSuccess,
                        orderNumber = recentSuccessOrder?.orderNumber,
                        isHindi = strings.isHindi,
                        testTag = "food_order_success_banner"
                    )
                    Spacer(Modifier.height(8.dp))

                    if (selectedLineItems.isEmpty()) {
                        Text(
                            text = strings.selectItemsPrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                    } else {
                        val appliedDeliveryCharge = if (fulfillmentType == FulfillmentType.DELIVERY) deliverySettings.deliveryCharge.coerceAtLeast(0L) else 0L
                        val appliedPackagingCharge = deliverySettings.packagingCharge.coerceAtLeast(0L)
                        val computedGrandTotal = DomainCalculators.calculateGrandTotal(
                            foodTotal = foodTotal,
                            roomTotal = 0L,
                            freshenUpTotal = 0L,
                            deliveryCharge = appliedDeliveryCharge,
                            packagingCharge = appliedPackagingCharge
                        )
                        selectedLineItems.forEach { (title, qty, total) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$title x$qty", style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                                Text("₹$total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LuminousGold)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AntiqueGold.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.subtotalLabel, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                            Text("₹$foodTotal", style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                        }
                        if (appliedDeliveryCharge > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.deliveryChargeLabel, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                                Text("₹$appliedDeliveryCharge", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                            }
                        }
                        if (appliedPackagingCharge > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.packagingChargeLabel, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                                Text("₹$appliedPackagingCharge", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.grandTotalLabel, style = MaterialTheme.typography.titleMedium, color = CreamIvory)
                            Text("₹$computedGrandTotal", style = MaterialTheme.typography.titleLarge, color = LuminousGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Order Mode:", style = MaterialTheme.typography.labelLarge, color = CreamIvory)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = fulfillmentType == FulfillmentType.PICKUP,
                            onClick = { fulfillmentType = FulfillmentType.PICKUP },
                            enabled = deliverySettings.pickupEnabled,
                            label = { Text(strings.pickupLabel) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep),
                            modifier = Modifier.testTag("mode_pickup_chip")
                        )
                        FilterChip(
                            selected = fulfillmentType == FulfillmentType.DELIVERY,
                            onClick = { fulfillmentType = FulfillmentType.DELIVERY },
                            enabled = deliverySettings.deliveryEnabled,
                            label = { Text(strings.deliveryLabel) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep),
                            modifier = Modifier.testTag("mode_delivery_chip")
                        )
                    }
                    if (!deliverySettings.pickupEnabled && fulfillmentType == FulfillmentType.PICKUP) {
                        Text(
                            text = strings.pickupPausedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusClosedRed
                        )
                    }

                    if (fulfillmentType == FulfillmentType.DELIVERY) {
                        Spacer(Modifier.height(8.dp))
                        Text(strings.deliveryLocalityLabel, style = MaterialTheme.typography.labelMedium, color = LuminousGold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            deliverySettings.allowedLocalities.forEach { loc ->
                                FilterChip(
                                    selected = deliveryLocality == loc,
                                    onClick = { deliveryLocality = loc },
                                    label = { Text(loc) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text(strings.deliveryAddressLabel) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalGold) },
                            colors = tfColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (deliveryAddress.isNotBlank() && !areaValidation.isValid) {
                            Text(
                                text = areaValidation.reason ?: strings.deliveryOutsideError,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusClosedRed
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(strings.paymentMethodLabel, style = MaterialTheme.typography.labelLarge, color = CreamIvory)
                    if (!shopSettings.upiEnabled) {
                        LaunchedEffect(shopSettings.upiEnabled) {
                            paymentMethod = PaymentMethod.CASH
                            upiRef = ""
                        }
                        Spacer(Modifier.height(4.dp))
                        StatusPill(
                            text = strings.cashOnlyBadge,
                            color = RoyalGold,
                            modifier = Modifier.testTag("cash_only_badge")
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = paymentMethod == PaymentMethod.CASH,
                                onClick = { paymentMethod = PaymentMethod.CASH },
                                label = { Text(strings.cashPaymentLabel) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep),
                                modifier = Modifier.testTag("payment_cash_chip")
                            )
                            FilterChip(
                                selected = paymentMethod == PaymentMethod.UPI,
                                onClick = { paymentMethod = PaymentMethod.UPI },
                                label = { Text(strings.upiPaymentLabel) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep),
                                modifier = Modifier.testTag("payment_upi_chip")
                            )
                        }

                        if (paymentMethod == PaymentMethod.UPI) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = LuminousGold, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (shopSettings.upiId.isNotBlank()) {
                                        "Pay to UPI ID: ${shopSettings.upiId} (Owner verifies before marking Paid)"
                                    } else {
                                        "Pay at stall UPI QR; owner verifies payment before marking Paid."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusPendingAmber
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = upiRef,
                                onValueChange = { upiRef = it },
                                label = { Text("UPI Transaction Ref / UTR (Optional)") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(strings.customerNameLabel) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("food_customer_name")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(strings.contactPhoneLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("food_contact_phone")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = arrivalTime,
                        onValueChange = { arrivalTime = it },
                        label = {
                            Text(
                                if (fulfillmentType == FulfillmentType.DELIVERY) strings.expectedDeliveryTimeLabel
                                else strings.expectedArrivalTimeLabel
                            )
                        },
                        placeholder = { Text("e.g., 09:30 AM / In 20 mins") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = RoyalGold) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("food_arrival_time")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.optionalNotesLabel) },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = strings.noAdvancePaymentNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Spacer(Modifier.height(10.dp))
                    val buttonContainerColor by animateColorAsState(
                        targetValue = if (showSuccess) StatusOpenGreen else RoyalGold,
                        animationSpec = tween(durationMillis = 280),
                        label = "food_submit_btn_color"
                    )
                    Button(
                        onClick = {
                            if (userProfile != null && !userProfile.registrationCompleted) {
                                showRegistrationDialog = true
                            } else {
                                onSubmitRequest(
                                    customerName,
                                    contactPhone,
                                    arrivalTime,
                                    fulfillmentType,
                                    if (fulfillmentType == FulfillmentType.DELIVERY) deliveryLocality else null,
                                    if (fulfillmentType == FulfillmentType.DELIVERY) deliveryAddress else null,
                                    paymentMethod,
                                    upiRef.takeIf { it.isNotBlank() },
                                    notes,
                                    allProducts,
                                    deliverySettings
                                )
                            }
                        },
                        enabled = shopEvaluation.isOpen &&
                            !isSubmitting &&
                            (showSuccess || (
                                selectedLineItems.isNotEmpty() &&
                                    areaValidation.isValid &&
                                    (fulfillmentType != FulfillmentType.PICKUP || deliverySettings.pickupEnabled)
                                )),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonContainerColor,
                            contentColor = if (showSuccess) Color.White else NavyDeep
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_food_request_button")
                    ) {
                        val btnState = when {
                            isSubmitting -> "LOADING"
                            showSuccess -> "SUCCESS"
                            else -> "IDLE"
                        }
                        AnimatedContent(targetState = btnState, label = "food_submit_btn_content") { state ->
                            when (state) {
                                "LOADING" -> CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyDeep, strokeWidth = 2.dp)
                                "SUCCESS" -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedSuccessCheckmark(modifier = Modifier.size(22.dp), tint = Color.White)
                                    Text(
                                        text = if (strings.isHindi) "ऑर्डर दर्ज हो गया" else "Order Placed",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> Text(
                                    text = if (shopEvaluation.isOpen) strings.submitFoodRequest else strings.shopClosed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            }

            ScrollToTopIndicator(
                listState = listState,
                label = if (strings.isHindi) "ऊपर" else "Top",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                testTag = "food_scroll_to_top"
            )
        }
    }

    if (showRegistrationDialog) {
        CustomerRegistrationDialog(
            userProfile = userProfile,
            defaultName = customerName.ifBlank { defaultUserName },
            defaultEmail = defaultUserEmail,
            allowedLocalities = deliverySettings.allowedLocalities,
            isSubmitting = isSubmitting,
            strings = strings,
            onSaveRegistration = onRegisterCustomer,
            onDismiss = { showRegistrationDialog = false }
        )
    }
}

@Composable
fun FoodProductCard(
    product: Product,
    cart: Map<CartKey, Long>,
    shopIsOpen: Boolean,
    strings: AppStrings,
    onUpdateQuantity: (String, Long, Long) -> Unit
) {
    val inStock = product.available && (!product.trackStock || product.stockQuantity > 0L)
    val stockBadgeText = when {
        !product.available -> strings.unavailableLabel
        product.trackStock && product.stockQuantity <= 0L -> strings.outOfStockLabel
        product.trackStock && product.stockQuantity <= 5L -> "Low Stock (${product.stockQuantity})"
        else -> strings.availableLabel
    }
    val stockBadgeColor = when {
        !inStock -> StatusClosedRed
        product.trackStock && product.stockQuantity <= 5L -> StatusPendingAmber
        else -> StatusOpenGreen
    }

    GoldOutlineCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductOrServiceImage(
                imageUrl = product.imageUrl,
                localFallbackRes = localDrawableForProductKey(product.localImageKey),
                contentDescription = product.name,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val displayName = if (strings.isHindi && product.nameHindi.isNotBlank()) {
                    "${product.nameHindi} (${product.name})"
                } else {
                    if (product.nameHindi.isNotBlank()) "${product.name} • ${product.nameHindi}" else product.name
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = CreamIvory
                )
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate,
                        maxLines = 2
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    StatusPill(
                        text = stockBadgeText,
                        color = stockBadgeColor
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = AntiqueGold.copy(alpha = 0.25f))
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            product.priceOptions.forEach { option ->
                val qty = cart[CartKey(product.id, option.label, option.price)] ?: 0L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CreamIvory
                        )
                        Text(
                            text = "₹${option.price}",
                            style = MaterialTheme.typography.titleSmall,
                            color = LuminousGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    QuantityStepper(
                        quantity = qty,
                        enabled = shopIsOpen && inStock && option.price > 0L,
                        onDecrement = { onUpdateQuantity(option.label, option.price, -1L) },
                        onIncrement = { onUpdateQuantity(option.label, option.price, 1L) },
                        testTagPrefix = "food_${product.id}_${option.price}"
                    )
                }
            }
        }
    }
}
