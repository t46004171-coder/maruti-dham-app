package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerSummary
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.ShopSettings
import com.example.data.model.SiteConfig
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemeFontStyle
import com.example.data.model.ThemeLayoutVariant
import com.example.data.model.ThemePreset
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.parseHexColorSafe
import com.example.util.DefaultCatalogData
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminThemeCustomizerSection(
    siteConfig: SiteConfig,
    shopName: String,
    isPreviewActive: Boolean,
    onTogglePreview: () -> Unit,
    onSaveDraft: (SiteConfig) -> Unit,
    onPublishTheme: (SiteConfig) -> Unit
) {
    val palette = LocalMarutiPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var preset by remember(siteConfig) { mutableStateOf(ThemePreset.fromRaw(siteConfig.draftTheme.preset)) }
    var primaryHex by remember(siteConfig) { mutableStateOf(siteConfig.draftTheme.primaryColorHex) }
    var backgroundHex by remember(siteConfig) { mutableStateOf(siteConfig.draftTheme.backgroundColorHex) }
    var surfaceHex by remember(siteConfig) { mutableStateOf(siteConfig.draftTheme.surfaceColorHex) }
    var accentHex by remember(siteConfig) { mutableStateOf(siteConfig.draftTheme.accentColorHex) }
    var textHex by remember(siteConfig) { mutableStateOf(siteConfig.draftTheme.textColorHex) }
    var fontStyle by remember(siteConfig) { mutableStateOf(ThemeFontStyle.fromRaw(siteConfig.draftTheme.fontStyle)) }
    var layoutVariant by remember(siteConfig) { mutableStateOf(ThemeLayoutVariant.fromRaw(siteConfig.draftTheme.layoutVariant)) }
    var logoUrl by remember(siteConfig) { mutableStateOf(siteConfig.logoUrl) }
    var heroBannerUrl by remember(siteConfig) { mutableStateOf(siteConfig.heroBannerUrl) }
    var showAnnouncements by remember(siteConfig) { mutableStateOf(siteConfig.showAnnouncementsSection) }
    var showDailySpecials by remember(siteConfig) { mutableStateOf(siteConfig.showDailySpecialsSection) }
    var showRooms by remember(siteConfig) { mutableStateOf(siteConfig.showRoomsSection) }
    var showDelivery by remember(siteConfig) { mutableStateOf(siteConfig.showDeliverySection) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                ImageCompressionUtil.compressPickedImage(context, uri, "site_logo")
                    .onSuccess { logoUrl = it }
                    .onFailure { Toast.makeText(context, "Unable to process logo image", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    val heroPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                ImageCompressionUtil.compressPickedImage(context, uri, "site_hero")
                    .onSuccess { heroBannerUrl = it }
                    .onFailure { Toast.makeText(context, "Unable to process hero image", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun buildDraftSiteConfig(): SiteConfig {
        val draftTheme = ThemeConfig(
            preset = preset.name,
            primaryColorHex = primaryHex.trim(),
            backgroundColorHex = backgroundHex.trim(),
            surfaceColorHex = surfaceHex.trim(),
            accentColorHex = accentHex.trim(),
            textColorHex = textHex.trim(),
            fontStyle = fontStyle.name,
            layoutVariant = layoutVariant.name
        )
        return siteConfig.copy(
            logoUrl = logoUrl.trim(),
            heroBannerUrl = heroBannerUrl.trim(),
            showAnnouncementsSection = showAnnouncements,
            showDailySpecialsSection = showDailySpecials,
            showRoomsSection = showRooms,
            showDeliverySection = showDelivery,
            draftTheme = draftTheme
        )
    }

    val previewBg = parseHexColorSafe(backgroundHex, palette.background)
    val previewSurface = parseHexColorSafe(surfaceHex, palette.surface)
    val previewPrimary = parseHexColorSafe(primaryHex, palette.primary)
    val previewAccent = parseHexColorSafe(accentHex, palette.accent)
    val previewText = parseHexColorSafe(textHex, palette.textPrimary)

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoldOutlineCard(containerColor = palette.elevated) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = palette.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Website & App Design Studio", style = MaterialTheme.typography.titleLarge, color = palette.accent)
                        Text(
                            "Customize live presets, hex palette, typography style, hero media, and home sections. Save Draft, Preview Live, or Publish to all users.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        // Live Preview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = previewBg),
                border = BorderStroke(1.5.dp, previewPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIVE THEME PREVIEW", style = MaterialTheme.typography.labelSmall, color = previewAccent)
                            Text(shopName, style = MaterialTheme.typography.titleLarge, color = previewText, fontWeight = FontWeight.Bold)
                            Text("${fontStyle.displayName} • ${layoutVariant.displayName}", style = MaterialTheme.typography.labelSmall, color = previewPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(previewPrimary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("OPEN", color = previewBg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = previewSurface),
                        border = BorderStroke(1.dp, previewPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Desi Tea • देसी चाय", color = previewText, fontWeight = FontWeight.SemiBold)
                                Text("Pure Devotional Food Stall", color = previewAccent, style = MaterialTheme.typography.bodySmall)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(previewPrimary)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("₹10", color = previewBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Theme Presets
        item {
            GoldOutlineCard {
                Text("Theme Presets", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreset.entries.forEach { entry ->
                        FilterChip(
                            selected = preset == entry,
                            onClick = {
                                preset = entry
                                if (entry != ThemePreset.CUSTOM) {
                                    val cfg = DefaultCatalogData.themePresetFor(entry)
                                    primaryHex = cfg.primaryColorHex
                                    backgroundHex = cfg.backgroundColorHex
                                    surfaceHex = cfg.surfaceColorHex
                                    accentHex = cfg.accentColorHex
                                    textHex = cfg.textColorHex
                                }
                            },
                            label = { Text(entry.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.background
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Font Style", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeFontStyle.entries.forEach { fs ->
                        FilterChip(
                            selected = fontStyle == fs,
                            onClick = { fontStyle = fs },
                            label = { Text(fs.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.background
                            )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Layout Variant", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeLayoutVariant.entries.forEach { lv ->
                        FilterChip(
                            selected = layoutVariant == lv,
                            onClick = { layoutVariant = lv },
                            label = { Text(lv.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary,
                                selectedLabelColor = palette.background
                            )
                        )
                    }
                }
            }
        }

        // Custom Hex Colors
        item {
            GoldOutlineCard {
                Text("Custom Hex Colors", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                Spacer(Modifier.height(8.dp))
                HexColorInputRow("Primary / Button Color", primaryHex) { primaryHex = it; preset = ThemePreset.CUSTOM }
                HexColorInputRow("Background Color", backgroundHex) { backgroundHex = it; preset = ThemePreset.CUSTOM }
                HexColorInputRow("Card / Surface Color", surfaceHex) { surfaceHex = it; preset = ThemePreset.CUSTOM }
                HexColorInputRow("Accent / Highlight Color", accentHex) { accentHex = it; preset = ThemePreset.CUSTOM }
                HexColorInputRow("Text Color", textHex) { textHex = it; preset = ThemePreset.CUSTOM }
            }
        }

        // Brand Media & Section Visibility
        item {
            GoldOutlineCard {
                Text("Brand Media & Home Sections", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("Logo Image URL (Optional)") },
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, palette.primary)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick logo", tint = palette.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = heroBannerUrl,
                        onValueChange = { heroBannerUrl = it },
                        label = { Text("Hero Banner Image URL (Optional)") },
                        colors = adminTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { heroPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, palette.primary)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick hero banner", tint = palette.primary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SectionToggleRow("Show Announcements Section on Home", showAnnouncements) { showAnnouncements = it }
                SectionToggleRow("Show Daily Specials Section on Home", showDailySpecials) { showDailySpecials = it }
                SectionToggleRow("Show Rooms & Freshen Up on Home", showRooms) { showRooms = it }
                SectionToggleRow("Show Khatu Delivery Banner on Home", showDelivery) { showDelivery = it }
            }
        }

        // Actions: Save Draft / Live Preview / Publish Theme
        item {
            GoldOutlineCard(containerColor = palette.elevated) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onSaveDraft(buildDraftSiteConfig()) },
                        border = BorderStroke(1.dp, palette.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_save_draft_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Draft", color = palette.primary)
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveDraft(buildDraftSiteConfig())
                            onTogglePreview()
                        },
                        border = BorderStroke(1.dp, if (isPreviewActive) StatusOpenGreen else palette.accent),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_live_preview_button")
                    ) {
                        Icon(Icons.Default.Preview, contentDescription = null, tint = if (isPreviewActive) StatusOpenGreen else palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPreviewActive) "Exit Preview" else "Live Preview", color = if (isPreviewActive) StatusOpenGreen else palette.accent)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onPublishTheme(buildDraftSiteConfig()) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.background),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("theme_publish_button")
                ) {
                    Icon(Icons.Default.Publish, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publish Theme to App & Website", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HexColorInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val palette = LocalMarutiPalette.current
    val previewColor = parseHexColorSafe(value, palette.primary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(previewColor)
                .border(1.dp, palette.accent, CircleShape)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            colors = adminTextFieldColors(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalMarutiPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AdminPaymentSettingsCard(
    shop: ShopSettings,
    onSaveShop: (ShopSettings) -> Unit
) {
    val palette = LocalMarutiPalette.current
    var cashEnabled by remember(shop) { mutableStateOf(shop.cashEnabled) }
    var upiEnabled by remember(shop) { mutableStateOf(shop.upiEnabled) }
    var upiId by remember(shop) { mutableStateOf(shop.upiId) }

    GoldOutlineCard(containerColor = palette.elevated) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = palette.primary)
                Text("Payment Mode Control", style = MaterialTheme.typography.titleMedium, color = palette.accent)
            }
            StatusPill(
                text = if (!upiEnabled) "CASH ONLY ACTIVE" else "CASH + UPI ACTIVE",
                color = if (!upiEnabled) StatusOpenGreen else StatusPendingAmber
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "By default MARUTI DHAM accepts Cash Only and hides all UPI / online payment options from customers. Enable UPI only when you want to accept online UPI payments.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cash on Counter / Cash on Delivery", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = cashEnabled, onCheckedChange = { cashEnabled = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Future UPI Online Payments", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = upiEnabled,
                onCheckedChange = { upiEnabled = it },
                modifier = Modifier.testTag("admin_upi_toggle")
            )
        }
        if (upiEnabled) {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("Owner UPI ID (Required for UPI Checkout)") },
                placeholder = { Text("e.g., marutidham@upi") },
                colors = adminTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                onSaveShop(
                    shop.copy(
                        cashEnabled = cashEnabled || !upiEnabled,
                        upiEnabled = upiEnabled,
                        upiId = upiId.trim()
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.background),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_save_payment_button")
        ) {
            Text("Save Payment Settings", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminPrivacyAndSecuritySection(
    ownerEmail: String,
    shop: ShopSettings,
    ownerPrivateSettings: OwnerPrivateSettings,
    customers: List<CustomerSummary>,
    onSaveShop: (ShopSettings) -> Unit,
    onSaveOwnerPrivateSettings: (OwnerPrivateSettings) -> Unit,
    onSignOut: () -> Unit
) {
    val palette = LocalMarutiPalette.current
    var privateWhatsapp by remember(ownerPrivateSettings) { mutableStateOf(ownerPrivateSettings.ownerWhatsAppNumber) }
    var privateWebhookUrl by remember(ownerPrivateSettings) { mutableStateOf(ownerPrivateSettings.whatsappWebhookUrl) }
    val deletionRequests = remember(customers) { customers.filter { it.profile.deletionRequested } }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoldOutlineCard(containerColor = palette.elevated) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusOpenGreen)
                    Column {
                        Text("Verified Owner Session", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                        Text("Authenticated Owner: ${ownerEmail.ifBlank { "Verified Owner" }}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                        Text("Protected by Firebase Auth + /admins RBAC + Cloud Firestore zero-trust rules.", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
            }
        }

        item {
            AdminPaymentSettingsCard(shop = shop, onSaveShop = onSaveShop)
        }

        item {
            GoldOutlineCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = palette.primary)
                    Text("Private Owner Notification Settings", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                }
                Spacer(Modifier.height(4.dp))
                Text("Customers cannot read this document. Used strictly by Owner Admin & Cloud Functions.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = privateWhatsapp,
                    onValueChange = { privateWhatsapp = it },
                    label = { Text("Private Owner WhatsApp Alert Number") },
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
                    border = BorderStroke(1.dp, palette.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Private Notification Settings", color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            GoldOutlineCard {
                Text("Customer Deletion Requests (${deletionRequests.size})", style = MaterialTheme.typography.titleMedium, color = palette.accent)
                Spacer(Modifier.height(6.dp))
                if (deletionRequests.isEmpty()) {
                    Text("No pending customer account deletion requests.", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                } else {
                    deletionRequests.forEach { summary ->
                        Text(
                            text = "• ${summary.profile.displayName} (${summary.profile.phone.ifBlank { summary.profile.email }})",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusClosedRed
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, StatusClosedRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = StatusClosedRed)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out of Owner Admin Panel", color = StatusClosedRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}
