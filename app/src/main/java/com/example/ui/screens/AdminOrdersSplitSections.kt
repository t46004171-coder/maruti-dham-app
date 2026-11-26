package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.PaymentStatus
import com.example.data.model.RequestCategory
import com.example.data.model.RequestStatus
import com.example.data.model.ShopSettings
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.PaymentStatusPill
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.StatusPill
import com.example.ui.components.WhatsAppStatusPill
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusConfirmedBlue
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusPreparingPurple
import com.example.ui.theme.StatusReadyCyan
import com.example.util.DomainCalculators

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoryOrdersSection(
    sectionTitle: String,
    sectionSubtitle: String,
    emptyMessage: String,
    categoryFilter: RequestCategory,
    allRequests: List<CustomerRequest>,
    shop: ShopSettings,
    ownerPrivateSettings: OwnerPrivateSettings,
    strings: AppStrings,
    onUpdateStatus: (String, RequestStatus, String?) -> Unit,
    onVerifyPayment: (String, PaymentStatus, String?) -> Unit,
    onMarkWhatsAppShared: (String) -> Unit,
    onLoadMoreOrders: () -> Unit,
    searchTestTag: String
) {
    val palette = LocalMarutiPalette.current
    var statusFilter by remember(categoryFilter) { mutableStateOf<String?>(null) }
    var statusDropdownExpanded by remember(categoryFilter) { mutableStateOf(false) }
    var searchQuery by remember(categoryFilter) { mutableStateOf("") }
    var receiptTarget by remember { mutableStateOf<CustomerRequest?>(null) }

    val categoryRequests by remember(allRequests, categoryFilter) {
        derivedStateOf {
            allRequests.filter { it.category == categoryFilter.name }
        }
    }

    val filtered by remember(categoryRequests, statusFilter, searchQuery) {
        derivedStateOf {
            DomainCalculators.filterAdminOrders(
                orders = categoryRequests,
                statusFilter = statusFilter,
                searchQuery = searchQuery
            )
        }
    }

    val categoryRevenue = remember(categoryRequests) {
        categoryRequests.filter { it.status != RequestStatus.REJECTED.name }.sumOf { it.grandTotal }
    }
    val categoryPending = remember(categoryRequests) {
        categoryRequests.count {
            it.status in setOf(
                RequestStatus.PENDING.name,
                RequestStatus.CONFIRMED.name,
                RequestStatus.PREPARING.name,
                RequestStatus.READY.name
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoldOutlineCard(containerColor = palette.elevated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sectionTitle, style = MaterialTheme.typography.titleLarge, color = palette.accent)
                        Text(sectionSubtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    StatusPill(text = "${categoryRequests.size} Total", color = palette.primary)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusPill(text = "Active Revenue: ₹$categoryRevenue", color = StatusOpenGreen)
                    StatusPill(text = "Active/Pending: $categoryPending", color = StatusPendingAmber)
                }
            }
        }

        item {
            val placeholderHint = if (categoryFilter == RequestCategory.FOOD_DELIVERY_REQUEST) {
                "Customer name, mobile number, order ID, or address"
            } else {
                "Customer name, mobile number, or order ID"
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by customer name or mobile") },
                placeholder = { Text(placeholderHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search orders", tint = palette.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${searchTestTag}_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = palette.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = adminTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(searchTestTag)
            )
            if (searchQuery.isNotBlank() || statusFilter != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Showing ${filtered.size} of ${categoryRequests.size} orders",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
            val statusCounts = remember(categoryRequests) {
                categoryRequests.groupingBy { it.status }.eachCount()
            }
            val statusOptions = listOf(
                Triple<String?, String, Color>(null, "All Statuses (${categoryRequests.size})", palette.primary),
                Triple(RequestStatus.PENDING.name, "Pending (${statusCounts[RequestStatus.PENDING.name] ?: 0})", StatusPendingAmber),
                Triple(RequestStatus.CONFIRMED.name, "Confirmed (${statusCounts[RequestStatus.CONFIRMED.name] ?: 0})", StatusConfirmedBlue),
                Triple(RequestStatus.PREPARING.name, "Preparing (${statusCounts[RequestStatus.PREPARING.name] ?: 0})", StatusPreparingPurple),
                Triple(RequestStatus.READY.name, "Ready (${statusCounts[RequestStatus.READY.name] ?: 0})", StatusReadyCyan),
                Triple(RequestStatus.COMPLETED.name, "Completed (${statusCounts[RequestStatus.COMPLETED.name] ?: 0})", StatusOpenGreen),
                Triple(RequestStatus.REJECTED.name, "Cancelled (${statusCounts[RequestStatus.REJECTED.name] ?: 0})", StatusClosedRed)
            )
            val selectedOption = statusOptions.firstOrNull { it.first == statusFilter } ?: statusOptions.first()

            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = statusDropdownExpanded,
                onExpandedChange = { statusDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedOption.second,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Filter by Order Status") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter by status",
                            tint = selectedOption.third
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                    colors = adminTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("${searchTestTag}_status_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = statusDropdownExpanded,
                    onDismissRequest = { statusDropdownExpanded = false },
                    containerColor = palette.card
                ) {
                    statusOptions.forEach { (key, label, dotColor) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (statusFilter == key) dotColor else palette.textPrimary,
                                    fontWeight = if (statusFilter == key) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            },
                            onClick = {
                                statusFilter = key
                                statusDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("${searchTestTag}_status_option_${key ?: "ALL"}")
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                statusOptions.forEach { (key, label, chipColor) ->
                    val shortLabel = if (key == null) "All (${categoryRequests.size})" else label
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(shortLabel) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = statusFilter == key,
                            borderColor = chipColor.copy(alpha = 0.55f),
                            selectedBorderColor = chipColor
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.24f),
                            selectedLabelColor = chipColor,
                            labelColor = palette.textPrimary
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GoldOutlineCard {
                    if (categoryRequests.isEmpty()) {
                        Text(emptyMessage, color = palette.textSecondary)
                    } else {
                        Text(
                            text = "No matching orders found${if (searchQuery.isNotBlank()) " for \"${searchQuery.trim()}\"" else ""}.",
                            color = palette.textSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                statusFilter = null
                            },
                            border = BorderStroke(1.dp, palette.primary),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Clear Search & Filters", color = palette.primary)
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { req ->
                AdminSeparatedOrderCard(
                    req = req,
                    strings = strings,
                    onUpdateStatus = onUpdateStatus,
                    onVerifyPayment = onVerifyPayment,
                    onOpenReceipt = { receiptTarget = req }
                )
            }

            if (allRequests.size >= 60) {
                item {
                    OutlinedButton(
                        onClick = onLoadMoreOrders,
                        border = BorderStroke(1.dp, palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load More Orders", color = palette.primary)
                    }
                }
            }
        }
    }

    receiptTarget?.let { req ->
        DigitalReceiptDialog(
            request = req,
            shopSettings = shop,
            strings = strings,
            isNewSubmission = false,
            targetWhatsAppOverride = ownerPrivateSettings.ownerWhatsAppNumber,
            onMarkWhatsAppShared = onMarkWhatsAppShared,
            onDismiss = { receiptTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSeparatedOrderCard(
    req: CustomerRequest,
    strings: AppStrings,
    onUpdateStatus: (String, RequestStatus, String?) -> Unit,
    onVerifyPayment: (String, PaymentStatus, String?) -> Unit,
    onOpenReceipt: () -> Unit
) {
    val palette = LocalMarutiPalette.current
    var adminNote by remember(req.id, req.adminNote) { mutableStateOf(req.adminNote ?: "") }
    val orderNum = req.orderNumber.ifBlank { req.id.take(10) }

    GoldOutlineCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$orderNum • ${req.customerName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mobile: ${req.contactPhone} • ${DomainCalculators.formatIstDateTime(req.createdAt?.toDate()?.time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            RequestStatusPill(
                statusRaw = req.status,
                strings = strings,
                modifier = Modifier.testTag("order_status_badge_${req.id}")
            )
        }

        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PaymentStatusPill(req.paymentMethod, req.paymentStatus)
            WhatsAppStatusPill(req.whatsappStatus)
            if (req.category == RequestCategory.ROOM_REQUEST.name && req.roomRateConfigured == false) {
                StatusPill(text = "Rate TBD", color = StatusPendingAmber)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Expected Time: ${req.expectedTime} • Mode: ${req.fulfillmentType}",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )

        if (req.category == RequestCategory.ROOM_REQUEST.name) {
            Text(
                text = "Room Type: ${req.roomType ?: "-"} • Rooms: ${req.numberOfRooms ?: 1} • People: ${req.numberOfPeople ?: 1} • Duration: ${req.roomDurationHours ?: 6}h",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
        }

        if (req.category == RequestCategory.FRESHEN_UP_REQUEST.name) {
            Text(
                text = "Freshen-Up Tier: ${req.freshenUpOption ?: "-"} • People: ${req.numberOfPeople ?: 1}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
        }

        req.items.forEach { item ->
            Text(
                text = "• ${item.name} (${item.variantLabel}) x${item.quantity} = ₹${item.lineTotal}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
        }

        if (!req.deliveryAddress.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Khatu Delivery Address: ${req.deliveryLocality ?: "Khatu Shyam Ji"} — ${req.deliveryAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.accent,
                fontWeight = FontWeight.Medium
            )
        }

        if (!req.notes.isNullOrBlank()) {
            Text("Customer Note: ${req.notes}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }

        if (!req.upiTransactionRef.isNullOrBlank()) {
            Text("Customer UPI Ref: ${req.upiTransactionRef}", style = MaterialTheme.typography.bodySmall, color = palette.primary)
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (req.category == RequestCategory.ROOM_REQUEST.name && req.roomRateConfigured == false) {
                "Total: Rate TBD at Counter"
            } else {
                "Grand Total: ₹${req.grandTotal}"
            },
            style = MaterialTheme.typography.titleSmall,
            color = palette.accent,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = adminNote,
            onValueChange = { adminNote = it },
            label = { Text("Owner Note / Room Rate Note (Optional)") },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { onUpdateStatus(req.id, RequestStatus.CONFIRMED, adminNote) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusConfirmedBlue, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Confirm")
            }
            OutlinedButton(
                onClick = { onUpdateStatus(req.id, RequestStatus.PREPARING, adminNote) },
                border = BorderStroke(1.dp, StatusPreparingPurple)
            ) { Text("Preparing", color = StatusPreparingPurple) }
            OutlinedButton(
                onClick = { onUpdateStatus(req.id, RequestStatus.READY, adminNote) },
                border = BorderStroke(1.dp, StatusReadyCyan)
            ) { Text("Ready", color = StatusReadyCyan) }
            Button(
                onClick = { onUpdateStatus(req.id, RequestStatus.COMPLETED, adminNote) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusOpenGreen, contentColor = Color.White)
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Completed")
            }
            Button(
                onClick = { onUpdateStatus(req.id, RequestStatus.REJECTED, adminNote) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusClosedRed, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (PaymentStatus.fromRaw(req.paymentStatus) != PaymentStatus.PAID) {
                OutlinedButton(
                    onClick = { onVerifyPayment(req.id, PaymentStatus.PAID, adminNote) },
                    border = BorderStroke(1.dp, StatusOpenGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = StatusOpenGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Paid", color = StatusOpenGreen)
                }
            }
            OutlinedButton(
                onClick = onOpenReceipt,
                border = BorderStroke(1.dp, palette.primary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Digital Receipt", color = palette.primary)
            }
        }
    }
}
