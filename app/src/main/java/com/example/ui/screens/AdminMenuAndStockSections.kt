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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.minimumInteractiveComponentSize
import com.example.data.model.LowStockAlertSummary
import com.example.data.model.LowStockSeverity
import com.example.data.model.PriceOption
import com.example.data.model.Product
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.StatusPill
import com.example.util.DomainCalculators
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.util.DefaultCatalogData
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMenuAndPriceSection(
    products: List<Product>,
    categories: List<String> = DefaultCatalogData.menuCategories,
    onUpdateCategories: (List<String>) -> Unit = {},
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    val activeCategories = remember(categories) {
        categories.ifEmpty { DefaultCatalogData.menuCategories }
    }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showSuggestedCatalog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    val existingIds = remember(products) { products.map { it.id }.toSet() }
    val unaddedSuggestions = remember(existingIds) {
        DefaultCatalogData.suggestedMenuTemplates.filter { it.id !in existingIds }
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoldOutlineCard(containerColor = NavyElevated) {
                Text("Editable Menu Categories (${activeCategories.size})", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeCategories.forEach { cat ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                if (activeCategories.size > 1) {
                                    onUpdateCategories(activeCategories.filterNot { it == cat })
                                }
                            },
                            label = { Text("$cat  ✕") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        label = { Text("Add Category (e.g., Cold Drinks)") },
                        singleLine = true,
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f).testTag("admin_new_category_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = newCategoryText.trim()
                            if (trimmed.isNotBlank() && trimmed !in activeCategories) {
                                onUpdateCategories(activeCategories + trimmed)
                                newCategoryText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                        modifier = Modifier.testTag("admin_add_category_button")
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        item {
            Text("9 & 10. Menu & Item Price Management", style = MaterialTheme.typography.titleLarge, color = LuminousGold)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    editingProduct = Product(
                        name = "",
                        nameHindi = "",
                        category = activeCategories.first(),
                        priceOptions = listOf(PriceOption("Regular", 20L)),
                        available = true,
                        visible = true,
                        foodOrderEnabled = true,
                        deliveryEnabled = true,
                        stockQuantity = 20L,
                        trackStock = false,
                        sortOrder = (products.size + 1).toLong()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                modifier = Modifier.fillMaxWidth().testTag("admin_add_product_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add New Menu Item", fontWeight = FontWeight.Bold)
            }
        }

        items(products, key = { it.id }) { product ->
            var quickPriceText by remember(product.id, product.priceOptions) {
                mutableStateOf(product.priceOptions.firstOrNull()?.price?.toString() ?: "0")
            }

            GoldOutlineCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${product.name} (${product.nameHindi})", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                        Text(product.category, style = MaterialTheme.typography.labelSmall, color = RoyalGold)
                        if (product.description.isNotBlank()) {
                            Text(product.description, style = MaterialTheme.typography.bodySmall, color = MutedSlate, maxLines = 2)
                        }
                        Text(
                            text = product.priceOptions.joinToString(" • ") { "${it.label}: ₹${it.price}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = CreamIvory
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            StatusPill(
                                text = if (product.available) "Available" else "Unavailable",
                                color = if (product.available) StatusOpenGreen else StatusClosedRed
                            )
                            StatusPill(
                                text = if (product.visible) "Visible" else "Hidden",
                                color = if (product.visible) RoyalGold else MutedSlate
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = { onSaveProduct(product.copy(visible = !product.visible)) }) {
                            Icon(
                                imageVector = if (product.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                tint = RoyalGold
                            )
                        }
                        IconButton(onClick = { editingProduct = product }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = LuminousGold)
                        }
                        IconButton(onClick = { onDeleteProduct(product.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusClosedRed)
                        }
                    }
                }

                // Inline Quick Price Management for primary variant
                if (product.priceOptions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickPriceText,
                            onValueChange = { quickPriceText = it },
                            label = { Text("Quick ₹ (${product.priceOptions.first().label})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = adminTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val parsed = quickPriceText.trim().toLongOrNull()
                                if (parsed != null && parsed > 0L) {
                                    val updatedOpts = product.priceOptions.mapIndexed { idx, opt ->
                                        if (idx == 0) opt.copy(price = parsed) else opt
                                    }
                                    onSaveProduct(product.copy(priceOptions = updatedOpts))
                                }
                            },
                            border = BorderStroke(1.dp, RoyalGold)
                        ) {
                            Text("Update ₹", color = RoyalGold)
                        }
                    }
                }
            }
        }

        if (unaddedSuggestions.isNotEmpty()) {
            item {
                HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showSuggestedCatalog = !showSuggestedCatalog },
                    border = BorderStroke(1.dp, RoyalGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (showSuggestedCatalog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = RoyalGold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Suggested Food-Stall Items (${unaddedSuggestions.size})", color = RoyalGold)
                }
            }
            if (showSuggestedCatalog) {
                items(unaddedSuggestions, key = { "sug_${it.id}" }) { template ->
                    GoldOutlineCard(containerColor = NavyElevated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${template.name} • ${template.nameHindi}", style = MaterialTheme.typography.titleSmall, color = CreamIvory)
                                Text(
                                    text = "${template.category} • ${if (template.priceOptions.firstOrNull()?.price == 0L) "Set your price" else "₹${template.priceOptions.first().price}"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate
                                )
                            }
                            Button(
                                onClick = {
                                    editingProduct = template.copy(available = true, visible = true, stockQuantity = 20L, trackStock = false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep)
                            ) {
                                Text("Configure & Add")
                            }
                        }
                    }
                }
            }
        }
    }

    editingProduct?.let { target ->
        ProductEditorDialog(
            initial = target,
            categories = activeCategories,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                onSaveProduct(updated)
                editingProduct = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminStockSection(
    products: List<Product>,
    lowStockAlerts: LowStockAlertSummary = DomainCalculators.evaluateLowStockAlerts(products),
    onSaveProduct: (Product) -> Unit
) {
    var showFlaggedOnly by remember { mutableStateOf(false) }
    val flaggedIds = remember(lowStockAlerts) {
        lowStockAlerts.flaggedItems.map { it.product.id }.toSet()
    }
    val visibleProducts = remember(products, showFlaggedOnly, flaggedIds) {
        if (!showFlaggedOnly) products else products.filter { it.id in flaggedIds }
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("11. Stock & Inventory Management", style = MaterialTheme.typography.titleLarge, color = LuminousGold)
            Text(
                text = "Toggle availability, enable stock tracking, or tune critical low-stock alert thresholds.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showFlaggedOnly,
                    onClick = { showFlaggedOnly = false },
                    label = { Text("All Items (${products.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("stock_filter_all")
                )
                FilterChip(
                    selected = showFlaggedOnly,
                    onClick = { showFlaggedOnly = true },
                    label = { Text("Low Stock Alerts (${lowStockAlerts.totalFlaggedCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (lowStockAlerts.outOfStockCount > 0) StatusClosedRed else StatusPendingAmber,
                        selectedLabelColor = NavyDeep
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("stock_filter_flagged")
                )
            }
        }

        if (lowStockAlerts.hasAlerts) {
            item {
                val alertBorder = if (lowStockAlerts.outOfStockCount > 0) StatusClosedRed else StatusPendingAmber
                GoldOutlineCard(
                    containerColor = NavyElevated,
                    borderColor = alertBorder,
                    modifier = Modifier.testTag("admin_stock_alert_summary_card")
                ) {
                    Text(
                        text = "Automated Low-Stock Alert Triggered",
                        style = MaterialTheme.typography.titleSmall,
                        color = alertBorder,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${lowStockAlerts.outOfStockCount} out of stock • ${lowStockAlerts.criticalLowCount} at or below critical threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamIvory
                    )
                    Spacer(Modifier.height(6.dp))
                    lowStockAlerts.flaggedItems.take(5).forEach { alert ->
                        Text(
                            text = "• ${alert.product.name}: ${alert.stockQuantity} left (Alert <= ${alert.threshold})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (alert.severity == LowStockSeverity.OUT_OF_STOCK) StatusClosedRed else StatusPendingAmber
                        )
                    }
                }
            }
        }

        if (visibleProducts.isEmpty()) {
            item {
                GoldOutlineCard {
                    Text("No menu items match the selected inventory filter.", color = MutedSlate)
                }
            }
        }

        items(visibleProducts, key = { "stock_${it.id}" }) { product ->
            val severity = DomainCalculators.evaluateProductStockSeverity(product)
            val threshold = product.lowStockThreshold.coerceAtLeast(0L)
            val stockColor = when {
                !product.available || severity == LowStockSeverity.OUT_OF_STOCK -> StatusClosedRed
                severity == LowStockSeverity.CRITICAL_LOW -> StatusPendingAmber
                else -> StatusOpenGreen
            }
            val stockLabel = when {
                severity == LowStockSeverity.OUT_OF_STOCK -> "Out of Stock (0 / min $threshold)"
                !product.available -> "Disabled"
                severity == LowStockSeverity.CRITICAL_LOW -> "LOW STOCK (${product.stockQuantity} / min $threshold)"
                product.trackStock -> "In Stock (${product.stockQuantity})"
                else -> "Unlimited / Open"
            }
            val cardBorder = when (severity) {
                LowStockSeverity.OUT_OF_STOCK -> StatusClosedRed
                LowStockSeverity.CRITICAL_LOW -> StatusPendingAmber
                LowStockSeverity.HEALTHY -> RoyalGold.copy(alpha = 0.45f)
            }

            GoldOutlineCard(
                borderColor = cardBorder,
                modifier = Modifier.testTag("admin_stock_card_${product.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                        Text(product.category, style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    }
                    StatusPill(text = stockLabel, color = stockColor)
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Item Available", color = CreamIvory, style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = product.available,
                        onCheckedChange = { avail -> onSaveProduct(product.copy(available = avail)) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Track Stock Quantity", color = CreamIvory, style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = product.trackStock,
                        onCheckedChange = { track -> onSaveProduct(product.copy(trackStock = track)) }
                    )
                }

                if (product.trackStock) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Stock:", color = CreamIvory, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val next = (product.stockQuantity - 1L).coerceAtLeast(0L)
                                    onSaveProduct(product.copy(stockQuantity = next, available = next > 0L))
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease stock", tint = RoyalGold)
                            }
                            Text(
                                text = product.stockQuantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (severity != LowStockSeverity.HEALTHY) stockColor else LuminousGold,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val next = product.stockQuantity + 1L
                                    onSaveProduct(product.copy(stockQuantity = next, available = true))
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase stock", tint = RoyalGold)
                            }
                            OutlinedButton(
                                onClick = { onSaveProduct(product.copy(stockQuantity = product.stockQuantity + 10L, available = true)) },
                                border = BorderStroke(1.dp, RoyalGold),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("+10", color = RoyalGold)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Critical Alert <= :", color = CreamIvory, style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    val nextThreshold = (threshold - 1L).coerceAtLeast(0L)
                                    onSaveProduct(product.copy(lowStockThreshold = nextThreshold))
                                },
                                modifier = Modifier.testTag("stock_threshold_minus_${product.id}")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease critical threshold", tint = RoyalGold)
                            }
                            Text(
                                text = threshold.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = LuminousGold,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val nextThreshold = (threshold + 1L).coerceAtMost(10000L)
                                    onSaveProduct(product.copy(lowStockThreshold = nextThreshold))
                                },
                                modifier = Modifier.testTag("stock_threshold_plus_${product.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase critical threshold", tint = RoyalGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductEditorDialog(
    initial: Product,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial.name) }
    var nameHindi by remember { mutableStateOf(initial.nameHindi) }
    var description by remember { mutableStateOf(initial.description) }
    var category by remember { mutableStateOf(initial.category) }
    var price1Label by remember { mutableStateOf(initial.priceOptions.getOrNull(0)?.label ?: "Regular") }
    var price1Value by remember {
        mutableStateOf(
            initial.priceOptions.getOrNull(0)?.price?.takeIf { it > 0L }?.toString() ?: ""
        )
    }
    var price2Label by remember { mutableStateOf(initial.priceOptions.getOrNull(1)?.label ?: "") }
    var price2Value by remember {
        mutableStateOf(
            initial.priceOptions.getOrNull(1)?.price?.takeIf { it > 0L }?.toString() ?: ""
        )
    }
    var imageUrl by remember { mutableStateOf(initial.imageUrl) }
    var available by remember { mutableStateOf(initial.available) }
    var visible by remember { mutableStateOf(initial.visible) }
    var foodOrderEnabled by remember { mutableStateOf(initial.foodOrderEnabled) }
    var deliveryEnabled by remember { mutableStateOf(initial.deliveryEnabled) }
    var trackStock by remember { mutableStateOf(initial.trackStock) }
    var stockQty by remember { mutableStateOf(initial.stockQuantity.toString()) }
    var lowStockThreshold by remember { mutableStateOf(initial.lowStockThreshold.toString()) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                ImageCompressionUtil.compressPickedImage(context, uri, name.ifBlank { "menu" })
                    .onSuccess { compressedUri -> imageUrl = compressedUri }
                    .onFailure { Toast.makeText(context, "Could not compress image", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = LuminousGold,
        textContentColor = CreamIvory,
        title = { Text(if (initial.id.isBlank()) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (English)") }, colors = adminTextFieldColors())
                OutlinedTextField(value = nameHindi, onValueChange = { nameHindi = it }, label = { Text("Name (Hindi)") }, colors = adminTextFieldColors())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Short Description (Optional)") }, colors = adminTextFieldColors())
                Text("Category:", style = MaterialTheme.typography.labelMedium, color = LuminousGold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                        )
                    }
                }
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Or Custom Category") }, colors = adminTextFieldColors())

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = price1Label, onValueChange = { price1Label = it }, label = { Text("Option 1 Label") }, colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = price1Value, onValueChange = { price1Value = it }, label = { Text("₹ Price 1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = price2Label, onValueChange = { price2Label = it }, label = { Text("Option 2 (Opt)") }, colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = price2Value, onValueChange = { price2Value = it }, label = { Text("₹ Price 2") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = adminTextFieldColors(), modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Photo HTTPS URL or Local Compressed Photo") }, colors = adminTextFieldColors())
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    border = BorderStroke(1.dp, RoyalGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = RoyalGold)
                    Spacer(Modifier.width(6.dp))
                    Text("Pick & Compress Photo", color = RoyalGold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Available", color = CreamIvory)
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Visible to Customers", color = CreamIvory)
                    Switch(checked = visible, onCheckedChange = { visible = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Show in Food Order", color = CreamIvory)
                    Switch(checked = foodOrderEnabled, onCheckedChange = { foodOrderEnabled = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Show in Food Delivery", color = CreamIvory)
                    Switch(checked = deliveryEnabled, onCheckedChange = { deliveryEnabled = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Track Stock Quantity", color = CreamIvory)
                    Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                }
                if (trackStock) {
                    OutlinedTextField(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = adminTextFieldColors()
                    )
                    OutlinedTextField(
                        value = lowStockThreshold,
                        onValueChange = { lowStockThreshold = it },
                        label = { Text("Critical Low-Stock Threshold (Alert <=)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = adminTextFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val options = buildList {
                        val p1 = price1Value.trim().toLongOrNull() ?: 0L
                        if (price1Label.isNotBlank() && p1 > 0L) add(PriceOption(price1Label.trim(), p1))
                        val p2 = price2Value.trim().toLongOrNull()
                        if (price2Label.isNotBlank() && p2 != null && p2 > 0L) add(PriceOption(price2Label.trim(), p2))
                    }
                    if (name.isNotBlank() && options.isNotEmpty()) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                nameHindi = nameHindi.trim(),
                                description = description.trim(),
                                category = category.trim().ifBlank { "Snacks and Desserts" },
                                priceOptions = options,
                                imageUrl = imageUrl.trim(),
                                available = available,
                                visible = visible,
                                foodOrderEnabled = foodOrderEnabled,
                                deliveryEnabled = deliveryEnabled,
                                trackStock = trackStock,
                                stockQuantity = stockQty.trim().toLongOrNull()?.coerceIn(0L, 100000L) ?: 20L,
                                lowStockThreshold = lowStockThreshold.trim().toLongOrNull()?.coerceIn(0L, 10000L) ?: 5L
                            )
                        )
                    } else {
                        Toast.makeText(context, "Enter name and valid positive price", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlate)
            }
        }
    )
}
