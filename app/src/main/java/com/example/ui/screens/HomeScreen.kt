package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AppStrings
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.data.model.SiteConfig
import com.example.data.model.UiState
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.ProductOrServiceImage
import com.example.ui.components.ScrollToTopIndicator
import com.example.ui.components.ShopClosedBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.WarmSaffron
import com.example.util.DefaultCatalogData
import com.example.util.ExternalIntentHelper

@Composable
fun HomeScreen(
    shopState: UiState<ShopSettings>,
    siteConfig: SiteConfig = DefaultCatalogData.defaultSiteConfig,
    shopEvaluation: ShopOpenEvaluation,
    announcementsState: UiState<List<Announcement>>,
    strings: AppStrings,
    isOwner: Boolean,
    pendingNewOrdersCount: Int = 0,
    hasNewlyArrivedOrders: Boolean = false,
    onNavigateToFoodOrder: () -> Unit,
    onNavigateToRooms: () -> Unit,
    onNavigateToDelivery: () -> Unit,
    onNavigateToMyRequests: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalMarutiPalette.current
    val shop = (shopState as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
    val activeAnnouncements = (announcementsState as? UiState.Success)?.data?.filter { it.active } ?: emptyList()
    val visibleAnnouncements = remember(activeAnnouncements, siteConfig.showAnnouncementsSection, siteConfig.showDailySpecialsSection) {
        activeAnnouncements.filter { ann ->
            if (ann.dailySpecial) siteConfig.showDailySpecialsSection else siteConfig.showAnnouncementsSection
        }
    }

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Khatu Shyam Ji Devotional Header Banner
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = palette.card),
                    border = BorderStroke(1.5.dp, palette.primary.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(185.dp)) {
                            ProductOrServiceImage(
                                imageUrl = siteConfig.heroBannerUrl,
                                localFallbackRes = R.drawable.img_khatu_shyam_banner,
                                contentDescription = "Khatu Shyam Ji devotional banner",
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF07122B).copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductOrServiceImage(
                                    imageUrl = siteConfig.logoUrl,
                                    localFallbackRes = R.drawable.ic_maruti_logo,
                                    contentDescription = "MARUTI DHAM emblem",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.jaiShriShyam,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFF7D774)
                                    )
                                    Text(
                                        text = shop.shopName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color(0xFFF8F6F0)
                                    )
                                }
                                StatusPill(
                                    text = if (shopEvaluation.isOpen) strings.shopOpen else strings.shopClosed,
                                    color = if (shopEvaluation.isOpen) StatusOpenGreen else StatusClosedRed
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (strings.isHindi) "संचालक: ${shop.ownerName} • शुद्ध शाकाहारी फूड स्टॉल"
                                    else "Owner: ${shop.ownerName} • Pure Veg Food Stall",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.accent
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                val timingsText = if (shop.openingTime.isNotBlank() && shop.closingTime.isNotBlank()) {
                                    "${shop.openingTime} – ${shop.closingTime} IST"
                                } else {
                                    strings.timingsNotConfigured
                                }
                                Text(
                                    text = "${strings.timingsLabel}: $timingsText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = shop.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { ExternalIntentHelper.launchGoogleMapsDirections(context, shop.mapsQuery) },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_directions_button")
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.getDirections, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onNavigateToMyRequests,
                                    border = BorderStroke(1.dp, palette.primary),
                                    modifier = Modifier.testTag("home_my_requests_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.myRequestsTitle, color = palette.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Shop Closed Alert Banner
            if (!shopEvaluation.isOpen) {
                item {
                    ShopClosedBanner(evaluation = shopEvaluation, strings = strings)
                }
            }

            // Owner Quick Admin Banner (visible only to authenticated owner)
            if (isOwner) {
                item {
                    GoldOutlineCard(containerColor = palette.elevated) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = palette.accent)
                                Column {
                                    Text(strings.ownerAdminPanel, style = MaterialTheme.typography.titleMedium, color = palette.accent)
                                    Text(
                                        text = if (pendingNewOrdersCount > 0) {
                                            "$pendingNewOrdersCount pending order(s) waiting for action"
                                        } else {
                                            "Manage orders, customers, menu, theme & security"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (pendingNewOrdersCount > 0) palette.accent else palette.textSecondary
                                    )
                                }
                            }
                            BadgedBox(
                                badge = {
                                    if (pendingNewOrdersCount > 0) {
                                        Badge(
                                            containerColor = if (hasNewlyArrivedOrders) StatusClosedRed else WarmSaffron,
                                            contentColor = Color.White,
                                            modifier = Modifier.testTag("home_owner_admin_badge")
                                        ) {
                                            Text(pendingNewOrdersCount.coerceAtMost(99).toString())
                                        }
                                    }
                                }
                            ) {
                                Button(
                                    onClick = onNavigateToAdmin,
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier.testTag("home_owner_admin_button")
                                ) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }
            }

            // Available Services Grid
            item {
                Text(
                    text = strings.availableServices,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.accent,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ServiceShortcutCard(
                            title = strings.navFoodOrder,
                            subtitle = if (strings.isHindi) "चाय, पोहा, मैगी, छोले कुलचे" else "Tea, Poha, Maggi, Chole Kulche",
                            icon = Icons.Default.RestaurantMenu,
                            onClick = onNavigateToFoodOrder,
                            modifier = Modifier.weight(1f).testTag("home_card_food")
                        )
                        if (siteConfig.showRoomsSection) {
                            ServiceShortcutCard(
                                title = strings.navRoomsFreshenUp,
                                subtitle = if (strings.isHindi) "AC / Non-AC एवं फ्रेश-अप" else "AC / Non-AC & Freshen Up",
                                icon = Icons.Default.Hotel,
                                onClick = onNavigateToRooms,
                                modifier = Modifier.weight(1f).testTag("home_card_rooms")
                            )
                        }
                    }
                    if (siteConfig.showDeliverySection || siteConfig.showRoomsSection) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            if (siteConfig.showDeliverySection) {
                                ServiceShortcutCard(
                                    title = strings.navFoodDelivery,
                                    subtitle = if (strings.isHindi) "केवल खाटू श्याम जी क्षेत्र" else "Khatu Shyam Ji Local Only",
                                    icon = Icons.Default.DeliveryDining,
                                    onClick = onNavigateToDelivery,
                                    modifier = Modifier.weight(1f).testTag("home_card_delivery")
                                )
                            }
                            if (siteConfig.showRoomsSection) {
                                ServiceShortcutCard(
                                    title = strings.freshenUpSectionTitle.substringBefore("(").trim(),
                                    subtitle = if (strings.isHindi) "₹50 / ₹100 प्रति व्यक्ति" else "₹50 / ₹100 per person",
                                    icon = Icons.Default.Shower,
                                    onClick = onNavigateToRooms,
                                    modifier = Modifier.weight(1f).testTag("home_card_freshen")
                                )
                            }
                        }
                    }
                }
            }

            if (siteConfig.showAnnouncementsSection || siteConfig.showDailySpecialsSection) {
                item {
                    Text(
                        text = strings.specialsAndAnnouncements,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.accent,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (visibleAnnouncements.isEmpty()) {
                    item {
                        GoldOutlineCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = palette.primary)
                                Text(
                                    text = strings.noAnnouncementsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(visibleAnnouncements, key = { it.id }) { ann ->
                        AnimatedVisibility(visible = true) {
                            GoldOutlineCard(
                                containerColor = if (ann.dailySpecial) palette.elevated else palette.card
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (ann.dailySpecial) Icons.Default.Star else Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = if (ann.dailySpecial) WarmSaffron else palette.primary
                                        )
                                        Text(
                                            text = ann.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.accent
                                        )
                                    }
                                    if (ann.dailySpecial) {
                                        StatusPill(text = strings.dailySpecialTag, color = WarmSaffron)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = ann.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
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
            testTag = "home_scroll_to_top"
        )
    }
}

@Composable
private fun ServiceShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMarutiPalette.current
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = palette.elevated,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = palette.primary, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2
            )
        }
    }
}
