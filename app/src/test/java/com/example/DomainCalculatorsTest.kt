package com.example

import com.example.data.model.CustomerRequest
import com.example.data.model.FulfillmentType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.ReportPeriod
import com.example.data.model.RequestCategory
import com.example.data.model.RequestLineItem
import com.example.data.model.RequestStatus
import com.example.data.model.RoomType
import com.example.data.model.ShopControlMode
import com.example.data.model.ShopSettings
import com.example.data.model.ThemePreset
import com.example.data.model.UserProfile
import com.example.data.model.getAppStrings
import com.example.util.DefaultCatalogData
import com.example.util.DomainCalculators
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class DomainCalculatorsTest {

    @Test
    fun `initial food menu matches exact requested prices`() {
        val products = DefaultCatalogData.initialProducts.associateBy { it.id }
        assertEquals(listOf(10L, 20L), products.getValue("tea").priceOptions.map { it.price })
        assertEquals(listOf(20L, 30L), products.getValue("poha").priceOptions.map { it.price })
        assertEquals(listOf(50L), products.getValue("maggi").priceOptions.map { it.price })
        assertEquals(listOf(50L), products.getValue("chole_kulche").priceOptions.map { it.price })
        assertTrue(DefaultCatalogData.suggestedMenuTemplates.none { it.visible || it.available })
    }

    @Test
    fun `food total calculates multiple items and quantities accurately`() {
        val items = listOf(
            RequestLineItem(name = "Desi Tea", variantLabel = "Regular", unitPrice = 10L, quantity = 3L, lineTotal = 30L),
            RequestLineItem(name = "Poha", variantLabel = "Full", unitPrice = 30L, quantity = 2L, lineTotal = 60L),
            RequestLineItem(name = "Maggi", variantLabel = "Standard", unitPrice = 50L, quantity = 1L, lineTotal = 50L)
        )
        assertEquals(140L, DomainCalculators.calculateFoodTotal(items))
    }

    @Test
    fun `room rates match AC and Non-AC defaults and leave 1-5h unconfigured until owner sets`() {
        val ac = DefaultCatalogData.initialRoomConfigs.first { it.roomType == RoomType.AC.name }
        val nonAc = DefaultCatalogData.initialRoomConfigs.first { it.roomType == RoomType.NON_AC.name }

        assertEquals(1200L, DomainCalculators.resolveRoomRateForDuration(ac, 6))
        assertEquals(2000L, DomainCalculators.resolveRoomRateForDuration(ac, 12))
        assertEquals(800L, DomainCalculators.resolveRoomRateForDuration(nonAc, 6))
        assertEquals(1500L, DomainCalculators.resolveRoomRateForDuration(nonAc, 12))

        for (hr in 1..5) {
            assertNull(DomainCalculators.resolveRoomRateForDuration(ac, hr))
            assertNull(DomainCalculators.resolveRoomRateForDuration(nonAc, hr))
        }

        val ownerConfiguredAc = ac.copy(rate2Hours = 500L)
        assertEquals(500L, DomainCalculators.resolveRoomRateForDuration(ownerConfiguredAc, 2))
        assertEquals(1000L, DomainCalculators.calculateRoomTotal(500L, 2))
    }

    @Test
    fun `freshen up per-person calculation matches ₹50 and ₹100 tiers`() {
        val onlyFresh = DefaultCatalogData.initialFreshenUpConfigs.first { it.optionKey == "ONLY_FRESH" }
        val bathFresh = DefaultCatalogData.initialFreshenUpConfigs.first { it.optionKey == "BATH_FRESH" }

        assertEquals(50L, onlyFresh.pricePerPerson)
        assertEquals(100L, bathFresh.pricePerPerson)
        assertEquals(200L, DomainCalculators.calculateFreshenUpTotal(onlyFresh.pricePerPerson, 4))
        assertEquals(300L, DomainCalculators.calculateFreshenUpTotal(bathFresh.pricePerPerson, 3))
    }

    @Test
    fun `delivery area validation allows Khatu Shyam Ji and blocks outside areas or paused delivery`() {
        val settings = DefaultCatalogData.defaultDeliverySettings

        val validResult = DomainCalculators.validateDeliveryArea(
            address = "Shyam Dharamshala, Near Manda Chauraha, Khatu Shyam Ji",
            selectedLocality = "Manda Chauraha",
            settings = settings
        )
        assertTrue(validResult.isValid)

        val outsideCity = DomainCalculators.validateDeliveryArea(
            address = "Station Road, Sikar",
            selectedLocality = "Other",
            settings = settings
        )
        assertFalse(outsideCity.isValid)

        val paused = settings.copy(deliveryEnabled = false)
        assertFalse(
            DomainCalculators.validateDeliveryArea(
                address = "Toran Dwar, Khatu Shyam Ji",
                selectedLocality = "Toran Dwar",
                settings = paused
            ).isValid
        )
    }

    @Test
    fun `shop open evaluation supports all 4 IST modes accurately`() {
        val base = DefaultCatalogData.defaultShopSettings.copy(
            openingTime = "7:00 AM",
            closingTime = "10:00 PM"
        )
        val cal11Am = Calendar.getInstance(DomainCalculators.istTimeZone).apply {
            set(2026, Calendar.MARCH, 15, 11, 0, 0)
        }.timeInMillis
        val cal11Pm = Calendar.getInstance(DomainCalculators.istTimeZone).apply {
            set(2026, Calendar.MARCH, 15, 23, 0, 0)
        }.timeInMillis

        assertTrue(DomainCalculators.evaluateShopOpenStatus(base.copy(shopControlMode = ShopControlMode.OPEN_NOW.name), cal11Pm).isOpen)
        assertFalse(DomainCalculators.evaluateShopOpenStatus(base.copy(shopControlMode = ShopControlMode.CLOSED_NOW.name), cal11Am).isOpen)

        assertTrue(DomainCalculators.evaluateShopOpenStatus(base.copy(shopControlMode = ShopControlMode.OPEN_AT_SCHEDULED.name), cal11Am).isOpen)
        assertFalse(DomainCalculators.evaluateShopOpenStatus(base.copy(shopControlMode = ShopControlMode.OPEN_AT_SCHEDULED.name), cal11Pm).isOpen)

        assertTrue(DomainCalculators.evaluateShopOpenStatus(base.copy(shopControlMode = ShopControlMode.CLOSE_AT_SCHEDULED.name), cal11Am).isOpen)
        assertFalse(DomainCalculators.evaluateShopOpenStatus(base.copy(shopControlMode = ShopControlMode.CLOSE_AT_SCHEDULED.name), cal11Pm).isOpen)
    }

    @Test
    fun `indian mobile validation and normalization work reliably`() {
        assertTrue(DomainCalculators.isValidMobileNumber("7850068566"))
        assertTrue(DomainCalculators.isValidMobileNumber("+91 7850068566"))
        assertEquals("917850068566", DomainCalculators.normalizeIndianMobile("7850068566"))
        assertEquals("917850068566", DomainCalculators.normalizeIndianMobile("07850068566"))
        assertFalse(DomainCalculators.isValidMobileNumber("12345"))
    }

    @Test
    fun `receipt and whatsapp formatters output order ID, IST time, and itemized total`() {
        val shop = DefaultCatalogData.defaultShopSettings
        val ts = Timestamp(Date(1773552600000L))
        val foodReq = CustomerRequest(
            id = "req1",
            userId = "u1",
            orderNumber = "MD-000001",
            category = RequestCategory.FOOD_DELIVERY_REQUEST.name,
            customerName = "Ramesh",
            contactPhone = "9876500000",
            items = listOf(RequestLineItem("Chole Kulche", "Full Plate", 50L, 2L, 100L)),
            foodTotal = 100L,
            grandTotal = 100L,
            expectedTime = "01:00 PM",
            fulfillmentType = FulfillmentType.DELIVERY.name,
            paymentMethod = PaymentMethod.CASH.name,
            paymentStatus = PaymentStatus.UNPAID.name,
            deliveryLocality = "Toran Dwar",
            deliveryAddress = "Gali 2, Toran Dwar, Khatu Shyam Ji",
            createdAt = ts
        )
        val msg = DomainCalculators.formatWhatsAppRequestMessage(foodReq, shop)
        assertTrue(msg.contains("MD-000001"))
        assertTrue(msg.contains("NEW DELIVERY ORDER"))
        assertTrue(msg.contains("ITEM | QTY | RATE | TOTAL"))
        assertTrue(msg.contains("Chole Kulche"))
        assertTrue(msg.contains("₹100"))
        assertTrue(msg.contains("IST"))

        val receipt = DomainCalculators.formatReceiptText(foodReq, shop)
        assertTrue(receipt.contains("MARUTI DHAM"))
        assertTrue(receipt.contains("DIGITAL ORDER RECEIPT"))
        assertTrue(receipt.contains("ITEM | QTY | RATE | TOTAL"))
        assertTrue(receipt.contains("MD-000001"))
        assertTrue(receipt.contains("THANK YOU FOR VISITING"))
    }

    @Test
    fun `grand total and formatted receipt include delivery and packaging charges`() {
        val shop = DefaultCatalogData.defaultShopSettings
        val grand = DomainCalculators.calculateGrandTotal(
            foodTotal = 120L,
            roomTotal = 0L,
            freshenUpTotal = 0L,
            deliveryCharge = 20L,
            packagingCharge = 10L
        )
        assertEquals(150L, grand)

        val req = CustomerRequest(
            id = "req_chg",
            userId = "u1",
            orderNumber = "MD-000007",
            category = RequestCategory.FOOD_DELIVERY_REQUEST.name,
            customerName = "Suresh",
            contactPhone = "9876500001",
            items = listOf(RequestLineItem("Chole Kulche", "Full", 50L, 2L, 100L), RequestLineItem("Desi Tea", "Special", 20L, 1L, 20L)),
            foodTotal = 120L,
            deliveryCharge = 20L,
            packagingCharge = 10L,
            grandTotal = grand,
            expectedTime = "01:15 PM",
            fulfillmentType = FulfillmentType.DELIVERY.name
        )
        val receipt = DomainCalculators.formatReceiptText(req, shop)
        assertTrue(receipt.contains("Subtotal:          ₹120"))
        assertTrue(receipt.contains("Delivery Charge:   ₹20"))
        assertTrue(receipt.contains("Packaging Charge:  ₹10"))
        assertTrue(receipt.contains("Grand Total:       ₹150"))

        val wa = DomainCalculators.formatWhatsAppRequestMessage(req, shop)
        assertTrue(wa.contains("Subtotal: ₹120"))
        assertTrue(wa.contains("Delivery Charge: ₹20"))
        assertTrue(wa.contains("Packaging Charge: ₹10"))
        assertTrue(wa.contains("Grand Total: ₹150"))
    }

    @Test
    fun `owner dashboard stats and sales reports aggregate sales, payments, fulfillment counts, and product stats`() {
        val now = System.currentTimeMillis()
        val tsNow = Timestamp(Date(now))
        val reqs = listOf(
            CustomerRequest(
                id = "r1",
                userId = "u1",
                orderNumber = "MD-000001",
                category = RequestCategory.FOOD_REQUEST.name,
                fulfillmentType = FulfillmentType.PICKUP.name,
                status = RequestStatus.COMPLETED.name,
                paymentMethod = PaymentMethod.CASH.name,
                paymentStatus = PaymentStatus.PAID.name,
                items = listOf(RequestLineItem("Desi Tea", "Regular", 10L, 3L, 30L)),
                grandTotal = 30L,
                createdAt = tsNow
            ),
            CustomerRequest(
                id = "r2",
                userId = "u2",
                orderNumber = "MD-000002",
                category = RequestCategory.FOOD_DELIVERY_REQUEST.name,
                fulfillmentType = FulfillmentType.DELIVERY.name,
                status = RequestStatus.PENDING.name,
                paymentMethod = PaymentMethod.UPI.name,
                paymentStatus = PaymentStatus.UPI_PENDING_VERIFICATION.name,
                items = listOf(RequestLineItem("Maggi", "Standard", 50L, 2L, 100L)),
                grandTotal = 100L,
                createdAt = tsNow
            ),
            CustomerRequest(
                id = "r3",
                userId = "u1",
                orderNumber = "MD-000003",
                category = RequestCategory.ROOM_REQUEST.name,
                status = RequestStatus.REJECTED.name,
                grandTotal = 50L,
                createdAt = tsNow
            )
        )
        val users = listOf(
            UserProfile(userId = "u1", displayName = "A", phone = "9876543210", createdAt = tsNow),
            UserProfile(userId = "u2", displayName = "B", phone = "9876543211", createdAt = tsNow)
        )

        val stats = DomainCalculators.computeOwnerDashboardStats(reqs, users, now)
        assertEquals(130L, stats.todaysSales)
        assertEquals(3, stats.totalOrders)
        assertEquals(1, stats.pendingOrders)
        assertEquals(1, stats.completedOrders)
        assertEquals(1, stats.cancelledOrders)
        assertEquals(2, stats.totalCustomers)
        assertEquals(2, stats.newSignupsToday)
        assertEquals(1, stats.shopOrdersCount)
        assertEquals(1, stats.deliveryOrdersCount)
        assertEquals(1, stats.roomBookingsCount)
        assertEquals(0, stats.freshenUpCount)
        assertEquals(30L, stats.shopOrdersSales)
        assertEquals(100L, stats.deliveryOrdersSales)
        assertEquals("Desi Tea", stats.mostPopularProductName)
        assertEquals(3L, stats.mostPopularProductUnitsSold)
        assertEquals(30L, stats.mostPopularProductRevenue)

        val emptyStats = DomainCalculators.computeOwnerDashboardStats(emptyList(), emptyList(), now)
        assertNull(emptyStats.mostPopularProductName)
        assertEquals(0L, emptyStats.mostPopularProductUnitsSold)

        val report = DomainCalculators.generateSalesReport(reqs, ReportPeriod.DAILY, now)
        assertEquals(130L, report.totalSales)
        assertEquals(30L, report.shopSales)
        assertEquals(100L, report.deliverySales)
        assertEquals(0L, report.roomSales)
        assertEquals(30L, report.cashSales)
        assertEquals(100L, report.upiSales)
        assertEquals(30L, report.verifiedPaidTotal)
        assertEquals(100L, report.unpaidOrPendingTotal)
        assertEquals(1, report.pickupOrders)
        assertEquals(1, report.deliveryOrders)
        assertEquals(1, report.roomBookings)
        assertEquals("Desi Tea", report.productStats.first().productName)
    }

    @Test
    fun `cash only default and theme presets are configured properly`() {
        assertTrue(DefaultCatalogData.defaultShopSettings.cashEnabled)
        assertFalse(DefaultCatalogData.defaultShopSettings.upiEnabled)
        assertTrue(DefaultCatalogData.defaultShopSettings.contactEmail.isBlank())
        assertTrue(DefaultCatalogData.defaultShopSettings.contactPhone.isBlank())

        val navy = DefaultCatalogData.themePresetFor(ThemePreset.NAVY_ROYAL_GOLD)
        val whiteGold = DefaultCatalogData.themePresetFor(ThemePreset.WHITE_GOLD)
        val purple = DefaultCatalogData.themePresetFor(ThemePreset.MODERN_ROYAL_PURPLE)
        assertEquals("#07122B", navy.backgroundColorHex)
        assertEquals("#FFFFFF", whiteGold.backgroundColorHex)
        assertEquals("#1A1033", purple.backgroundColorHex)
        assertEquals(DefaultCatalogData.menuCategories, DefaultCatalogData.defaultSiteConfig.categories)
    }

    @Test
    fun `bilingual strings keep exact 5 bottom navigation tab names`() {
        val en = getAppStrings("en")
        val hi = getAppStrings("hi")
        for (s in listOf(en, hi)) {
            assertEquals("Home", s.navHome)
            assertEquals("Food Order", s.navFoodOrder)
            assertEquals("Rooms & Freshen Up", s.navRoomsFreshenUp)
            assertEquals("Food Delivery", s.navFoodDelivery)
            assertEquals("Settings", s.navSettings)
        }
        assertTrue(hi.brandTitle.contains("मारुति"))
    }

    @Test
    fun `filterAdminOrders filters shop and delivery orders by customer name or mobile number`() {
        val orders = listOf(
            CustomerRequest(
                id = "o1",
                orderNumber = "MD-000010",
                category = RequestCategory.FOOD_REQUEST.name,
                customerName = "Ramesh Sharma",
                contactPhone = "9876543210",
                status = RequestStatus.PENDING.name
            ),
            CustomerRequest(
                id = "o2",
                orderNumber = "MD-000011",
                category = RequestCategory.FOOD_REQUEST.name,
                customerName = "Sita Devi",
                contactPhone = "+91 91234-56789",
                status = RequestStatus.COMPLETED.name
            ),
            CustomerRequest(
                id = "o3",
                orderNumber = "MD-000012",
                category = RequestCategory.FOOD_DELIVERY_REQUEST.name,
                customerName = "Mohan Verma",
                contactPhone = "9988776655",
                deliveryLocality = "Toran Dwar",
                status = RequestStatus.PENDING.name
            )
        )

        // Filter by partial customer name (case-insensitive)
        val byName = DomainCalculators.filterAdminOrders(orders, statusFilter = null, searchQuery = "ramesh")
        assertEquals(listOf("o1"), byName.map { it.id })

        // Filter by mobile number digits ignoring formatting/spaces
        val byPhoneDigits = DomainCalculators.filterAdminOrders(orders, statusFilter = null, searchQuery = "9123456789")
        assertEquals(listOf("o2"), byPhoneDigits.map { it.id })

        // Filter by partial mobile + status filter
        val byPhoneAndStatus = DomainCalculators.filterAdminOrders(orders, statusFilter = RequestStatus.PENDING.name, searchQuery = "9988")
        assertEquals(listOf("o3"), byPhoneAndStatus.map { it.id })

        // Filter by all 5 workflow statuses cleanly
        val statusOrders = listOf(
            CustomerRequest(id = "s1", status = RequestStatus.PENDING.name),
            CustomerRequest(id = "s2", status = RequestStatus.CONFIRMED.name),
            CustomerRequest(id = "s3", status = RequestStatus.PREPARING.name),
            CustomerRequest(id = "s4", status = RequestStatus.READY.name),
            CustomerRequest(id = "s5", status = RequestStatus.COMPLETED.name)
        )
        listOf(
            RequestStatus.PENDING.name to "s1",
            RequestStatus.CONFIRMED.name to "s2",
            RequestStatus.PREPARING.name to "s3",
            RequestStatus.READY.name to "s4",
            RequestStatus.COMPLETED.name to "s5"
        ).forEach { (status, expectedId) ->
            val res = DomainCalculators.filterAdminOrders(statusOrders, statusFilter = status, searchQuery = "")
            assertEquals(listOf(expectedId), res.map { it.id })
        }

        // Non-matching query returns empty list
        val noMatch = DomainCalculators.filterAdminOrders(orders, statusFilter = null, searchQuery = "0000000000")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `generateSalesReport filters revenue and order totals within custom start and end dates`() {
        fun istTimestamp(year: Int, month: Int, day: Int, hour: Int): Timestamp {
            val cal = Calendar.getInstance(DomainCalculators.istTimeZone).apply {
                set(year, month, day, hour, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return Timestamp(Date(cal.timeInMillis))
        }

        fun utcPickerMillis(year: Int, month: Int, day: Int): Long {
            return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val reqs = listOf(
            CustomerRequest(
                id = "d1",
                status = RequestStatus.COMPLETED.name,
                grandTotal = 120L,
                createdAt = istTimestamp(2026, Calendar.NOVEMBER, 10, 10)
            ),
            CustomerRequest(
                id = "d2",
                status = RequestStatus.COMPLETED.name,
                grandTotal = 250L,
                createdAt = istTimestamp(2026, Calendar.NOVEMBER, 15, 14)
            ),
            CustomerRequest(
                id = "d3",
                status = RequestStatus.PENDING.name,
                grandTotal = 80L,
                createdAt = istTimestamp(2026, Calendar.NOVEMBER, 18, 22)
            ),
            CustomerRequest(
                id = "d4",
                status = RequestStatus.REJECTED.name,
                grandTotal = 999L,
                createdAt = istTimestamp(2026, Calendar.NOVEMBER, 16, 12)
            ),
            CustomerRequest(
                id = "d5",
                status = RequestStatus.COMPLETED.name,
                grandTotal = 400L,
                createdAt = istTimestamp(2026, Calendar.NOVEMBER, 22, 9)
            )
        )

        val startUtc = utcPickerMillis(2026, Calendar.NOVEMBER, 15)
        val endUtc = utcPickerMillis(2026, Calendar.NOVEMBER, 18)

        val report = DomainCalculators.generateSalesReport(
            requests = reqs,
            period = ReportPeriod.CUSTOM_RANGE,
            customStartDateMillis = startUtc,
            customEndDateMillis = endUtc
        )

        assertEquals(ReportPeriod.CUSTOM_RANGE, report.period)
        assertEquals(330L, report.totalSales)
        assertEquals(3, report.totalOrders)
        assertEquals(1, report.completedOrders)
        assertEquals(1, report.cancelledOrders)
        assertTrue(report.dateRangeLabel.contains("15/11/2026"))
        assertTrue(report.dateRangeLabel.contains("18/11/2026"))
    }

    @Test
    fun `computeAdminOrderBadgeCounts and detectNewlyArrivedPendingOrders update per-category counts in real time`() {
        val initial = listOf(
            CustomerRequest(id = "r1", category = RequestCategory.FOOD_REQUEST.name, status = RequestStatus.PENDING.name),
            CustomerRequest(id = "r2", category = RequestCategory.FOOD_DELIVERY_REQUEST.name, status = RequestStatus.CONFIRMED.name)
        )
        val initialBadges = DomainCalculators.computeAdminOrderBadgeCounts(initial)
        assertEquals(1, initialBadges.pendingShopOrders)
        assertEquals(0, initialBadges.pendingDeliveryOrders)
        assertEquals(1, initialBadges.totalPendingNewOrders)
        assertFalse(initialBadges.hasNewlyArrived)

        val updatedSnapshot = initial + listOf(
            CustomerRequest(id = "r3", orderNumber = "MD-000020", customerName = "Anil", grandTotal = 90L, category = RequestCategory.FOOD_DELIVERY_REQUEST.name, status = RequestStatus.PENDING.name),
            CustomerRequest(id = "r4", orderNumber = "MD-000021", customerName = "Pooja", grandTotal = 800L, category = RequestCategory.ROOM_REQUEST.name, status = RequestStatus.PENDING.name),
            CustomerRequest(id = "r5", category = RequestCategory.FRESHEN_UP_REQUEST.name, status = RequestStatus.COMPLETED.name)
        )
        val newPending = DomainCalculators.detectNewlyArrivedPendingOrders(
            incomingRequests = updatedSnapshot,
            previouslyKnownIds = setOf("r1", "r2")
        )
        assertEquals(listOf("r3", "r4"), newPending.map { it.id })

        val liveBadges = DomainCalculators.computeAdminOrderBadgeCounts(
            requests = updatedSnapshot,
            newlyArrivedOrderIds = newPending.map { it.id }.toSet(),
            latestNewOrderSummary = "MD-000020 • Anil (₹90)"
        )
        assertEquals(1, liveBadges.pendingShopOrders)
        assertEquals(1, liveBadges.pendingDeliveryOrders)
        assertEquals(1, liveBadges.pendingRoomBookings)
        assertEquals(0, liveBadges.pendingFreshenUp)
        assertEquals(3, liveBadges.totalPendingNewOrders)
        assertTrue(liveBadges.hasNewlyArrived)
        assertEquals("MD-000020 • Anil (₹90)", liveBadges.latestNewOrderSummary)

        // When r3 & r4 move out of PENDING, newlyArrivedOrderIds automatically prunes them
        val processedSnapshot = updatedSnapshot.map { req ->
            if (req.id == "r3" || req.id == "r4") req.copy(status = RequestStatus.CONFIRMED.name) else req
        }
        val afterProcessBadges = DomainCalculators.computeAdminOrderBadgeCounts(
            requests = processedSnapshot,
            newlyArrivedOrderIds = setOf("r3", "r4"),
            latestNewOrderSummary = "MD-000020 • Anil (₹90)"
        )
        assertEquals(1, afterProcessBadges.totalPendingNewOrders)
        assertFalse(afterProcessBadges.hasNewlyArrived)
        assertNull(afterProcessBadges.latestNewOrderSummary)
    }
}
