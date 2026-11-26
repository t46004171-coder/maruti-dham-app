package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.data.model.AppStrings
import com.example.data.model.DeliverySettings
import com.example.data.model.FulfillmentType
import com.example.data.model.ShopSettings
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthHelper
import com.example.ui.components.DevotionalScreenHeader
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.ScrollToTopIndicator
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.util.DefaultCatalogData
import com.example.util.ExternalIntentHelper

@Composable
fun SettingsScreen(
    shopSettingsState: UiState<ShopSettings>,
    deliverySettingsState: UiState<DeliverySettings>,
    userProfile: UserProfile?,
    languageCode: String,
    userName: String,
    userEmail: String,
    isOwner: Boolean,
    pendingNewOrdersCount: Int = 0,
    hasNewlyArrivedOrders: Boolean = false,
    isSubmitting: Boolean,
    strings: AppStrings,
    onLanguageChange: (String) -> Unit,
    onRegisterCustomer: (String, String, String, FulfillmentType, String, String, () -> Unit) -> Unit,
    onRequestAccountDeletion: () -> Unit,
    onNavigateToMyRequests: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val palette = LocalMarutiPalette.current
    val shop = (shopSettingsState as? UiState.Success)?.data ?: DefaultCatalogData.defaultShopSettings
    val deliverySettings = (deliverySettingsState as? UiState.Success)?.data ?: DefaultCatalogData.defaultDeliverySettings

    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogBody by remember { mutableStateOf<String?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DevotionalScreenHeader(
            title = strings.navSettings,
            subtitle = "$userName ($userEmail)"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Customer Profile Card
            item {
                GoldOutlineCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = RoyalGold)
                            Text("Customer Profile", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                        }
                        TextButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.editLabel, color = RoyalGold)
                        }
                    }
                    Text("Name: ${userProfile?.displayName?.ifBlank { userName } ?: userName}", color = CreamIvory, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Mobile: ${userProfile?.phone?.ifBlank { "Not registered yet" } ?: "Not registered yet"}",
                        color = if (userProfile?.phone.isNullOrBlank()) MutedSlate else CreamIvory,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Preference: ${userProfile?.preferredFulfillment ?: FulfillmentType.PICKUP.name}",
                        color = MutedSlate,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!userProfile?.defaultAddress.isNullOrBlank()) {
                        Text(
                            text = "Address: ${userProfile?.defaultLocality ?: ""} — ${userProfile?.defaultAddress}",
                            color = MutedSlate,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Language selector
            item {
                GoldOutlineCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = RoyalGold)
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = languageCode == "en",
                            onClick = { onLanguageChange("en") },
                            label = { Text(strings.englishOption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = NavyDeep
                            ),
                            modifier = Modifier.testTag("settings_lang_en")
                        )
                        FilterChip(
                            selected = languageCode == "hi",
                            onClick = { onLanguageChange("hi") },
                            label = { Text(strings.hindiOption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalGold,
                                selectedLabelColor = NavyDeep
                            ),
                            modifier = Modifier.testTag("settings_lang_hi")
                        )
                    }
                }
            }

            // My Requests & Owner Admin Panel
            item {
                GoldOutlineCard(containerColor = NavyElevated) {
                    Button(
                        onClick = onNavigateToMyRequests,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_my_requests_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.myRequestsTitle, fontWeight = FontWeight.Bold)
                    }

                    if (isOwner) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onNavigateToAdmin,
                            border = BorderStroke(1.dp, LuminousGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_admin_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LuminousGold)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.ownerAdminPanel, color = LuminousGold, fontWeight = FontWeight.Bold)
                            if (pendingNewOrdersCount > 0) {
                                Spacer(Modifier.width(8.dp))
                                Badge(
                                    containerColor = if (hasNewlyArrivedOrders) StatusClosedRed else RoyalGold,
                                    contentColor = if (hasNewlyArrivedOrders) Color.White else NavyDeep,
                                    modifier = Modifier.testTag("settings_owner_admin_badge")
                                ) {
                                    Text(
                                        text = if (hasNewlyArrivedOrders) {
                                            "$pendingNewOrdersCount New"
                                        } else {
                                            pendingNewOrdersCount.coerceAtMost(99).toString()
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Storefront Contact Info (never exposes personal owner phone/email unless explicitly published)
            item {
                GoldOutlineCard {
                    Text(strings.contactInfoTitle, style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                    Text("Owner: ${shop.ownerName}", style = MaterialTheme.typography.bodySmall, color = RoyalGold)
                    Text(shop.address, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                    if (shop.contactPhone.isNotBlank() || shop.contactEmail.isNotBlank() || shop.whatsappNumber.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        if (shop.contactPhone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { ExternalIntentHelper.launchPhoneDialer(context, shop.contactPhone) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(18.dp))
                                Text(shop.contactPhone, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                            }
                        }
                        if (shop.contactEmail.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(18.dp))
                                Text(shop.contactEmail, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                            }
                        }
                        if (shop.whatsappNumber.isNotBlank()) {
                            Text(
                                text = "Storefront WhatsApp: ${shop.whatsappNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (strings.isHindi) "सभी ऑर्डर और कमरे की सहायता दुकान काउंटर (मांडा चौराहा) पर उपलब्ध है।"
                            else "Order & room assistance is handled directly at the MARUTI DHAM stall, Manda Chauraha.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
            }

            // About, Privacy, Terms, Help, Version
            item {
                GoldOutlineCard {
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = strings.aboutMarutiDham,
                        onClick = {
                            dialogTitle = strings.aboutMarutiDham
                            dialogBody = "${shop.aboutText}\n\n${strings.addressLabel}: ${shop.address}"
                        }
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Policy,
                        title = strings.privacyPolicyTitle,
                        onClick = {
                            dialogTitle = strings.privacyPolicyTitle
                            dialogBody = shop.privacyPolicy
                        }
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.VerifiedUser,
                        title = strings.termsTitle,
                        onClick = {
                            dialogTitle = strings.termsTitle
                            dialogBody = shop.termsText
                        }
                    )
                    SettingsInfoRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = strings.helpSupportTitle,
                        onClick = {
                            dialogTitle = strings.helpSupportTitle
                            dialogBody = "${strings.whatsAppApiSetupNote}\n\n${strings.roomInspectNote}\n\nLocation: ${shop.address}"
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${strings.appVersionTitle}: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSlate
                    )
                }
            }

            // Data Consent & Account Deletion
            item {
                GoldOutlineCard {
                    Text(
                        text = if (strings.isHindi) "डेटा गोपनीयता एवं खाता हटाना" else "Data Consent & Account Control",
                        style = MaterialTheme.typography.titleSmall,
                        color = LuminousGold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (userProfile?.deletionRequested == true) strings.accountDeletionRequestedNote else shop.privacyPolicy,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (userProfile?.deletionRequested == true) RoyalGold else MutedSlate
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRequestAccountDeletion,
                        enabled = !isSubmitting && userProfile?.deletionRequested != true,
                        border = BorderStroke(1.dp, StatusClosedRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("request_account_deletion_button")
                    ) {
                        Text(
                            text = if (userProfile?.deletionRequested == true) "Deletion Requested" else strings.requestAccountDeletionLabel,
                            color = if (userProfile?.deletionRequested == true) MutedSlate else StatusClosedRed
                        )
                    }
                }
            }

            // Sign Out
            item {
                OutlinedButton(
                    onClick = {
                        AuthHelper.signOut(credentialManager = credentialManager, scope = scope)
                    },
                    border = BorderStroke(1.dp, StatusClosedRed.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = StatusClosedRed)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.signOutLabel, color = StatusClosedRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        ScrollToTopIndicator(
            listState = listState,
            label = if (strings.isHindi) "ऊपर" else "Top",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            testTag = "settings_scroll_to_top"
        )
    }
    }

    if (showEditProfileDialog) {
        CustomerRegistrationDialog(
            userProfile = userProfile,
            defaultName = userName,
            defaultEmail = userEmail,
            allowedLocalities = deliverySettings.allowedLocalities,
            isSubmitting = isSubmitting,
            strings = strings,
            onSaveRegistration = onRegisterCustomer,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (dialogTitle != null && dialogBody != null) {
        AlertDialog(
            onDismissRequest = { dialogTitle = null; dialogBody = null },
            containerColor = NavyCard,
            titleContentColor = LuminousGold,
            textContentColor = CreamIvory,
            title = { Text(dialogTitle!!) },
            text = { Text(dialogBody!!, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { dialogTitle = null; dialogBody = null }) {
                    Text(strings.closeLabel, color = RoyalGold)
                }
            }
        )
    }
}

@Composable
private fun SettingsInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = CreamIvory)
    }
}
