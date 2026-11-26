package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.CustomerSummary
import com.example.data.model.LowStockAlertSummary
import com.example.data.model.LowStockSeverity
import com.example.data.model.OwnerDashboardStats
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.Product
import com.example.data.model.ReportPeriod
import com.example.data.model.SalesReportSummary
import com.example.data.model.ShopSettings
import com.example.data.model.WhatsAppDeliveryStatus
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.PaymentStatusPill
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.StatusPill
import com.example.ui.components.WhatsAppStatusPill
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusCompletedTeal
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.util.DomainCalculators
import com.example.util.ExternalIntentHelper
import com.example.util.ReceiptAndReportExporter

@Composable
fun AdminOverviewAndCustomersSection(
    stats: OwnerDashboardStats,
    customers: List<CustomerSummary>,
    shop: ShopSettings,
    ownerPrivateSettings: OwnerPrivateSettings,
    strings: AppStrings,
    lowStockAlerts: LowStockAlertSummary = LowStockAlertSummary(),
    onQuickRestockProduct: (Product) -> Unit = {},
    onJumpToStockTab: () -> Unit = {},
    onMarkWhatsAppShared: (String) -> Unit,
    onLoadMoreCustomers: () -> Unit = {},
    showKpis: Boolean = true,
    showCustomerDirectory: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerSummary?>(null) }
    var receiptTarget by remember { mutableStateOf<CustomerRequest?>(null) }

    val filteredCustomers by remember(customers, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) customers
            else customers.filter { c ->
                c.profile.displayName.lowercase().contains(q) ||
                    c.profile.phone.lowercase().contains(q) ||
                    c.profile.email.lowercase().contains(q)
            }
        }
    }

    val newSignupsTodayList = remember(customers) {
        customers.filter { c ->
            val createdMillis = c.profile.createdAt?.toDate()?.time ?: return@filter false
            DomainCalculators.isSameIstDay(createdMillis)
        }
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showKpis) {
            item {
                val palette = LocalMarutiPalette.current
                Text("Live Overview (IST)", style = MaterialTheme.typography.titleLarge, color = palette.accent)
            }
            item {
                val palette = LocalMarutiPalette.current
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OverviewHighlightCard(
                            title = "Total Today's Sales",
                            value = "₹${stats.todaysSales}",
                            subtitle = "Live IST revenue today",
                            icon = Icons.Default.CurrencyRupee,
                            accentColor = palette.accent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_total_todays_sales")
                        )
                        OverviewHighlightCard(
                            title = "Total Pending Orders",
                            value = stats.pendingOrders.toString(),
                            subtitle = "Active & awaiting action",
                            icon = Icons.Default.PendingActions,
                            accentColor = StatusPendingAmber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_total_pending_orders")
                        )
                    }
                    OverviewHighlightCard(
                        title = "Most Popular Product",
                        value = stats.mostPopularProductName ?: "No orders yet",
                        subtitle = if (stats.mostPopularProductUnitsSold > 0L) {
                            "${stats.mostPopularProductUnitsSold} units sold • ₹${stats.mostPopularProductRevenue} revenue"
                        } else {
                            "Updates live from Firestore orders"
                        },
                        icon = Icons.Default.Star,
                        accentColor = StatusOpenGreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kpi_most_popular_product")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiCard("Shop Orders (${stats.shopOrdersCount})", "₹${stats.shopOrdersSales}", palette.primary, Modifier.weight(1f))
                        KpiCard("Khatu Delivery (${stats.deliveryOrdersCount})", "₹${stats.deliveryOrdersSales}", StatusCompletedTeal, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiCard("Room Bookings", stats.roomBookingsCount.toString(), palette.primary, Modifier.weight(1f))
                        KpiCard("Freshen-Up Req", stats.freshenUpCount.toString(), palette.accent, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiCard("Total Orders", stats.totalOrders.toString(), palette.textPrimary, Modifier.weight(1f))
                        KpiCard("Completed Orders", stats.completedOrders.toString(), StatusOpenGreen, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        KpiCard("Cancelled Orders", stats.cancelledOrders.toString(), StatusClosedRed, Modifier.weight(1f))
                        KpiCard("Total Customers", stats.totalCustomers.toString(), palette.primary, Modifier.weight(1f))
                    }
                    KpiCard("New Signups Today (IST)", stats.newSignupsToday.toString(), StatusCompletedTeal, Modifier.fillMaxWidth())
                }
            }

            item {
                val hasAlerts = lowStockAlerts.hasAlerts
                val cardBorder = when {
                    lowStockAlerts.outOfStockCount > 0 -> StatusClosedRed
                    hasAlerts -> StatusPendingAmber
                    else -> StatusOpenGreen.copy(alpha = 0.55f)
                }
                GoldOutlineCard(
                    containerColor = NavyElevated,
                    borderColor = cardBorder,
                    modifier = Modifier.testTag("admin_overview_low_stock_card")
                ) {
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
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Low-Stock Alerts",
                                tint = if (hasAlerts) cardBorder else StatusOpenGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (hasAlerts) {
                                    "Low-Stock Alerts (${lowStockAlerts.totalFlaggedCount} Flagged)"
                                } else {
                                    "Inventory Health (All Good)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (hasAlerts) LuminousGold else StatusOpenGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = onJumpToStockTab,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Manage Stock", color = RoyalGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!hasAlerts) {
                        Text(
                            text = "No tracked menu items are at or below their critical threshold.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    } else {
                        Text(
                            text = "${lowStockAlerts.outOfStockCount} out of stock • ${lowStockAlerts.criticalLowCount} at critical threshold",
                            style = MaterialTheme.typography.labelMedium,
                            color = CreamIvory
                        )
                        Spacer(Modifier.height(8.dp))
                        lowStockAlerts.flaggedItems.forEach { alertItem ->
                            val isOut = alertItem.severity == LowStockSeverity.OUT_OF_STOCK
                            val pillColor = if (isOut) StatusClosedRed else StatusPendingAmber
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alertItem.product.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LuminousGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Qty: ${alertItem.stockQuantity} • Critical <= ${alertItem.threshold}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedSlate
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusPill(
                                        text = if (isOut) "OUT OF STOCK" else "LOW (${alertItem.stockQuantity})",
                                        color = pillColor
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onQuickRestockProduct(
                                                alertItem.product.copy(
                                                    stockQuantity = alertItem.stockQuantity + 10L,
                                                    available = true
                                                )
                                            )
                                        },
                                        border = BorderStroke(1.dp, RoyalGold),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text("+10", color = RoyalGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (newSignupsTodayList.isNotEmpty()) {
                item {
                    GoldOutlineCard(containerColor = NavyElevated) {
                        Text("Today's New Signups", style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                        Spacer(Modifier.height(6.dp))
                        newSignupsTodayList.take(5).forEach { cust ->
                            val ts = DomainCalculators.formatIstDateTime(cust.profile.createdAt?.toDate()?.time)
                            Text(
                                text = "• ${cust.profile.displayName} (${cust.profile.phone.ifBlank { cust.profile.email }}) — $ts",
                                style = MaterialTheme.typography.bodySmall,
                                color = CreamIvory
                            )
                        }
                    }
                }
            }
        }

        if (showCustomerDirectory) {

            // customer directory continues below

        item {
            HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
            Spacer(Modifier.height(6.dp))
            Text("8. Customer Details & Order History (${filteredCustomers.size})", style = MaterialTheme.typography.titleLarge, color = LuminousGold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search customer by name, mobile, or email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RoyalGold) },
                singleLine = true,
                colors = adminTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("admin_customer_search")
            )
        }

        if (filteredCustomers.isEmpty()) {
            item {
                GoldOutlineCard {
                    Text("No registered customers match this search.", color = MutedSlate)
                }
            }
        } else {
            items(filteredCustomers, key = { it.profile.userId.ifBlank { it.profile.id } }) { summary ->
                val p = summary.profile
                GoldOutlineCard(
                    modifier = Modifier.clickable { selectedCustomer = summary }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = RoyalGold)
                            Column {
                                Text(p.displayName.ifBlank { "Customer" }, style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                                Text(
                                    text = "Mobile: ${p.phone.ifBlank { "Not added" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CreamIvory
                                )
                                if (p.email.isNotBlank()) {
                                    Text("Email: ${p.email}", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusPill(text = "${summary.totalOrders} Orders", color = RoyalGold)
                            Spacer(Modifier.height(4.dp))
                            Text("₹${summary.totalSpending}", style = MaterialTheme.typography.titleSmall, color = StatusOpenGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Signup: ${DomainCalculators.formatIstDateTime(p.createdAt?.toDate()?.time)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                    Text(
                        text = "Last Order: ${summary.lastOrderMillis?.let { DomainCalculators.formatIstDateTime(it) } ?: "No orders yet"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
            }
            if (customers.size >= 50) {
                item {
                    OutlinedButton(
                        onClick = onLoadMoreCustomers,
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load More Customers", color = RoyalGold)
                    }
                }
            }
        }
        }
    }

    selectedCustomer?.let { summary ->
        AlertDialog(
            onDismissRequest = { selectedCustomer = null },
            containerColor = NavyCard,
            titleContentColor = LuminousGold,
            textContentColor = CreamIvory,
            title = { Text("${summary.profile.displayName} • Order History") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Mobile: ${summary.profile.phone.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                    Text("Email: ${summary.profile.email.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                    Text("Total Orders: ${summary.totalOrders} • Total Spending: ₹${summary.totalSpending}", color = LuminousGold, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(color = AntiqueGold.copy(alpha = 0.3f))
                    if (summary.orderHistory.isEmpty()) {
                        Text("No orders placed yet.", color = MutedSlate)
                    } else {
                        summary.orderHistory.forEach { order ->
                            GoldOutlineCard(containerColor = NavyDeep) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(order.orderNumber.ifBlank { order.id.take(8) }, color = LuminousGold, fontWeight = FontWeight.Bold)
                                    Text("₹${order.grandTotal}", color = StatusOpenGreen, fontWeight = FontWeight.Bold)
                                }
                                Text(DomainCalculators.formatIstDateTime(order.createdAt?.toDate()?.time), style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                Text("Status: ${order.status} • ${order.paymentMethod} (${order.paymentStatus})", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { receiptTarget = order }) {
                                    Text("Open Digital Receipt", color = RoyalGold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomer = null }) {
                    Text(strings.closeLabel, color = RoyalGold)
                }
            }
        )
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsAndWhatsAppSection(
    requests: List<CustomerRequest>,
    report: SalesReportSummary,
    selectedPeriod: ReportPeriod,
    reportStartDateMillis: Long?,
    reportEndDateMillis: Long?,
    shop: ShopSettings,
    ownerPrivateSettings: OwnerPrivateSettings,
    strings: AppStrings,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onSelectCustomDateRange: (Long?, Long?) -> Unit,
    onRetryWhatsApp: (String) -> Unit,
    onMarkWhatsAppShared: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMarutiPalette.current
    var receiptTarget by remember { mutableStateOf<CustomerRequest?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val periodDisplayName: (ReportPeriod) -> String = { period ->
        when (period) {
            ReportPeriod.DAILY -> "Daily"
            ReportPeriod.WEEKLY -> "Weekly"
            ReportPeriod.MONTHLY -> "Monthly"
            ReportPeriod.CUSTOM_RANGE -> "Custom Range"
        }
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 13. Daily / Weekly / Monthly / Custom Date Range Reports
        item {
            Text("13. Sales & Product Reports (IST)", style = MaterialTheme.typography.titleLarge, color = LuminousGold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(periodDisplayName(period)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            GoldOutlineCard(containerColor = NavyCard) {
                Text(
                    text = "Custom Date Range Filter (IST)",
                    style = MaterialTheme.typography.titleSmall,
                    color = LuminousGold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("report_start_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Start Date",
                            tint = RoyalGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (reportStartDateMillis != null) {
                                "Start: ${DomainCalculators.formatPickerUtcDateForDisplay(reportStartDateMillis)}"
                            } else {
                                "Start Date"
                            },
                            color = CreamIvory
                        )
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("report_end_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select End Date",
                            tint = RoyalGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (reportEndDateMillis != null) {
                                "End: ${DomainCalculators.formatPickerUtcDateForDisplay(reportEndDateMillis)}"
                            } else {
                                "End Date"
                            },
                            color = CreamIvory
                        )
                    }
                }
                if (reportStartDateMillis != null || reportEndDateMillis != null || selectedPeriod == ReportPeriod.CUSTOM_RANGE) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing IST: ${report.dateRangeLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusOpenGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onSelectCustomDateRange(null, null) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("report_clear_dates_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Reset date filter",
                                tint = RoyalGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", color = RoyalGold)
                        }
                    }
                }
            }
        }

        item {
            GoldOutlineCard(containerColor = NavyElevated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${periodDisplayName(report.period).uppercase()} SUMMARY",
                            style = MaterialTheme.typography.titleMedium,
                            color = LuminousGold
                        )
                        if (report.dateRangeLabel.isNotBlank()) {
                            Text(
                                text = report.dateRangeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                modifier = Modifier.testTag("report_date_range_label")
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { ReceiptAndReportExporter.exportSalesReportCsv(context, report) },
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export CSV", color = RoyalGold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Total Sales: ₹${report.totalSales}", style = MaterialTheme.typography.titleLarge, color = LuminousGold, fontWeight = FontWeight.Bold)
                Text("Total Orders: ${report.totalOrders}  •  Completed: ${report.completedOrders}  •  Cancelled: ${report.cancelledOrders}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                Text("Shop Pickup: ${report.pickupOrders}  •  Khatu Delivery: ${report.deliveryOrders}  •  Rooms: ${report.roomBookings}", style = MaterialTheme.typography.bodySmall, color = LuminousGold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Shop Orders Sales: ₹${report.shopSales}  |  Delivery Sales: ₹${report.deliverySales}  |  Rooms & Freshen: ₹${report.roomSales}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusCompletedTeal,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = AntiqueGold.copy(alpha = 0.3f))
                Spacer(Modifier.height(6.dp))
                Text("Cash Sales: ₹${report.cashSales}  |  UPI Sales: ₹${report.upiSales}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                Text("Verified Paid: ₹${report.verifiedPaidTotal}  |  Unpaid/Pending: ₹${report.unpaidOrPendingTotal}", style = MaterialTheme.typography.bodySmall, color = StatusPendingAmber)

                if (report.productStats.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Popular & Product-wise Sales:", style = MaterialTheme.typography.titleSmall, color = RoyalGold)
                    report.productStats.take(12).forEach { stat ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${stat.productName} (x${stat.quantitySold})", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                            Text("₹${stat.totalRevenue}", style = MaterialTheme.typography.bodySmall, color = LuminousGold, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 12. WhatsApp Billing
        item {
            HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
            Spacer(Modifier.height(6.dp))
            Text("12. WhatsApp Billing & Delivery Logs", style = MaterialTheme.typography.titleLarge, color = LuminousGold)
            Text(
                text = "Owner Private WhatsApp: ${ownerPrivateSettings.ownerWhatsAppNumber.ifBlank { "Not configured" }} • Server Webhook: ${ownerPrivateSettings.whatsappWebhookUrl.ifBlank { shop.whatsappWebhookUrl.ifBlank { "Not configured (manual WhatsApp share active)" } }}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate
            )
        }

        items(requests.take(40), key = { "wa_${it.id}" }) { req ->
            val orderNum = req.orderNumber.ifBlank { req.id.take(10) }
            GoldOutlineCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$orderNum • ${req.customerName} (${req.contactPhone})", style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                        Text(DomainCalculators.formatIstDateTime(req.createdAt?.toDate()?.time), style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    }
                    WhatsAppStatusPill(req.whatsappStatus)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    RequestStatusPill(req.status, strings)
                    PaymentStatusPill(req.paymentMethod, req.paymentStatus)
                    Text("₹${req.grandTotal}", color = LuminousGold, fontWeight = FontWeight.Bold)
                }
                if (!req.whatsappError.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Cloud API Note: ${req.whatsappError}", style = MaterialTheme.typography.labelSmall, color = StatusClosedRed)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { receiptTarget = req },
                        border = BorderStroke(1.dp, RoyalGold)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Receipt", color = RoyalGold)
                    }
                    if ((ownerPrivateSettings.whatsappWebhookUrl.isNotBlank() || shop.whatsappWebhookUrl.isNotBlank()) &&
                        WhatsAppDeliveryStatus.fromRaw(req.whatsappStatus) != WhatsAppDeliveryStatus.SENT
                    ) {
                        OutlinedButton(
                            onClick = { onRetryWhatsApp(req.id) },
                            border = BorderStroke(1.dp, StatusPendingAmber)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = StatusPendingAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Retry Auto-Send", color = StatusPendingAmber)
                        }
                    }
                    Button(
                        onClick = {
                            onMarkWhatsAppShared(req.id)
                            val msg = DomainCalculators.formatWhatsAppRequestMessage(req, shop)
                            ExternalIntentHelper.launchWhatsAppShare(context, msg, ownerPrivateSettings.ownerWhatsAppNumber)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusOpenGreen, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Send WhatsApp")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val startPickerState = rememberDatePickerState(
            initialSelectedDateMillis = reportStartDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = startPickerState.selectedDateMillis
                        if (picked != null) {
                            val existingEnd = reportEndDateMillis
                            if (existingEnd != null && picked > existingEnd) {
                                onSelectCustomDateRange(existingEnd, picked)
                            } else {
                                onSelectCustomDateRange(picked, existingEnd ?: picked)
                            }
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Apply Start", color = RoyalGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = CreamIvory)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = NavyCard)
        ) {
            DatePicker(
                state = startPickerState,
                title = {
                    Text(
                        text = "Select Report Start Date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        color = LuminousGold
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = NavyCard,
                    titleContentColor = LuminousGold,
                    headlineContentColor = CreamIvory,
                    weekdayContentColor = RoyalGold,
                    subheadContentColor = CreamIvory,
                    yearContentColor = CreamIvory,
                    currentYearContentColor = RoyalGold,
                    selectedYearContentColor = NavyDeep,
                    selectedYearContainerColor = RoyalGold,
                    dayContentColor = CreamIvory,
                    todayContentColor = LuminousGold,
                    todayDateBorderColor = RoyalGold,
                    selectedDayContentColor = NavyDeep,
                    selectedDayContainerColor = RoyalGold
                )
            )
        }
    }

    if (showEndDatePicker) {
        val endPickerState = rememberDatePickerState(
            initialSelectedDateMillis = reportEndDateMillis ?: reportStartDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = endPickerState.selectedDateMillis
                        if (picked != null) {
                            val existingStart = reportStartDateMillis
                            if (existingStart != null && picked < existingStart) {
                                onSelectCustomDateRange(picked, existingStart)
                            } else {
                                onSelectCustomDateRange(existingStart ?: picked, picked)
                            }
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Apply End", color = RoyalGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = CreamIvory)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = NavyCard)
        ) {
            DatePicker(
                state = endPickerState,
                title = {
                    Text(
                        text = "Select Report End Date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        color = LuminousGold
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = NavyCard,
                    titleContentColor = LuminousGold,
                    headlineContentColor = CreamIvory,
                    weekdayContentColor = RoyalGold,
                    subheadContentColor = CreamIvory,
                    yearContentColor = CreamIvory,
                    currentYearContentColor = RoyalGold,
                    selectedYearContentColor = NavyDeep,
                    selectedYearContainerColor = RoyalGold,
                    dayContentColor = CreamIvory,
                    todayContentColor = LuminousGold,
                    todayDateBorderColor = RoyalGold,
                    selectedDayContentColor = NavyDeep,
                    selectedDayContainerColor = RoyalGold
                )
            )
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

@Composable
private fun OverviewHighlightCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalMarutiPalette.current
    GoldOutlineCard(modifier = modifier, containerColor = palette.elevated) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalMarutiPalette.current
    GoldOutlineCard(modifier = modifier, containerColor = palette.elevated) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
