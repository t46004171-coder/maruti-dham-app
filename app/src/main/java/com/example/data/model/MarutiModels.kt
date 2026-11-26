package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class RequestCategory {
    FOOD_REQUEST,
    ROOM_REQUEST,
    FRESHEN_UP_REQUEST,
    FOOD_DELIVERY_REQUEST;

    companion object {
        fun fromRaw(raw: String?): RequestCategory =
            entries.firstOrNull { it.name == raw } ?: FOOD_REQUEST
    }
}

enum class RequestStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    COMPLETED,
    REJECTED;

    companion object {
        fun fromRaw(raw: String?): RequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class ShopControlMode {
    OPEN_NOW,
    CLOSED_NOW,
    OPEN_AT_SCHEDULED,
    CLOSE_AT_SCHEDULED;

    companion object {
        fun fromRaw(raw: String?): ShopControlMode =
            entries.firstOrNull { it.name == raw } ?: OPEN_NOW
    }
}

enum class FulfillmentType {
    PICKUP,
    DELIVERY;

    companion object {
        fun fromRaw(raw: String?): FulfillmentType =
            entries.firstOrNull { it.name == raw } ?: PICKUP
    }
}

enum class PaymentMethod {
    CASH,
    UPI;

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class PaymentStatus {
    UNPAID,
    UPI_PENDING_VERIFICATION,
    PAID,
    REFUNDED;

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: UNPAID
    }
}

enum class WhatsAppDeliveryStatus {
    PENDING_SERVER,
    SENT,
    FAILED,
    MANUAL_SHARED,
    NOT_CONFIGURED;

    companion object {
        fun fromRaw(raw: String?): WhatsAppDeliveryStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_SERVER
    }
}

enum class RoomType {
    AC,
    NON_AC;

    companion object {
        fun fromRaw(raw: String?): RoomType =
            entries.firstOrNull { it.name == raw } ?: AC
    }
}

enum class RoomStatusLabel {
    AVAILABLE,
    FULLY_BOOKED,
    CLOSED;

    companion object {
        fun fromRaw(raw: String?): RoomStatusLabel =
            entries.firstOrNull { it.name == raw } ?: AVAILABLE
    }
}

enum class FreshenUpOption {
    ONLY_FRESH,
    BATH_FRESH;

    companion object {
        fun fromRaw(raw: String?): FreshenUpOption =
            entries.firstOrNull { it.name == raw } ?: ONLY_FRESH
    }
}

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM_RANGE
}

enum class ThemePreset(val displayName: String) {
    NAVY_ROYAL_GOLD("Navy Blue + Royal Gold"),
    WHITE_GOLD("White + Gold"),
    PREMIUM_DARK("Premium Dark"),
    CREAM_ROYAL_BLUE("Cream + Royal Blue"),
    MODERN_ROYAL_PURPLE("Modern Royal Purple"),
    CUSTOM("Custom Theme");

    companion object {
        fun fromRaw(raw: String?): ThemePreset =
            entries.firstOrNull { it.name == raw } ?: NAVY_ROYAL_GOLD
    }
}

enum class ThemeFontStyle(val displayName: String) {
    DEVOTIONAL_SERIF("Devotional Royal"),
    MODERN_SANS("Modern Clean"),
    CLASSIC_ROYAL("Classic Bold");

    companion object {
        fun fromRaw(raw: String?): ThemeFontStyle =
            entries.firstOrNull { it.name == raw } ?: DEVOTIONAL_SERIF
    }
}

enum class ThemeLayoutVariant(val displayName: String) {
    DEVOTIONAL_CARD("Devotional Cards"),
    CLEAN_MINIMAL("Clean Minimal"),
    COMPACT_GRID("Compact Stall View");

    companion object {
        fun fromRaw(raw: String?): ThemeLayoutVariant =
            entries.firstOrNull { it.name == raw } ?: DEVOTIONAL_CARD
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val preferredLanguage: String = "en",
    val registrationCompleted: Boolean = false,
    val preferredFulfillment: String = FulfillmentType.PICKUP.name,
    val defaultAddress: String = "",
    val defaultLocality: String = "",
    val consentAccepted: Boolean = true,
    val deletionRequested: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShopSettings(
    @DocumentId val id: String = "main",
    val shopName: String = "MARUTI DHAM",
    val ownerName: String = "Tarun Kumar",
    val address: String = "Near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
    val mapsQuery: String = "Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
    val shopOpen: Boolean = true,
    val shopControlMode: String = ShopControlMode.OPEN_NOW.name,
    val openingTime: String = "7:00 AM",
    val closingTime: String = "10:00 PM",
    // Public optional storefront contact fields (blank by default so owner's personal phone/Gmail stay private)
    val whatsappNumber: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val upiId: String = "",
    val cashEnabled: Boolean = true,
    val upiEnabled: Boolean = false,
    val whatsappWebhookUrl: String = "",
    val aboutText: String = "MARUTI DHAM (Owner: Tarun Kumar) is a humble devotional food stall near Manda Chauraha, Khatu Shyam Ji, offering fresh pure-vegetarian snacks, tea, refreshments, rest rooms, freshen-up facilities, and local delivery.",
    val privacyPolicy: String = "MARUTI DHAM collects your name, mandatory mobile number, optional email, and order details strictly to prepare food orders, manage customer order history, and deliver within Khatu Shyam Ji. Customer records are private and accessible only to the authorized shop owner.",
    val termsText: String = "Orders are accepted during configured shop hours. Pay via Cash Only (or verified UPI when enabled by owner). Food delivery is restricted strictly to configured Khatu Shyam Ji localities.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ThemeConfig(
    val preset: String = ThemePreset.NAVY_ROYAL_GOLD.name,
    val primaryColorHex: String = "#E5B94C",
    val backgroundColorHex: String = "#07122B",
    val surfaceColorHex: String = "#142652",
    val accentColorHex: String = "#F7D774",
    val textColorHex: String = "#F8F6F0",
    val fontStyle: String = ThemeFontStyle.DEVOTIONAL_SERIF.name,
    val layoutVariant: String = ThemeLayoutVariant.DEVOTIONAL_CARD.name
)

@Keep
@IgnoreExtraProperties
data class SiteConfig(
    @DocumentId val id: String = "main",
    val logoUrl: String = "",
    val heroBannerUrl: String = "",
    val showAnnouncementsSection: Boolean = true,
    val showDailySpecialsSection: Boolean = true,
    val showRoomsSection: Boolean = true,
    val showDeliverySection: Boolean = true,
    val categories: List<String> = emptyList(),
    val draftTheme: ThemeConfig = ThemeConfig(),
    val publishedTheme: ThemeConfig = ThemeConfig(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Private owner-only notification & webhook configuration stored at /owner_private_settings/main.
 * Customers cannot read or list this document.
 */
@Keep
@IgnoreExtraProperties
data class OwnerPrivateSettings(
    @DocumentId val id: String = "main",
    val ownerWhatsAppNumber: String = "+91 7850068566",
    val whatsappWebhookUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val active: Boolean = true,
    val dailySpecial: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PriceOption(
    val label: String = "Regular",
    val price: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameHindi: String = "",
    val description: String = "",
    val category: String = "Tea & Drinks",
    val priceOptions: List<PriceOption> = emptyList(),
    val imageUrl: String = "",
    val localImageKey: String = "food_platter",
    val available: Boolean = true,
    val visible: Boolean = true,
    val foodOrderEnabled: Boolean = true,
    val deliveryEnabled: Boolean = true,
    val stockQuantity: Long = 20L,
    val lowStockThreshold: Long = 5L,
    val trackStock: Boolean = false,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomConfig(
    @DocumentId val id: String = "",
    val roomType: String = RoomType.AC.name,
    val title: String = "",
    val titleHindi: String = "",
    val available: Boolean = true,
    val statusLabel: String = RoomStatusLabel.AVAILABLE.name,
    val availableCount: Long = 0L,
    val imageUrl: String = "",
    val rate6Hours: Long = 0L,
    val rate12Hours: Long = 0L,
    val rate1Hour: Long? = null,
    val rate2Hours: Long? = null,
    val rate3Hours: Long? = null,
    val rate4Hours: Long? = null,
    val rate5Hours: Long? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FreshenUpConfig(
    @DocumentId val id: String = "",
    val optionKey: String = FreshenUpOption.ONLY_FRESH.name,
    val title: String = "",
    val titleHindi: String = "",
    val pricePerPerson: Long = 0L,
    val available: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliverySettings(
    @DocumentId val id: String = "main",
    val deliveryEnabled: Boolean = true,
    val pickupEnabled: Boolean = true,
    val deliveryCharge: Long = 0L,
    val packagingCharge: Long = 0L,
    val serviceAreaName: String = "Khatu Shyam Ji",
    val allowedLocalities: List<String> = listOf("Khatu Shyam Ji", "Manda Chauraha"),
    val minimumOrderAmount: Long = 0L,
    val deliveryNote: String = "Delivery available ONLY within Khatu Shyam Ji. Pay by Cash or verified UPI.",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RequestLineItem(
    val name: String = "",
    val variantLabel: String = "",
    val unitPrice: Long = 0L,
    val quantity: Long = 0L,
    val lineTotal: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class CustomerRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val category: String = RequestCategory.FOOD_REQUEST.name,
    val customerName: String = "",
    val contactPhone: String = "",
    val customerEmail: String? = null,
    val items: List<RequestLineItem> = emptyList(),
    val foodTotal: Long = 0L,
    val roomTotal: Long = 0L,
    val freshenUpTotal: Long = 0L,
    val deliveryCharge: Long = 0L,
    val packagingCharge: Long = 0L,
    val grandTotal: Long = 0L,
    val expectedTime: String = "",
    val fulfillmentType: String = FulfillmentType.PICKUP.name,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val upiTransactionRef: String? = null,
    val whatsappStatus: String = WhatsAppDeliveryStatus.PENDING_SERVER.name,
    val whatsappError: String? = null,
    val whatsappRetryCount: Long = 0L,
    val roomType: String? = null,
    val numberOfRooms: Long? = null,
    val numberOfPeople: Long? = null,
    val roomDurationHours: Long? = null,
    val roomRateConfigured: Boolean? = null,
    val freshenUpOption: String? = null,
    val deliveryAddress: String? = null,
    val deliveryLocality: String? = null,
    val notes: String? = null,
    val status: String = RequestStatus.PENDING.name,
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityLogEntry(
    @DocumentId val id: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val performedByEmail: String = "",
    val performedByUid: String = "",
    val viaAi: Boolean = false,
    val istTimestampText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OwnerNotification(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val istTimestampText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ShopOpenEvaluation(
    val isOpen: Boolean,
    val badgeText: String,
    val nextOpeningText: String?,
    val mode: ShopControlMode
)

data class OwnerDashboardStats(
    val todaysSales: Long = 0L,
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val completedOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val totalCustomers: Int = 0,
    val newSignupsToday: Int = 0,
    val shopOrdersCount: Int = 0,
    val deliveryOrdersCount: Int = 0,
    val roomBookingsCount: Int = 0,
    val freshenUpCount: Int = 0,
    val shopOrdersSales: Long = 0L,
    val deliveryOrdersSales: Long = 0L,
    val mostPopularProductName: String? = null,
    val mostPopularProductUnitsSold: Long = 0L,
    val mostPopularProductRevenue: Long = 0L
)

data class AdminOrderBadgeState(
    val pendingShopOrders: Int = 0,
    val pendingDeliveryOrders: Int = 0,
    val pendingRoomBookings: Int = 0,
    val pendingFreshenUp: Int = 0,
    val totalPendingNewOrders: Int = 0,
    val newlyArrivedOrderIds: Set<String> = emptySet(),
    val latestNewOrderSummary: String? = null
) {
    val hasNewlyArrived: Boolean get() = newlyArrivedOrderIds.isNotEmpty()

    fun pendingCountForCategory(category: RequestCategory): Int = when (category) {
        RequestCategory.FOOD_REQUEST -> pendingShopOrders
        RequestCategory.FOOD_DELIVERY_REQUEST -> pendingDeliveryOrders
        RequestCategory.ROOM_REQUEST -> pendingRoomBookings
        RequestCategory.FRESHEN_UP_REQUEST -> pendingFreshenUp
    }
}

enum class LowStockSeverity {
    HEALTHY,
    CRITICAL_LOW,
    OUT_OF_STOCK
}

data class LowStockAlertItem(
    val product: Product,
    val stockQuantity: Long,
    val threshold: Long,
    val severity: LowStockSeverity
)

data class LowStockAlertSummary(
    val flaggedItems: List<LowStockAlertItem> = emptyList(),
    val criticalLowCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalFlaggedCount: Int = 0,
    val newlyFlaggedProductIds: Set<String> = emptySet()
) {
    val hasAlerts: Boolean get() = totalFlaggedCount > 0
    val hasNewlyFlagged: Boolean get() = newlyFlaggedProductIds.isNotEmpty()
}

data class CustomerSummary(
    val profile: UserProfile,
    val totalOrders: Int,
    val totalSpending: Long,
    val lastOrderMillis: Long?,
    val orderHistory: List<CustomerRequest>
)

data class ProductSalesStat(
    val productName: String,
    val quantitySold: Long,
    val totalRevenue: Long
)

data class SalesReportSummary(
    val period: ReportPeriod,
    val dateRangeLabel: String = "",
    val totalSales: Long,
    val totalOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int,
    val pickupOrders: Int,
    val deliveryOrders: Int,
    val roomBookings: Int,
    val shopSales: Long = 0L,
    val deliverySales: Long = 0L,
    val roomSales: Long = 0L,
    val cashSales: Long,
    val upiSales: Long,
    val verifiedPaidTotal: Long,
    val unpaidOrPendingTotal: Long,
    val productStats: List<ProductSalesStat>
)
