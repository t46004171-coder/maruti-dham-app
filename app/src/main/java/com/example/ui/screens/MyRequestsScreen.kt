package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.RequestCategory
import com.example.data.model.ShopSettings
import com.example.data.model.UiState
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.PaymentStatusPill
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.ScrollToTopIndicator
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.util.DefaultCatalogData
import com.example.util.DomainCalculators
import com.example.util.ExternalIntentHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    requestsState: UiState<List<CustomerRequest>>,
    shopSettingsState: UiState<ShopSettings>,
    strings: AppStrings,
    onLoadMoreOrders: () -> Unit,
    onMarkWhatsAppShared: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val shop = (shopSettingsState as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var receiptRequest by remember { mutableStateOf<CustomerRequest?>(null) }

    val filterOptions = listOf(
        null to strings.allFilter,
        RequestCategory.FOOD_REQUEST.name to strings.foodFilter,
        RequestCategory.ROOM_REQUEST.name to strings.roomFilter,
        RequestCategory.FRESHEN_UP_REQUEST.name to strings.freshenUpFilter,
        RequestCategory.FOOD_DELIVERY_REQUEST.name to strings.deliveryFilter
    )

    Scaffold(
        containerColor = NavyDeep,
        topBar = {
            TopAppBar(
                title = { Text(strings.myRequestsTitle, color = LuminousGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RoyalGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyRow(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (cat, label) ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalGold,
                            selectedLabelColor = NavyDeep
                        )
                    )
                }
            }

            when (requestsState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = RoyalGold)
                    }
                }
                is UiState.Error -> {
                    GoldOutlineCard(modifier = Modifier.padding(16.dp)) {
                        Text(requestsState.message, color = StatusClosedRed)
                    }
                }
                is UiState.Success -> {
                    val filtered = if (selectedFilter == null) requestsState.data
                    else requestsState.data.filter { it.category == selectedFilter }

                    if (filtered.isEmpty()) {
                        GoldOutlineCard(modifier = Modifier.padding(16.dp)) {
                            Text(strings.noRequestsYet, color = MutedSlate)
                        }
                    } else {
                        val listState = rememberLazyListState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filtered, key = { it.id }) { req ->
                                    CustomerRequestCard(
                                        request = req,
                                        strings = strings,
                                        onOpenReceipt = { receiptRequest = req },
                                        onShareWhatsApp = {
                                            onMarkWhatsAppShared(req.id)
                                            val msg = DomainCalculators.formatWhatsAppRequestMessage(req, shop)
                                            ExternalIntentHelper.launchWhatsAppShare(context, msg, shop.whatsappNumber)
                                        }
                                    )
                                }
                                if (requestsState.data.size >= 30) {
                                    item {
                                        OutlinedButton(
                                            onClick = onLoadMoreOrders,
                                            border = BorderStroke(1.dp, RoyalGold),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Load Older Orders", color = RoyalGold)
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
                                testTag = "my_requests_scroll_to_top"
                            )
                        }
                    }
                }
            }
        }
    }

    receiptRequest?.let { req ->
        DigitalReceiptDialog(
            request = req,
            shopSettings = shop,
            strings = strings,
            isNewSubmission = false,
            onMarkWhatsAppShared = onMarkWhatsAppShared,
            onDismiss = { receiptRequest = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerRequestCard(
    request: CustomerRequest,
    strings: AppStrings,
    onOpenReceipt: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val categoryHeading = when (RequestCategory.fromRaw(request.category)) {
        RequestCategory.FOOD_REQUEST -> "FOOD ORDER"
        RequestCategory.ROOM_REQUEST -> "ROOM REQUEST"
        RequestCategory.FRESHEN_UP_REQUEST -> "FRESHEN UP"
        RequestCategory.FOOD_DELIVERY_REQUEST -> "FOOD DELIVERY"
    }
    val createdMillis = request.createdAt?.toDate()?.time
    val istDateTime = remember(createdMillis) { DomainCalculators.formatIstDateTime(createdMillis) }
    val orderNum = request.orderNumber.ifBlank { request.id.take(10) }

    GoldOutlineCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$orderNum • $categoryHeading", style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                Text(istDateTime, style = MaterialTheme.typography.labelSmall, color = MutedSlate)
            }
            RequestStatusPill(statusRaw = request.status, strings = strings)
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PaymentStatusPill(paymentMethod = request.paymentMethod, paymentStatusRaw = request.paymentStatus)
        }
        Spacer(Modifier.height(6.dp))
        Text("Mode: ${request.fulfillmentType} • Time: ${request.expectedTime} • Phone: ${request.contactPhone}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)

        if (!request.deliveryAddress.isNullOrBlank()) {
            Text(
                text = "Delivery: ${request.deliveryLocality ?: ""} — ${request.deliveryAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = CreamIvory
            )
        }

        if (request.items.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            request.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.name} (${item.variantLabel}) x${item.quantity}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                    Text(if (item.lineTotal > 0L) "₹${item.lineTotal}" else "TBD", style = MaterialTheme.typography.bodySmall, color = LuminousGold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = AntiqueGold.copy(alpha = 0.25f))
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings.grandTotalLabel, style = MaterialTheme.typography.titleSmall, color = CreamIvory)
            Text(
                text = if (request.grandTotal > 0L) "₹${request.grandTotal}" else "Rate confirmed by owner",
                style = MaterialTheme.typography.titleSmall,
                color = LuminousGold,
                fontWeight = FontWeight.Bold
            )
        }

        if (!request.adminNote.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Owner Note: ${request.adminNote}", style = MaterialTheme.typography.bodySmall, color = RoyalGold)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onOpenReceipt,
                border = BorderStroke(1.dp, RoyalGold),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalGold)
                Spacer(Modifier.width(4.dp))
                Text(strings.viewReceiptLabel, color = RoyalGold)
            }
            Button(
                onClick = onShareWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = StatusOpenGreen, contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("WhatsApp")
            }
        }
    }
}
