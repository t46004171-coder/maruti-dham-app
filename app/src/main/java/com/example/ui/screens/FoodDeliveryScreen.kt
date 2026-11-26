package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.ScrollToTopIndicator
import com.example.ui.components.ShopClosedBanner
import com.example.ui.components.StatusPill
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
import com.example.util.DefaultCatalogData
import com.example.util.DomainCalculators

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDeliveryScreen(
    productsState: UiState<List<Product>>,
    deliverySettingsState: UiState<DeliverySettings>,
    shopEvaluation: ShopOpenEvaluation,
    shopSettings: ShopSettings,
    deliveryCart: Map<CartKey, Long>,
    userProfile: UserProfile?,
    defaultUserName: String,
    defaultUserEmail: String,
    isSubmitting: Boolean,
    recentSuccessOrder: CustomerRequest? = null,
    strings: AppStrings,
    onUpdateQuantity: (Product, String, Long, Long) -> Unit,
    onRegisterCustomer: (String, String, String, FulfillmentType, String, String, () -> Unit) -> Unit,
    onSubmitDeliveryRequest: (
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
    ) -> Unit
) {
    val allProducts = (productsState as? UiState.Success)?.data ?: emptyList()
    val deliveryProducts = remember(allProducts) {
        allProducts.filter { it.visible && it.deliveryEnabled }
    }
    val settings = (deliverySettingsState as? UiState.Success)?.data ?: DefaultCatalogData.defaultDeliverySettings

    var selectedLocality by remember(settings.allowedLocalities) {
        mutableStateOf(settings.allowedLocalities.firstOrNull() ?: "Khatu Shyam Ji")
    }
    var deliveryAddress by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf(defaultUserName) }
    var contactPhone by remember { mutableStateOf("") }
    var deliveryTime by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var upiRef by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showRegistrationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            if (customerName.isBlank() && profile.displayName.isNotBlank()) customerName = profile.displayName
            if (contactPhone.isBlank() && profile.phone.isNotBlank()) contactPhone = profile.phone
            if (deliveryAddress.isBlank() && profile.defaultAddress.isNotBlank()) deliveryAddress = profile.defaultAddress
            if (profile.defaultLocality.isNotBlank() && profile.defaultLocality in settings.allowedLocalities) {
                selectedLocality = profile.defaultLocality
            }
        }
    }

    val selectedLineItems by remember(deliveryCart, allProducts) {
        derivedStateOf {
            val productMap = allProducts.associateBy { it.id }
            deliveryCart.entries.mapNotNull { (key, qty) ->
                if (qty <= 0L) null else {
                    val p = productMap[key.productId]
                    Triple("${p?.name ?: key.productId} (${key.variantLabel})", qty, key.unitPrice * qty)
                }
            }
        }
    }
    val deliveryTotal by remember(deliveryCart) {
        derivedStateOf { deliveryCart.entries.sumOf { (key, qty) -> key.unitPrice * qty } }
    }
    val areaValidation = remember(deliveryAddress, selectedLocality, settings) {
        DomainCalculators.validateDeliveryArea(deliveryAddress, selectedLocality, settings)
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
            title = strings.navFoodDelivery,
            subtitle = "${shopEvaluation.badgeText} • ${strings.deliveryAreaBadge}"
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

            item {
                GoldOutlineCard(containerColor = NavyElevated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = RoyalGold)
                            Text(
                                text = "Area: ${settings.serviceAreaName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = LuminousGold
                            )
                        }
                        StatusPill(
                            text = if (settings.deliveryEnabled && shopEvaluation.isOpen) strings.availableLabel else strings.unavailableLabel,
                            color = if (settings.deliveryEnabled && shopEvaluation.isOpen) StatusOpenGreen else StatusClosedRed
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (settings.deliveryEnabled) settings.deliveryNote else strings.deliveryPausedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.deliveryEnabled) MutedSlate else StatusClosedRed
                    )
                    if (settings.minimumOrderAmount > 0L) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Minimum delivery order: ₹${settings.minimumOrderAmount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LuminousGold
                        )
                    }
                }
            }

            items(deliveryProducts, key = { it.id }) { product ->
                FoodProductCard(
                    product = product,
                    cart = deliveryCart,
                    shopIsOpen = shopEvaluation.isOpen && settings.deliveryEnabled,
                    strings = strings,
                    onUpdateQuantity = { label, price, delta ->
                        onUpdateQuantity(product, label, price, delta)
                    }
                )
            }

            item {
                val showSuccess = recentSuccessOrder?.category == RequestCategory.FOOD_DELIVERY_REQUEST.name
                val summaryBorderColor by animateColorAsState(
                    targetValue = if (showSuccess) StatusOpenGreen else RoyalGold.copy(alpha = 0.45f),
                    animationSpec = tween(durationMillis = 320),
                    label = "delivery_summary_border"
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
                        testTag = "delivery_order_success_banner"
                    )
                    Spacer(Modifier.height(8.dp))

                    if (selectedLineItems.isEmpty()) {
                        Text(strings.selectItemsPrompt, color = MutedSlate)
                    } else {
                        selectedLineItems.forEach { (title, qty, total) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$title x$qty", color = CreamIvory, style = MaterialTheme.typography.bodyMedium)
                                Text("₹$total", color = LuminousGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AntiqueGold.copy(alpha = 0.35f))
                        val deliveryGrandTotal = DomainCalculators.calculateGrandTotal(
                            foodTotal = deliveryTotal,
                            roomTotal = 0L,
                            freshenUpTotal = 0L,
                            deliveryCharge = settings.deliveryCharge.coerceAtLeast(0L),
                            packagingCharge = settings.packagingCharge.coerceAtLeast(0L)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.subtotalLabel, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                            Text("₹$deliveryTotal", style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                        }
                        if (settings.deliveryCharge > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.deliveryChargeLabel, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                                Text("₹${settings.deliveryCharge}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                            }
                        }
                        if (settings.packagingCharge > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(strings.packagingChargeLabel, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                                Text("₹${settings.packagingCharge}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.grandTotalLabel, style = MaterialTheme.typography.titleMedium, color = CreamIvory)
                            Text("₹$deliveryGrandTotal", style = MaterialTheme.typography.titleLarge, color = LuminousGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(strings.deliveryLocalityLabel, style = MaterialTheme.typography.labelLarge, color = CreamIvory)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settings.allowedLocalities.forEach { loc ->
                            FilterChip(
                                selected = selectedLocality == loc,
                                onClick = { selectedLocality = loc },
                                label = { Text(loc) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalGold,
                                    selectedLabelColor = NavyDeep
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text(strings.deliveryAddressLabel) },
                        placeholder = { Text("e.g., Shyam Dharamshala, Near Manda Chauraha, Khatu Shyam Ji") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalGold) },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("delivery_address_input")
                    )

                    if (deliveryAddress.isNotBlank() && !areaValidation.isValid) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusClosedRed, modifier = Modifier.size(18.dp))
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
                            modifier = Modifier.testTag("delivery_cash_only_badge")
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = paymentMethod == PaymentMethod.CASH,
                                onClick = { paymentMethod = PaymentMethod.CASH },
                                label = { Text(strings.cashPaymentLabel) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                            )
                            FilterChip(
                                selected = paymentMethod == PaymentMethod.UPI,
                                onClick = { paymentMethod = PaymentMethod.UPI },
                                label = { Text(strings.upiPaymentLabel) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                            )
                        }
                        if (paymentMethod == PaymentMethod.UPI) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = LuminousGold, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (shopSettings.upiId.isNotBlank()) "UPI ID: ${shopSettings.upiId} (verified by owner)"
                                    else "Pay UPI on delivery; owner confirms before marking Paid.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusPendingAmber
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = upiRef,
                                onValueChange = { upiRef = it },
                                label = { Text("UPI Transaction Ref (Optional)") },
                                singleLine = true,
                                colors = tfColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(strings.customerNameLabel) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("delivery_customer_name")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(strings.contactPhoneLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("delivery_contact_phone")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deliveryTime,
                        onValueChange = { deliveryTime = it },
                        label = { Text(strings.expectedDeliveryTimeLabel) },
                        placeholder = { Text("e.g., 12:15 PM / ASAP") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = RoyalGold) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("delivery_time_input")
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
                    val meetsMinOrder = deliveryTotal >= settings.minimumOrderAmount
                    if (!meetsMinOrder && selectedLineItems.isNotEmpty()) {
                        Text(
                            text = "${strings.deliveryMinimumError} (Min: ₹${settings.minimumOrderAmount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusClosedRed
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    val deliveryButtonColor by animateColorAsState(
                        targetValue = if (showSuccess) StatusOpenGreen else RoyalGold,
                        animationSpec = tween(durationMillis = 280),
                        label = "delivery_submit_btn_color"
                    )
                    Button(
                        onClick = {
                            if (userProfile != null && !userProfile.registrationCompleted) {
                                showRegistrationDialog = true
                            } else {
                                onSubmitDeliveryRequest(
                                    customerName,
                                    contactPhone,
                                    selectedLocality,
                                    deliveryAddress,
                                    deliveryTime,
                                    paymentMethod,
                                    upiRef.takeIf { it.isNotBlank() },
                                    notes,
                                    allProducts,
                                    settings
                                )
                            }
                        },
                        enabled = shopEvaluation.isOpen &&
                            !isSubmitting &&
                            settings.deliveryEnabled &&
                            (showSuccess || (selectedLineItems.isNotEmpty() && meetsMinOrder && areaValidation.isValid)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = deliveryButtonColor,
                            contentColor = if (showSuccess) Color.White else NavyDeep
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_delivery_request_button")
                    ) {
                        val btnState = when {
                            isSubmitting -> "LOADING"
                            showSuccess -> "SUCCESS"
                            else -> "IDLE"
                        }
                        AnimatedContent(targetState = btnState, label = "delivery_submit_btn_content") { state ->
                            when (state) {
                                "LOADING" -> CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyDeep, strokeWidth = 2.dp)
                                "SUCCESS" -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedSuccessCheckmark(modifier = Modifier.size(22.dp), tint = Color.White)
                                    Text(
                                        text = if (strings.isHindi) "डिलीवरी ऑर्डर दर्ज!" else "Delivery Order Placed",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> Text(
                                    text = if (shopEvaluation.isOpen) strings.submitDeliveryRequest else strings.shopClosed,
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
                testTag = "delivery_scroll_to_top"
            )
        }
    }

    if (showRegistrationDialog) {
        CustomerRegistrationDialog(
            userProfile = userProfile,
            defaultName = customerName.ifBlank { defaultUserName },
            defaultEmail = defaultUserEmail,
            allowedLocalities = settings.allowedLocalities,
            isSubmitting = isSubmitting,
            strings = strings,
            onSaveRegistration = onRegisterCustomer,
            onDismiss = { showRegistrationDialog = false }
        )
    }
}
