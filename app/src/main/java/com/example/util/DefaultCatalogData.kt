package com.example.util

import com.example.data.model.DeliverySettings
import com.example.data.model.FreshenUpConfig
import com.example.data.model.FreshenUpOption
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.PriceOption
import com.example.data.model.Product
import com.example.data.model.RoomConfig
import com.example.data.model.RoomStatusLabel
import com.example.data.model.RoomType
import com.example.data.model.ShopControlMode
import com.example.data.model.ShopSettings
import com.example.data.model.SiteConfig
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemePreset

object DefaultCatalogData {

    val menuCategories: List<String> = listOf(
        "Tea & Drinks",
        "Breakfast",
        "Maggi",
        "Fast Food",
        "Indian Food",
        "Snacks and Desserts"
    )

    // Public storefront settings keep owner's personal phone/email private by default.
    val defaultShopSettings = ShopSettings(
        id = "main",
        shopName = "MARUTI DHAM",
        ownerName = "Tarun Kumar",
        address = "Near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
        mapsQuery = "Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
        shopOpen = true,
        shopControlMode = ShopControlMode.OPEN_NOW.name,
        openingTime = "7:00 AM",
        closingTime = "10:00 PM",
        whatsappNumber = "",
        contactPhone = "",
        contactEmail = "",
        upiId = "",
        cashEnabled = true,
        upiEnabled = false,
        whatsappWebhookUrl = "",
        aboutText = "MARUTI DHAM (Owner: Tarun Kumar) is a welcoming small food stall near Manda Chauraha, Khatu Shyam Ji, serving devotees with fresh tea, snacks, meals, rest rooms, freshen-up suites, and local Khatu Shyam Ji food delivery.",
        privacyPolicy = "MARUTI DHAM collects your name, verified mobile number, optional email, and order details solely to prepare your food/room/delivery orders and keep your personal order history. Customer information is private and accessible only to the authorized shop owner.",
        termsText = "Orders can be placed while the shop is open. Cash Only payment is accepted at the stall or on delivery (online UPI is currently disabled unless enabled by owner). Food delivery is limited strictly to configured areas within Khatu Shyam Ji."
    )

    fun themePresetFor(preset: ThemePreset): ThemeConfig = when (preset) {
        ThemePreset.NAVY_ROYAL_GOLD -> ThemeConfig(
            preset = ThemePreset.NAVY_ROYAL_GOLD.name,
            primaryColorHex = "#E5B94C",
            backgroundColorHex = "#07122B",
            surfaceColorHex = "#142652",
            accentColorHex = "#F7D774",
            textColorHex = "#F8F6F0"
        )
        ThemePreset.WHITE_GOLD -> ThemeConfig(
            preset = ThemePreset.WHITE_GOLD.name,
            primaryColorHex = "#B8860B",
            backgroundColorHex = "#FFFFFF",
            surfaceColorHex = "#F7F3E9",
            accentColorHex = "#D4AF37",
            textColorHex = "#1A2238"
        )
        ThemePreset.PREMIUM_DARK -> ThemeConfig(
            preset = ThemePreset.PREMIUM_DARK.name,
            primaryColorHex = "#F5C451",
            backgroundColorHex = "#0D0F14",
            surfaceColorHex = "#1A1F2B",
            accentColorHex = "#FFE082",
            textColorHex = "#F3F4F6"
        )
        ThemePreset.CREAM_ROYAL_BLUE -> ThemeConfig(
            preset = ThemePreset.CREAM_ROYAL_BLUE.name,
            primaryColorHex = "#1E3A8A",
            backgroundColorHex = "#FFFDF5",
            surfaceColorHex = "#F3EFE0",
            accentColorHex = "#C89B2B",
            textColorHex = "#0F1E46"
        )
        ThemePreset.MODERN_ROYAL_PURPLE -> ThemeConfig(
            preset = ThemePreset.MODERN_ROYAL_PURPLE.name,
            primaryColorHex = "#E5B94C",
            backgroundColorHex = "#1A1033",
            surfaceColorHex = "#291B4E",
            accentColorHex = "#C084FC",
            textColorHex = "#FAF5FF"
        )
        ThemePreset.CUSTOM -> ThemeConfig(preset = ThemePreset.CUSTOM.name)
    }

    val defaultSiteConfig = SiteConfig(
        id = "main",
        logoUrl = "",
        heroBannerUrl = "",
        showAnnouncementsSection = true,
        showDailySpecialsSection = true,
        showRoomsSection = true,
        showDeliverySection = true,
        categories = menuCategories,
        draftTheme = themePresetFor(ThemePreset.NAVY_ROYAL_GOLD),
        publishedTheme = themePresetFor(ThemePreset.NAVY_ROYAL_GOLD)
    )

    // Stored in owner-only /owner_private_settings/main collection
    val defaultOwnerPrivateSettings = OwnerPrivateSettings(
        id = "main",
        ownerWhatsAppNumber = "+91 7850068566",
        whatsappWebhookUrl = ""
    )

    val initialProducts: List<Product> = listOf(
        Product(
            id = "tea",
            name = "Desi Tea",
            nameHindi = "देसी चाय",
            description = "Freshly brewed hot ginger-cardamom desi chai served at the stall.",
            category = "Tea & Drinks",
            priceOptions = listOf(
                PriceOption(label = "Regular", price = 10L),
                PriceOption(label = "Special", price = 20L)
            ),
            imageUrl = "",
            localImageKey = "food_platter",
            available = true,
            visible = true,
            foodOrderEnabled = true,
            deliveryEnabled = true,
            stockQuantity = 100L,
            trackStock = false,
            sortOrder = 1L
        ),
        Product(
            id = "poha",
            name = "Poha",
            nameHindi = "पोहा",
            description = "Warm flattened rice garnished with peanuts, sev, onion, and lemon.",
            category = "Breakfast",
            priceOptions = listOf(
                PriceOption(label = "Half", price = 20L),
                PriceOption(label = "Full", price = 30L)
            ),
            imageUrl = "",
            localImageKey = "food_platter",
            available = true,
            visible = true,
            foodOrderEnabled = true,
            deliveryEnabled = true,
            stockQuantity = 50L,
            trackStock = false,
            sortOrder = 2L
        ),
        Product(
            id = "maggi",
            name = "Maggi",
            nameHindi = "मैगी",
            description = "Classic hot masala Maggi prepared fresh to order.",
            category = "Maggi",
            priceOptions = listOf(
                PriceOption(label = "Standard", price = 50L)
            ),
            imageUrl = "",
            localImageKey = "food_platter",
            available = true,
            visible = true,
            foodOrderEnabled = true,
            deliveryEnabled = true,
            stockQuantity = 40L,
            trackStock = false,
            sortOrder = 3L
        ),
        Product(
            id = "chole_kulche",
            name = "Chole Kulche",
            nameHindi = "छोले कुलचे",
            description = "Spiced Khatu-style chole served with soft buttered kulche and pickle.",
            category = "Indian Food",
            priceOptions = listOf(
                PriceOption(label = "Full Plate", price = 50L)
            ),
            imageUrl = "",
            localImageKey = "food_platter",
            available = true,
            visible = true,
            foodOrderEnabled = true,
            deliveryEnabled = true,
            stockQuantity = 30L,
            trackStock = false,
            sortOrder = 4L
        )
    )

    /**
     * Suggested small food-stall catalog items across all 6 categories.
     * Kept inactive/unavailable until the owner explicitly activates them with real prices.
     */
    val suggestedMenuTemplates: List<Product> = listOf(
        // TEA & DRINKS
        templateProduct("normal_tea", "Normal Tea", "सामान्य चाय", "Fresh hot daily cup of chai.", "Tea & Drinks", 10L, 10),
        templateProduct("kulhad_tea", "Kulhad Tea", "कुल्हड़ चाय", "Traditional earthen pot aromatic chai.", "Tea & Drinks", 20L, 11),
        templateProduct("masala_tea", "Masala Tea", "मसाला चाय", "Spiced cardamom and ginger chai.", "Tea & Drinks", 20L, 12),
        templateProduct("coffee", "Coffee", "कॉफी", "Hot frothy milk coffee.", "Tea & Drinks", 0L, 13),
        templateProduct("cold_coffee", "Cold Coffee", "कोल्ड कॉफी", "Chilled blended milk coffee.", "Tea & Drinks", 0L, 14),
        templateProduct("water_bottle_10", "Water Bottle ₹10", "पानी बोतल ₹10", "Sealed chilled drinking water (small).", "Tea & Drinks", 10L, 15),
        templateProduct("water_bottle_20", "Water Bottle ₹20", "पानी बोतल ₹20", "Sealed chilled drinking water (1 Litre).", "Tea & Drinks", 20L, 16),
        templateProduct("water_bottle_30", "Water Bottle ₹30", "पानी बोतल ₹30", "Sealed chilled drinking water (Large).", "Tea & Drinks", 30L, 17),
        templateProduct("milk", "Milk", "दूध", "Warm sweetened saffron/plain milk.", "Tea & Drinks", 0L, 18),
        templateProduct("hot_water", "Hot Water", "गर्म पानी", "Clean hot drinking water.", "Tea & Drinks", 0L, 19),

        // BREAKFAST
        templateProduct("special_poha", "Special Poha", "स्पेशल पोहा", "Loaded poha with extra dry fruits and sev.", "Breakfast", 30L, 30),
        templateProduct("samosa", "Samosa", "समोसा", "Crispy golden potato stuffed samosa with chutney.", "Breakfast", 0L, 31),
        templateProduct("kachori", "Kachori", "कचौरी", "Hot Rajasthani khasta kachori.", "Breakfast", 0L, 32),
        templateProduct("bread_pakoda", "Bread Pakoda", "ब्रेड पकौड़ा", "Spiced potato stuffed fried bread pakoda.", "Breakfast", 0L, 33),
        templateProduct("paratha", "Paratha", "पराठा", "Freshly roasted tawa paratha served hot.", "Breakfast", 0L, 34),
        templateProduct("aloo_paratha", "Aloo Paratha", "आलू पराठा", "Stuffed potato paratha served with curd/pickle.", "Breakfast", 0L, 35),

        // MAGGI
        templateProduct("plain_maggi", "Plain Maggi", "सादा मैगी", "Classic hot soupy Maggi noodles.", "Maggi", 0L, 40),
        templateProduct("masala_maggi", "Masala Maggi", "मसाला मैगी", "Extra spiced masala Maggi noodles.", "Maggi", 0L, 41),
        templateProduct("cheese_maggi", "Cheese Maggi", "चीज़ मैगी", "Creamy melted cheese Maggi.", "Maggi", 0L, 42),
        templateProduct("vegetable_maggi", "Vegetable Maggi", "वेजिटेबल मैगी", "Fresh peas, onion, and tomato Maggi.", "Maggi", 0L, 43),

        // FAST FOOD
        templateProduct("veg_burger", "Veg Burger", "वेज बर्गर", "Crispy veg patty burger with fresh veggies.", "Fast Food", 0L, 50),
        templateProduct("cheese_burger", "Cheese Burger", "चीज़ बर्गर", "Veg patty burger with cheese slice.", "Fast Food", 0L, 51),
        templateProduct("pizza", "Pizza", "पिज़्ज़ा", "Freshly baked pure-veg pizza.", "Fast Food", 0L, 52),
        templateProduct("veg_sandwich", "Veg Sandwich", "वेज सैंडविच", "Grilled vegetable sandwich.", "Fast Food", 0L, 53),
        templateProduct("cheese_sandwich", "Cheese Sandwich", "चीज़ सैंडविच", "Grilled melted cheese sandwich.", "Fast Food", 0L, 54),
        templateProduct("chowmein", "Chowmein", "चाउमीन", "Desi street-style veg chowmein.", "Fast Food", 0L, 55),
        templateProduct("veg_momos", "Veg Momos", "वेज मोमोज़", "Steamed vegetable momos with spicy chutney.", "Fast Food", 0L, 56),
        templateProduct("french_fries", "French Fries", "फ्रेंच फ्राइज़", "Crispy salted potato fries.", "Fast Food", 0L, 57),
        templateProduct("pasta", "Pasta", "पास्ता", "Hot creamy/masala veg pasta.", "Fast Food", 0L, 58),

        // INDIAN FOOD
        templateProduct("chole_bhature", "Chole Bhature", "छोले भटूरे", "Fluffy bhature with spiced Punjabi chole.", "Indian Food", 0L, 60),
        templateProduct("chole_chawal", "Chole Chawal", "छोले चावल", "Steamed rice topped with hot chole.", "Indian Food", 0L, 61),
        templateProduct("dal_rice", "Dal Rice", "दाल चावल", "Homely comforting dal and steamed rice.", "Indian Food", 0L, 62),
        templateProduct("roti_sabzi", "Roti Sabzi", "रोटी सब्जी", "Fresh tawa roti served with seasonal sabzi.", "Indian Food", 0L, 63),
        templateProduct("thali", "Thali", "थाली", "Pure-veg devotional mini meal thali.", "Indian Food", 0L, 64),

        // SNACKS AND DESSERTS
        templateProduct("biscuits", "Biscuits", "बिस्कुट", "Fresh tea-time biscuit pack.", "Snacks and Desserts", 0L, 70),
        templateProduct("chips", "Chips", "चिप्स", "Crispy packaged potato chips.", "Snacks and Desserts", 0L, 71),
        templateProduct("namkeen", "Namkeen", "नमकीन", "Crunchy savory Indian namkeen.", "Snacks and Desserts", 0L, 72),
        templateProduct("chocolate", "Chocolate", "चॉकलेट", "Sweet chocolate bar.", "Snacks and Desserts", 0L, 73),
        templateProduct("toast", "Toast", "टोस्ट", "Crispy baked tea toast.", "Snacks and Desserts", 0L, 74),
        templateProduct("rusk", "Rusk", "रस्क", "Crunchy suji/milk rusk for chai.", "Snacks and Desserts", 0L, 75),
        templateProduct("ice_cream", "Ice Cream", "आइसक्रीम", "Chilled ice cream cup/cone.", "Snacks and Desserts", 0L, 80),
        templateProduct("sweet_items", "Sweet Items", "मिठाई", "Fresh devotional prasad/sweet items.", "Snacks and Desserts", 0L, 81)
    )

    private fun templateProduct(
        id: String,
        name: String,
        nameHindi: String,
        description: String,
        category: String,
        suggestedPrice: Long,
        sortOrder: Int
    ): Product = Product(
        id = id,
        name = name,
        nameHindi = nameHindi,
        description = description,
        category = category,
        priceOptions = listOf(PriceOption(label = "Regular", price = suggestedPrice)),
        imageUrl = "",
        localImageKey = "food_platter",
        available = false,
        visible = false,
        foodOrderEnabled = true,
        deliveryEnabled = true,
        stockQuantity = 0L,
        trackStock = true,
        sortOrder = sortOrder.toLong()
    )

    val initialRoomConfigs: List<RoomConfig> = listOf(
        RoomConfig(
            id = "ac",
            roomType = RoomType.AC.name,
            title = "AC Room",
            titleHindi = "एसी कमरा",
            available = true,
            statusLabel = RoomStatusLabel.AVAILABLE.name,
            availableCount = 1L,
            imageUrl = "",
            rate6Hours = 1200L,
            rate12Hours = 2000L,
            rate1Hour = null,
            rate2Hours = null,
            rate3Hours = null,
            rate4Hours = null,
            rate5Hours = null
        ),
        RoomConfig(
            id = "non_ac",
            roomType = RoomType.NON_AC.name,
            title = "Non-AC Room",
            titleHindi = "नॉन-एसी कमरा",
            available = true,
            statusLabel = RoomStatusLabel.AVAILABLE.name,
            availableCount = 1L,
            imageUrl = "",
            rate6Hours = 800L,
            rate12Hours = 1500L,
            rate1Hour = null,
            rate2Hours = null,
            rate3Hours = null,
            rate4Hours = null,
            rate5Hours = null
        )
    )

    val initialFreshenUpConfigs: List<FreshenUpConfig> = listOf(
        FreshenUpConfig(
            id = "only_fresh",
            optionKey = FreshenUpOption.ONLY_FRESH.name,
            title = "Only Fresh (without bathing)",
            titleHindi = "केवल फ्रेश (बिना स्नान)",
            pricePerPerson = 50L,
            available = true
        ),
        FreshenUpConfig(
            id = "bath_and_fresh",
            optionKey = FreshenUpOption.BATH_FRESH.name,
            title = "Bath + Fresh",
            titleHindi = "स्नान + फ्रेश",
            pricePerPerson = 100L,
            available = true
        )
    )

    val defaultDeliverySettings = DeliverySettings(
        id = "main",
        deliveryEnabled = true,
        pickupEnabled = true,
        deliveryCharge = 0L,
        packagingCharge = 0L,
        serviceAreaName = "Khatu Shyam Ji",
        allowedLocalities = listOf(
            "Khatu Shyam Ji",
            "Manda Chauraha",
            "Shyam Kund",
            "Toran Dwar",
            "Ringas Road Khatu",
            "Danta Road Khatu"
        ),
        minimumOrderAmount = 0L,
        deliveryNote = "Delivery available ONLY within Khatu Shyam Ji. Pay in Cash or verified UPI on delivery."
    )
}
