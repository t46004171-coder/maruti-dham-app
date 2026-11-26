package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.CustomerRequest
import com.example.data.model.CustomerSummary
import com.example.data.model.DeliverySettings
import com.example.data.model.FreshenUpOption
import com.example.data.model.FulfillmentType
import com.example.data.model.LowStockAlertItem
import com.example.data.model.LowStockAlertSummary
import com.example.data.model.LowStockSeverity
import com.example.data.model.OwnerDashboardStats
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductSalesStat
import com.example.data.model.ReportPeriod
import com.example.data.model.RequestCategory
import com.example.data.model.RequestLineItem
import com.example.data.model.RequestStatus
import com.example.data.model.RoomConfig
import com.example.data.model.RoomType
import com.example.data.model.SalesReportSummary
import com.example.data.model.ShopControlMode
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.data.model.UserProfile
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DeliveryValidationResult(
    val isValid: Boolean,
    val reason: String? = null
)

object DomainCalculators {

    val supportedRoomDurations = listOf(1, 2, 3, 4, 5, 6, 12)
    val istTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun normalizeIndianMobile(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
            digits.length >= 12 && digits.startsWith("91") -> digits.take(12)
            else -> digits
        }
    }

    fun isValidMobileNumber(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        val tenDigits = when {
            digits.length == 10 -> digits
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            else -> ""
        }
        return tenDigits.length == 10 && tenDigits.first() in '6'..'9'
    }

    fun formatIstDate(millis: Long?): String {
        if (millis == null || millis <= 0L) return "--/--/----"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply { timeZone = istTimeZone }
        return sdf.format(Date(millis))
    }

    fun formatIstTime(millis: Long?): String {
        if (millis == null || millis <= 0L) return "--:--"
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = istTimeZone }
        return "${sdf.format(Date(millis))} IST"
    }

    fun formatIstDateTime(millis: Long?): String {
        if (millis == null || millis <= 0L) return "Just now (IST)"
        return "${formatIstDate(millis)} • ${formatIstTime(millis)}"
    }

    fun isSameIstDay(epochMillis: Long, referenceMillis: Long = System.currentTimeMillis()): Boolean {
        val cal1 = Calendar.getInstance(istTimeZone).apply { timeInMillis = epochMillis }
        val cal2 = Calendar.getInstance(istTimeZone).apply { timeInMillis = referenceMillis }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun parseClockMinutes(timeStr: String): Int? {
        val cleaned = timeStr.trim().uppercase(Locale.ENGLISH)
        if (cleaned.isBlank()) return null
        val formats = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                    timeZone = istTimeZone
                }
                val parsed = parser.parse(cleaned) ?: continue
                val cal = Calendar.getInstance(istTimeZone).apply { time = parsed }
                return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    fun evaluateShopOpenStatus(
        settings: ShopSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): ShopOpenEvaluation {
        val mode = ShopControlMode.fromRaw(settings.shopControlMode)
        val cal = Calendar.getInstance(istTimeZone).apply { timeInMillis = nowMillis }
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val openMins = parseClockMinutes(settings.openingTime)
        val closeMins = parseClockMinutes(settings.closingTime)

        val withinSchedule = if (openMins != null && closeMins != null) {
            if (openMins <= closeMins) {
                nowMins in openMins until closeMins
            } else {
                nowMins >= openMins || nowMins < closeMins
            }
        } else {
            settings.shopOpen
        }

        val nextOpenText = settings.openingTime.trim().takeIf { it.isNotBlank() }?.let { "$it IST" }

        return when (mode) {
            ShopControlMode.OPEN_NOW -> ShopOpenEvaluation(
                isOpen = true,
                badgeText = "OPEN NOW",
                nextOpeningText = nextOpenText,
                mode = mode
            )
            ShopControlMode.CLOSED_NOW -> ShopOpenEvaluation(
                isOpen = false,
                badgeText = "SHOP CLOSED",
                nextOpeningText = nextOpenText,
                mode = mode
            )
            ShopControlMode.OPEN_AT_SCHEDULED -> ShopOpenEvaluation(
                isOpen = withinSchedule,
                badgeText = if (withinSchedule) "OPEN (SCHEDULED)" else "SHOP CLOSED",
                nextOpeningText = nextOpenText,
                mode = mode
            )
            ShopControlMode.CLOSE_AT_SCHEDULED -> {
                val openUntilClose = if (closeMins != null) {
                    if (openMins != null && openMins > closeMins) {
                        nowMins >= openMins || nowMins < closeMins
                    } else {
                        nowMins < closeMins
                    }
                } else {
                    settings.shopOpen
                }
                ShopOpenEvaluation(
                    isOpen = openUntilClose,
                    badgeText = if (openUntilClose) "OPEN UNTIL ${settings.closingTime}" else "SHOP CLOSED",
                    nextOpeningText = nextOpenText,
                    mode = mode
                )
            }
        }
    }

    fun calculateFoodTotal(items: List<RequestLineItem>): Long =
        items.sumOf { (it.unitPrice * it.quantity).coerceAtLeast(0L) }

    fun calculateRequestSubtotal(request: CustomerRequest): Long =
        (request.foodTotal + request.roomTotal + request.freshenUpTotal).coerceAtLeast(0L)

    fun calculateGrandTotal(
        foodTotal: Long,
        roomTotal: Long,
        freshenUpTotal: Long,
        deliveryCharge: Long,
        packagingCharge: Long
    ): Long = (foodTotal.coerceAtLeast(0L) +
        roomTotal.coerceAtLeast(0L) +
        freshenUpTotal.coerceAtLeast(0L) +
        deliveryCharge.coerceAtLeast(0L) +
        packagingCharge.coerceAtLeast(0L))

    fun resolveRoomRateForDuration(room: RoomConfig, durationHours: Int): Long? = when (durationHours) {
        1 -> room.rate1Hour
        2 -> room.rate2Hours
        3 -> room.rate3Hours
        4 -> room.rate4Hours
        5 -> room.rate5Hours
        6 -> room.rate6Hours
        12 -> room.rate12Hours
        else -> null
    }

    fun calculateRoomTotal(unitRate: Long?, numberOfRooms: Int): Long {
        if (unitRate == null || numberOfRooms <= 0) return 0L
        return unitRate * numberOfRooms
    }

    fun calculateFreshenUpTotal(pricePerPerson: Long, numberOfPeople: Int): Long {
        if (pricePerPerson <= 0L || numberOfPeople <= 0) return 0L
        return pricePerPerson * numberOfPeople
    }

    fun validateDeliveryArea(
        address: String,
        selectedLocality: String,
        settings: DeliverySettings
    ): DeliveryValidationResult {
        if (!settings.deliveryEnabled) {
            return DeliveryValidationResult(
                isValid = false,
                reason = "Delivery is currently paused by the owner."
            )
        }
        val cleanAddress = address.trim()
        if (cleanAddress.length < 4) {
            return DeliveryValidationResult(
                isValid = false,
                reason = "Please enter a complete delivery address in ${settings.serviceAreaName}."
            )
        }
        val lowerAddress = cleanAddress.lowercase()
        val outsideCities = listOf("jaipur", "sikar", "ringas", "reengus", "delhi", "gurgaon", "alwar", "ajmer", "kota", "jodhpur", "udaipur", "bikaner")
        val mentionsOutsideCity = outsideCities.any { city ->
            lowerAddress.contains(city) && !lowerAddress.contains("khatu")
        }
        if (mentionsOutsideCity) {
            return DeliveryValidationResult(
                isValid = false,
                reason = "Delivery is available ONLY within ${settings.serviceAreaName}."
            )
        }

        val normalizedAllowed = settings.allowedLocalities.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val localityLower = selectedLocality.trim().lowercase()
        val localityAllowed = localityLower.isNotEmpty() && normalizedAllowed.any {
            it == localityLower || localityLower.contains(it) || it.contains(localityLower)
        }
        val addressMentionsArea = normalizedAllowed.any { lowerAddress.contains(it) } ||
            lowerAddress.contains(settings.serviceAreaName.trim().lowercase()) ||
            lowerAddress.contains("khatu")

        return if (localityAllowed || addressMentionsArea) {
            DeliveryValidationResult(isValid = true)
        } else {
            DeliveryValidationResult(
                isValid = false,
                reason = "Address must be inside ${settings.serviceAreaName} (${settings.allowedLocalities.joinToString(", ")})."
            )
        }
    }

    fun formatReceiptText(
        request: CustomerRequest,
        shopSettings: ShopSettings
    ): String {
        val createdMillis = request.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val orderNum = request.orderNumber.ifBlank { request.id.take(10) }
        val subtotal = calculateRequestSubtotal(request)
        return buildString {
            appendLine(shopSettings.shopName)
            appendLine("DIGITAL ORDER RECEIPT")
            appendLine("Owner: ${shopSettings.ownerName} • ${shopSettings.address}")
            appendLine("----------------------------------------")
            appendLine("Order ID: $orderNum")
            appendLine("Date: ${formatIstDate(createdMillis)}")
            appendLine("Time: ${formatIstTime(createdMillis)}")
            appendLine("Customer: ${request.customerName}")
            appendLine("Mobile: ${request.contactPhone}")
            appendLine("Mode: ${request.fulfillmentType} (${request.expectedTime})")
            appendLine("Payment: ${request.paymentMethod} [${request.paymentStatus}]")
            if (!request.upiTransactionRef.isNullOrBlank()) {
                appendLine("UPI Ref: ${request.upiTransactionRef}")
            }
            if (!request.deliveryAddress.isNullOrBlank()) {
                appendLine("Address: ${request.deliveryLocality ?: ""} - ${request.deliveryAddress}")
            }
            appendLine("----------------------------------------")
            if (request.items.isNotEmpty()) {
                appendLine("ITEM | QTY | RATE | TOTAL")
                request.items.forEach { item ->
                    val label = if (item.variantLabel.isNotBlank()) "${item.name} (${item.variantLabel})" else item.name
                    appendLine("$label | ${item.quantity} | ₹${item.unitPrice} | ₹${item.lineTotal}")
                }
                appendLine("----------------------------------------")
            }
            appendLine("Subtotal:          ₹$subtotal")
            if (request.deliveryCharge > 0L) appendLine("Delivery Charge:   ₹${request.deliveryCharge}")
            if (request.packagingCharge > 0L) appendLine("Packaging Charge:  ₹${request.packagingCharge}")
            appendLine("Grand Total:       ₹${request.grandTotal}")
            appendLine("----------------------------------------")
            appendLine("THANK YOU FOR VISITING MARUTI DHAM")
        }.trim()
    }

    fun formatWhatsAppRequestMessage(
        request: CustomerRequest,
        shopSettings: ShopSettings
    ): String {
        val category = RequestCategory.fromRaw(request.category)
        val createdMillis = request.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val orderNum = request.orderNumber.ifBlank { request.id }
        val subtotal = calculateRequestSubtotal(request)
        val heading = when (category) {
            RequestCategory.FOOD_REQUEST -> "MARUTI DHAM – NEW ORDER (FOOD)"
            RequestCategory.ROOM_REQUEST -> "MARUTI DHAM – ROOM REQUEST"
            RequestCategory.FRESHEN_UP_REQUEST -> "MARUTI DHAM – FRESHEN UP REQUEST"
            RequestCategory.FOOD_DELIVERY_REQUEST -> "MARUTI DHAM – NEW DELIVERY ORDER"
        }

        return buildString {
            appendLine("*$heading*")
            appendLine("Order ID: $orderNum")
            appendLine("Customer Name: ${request.customerName}")
            appendLine("Customer Mobile: ${request.contactPhone}")
            appendLine("Order Date: ${formatIstDate(createdMillis)}")
            appendLine("Order Time: ${formatIstTime(createdMillis)}")
            appendLine("Order Status: ${request.status}")
            appendLine("Mode: ${request.fulfillmentType} • Time: ${request.expectedTime}")
            appendLine("Payment: ${request.paymentMethod} (${request.paymentStatus})")
            if (!request.upiTransactionRef.isNullOrBlank()) {
                appendLine("UPI Ref: ${request.upiTransactionRef}")
            }
            appendLine("--------------------------------")

            if (request.items.isNotEmpty()) {
                appendLine("*ITEM | QTY | RATE | TOTAL*")
                request.items.forEach { item ->
                    val variant = if (item.variantLabel.isNotBlank()) " (${item.variantLabel})" else ""
                    appendLine("• ${item.name}$variant | ${item.quantity} | ₹${item.unitPrice} | ₹${item.lineTotal}")
                }
            }

            if (category == RequestCategory.ROOM_REQUEST) {
                appendLine()
                appendLine("*Room Details:*")
                val rType = if (request.roomType == RoomType.AC.name) "AC Room" else "Non-AC Room"
                appendLine("• Type: $rType")
                request.numberOfRooms?.let { appendLine("• Rooms: $it") }
                request.numberOfPeople?.let { appendLine("• Guests: $it") }
                request.roomDurationHours?.let { appendLine("• Duration: $it hr(s)") }
                if (request.roomRateConfigured == false) {
                    appendLine("• Rate: To be confirmed by owner")
                }
            }

            if (category == RequestCategory.FRESHEN_UP_REQUEST) {
                appendLine()
                appendLine("*Freshen Up Details:*")
                val optLabel = if (request.freshenUpOption == FreshenUpOption.BATH_FRESH.name) {
                    "Bath + Fresh"
                } else {
                    "Only Fresh (without bathing)"
                }
                appendLine("• Service: $optLabel")
                request.numberOfPeople?.let { appendLine("• People: $it") }
            }

            if (category == RequestCategory.FOOD_DELIVERY_REQUEST || !request.deliveryAddress.isNullOrBlank()) {
                appendLine()
                appendLine("*Delivery Details:*")
                request.deliveryLocality?.let { appendLine("• Area: $it") }
                request.deliveryAddress?.let { appendLine("• Address: $it") }
            }

            if (!request.notes.isNullOrBlank()) {
                appendLine()
                appendLine("Notes: ${request.notes}")
            }

            appendLine("--------------------------------")
            appendLine("Subtotal: ₹$subtotal")
            if (request.deliveryCharge > 0L) appendLine("Delivery Charge: ₹${request.deliveryCharge}")
            if (request.packagingCharge > 0L) appendLine("Packaging Charge: ₹${request.packagingCharge}")
            appendLine("*Grand Total: ₹${request.grandTotal}*")
            appendLine("THANK YOU FOR VISITING ${shopSettings.shopName}")
        }.trim()
    }

    fun computeOwnerDashboardStats(
        requests: List<CustomerRequest>,
        users: List<UserProfile>,
        nowMillis: Long = System.currentTimeMillis()
    ): OwnerDashboardStats {
        val todaysRequests = requests.filter { req ->
            val millis = req.createdAt?.toDate()?.time ?: return@filter false
            isSameIstDay(millis, nowMillis)
        }
        val todaysSales = todaysRequests
            .filter { RequestStatus.fromRaw(it.status) != RequestStatus.REJECTED }
            .sumOf { it.grandTotal }

        val pendingCount = requests.count {
            val st = RequestStatus.fromRaw(it.status)
            st == RequestStatus.PENDING || st == RequestStatus.CONFIRMED || st == RequestStatus.PREPARING || st == RequestStatus.READY
        }
        val completedCount = requests.count { RequestStatus.fromRaw(it.status) == RequestStatus.COMPLETED }
        val cancelledCount = requests.count { RequestStatus.fromRaw(it.status) == RequestStatus.REJECTED }
        val newSignupsToday = users.count { u ->
            val millis = u.createdAt?.toDate()?.time ?: return@count false
            isSameIstDay(millis, nowMillis)
        }

        val shopReqs = requests.filter { RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_REQUEST }
        val deliveryReqs = requests.filter { RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_DELIVERY_REQUEST }
        val roomReqs = requests.filter { RequestCategory.fromRaw(it.category) == RequestCategory.ROOM_REQUEST }
        val freshenReqs = requests.filter { RequestCategory.fromRaw(it.category) == RequestCategory.FRESHEN_UP_REQUEST }

        val shopOrdersSales = shopReqs
            .filter { RequestStatus.fromRaw(it.status) != RequestStatus.REJECTED }
            .sumOf { it.grandTotal }
        val deliveryOrdersSales = deliveryReqs
            .filter { RequestStatus.fromRaw(it.status) != RequestStatus.REJECTED }
            .sumOf { it.grandTotal }

        val productAccumulator = mutableMapOf<String, Pair<Long, Long>>()
        requests
            .filter { RequestStatus.fromRaw(it.status) != RequestStatus.REJECTED }
            .forEach { req ->
                req.items.forEach { item ->
                    val name = item.name.trim()
                    if (name.isNotEmpty() && item.quantity > 0L) {
                        val prev = productAccumulator[name] ?: (0L to 0L)
                        productAccumulator[name] = (prev.first + item.quantity) to (prev.second + item.lineTotal)
                    }
                }
            }
        val topProduct = productAccumulator.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Pair<Long, Long>>> { it.value.first }
                    .thenByDescending { it.value.second }
                    .thenBy { it.key }
            )
            .firstOrNull()

        return OwnerDashboardStats(
            todaysSales = todaysSales,
            totalOrders = requests.size,
            pendingOrders = pendingCount,
            completedOrders = completedCount,
            cancelledOrders = cancelledCount,
            totalCustomers = users.size,
            newSignupsToday = newSignupsToday,
            shopOrdersCount = shopReqs.size,
            deliveryOrdersCount = deliveryReqs.size,
            roomBookingsCount = roomReqs.size,
            freshenUpCount = freshenReqs.size,
            shopOrdersSales = shopOrdersSales,
            deliveryOrdersSales = deliveryOrdersSales,
            mostPopularProductName = topProduct?.key,
            mostPopularProductUnitsSold = topProduct?.value?.first ?: 0L,
            mostPopularProductRevenue = topProduct?.value?.second ?: 0L
        )
    }

    fun buildCustomerDirectory(
        users: List<UserProfile>,
        requests: List<CustomerRequest>
    ): List<CustomerSummary> {
        val requestsByUid = requests.groupBy { it.userId }
        return users.map { profile ->
            val history = requestsByUid[profile.userId].orEmpty()
                .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            val spending = history
                .filter { RequestStatus.fromRaw(it.status) != RequestStatus.REJECTED }
                .sumOf { it.grandTotal }
            val lastOrderMillis = history.firstOrNull()?.createdAt?.toDate()?.time
            CustomerSummary(
                profile = profile,
                totalOrders = history.size,
                totalSpending = spending,
                lastOrderMillis = lastOrderMillis,
                orderHistory = history
            )
        }.sortedByDescending { it.profile.createdAt?.toDate()?.time ?: 0L }
    }

    fun istDayBoundsFromPickerUtcMillis(
        startUtcMillis: Long?,
        endUtcMillis: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Long, Long> {
        fun utcPickerMillisToIstYmd(utcMillis: Long): Triple<Int, Int, Int> {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
            }
            return Triple(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH)
            )
        }

        val todayIst = Calendar.getInstance(istTimeZone).apply { timeInMillis = nowMillis }
        val (sYear, sMonth, sDay) = startUtcMillis?.let(::utcPickerMillisToIstYmd) ?: Triple(
            todayIst.get(Calendar.YEAR),
            todayIst.get(Calendar.MONTH),
            todayIst.get(Calendar.DAY_OF_MONTH)
        )
        val (eYear, eMonth, eDay) = endUtcMillis?.let(::utcPickerMillisToIstYmd) ?: Triple(sYear, sMonth, sDay)

        val rawStart = Calendar.getInstance(istTimeZone).apply {
            set(sYear, sMonth, sDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val rawEnd = Calendar.getInstance(istTimeZone).apply {
            set(eYear, eMonth, eDay, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return if (rawStart <= rawEnd) {
            rawStart to rawEnd
        } else {
            val swappedStart = Calendar.getInstance(istTimeZone).apply {
                set(eYear, eMonth, eDay, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val swappedEnd = Calendar.getInstance(istTimeZone).apply {
                set(sYear, sMonth, sDay, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            swappedStart to swappedEnd
        }
    }

    fun formatPickerUtcDateForDisplay(utcMillis: Long?): String {
        if (utcMillis == null) return "Select Date"
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val istCal = Calendar.getInstance(istTimeZone).apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                12,
                0,
                0
            )
        }
        return formatIstDate(istCal.timeInMillis)
    }

    fun generateSalesReport(
        requests: List<CustomerRequest>,
        period: ReportPeriod,
        nowMillis: Long = System.currentTimeMillis(),
        customStartDateMillis: Long? = null,
        customEndDateMillis: Long? = null
    ): SalesReportSummary {
        val (minMillis, maxMillis, rangeLabel) = if (period == ReportPeriod.CUSTOM_RANGE) {
            val (startMs, endMs) = istDayBoundsFromPickerUtcMillis(
                startUtcMillis = customStartDateMillis,
                endUtcMillis = customEndDateMillis,
                nowMillis = nowMillis
            )
            Triple(startMs, endMs, "${formatIstDate(startMs)} – ${formatIstDate(endMs)}")
        } else {
            val windowDays = when (period) {
                ReportPeriod.DAILY -> 0
                ReportPeriod.WEEKLY -> 7
                ReportPeriod.MONTHLY -> 30
                ReportPeriod.CUSTOM_RANGE -> 0
            }
            val startCal = Calendar.getInstance(istTimeZone).apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (windowDays > 0) add(Calendar.DAY_OF_YEAR, -windowDays + 1)
            }
            val startMs = startCal.timeInMillis
            val label = when (period) {
                ReportPeriod.DAILY -> "Today (${formatIstDate(nowMillis)})"
                ReportPeriod.WEEKLY -> "Last 7 Days (${formatIstDate(startMs)} – ${formatIstDate(nowMillis)})"
                ReportPeriod.MONTHLY -> "Last 30 Days (${formatIstDate(startMs)} – ${formatIstDate(nowMillis)})"
                ReportPeriod.CUSTOM_RANGE -> ""
            }
            Triple(startMs, Long.MAX_VALUE, label)
        }

        val scoped = requests.filter { req ->
            val millis = req.createdAt?.toDate()?.time ?: return@filter false
            millis in minMillis..maxMillis
        }
        val activeOrders = scoped.filter { RequestStatus.fromRaw(it.status) != RequestStatus.REJECTED }
        val cancelledOrders = scoped.count { RequestStatus.fromRaw(it.status) == RequestStatus.REJECTED }
        val completedOrders = scoped.count { RequestStatus.fromRaw(it.status) == RequestStatus.COMPLETED }

        val pickupOrders = scoped.count {
            RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_REQUEST ||
                (RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_DELIVERY_REQUEST &&
                    FulfillmentType.fromRaw(it.fulfillmentType) == FulfillmentType.PICKUP)
        }
        val deliveryOrders = scoped.count {
            RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_DELIVERY_REQUEST
        }
        val roomBookings = scoped.count { RequestCategory.fromRaw(it.category) == RequestCategory.ROOM_REQUEST }

        val shopSales = activeOrders
            .filter { RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_REQUEST }
            .sumOf { it.grandTotal }
        val deliverySales = activeOrders
            .filter { RequestCategory.fromRaw(it.category) == RequestCategory.FOOD_DELIVERY_REQUEST }
            .sumOf { it.grandTotal }
        val roomSales = activeOrders
            .filter {
                val cat = RequestCategory.fromRaw(it.category)
                cat == RequestCategory.ROOM_REQUEST || cat == RequestCategory.FRESHEN_UP_REQUEST
            }
            .sumOf { it.grandTotal }

        val productAccumulator = mutableMapOf<String, Pair<Long, Long>>()
        activeOrders.forEach { req ->
            req.items.forEach { item ->
                val prev = productAccumulator[item.name] ?: (0L to 0L)
                productAccumulator[item.name] = (prev.first + item.quantity) to (prev.second + item.lineTotal)
            }
        }
        val productStats = productAccumulator.entries.map { (name, pair) ->
            ProductSalesStat(productName = name, quantitySold = pair.first, totalRevenue = pair.second)
        }.sortedByDescending { it.quantitySold }

        val cashSales = activeOrders
            .filter { PaymentMethod.fromRaw(it.paymentMethod) == PaymentMethod.CASH }
            .sumOf { it.grandTotal }
        val upiSales = activeOrders
            .filter { PaymentMethod.fromRaw(it.paymentMethod) == PaymentMethod.UPI }
            .sumOf { it.grandTotal }
        val paidTotal = activeOrders
            .filter { PaymentStatus.fromRaw(it.paymentStatus) == PaymentStatus.PAID }
            .sumOf { it.grandTotal }
        val unpaidTotal = activeOrders
            .filter { PaymentStatus.fromRaw(it.paymentStatus) != PaymentStatus.PAID }
            .sumOf { it.grandTotal }

        return SalesReportSummary(
            period = period,
            dateRangeLabel = rangeLabel,
            totalSales = activeOrders.sumOf { it.grandTotal },
            totalOrders = scoped.size,
            completedOrders = completedOrders,
            cancelledOrders = cancelledOrders,
            pickupOrders = pickupOrders,
            deliveryOrders = deliveryOrders,
            roomBookings = roomBookings,
            shopSales = shopSales,
            deliverySales = deliverySales,
            roomSales = roomSales,
            cashSales = cashSales,
            upiSales = upiSales,
            verifiedPaidTotal = paidTotal,
            unpaidOrPendingTotal = unpaidTotal,
            productStats = productStats
        )
    }

    fun filterAdminOrders(
        orders: List<CustomerRequest>,
        statusFilter: String?,
        searchQuery: String
    ): List<CustomerRequest> {
        val trimmed = searchQuery.trim()
        val q = trimmed.lowercase()
        val queryDigits = trimmed.filter { it.isDigit() }
        return orders.filter { req ->
            val statusOk = statusFilter == null || req.status == statusFilter
            if (!statusOk) return@filter false
            if (q.isEmpty()) return@filter true

            val phoneDigits = req.contactPhone.filter { it.isDigit() }
            req.customerName.lowercase().contains(q) ||
                req.contactPhone.lowercase().contains(q) ||
                (queryDigits.isNotEmpty() && phoneDigits.contains(queryDigits)) ||
                req.orderNumber.lowercase().contains(q) ||
                (req.deliveryLocality?.lowercase()?.contains(q) == true) ||
                (req.deliveryAddress?.lowercase()?.contains(q) == true)
        }
    }

    fun computeAdminOrderBadgeCounts(
        requests: List<CustomerRequest>,
        newlyArrivedOrderIds: Set<String> = emptySet(),
        latestNewOrderSummary: String? = null
    ): com.example.data.model.AdminOrderBadgeState {
        var pendingShop = 0
        var pendingDelivery = 0
        var pendingRooms = 0
        var pendingFreshen = 0

        val activePendingIds = mutableSetOf<String>()
        requests.forEach { req ->
            if (RequestStatus.fromRaw(req.status) == RequestStatus.PENDING) {
                if (req.id.isNotBlank()) activePendingIds.add(req.id)
                when (RequestCategory.fromRaw(req.category)) {
                    RequestCategory.FOOD_REQUEST -> pendingShop++
                    RequestCategory.FOOD_DELIVERY_REQUEST -> pendingDelivery++
                    RequestCategory.ROOM_REQUEST -> pendingRooms++
                    RequestCategory.FRESHEN_UP_REQUEST -> pendingFreshen++
                }
            }
        }

        val totalPending = pendingShop + pendingDelivery + pendingRooms + pendingFreshen
        val stillPendingArrivals = newlyArrivedOrderIds.filterTo(mutableSetOf()) { it in activePendingIds }

        return com.example.data.model.AdminOrderBadgeState(
            pendingShopOrders = pendingShop,
            pendingDeliveryOrders = pendingDelivery,
            pendingRoomBookings = pendingRooms,
            pendingFreshenUp = pendingFreshen,
            totalPendingNewOrders = totalPending,
            newlyArrivedOrderIds = stillPendingArrivals,
            latestNewOrderSummary = if (stillPendingArrivals.isNotEmpty()) latestNewOrderSummary else null
        )
    }

    fun detectNewlyArrivedPendingOrders(
        incomingRequests: List<CustomerRequest>,
        previouslyKnownIds: Set<String>
    ): List<CustomerRequest> {
        return incomingRequests.filter { req ->
            req.id.isNotBlank() &&
                req.id !in previouslyKnownIds &&
                RequestStatus.fromRaw(req.status) == RequestStatus.PENDING
        }
    }

    fun evaluateProductStockSeverity(product: Product): LowStockSeverity {
        if (!product.trackStock) return LowStockSeverity.HEALTHY
        val qty = product.stockQuantity.coerceAtLeast(0L)
        val threshold = product.lowStockThreshold.coerceAtLeast(0L)
        return when {
            qty <= 0L -> LowStockSeverity.OUT_OF_STOCK
            qty <= threshold -> LowStockSeverity.CRITICAL_LOW
            else -> LowStockSeverity.HEALTHY
        }
    }

    fun evaluateLowStockAlerts(
        products: List<Product>,
        newlyFlaggedProductIds: Set<String> = emptySet()
    ): LowStockAlertSummary {
        val flagged = products.mapNotNull { product ->
            val severity = evaluateProductStockSeverity(product)
            if (severity == LowStockSeverity.HEALTHY) null
            else LowStockAlertItem(
                product = product,
                stockQuantity = product.stockQuantity.coerceAtLeast(0L),
                threshold = product.lowStockThreshold.coerceAtLeast(0L),
                severity = severity
            )
        }.sortedWith(
            compareBy<LowStockAlertItem> {
                if (it.severity == LowStockSeverity.OUT_OF_STOCK) 0 else 1
            }
                .thenBy { it.stockQuantity }
                .thenBy { it.product.sortOrder }
                .thenBy { it.product.name }
        )

        val activeFlaggedIds = flagged.mapNotNull { it.product.id.takeIf(String::isNotBlank) }.toSet()
        val stillNew = newlyFlaggedProductIds.filterTo(mutableSetOf()) { it in activeFlaggedIds }
        val outCount = flagged.count { it.severity == LowStockSeverity.OUT_OF_STOCK }
        val lowCount = flagged.count { it.severity == LowStockSeverity.CRITICAL_LOW }

        return LowStockAlertSummary(
            flaggedItems = flagged,
            criticalLowCount = lowCount,
            outOfStockCount = outCount,
            totalFlaggedCount = flagged.size,
            newlyFlaggedProductIds = stillNew
        )
    }

    fun detectNewlyFlaggedLowStockProducts(
        products: List<Product>,
        previouslyFlaggedIds: Set<String>
    ): List<LowStockAlertItem> {
        return evaluateLowStockAlerts(products).flaggedItems.filter { item ->
            item.product.id.isNotBlank() && item.product.id !in previouslyFlaggedIds
        }
    }

    fun computeStockDeductionsOnOrderApproval(
        previousStatus: String?,
        newStatus: RequestStatus,
        order: CustomerRequest?,
        products: List<Product>
    ): List<Product> {
        if (order == null) return emptyList()
        if (RequestStatus.fromRaw(previousStatus) != RequestStatus.PENDING) return emptyList()
        if (newStatus == RequestStatus.PENDING || newStatus == RequestStatus.REJECTED) return emptyList()

        val cat = RequestCategory.fromRaw(order.category)
        if (cat != RequestCategory.FOOD_REQUEST && cat != RequestCategory.FOOD_DELIVERY_REQUEST) {
            return emptyList()
        }

        val orderedByName = mutableMapOf<String, Long>()
        order.items.forEach { item ->
            val key = item.name.trim().lowercase()
            if (key.isNotEmpty() && item.quantity > 0L) {
                orderedByName[key] = (orderedByName[key] ?: 0L) + item.quantity
            }
        }
        if (orderedByName.isEmpty()) return emptyList()

        return products.mapNotNull { product ->
            if (!product.trackStock) return@mapNotNull null
            val qtyOrdered = orderedByName[product.name.trim().lowercase()] ?: return@mapNotNull null
            val nextQty = (product.stockQuantity - qtyOrdered).coerceAtLeast(0L)
            if (nextQty == product.stockQuantity && product.available == (nextQty > 0L)) {
                null
            } else {
                product.copy(
                    stockQuantity = nextQty,
                    available = nextQty > 0L
                )
            }
        }
    }
}

object ExternalIntentHelper {

    fun sanitizePhoneForWhatsApp(rawNumber: String): String =
        DomainCalculators.normalizeIndianMobile(rawNumber)

    /**
     * Opens direct WhatsApp target ONLY if a number is explicitly passed (e.g. Owner Admin view
     * or owner-published shop WhatsApp). Customer receipts pass blank when private so the owner's
     * personal mobile number is never leaked, using Android's generic share sheet instead.
     */
    fun launchWhatsAppShare(
        context: Context,
        message: String,
        targetWhatsAppNumber: String = ""
    ) {
        val digits = sanitizePhoneForWhatsApp(targetWhatsAppNumber)
        if (digits.length >= 10) {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val url = "https://wa.me/$digits?text=$encoded"
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                context.startActivity(viewIntent)
                return
            } catch (_: Exception) {
                // Fall back to share sheet below
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Order Bill")
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyToClipboard(context, "MARUTI DHAM Order Bill", message)
            Toast.makeText(context, "No share app available. Bill copied to clipboard.", Toast.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun launchGoogleMapsDirections(context: Context, mapsQuery: String) {
        val encoded = URLEncoder.encode(mapsQuery, "UTF-8")
        val geoUri = Uri.parse("geo:0,0?q=$encoded")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
        try {
            context.startActivity(geoIntent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (_: Exception) {
                Toast.makeText(context, "Maps app not found on device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchPhoneDialer(context: Context, phone: String) {
        val cleaned = phone.trim()
        if (cleaned.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
        }
    }
}
