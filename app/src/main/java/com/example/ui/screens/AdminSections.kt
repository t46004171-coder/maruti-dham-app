package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.DeliverySettings
import com.example.data.model.FreshenUpConfig
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.PaymentStatus
import com.example.data.model.RequestStatus
import com.example.data.model.RoomConfig
import com.example.data.model.RoomStatusLabel
import com.example.data.model.ShopControlMode
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.PaymentStatusPill
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.StatusPill
import com.example.ui.components.WhatsAppStatusPill
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusCompletedTeal
import com.example.ui.theme.StatusOpenGreen
import com.example.util.DomainCalculators
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

@Composable
fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoyalGold,
    unfocusedBorderColor = AntiqueGold.copy(alpha = 0.45f),
    focusedLabelColor = LuminousGold,
    unfocusedLabelColor = MutedSlate,
    focusedTextColor = CreamIvory,
    unfocusedTextColor = CreamIvory
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminRequestsSection(
    requests: List<CustomerRequest>,
    shop: ShopSettings,
    ownerPrivateSettings: OwnerPrivateSettings,
    strings: AppStrings,
    onUpdateStatus: (String, RequestStatus, String?) -> Unit,
    onVerifyPayment: (String, PaymentStatus, String?) -> Unit,
    onMarkWhatsAppShared: (String) -> Unit,
    onLoadMoreOrders: () -> Unit
) {
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var receiptTarget by remember { mutableStateOf<CustomerRequest?>(null) }

    val filtered by remember(requests, statusFilter, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            requests.filter { req ->
                val statusOk = statusFilter == null || req.status == statusFilter
                val searchOk = q.isEmpty() ||
                    req.orderNumber.lowercase().contains(q) ||
                    req.customerName.lowercase().contains(q) ||
                    req.contactPhone.lowercase().contains(q)
                statusOk && searchOk
            }
        }
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Order ID (MD-...), customer, or mobile") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalGold) },
                singleLine = true,
                colors = adminTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("admin_order_search")
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    null to "All",
                    RequestStatus.PENDING.name to "Pending",
                    RequestStatus.CONFIRMED.name to "Confirmed",
                    RequestStatus.PREPARING.name to "Preparing",
                    RequestStatus.READY.name to "Ready",
                    RequestStatus.COMPLETED.name to "Completed",
                    RequestStatus.REJECTED.name to "Cancelled"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                GoldOutlineCard {
                    Text(strings.noRequestsYet, color = MutedSlate)
                }
            }
        } else {
            items(filtered, key = { it.id }) { req ->
                var adminNote by remember(req.id, req.adminNote) { mutableStateOf(req.adminNote ?: "") }
                val orderNum = req.orderNumber.ifBlank { req.id.take(10) }

                GoldOutlineCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$orderNum • ${req.category.replace('_', ' ')}", style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                            Text("${req.customerName} • ${req.contactPhone}", style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                            Text(DomainCalculators.formatIstDateTime(req.createdAt?.toDate()?.time), style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                        }
                        RequestStatusPill(statusRaw = req.status, strings = strings)
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentStatusPill(req.paymentMethod, req.paymentStatus)
                        WhatsAppStatusPill(req.whatsappStatus)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Mode: ${req.fulfillmentType} • Expected: ${req.expectedTime}", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                    req.items.forEach { item ->
                        Text("• ${item.name} (${item.variantLabel}) x${item.quantity} = ₹${item.lineTotal}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                    }
                    if (!req.deliveryAddress.isNullOrBlank()) {
                        Text("Delivery Address: ${req.deliveryLocality ?: ""} — ${req.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = LuminousGold)
                    }
                    if (!req.upiTransactionRef.isNullOrBlank()) {
                        Text("Customer UPI Ref: ${req.upiTransactionRef}", style = MaterialTheme.typography.bodySmall, color = RoyalGold)
                    }
                    Text(
                        text = "Total: ₹${req.grandTotal}",
                        style = MaterialTheme.typography.titleSmall,
                        color = LuminousGold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("Owner Note (Optional)") },
                        singleLine = true,
                        colors = adminTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onUpdateStatus(req.id, RequestStatus.CONFIRMED, adminNote) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOpenGreen, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Accept")
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus(req.id, RequestStatus.PREPARING, adminNote) },
                            border = BorderStroke(1.dp, RoyalGold)
                        ) { Text("Preparing", color = RoyalGold) }
                        OutlinedButton(
                            onClick = { onUpdateStatus(req.id, RequestStatus.READY, adminNote) },
                            border = BorderStroke(1.dp, LuminousGold)
                        ) { Text("Ready", color = LuminousGold) }
                        Button(
                            onClick = { onUpdateStatus(req.id, RequestStatus.COMPLETED, adminNote) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCompletedTeal, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Done")
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
                            onClick = { receiptTarget = req },
                            border = BorderStroke(1.dp, RoyalGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Digital Receipt", color = RoyalGold)
                        }
                    }
                }
            }

            if (requests.size >= 60) {
                item {
                    OutlinedButton(
                        onClick = onLoadMoreOrders,
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load More Orders", color = RoyalGold)
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
fun AdminShopAndNoticesSection(
    shop: ShopSettings,
    ownerPrivateSettings: OwnerPrivateSettings,
    shopEvaluation: ShopOpenEvaluation,
    announcements: List<Announcement>,
    onSaveShop: (ShopSettings) -> Unit,
    onSaveOwnerPrivateSettings: (OwnerPrivateSettings) -> Unit,
    onSaveAnnouncement: (Announcement) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    var controlMode by remember(shop) { mutableStateOf(ShopControlMode.fromRaw(shop.shopControlMode)) }
    var openingTime by remember(shop) { mutableStateOf(shop.openingTime) }
    var closingTime by remember(shop) { mutableStateOf(shop.closingTime) }
    var ownerName by remember(shop) { mutableStateOf(shop.ownerName) }
    var publicWhatsapp by remember(shop) { mutableStateOf(shop.whatsappNumber) }
    var contactPhone by remember(shop) { mutableStateOf(shop.contactPhone) }
    var contactEmail by remember(shop) { mutableStateOf(shop.contactEmail) }
    var upiId by remember(shop) { mutableStateOf(shop.upiId) }
    var address by remember(shop) { mutableStateOf(shop.address) }
    var aboutText by remember(shop) { mutableStateOf(shop.aboutText) }

    var privateWhatsapp by remember(ownerPrivateSettings) { mutableStateOf(ownerPrivateSettings.ownerWhatsAppNumber) }
    var privateWebhookUrl by remember(ownerPrivateSettings) { mutableStateOf(ownerPrivateSettings.whatsappWebhookUrl) }

    var newAnnTitle by remember { mutableStateOf("") }
    var newAnnMsg by remember { mutableStateOf("") }
    var newAnnSpecial by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoldOutlineCard(containerColor = NavyElevated) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("14. Opening & Closing Settings (IST)", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                    StatusPill(
                        text = shopEvaluation.badgeText,
                        color = if (shopEvaluation.isOpen) StatusOpenGreen else StatusClosedRed
                    )
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ShopControlMode.OPEN_NOW to "OPEN NOW",
                        ShopControlMode.CLOSED_NOW to "CLOSED NOW",
                        ShopControlMode.OPEN_AT_SCHEDULED to "OPEN AT SCHEDULED TIME",
                        ShopControlMode.CLOSE_AT_SCHEDULED to "CLOSE AT SCHEDULED TIME"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = controlMode == mode,
                            onClick = { controlMode = mode },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = openingTime,
                        onValueChange = { openingTime = it },
                        label = { Text("Opening Time (IST)") },
                        placeholder = { Text("7:00 AM") },
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = closingTime,
                        onValueChange = { closingTime = it },
                        label = { Text("Closing Time (IST)") },
                        placeholder = { Text("10:00 PM") },
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            AdminPaymentSettingsCard(
                shop = shop,
                onSaveShop = onSaveShop
            )
        }

        item {
            GoldOutlineCard {
                Text("15a. Public Storefront Settings (Visible to Customers)", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                Text("Leave Public Phone/Email/WhatsApp blank to keep personal contact numbers completely private.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = publicWhatsapp,
                    onValueChange = { publicWhatsapp = it },
                    label = { Text("Public Storefront WhatsApp (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = adminTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Public Contact Phone (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = adminTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Public Contact Email (Optional)") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = aboutText, onValueChange = { aboutText = it }, label = { Text("About MARUTI DHAM") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val draft = shop.copy(
                            ownerName = ownerName.trim().ifBlank { "Tarun Kumar" },
                            shopOpen = controlMode != ShopControlMode.CLOSED_NOW,
                            shopControlMode = controlMode.name,
                            openingTime = openingTime.trim(),
                            closingTime = closingTime.trim(),
                            whatsappNumber = publicWhatsapp.trim(),
                            contactPhone = contactPhone.trim(),
                            contactEmail = contactEmail.trim(),
                            upiId = upiId.trim(),
                            address = address.trim(),
                            aboutText = aboutText.trim()
                        )
                        val evaluated = DomainCalculators.evaluateShopOpenStatus(draft)
                        onSaveShop(draft.copy(shopOpen = evaluated.isOpen))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_shop_button")
                ) {
                    Text("Save Shop & Timing Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GoldOutlineCard(containerColor = NavyElevated) {
                Text("15b. Private Owner Notification Settings (Owner-Only)", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                Text("Customers cannot read this document. Used strictly by Owner Admin & Cloud Functions.", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = privateWhatsapp,
                    onValueChange = { privateWhatsapp = it },
                    label = { Text("Private Owner WhatsApp Alert Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = adminTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = privateWebhookUrl,
                    onValueChange = { privateWebhookUrl = it },
                    label = { Text("WhatsApp Retry Cloud Function URL (Optional)") },
                    colors = adminTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onSaveOwnerPrivateSettings(
                            ownerPrivateSettings.copy(
                                ownerWhatsAppNumber = privateWhatsapp.trim(),
                                whatsappWebhookUrl = privateWebhookUrl.trim()
                            )
                        )
                    },
                    border = BorderStroke(1.dp, RoyalGold),
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_private_settings_button")
                ) {
                    Text("Save Private Notification Settings", color = RoyalGold, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Announcements & Daily Specials
        item {
            GoldOutlineCard {
                Text("Add Daily Special / Announcement", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newAnnTitle, onValueChange = { newAnnTitle = it }, label = { Text("Title") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newAnnMsg, onValueChange = { newAnnMsg = it }, label = { Text("Message") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Daily Special", color = CreamIvory)
                    Switch(checked = newAnnSpecial, onCheckedChange = { newAnnSpecial = it })
                }
                Button(
                    onClick = {
                        if (newAnnTitle.isNotBlank() && newAnnMsg.isNotBlank()) {
                            onSaveAnnouncement(
                                Announcement(
                                    title = newAnnTitle.trim(),
                                    message = newAnnMsg.trim(),
                                    active = true,
                                    dailySpecial = newAnnSpecial
                                )
                            )
                            newAnnTitle = ""
                            newAnnMsg = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Publish Announcement", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(announcements, key = { it.id }) { ann ->
            GoldOutlineCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ann.title, style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                        Text(ann.message, style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                    }
                    Row {
                        Switch(checked = ann.active, onCheckedChange = { active -> onSaveAnnouncement(ann.copy(active = active)) })
                        IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusClosedRed)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminRoomsAndDeliverySection(
    rooms: List<RoomConfig>,
    freshenConfigs: List<FreshenUpConfig>,
    deliverySettings: DeliverySettings,
    onSaveRoom: (RoomConfig) -> Unit,
    onSaveFreshen: (FreshenUpConfig) -> Unit,
    onSaveDelivery: (DeliverySettings) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var enabled by remember(deliverySettings) { mutableStateOf(deliverySettings.deliveryEnabled) }
    var pickupEnabled by remember(deliverySettings) { mutableStateOf(deliverySettings.pickupEnabled) }
    var deliveryCharge by remember(deliverySettings) { mutableStateOf(deliverySettings.deliveryCharge.toString()) }
    var packagingCharge by remember(deliverySettings) { mutableStateOf(deliverySettings.packagingCharge.toString()) }
    var areaName by remember(deliverySettings) { mutableStateOf(deliverySettings.serviceAreaName) }
    var localitiesCsv by remember(deliverySettings) { mutableStateOf(deliverySettings.allowedLocalities.joinToString(", ")) }
    var minOrder by remember(deliverySettings) { mutableStateOf(deliverySettings.minimumOrderAmount.toString()) }
    var note by remember(deliverySettings) { mutableStateOf(deliverySettings.deliveryNote) }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(rooms, key = { it.roomType }) { room ->
            var statusLabel by remember(room) { mutableStateOf(room.statusLabel) }
            var availableCount by remember(room) { mutableStateOf(room.availableCount.toString()) }
            var imageUrl by remember(room) { mutableStateOf(room.imageUrl) }
            var rate6 by remember(room) { mutableStateOf(room.rate6Hours.toString()) }
            var rate12 by remember(room) { mutableStateOf(room.rate12Hours.toString()) }
            var r1 by remember(room) { mutableStateOf(room.rate1Hour?.toString() ?: "") }
            var r2 by remember(room) { mutableStateOf(room.rate2Hours?.toString() ?: "") }
            var r3 by remember(room) { mutableStateOf(room.rate3Hours?.toString() ?: "") }
            var r4 by remember(room) { mutableStateOf(room.rate4Hours?.toString() ?: "") }
            var r5 by remember(room) { mutableStateOf(room.rate5Hours?.toString() ?: "") }

            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        ImageCompressionUtil.compressPickedImage(context, uri, "room_${room.roomType}")
                            .onSuccess { compressed -> imageUrl = compressed }
                            .onFailure { Toast.makeText(context, "Unable to process photo", Toast.LENGTH_SHORT).show() }
                    }
                }
            }

            GoldOutlineCard {
                Text("${room.title} Settings", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomStatusLabel.entries.forEach { st ->
                        FilterChip(
                            selected = statusLabel == st.name,
                            onClick = { statusLabel = st.name },
                            label = { Text(st.name.replace('_', ' ')) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = availableCount, onValueChange = { availableCount = it }, label = { Text("Avail") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = rate6, onValueChange = { rate6 = it }, label = { Text("6h (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = rate12, onValueChange = { rate12 = it }, label = { Text("12h (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text("Optional 1–5h Rates:", style = MaterialTheme.typography.labelMedium, color = MutedSlate)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = r1, onValueChange = { r1 = it }, label = { Text("1h") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = r2, onValueChange = { r2 = it }, label = { Text("2h") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = r3, onValueChange = { r3 = it }, label = { Text("3h") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = r4, onValueChange = { r4 = it }, label = { Text("4h") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = r5, onValueChange = { r5 = it }, label = { Text("5h") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Room Photo") }, colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    IconButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.Image, contentDescription = "Pick room photo", tint = RoyalGold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSaveRoom(
                            room.copy(
                                available = statusLabel == RoomStatusLabel.AVAILABLE.name,
                                statusLabel = statusLabel,
                                availableCount = availableCount.toLongOrNull() ?: 0L,
                                imageUrl = imageUrl,
                                rate6Hours = rate6.toLongOrNull() ?: room.rate6Hours,
                                rate12Hours = rate12.toLongOrNull() ?: room.rate12Hours,
                                rate1Hour = r1.trim().toLongOrNull(),
                                rate2Hours = r2.trim().toLongOrNull(),
                                rate3Hours = r3.trim().toLongOrNull(),
                                rate4Hours = r4.trim().toLongOrNull(),
                                rate5Hours = r5.trim().toLongOrNull()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save ${room.title}", fontWeight = FontWeight.Bold) }
            }
        }

        items(freshenConfigs, key = { it.optionKey }) { cfg ->
            var title by remember(cfg) { mutableStateOf(cfg.title) }
            var price by remember(cfg) { mutableStateOf(cfg.pricePerPerson.toString()) }
            var available by remember(cfg) { mutableStateOf(cfg.available) }

            GoldOutlineCard {
                Text("Freshen Up: ${cfg.optionKey}", style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Option Title") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("₹ Per Person") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    Text("Available", color = CreamIvory)
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSaveFreshen(cfg.copy(title = title, pricePerPerson = price.toLongOrNull() ?: cfg.pricePerPerson, available = available))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Freshen Up Option", fontWeight = FontWeight.Bold) }
            }
        }

        item {
            HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
            Spacer(Modifier.height(4.dp))
            GoldOutlineCard {
                Text("Pickup, Delivery & Charge Configuration", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Shop Pickup Enabled", color = CreamIvory)
                    Switch(checked = pickupEnabled, onCheckedChange = { pickupEnabled = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Local Food Delivery Enabled", color = CreamIvory)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deliveryCharge,
                        onValueChange = { deliveryCharge = it },
                        label = { Text("Delivery Charge (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packagingCharge,
                        onValueChange = { packagingCharge = it },
                        label = { Text("Packaging Charge (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = areaName, onValueChange = { areaName = it }, label = { Text("Service Area Name") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = localitiesCsv, onValueChange = { localitiesCsv = it }, label = { Text("Allowed Localities (comma-separated)") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = minOrder, onValueChange = { minOrder = it }, label = { Text("Minimum Order Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Delivery Note") }, colors = adminTextFieldColors(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val localities = localitiesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onSaveDelivery(
                            deliverySettings.copy(
                                deliveryEnabled = enabled,
                                pickupEnabled = pickupEnabled,
                                deliveryCharge = deliveryCharge.trim().toLongOrNull() ?: 0L,
                                packagingCharge = packagingCharge.trim().toLongOrNull() ?: 0L,
                                serviceAreaName = areaName,
                                allowedLocalities = localities,
                                minimumOrderAmount = minOrder.toLongOrNull() ?: 0L,
                                deliveryNote = note
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                    modifier = Modifier.fillMaxWidth().testTag("admin_save_delivery_button")
                ) { Text("Save Pickup & Delivery Settings", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
