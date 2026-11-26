package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TempleHindu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AppStrings
import com.example.data.model.FulfillmentType
import com.example.data.model.UserProfile
import com.example.data.repository.AuthHelper
import com.example.ui.components.GoldOutlineCard
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.util.DomainCalculators
import kotlinx.coroutines.delay

@Composable
fun MarutiSplashScreen(
    strings: AppStrings,
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(550))
        delay(850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, NavyElevated, NavyDeep)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
                .alpha(alpha.value.coerceAtLeast(0.35f))
        ) {
            Surface(
                shape = CircleShape,
                border = BorderStroke(2.dp, LuminousGold),
                color = NavyDeep,
                shadowElevation = 12.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_maruti_logo),
                    contentDescription = "MARUTI DHAM emblem",
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = strings.jaiShriShyam,
                style = MaterialTheme.typography.labelLarge,
                color = LuminousGold
            )
            Text(
                text = strings.brandTitle,
                style = MaterialTheme.typography.displaySmall,
                color = CreamIvory,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.ownerSubtitle,
                style = MaterialTheme.typography.labelMedium,
                color = RoyalGold,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.subtitleLocation,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AuthScreen(
    strings: AppStrings,
    languageCode: String,
    loginMode: String,
    accessDeniedMessage: String?,
    onLoginModeChange: (String) -> Unit,
    onClearAccessDenied: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onAuthSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    if (loginMode != "SELECTOR") {
        BackHandler {
            authError = null
            onClearAccessDenied()
            onLoginModeChange("SELECTOR")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, NavyCard, NavyDeep)))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loginMode != "SELECTOR") {
                    TextButton(
                        onClick = {
                            authError = null
                            onClearAccessDenied()
                            onLoginModeChange("SELECTOR")
                        },
                        modifier = Modifier.testTag("auth_back_to_options")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RoyalGold)
                        Spacer(Modifier.size(6.dp))
                        Text(if (strings.isHindi) "वापस" else "Login Options", color = RoyalGold)
                    }
                } else {
                    Spacer(Modifier.size(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    FilterChip(
                        selected = languageCode == "en",
                        onClick = { onLanguageChange("en") },
                        label = { Text("English") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalGold,
                            selectedLabelColor = NavyDeep
                        ),
                        modifier = Modifier.testTag("auth_lang_en")
                    )
                    Spacer(Modifier.size(8.dp))
                    FilterChip(
                        selected = languageCode == "hi",
                        onClick = { onLanguageChange("hi") },
                        label = { Text("हिन्दी") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalGold,
                            selectedLabelColor = NavyDeep
                        ),
                        modifier = Modifier.testTag("auth_lang_hi")
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, AntiqueGold.copy(alpha = 0.7f)),
                color = NavyCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_khatu_shyam_banner),
                        contentDescription = "Khatu Shyam Ji devotional banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.TempleHindu, contentDescription = null, tint = LuminousGold, modifier = Modifier.size(20.dp))
                            Text(
                                text = strings.jaiShriShyam,
                                style = MaterialTheme.typography.labelLarge,
                                color = LuminousGold
                            )
                        }
                        Text(
                            text = strings.brandTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = CreamIvory,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.ownerSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalGold,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = strings.subtitleLocation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            val combinedError = accessDeniedMessage ?: authError
            if (!combinedError.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusClosedRed.copy(alpha = 0.16f)),
                    border = BorderStroke(1.dp, StatusClosedRed),
                    modifier = Modifier.fillMaxWidth().testTag("auth_access_denied_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = StatusClosedRed)
                        Text(
                            text = combinedError,
                            style = MaterialTheme.typography.bodySmall,
                            color = CreamIvory
                        )
                    }
                }
            }

            when (loginMode) {
                "SELECTOR" -> {
                    GoldOutlineCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.signInHeadline,
                                style = MaterialTheme.typography.titleLarge,
                                color = LuminousGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (strings.isHindi) "कृपया अपनी लॉगिन श्रेणी चुनें" else "Choose your login portal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    authError = null
                                    onClearAccessDenied()
                                    onLoginModeChange("CUSTOMER")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("choose_customer_login")
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null)
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    text = "1. ${strings.customerLoginLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    authError = null
                                    onClearAccessDenied()
                                    onLoginModeChange("OWNER")
                                },
                                border = BorderStroke(1.5.dp, LuminousGold),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("choose_owner_login")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LuminousGold)
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    text = "2. ${strings.ownerLoginLabel}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LuminousGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = strings.cashOnlyBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                "OWNER" -> {
                    GoldOutlineCard(containerColor = NavyElevated) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = LuminousGold,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = strings.ownerLoginLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = LuminousGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (strings.isHindi) {
                                    "केवल अधिकृत मालिक का Google खाता ही एडमिन डैशबोर्ड खोल सकता है। सार्वजनिक मालिक पंजीकरण बंद है।"
                                } else {
                                    "Restricted Admin Access: Only the verified authorized MARUTI DHAM Owner Google account is permitted. Public owner registration is disabled and verified via Firebase Security Rules."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = CreamIvory,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    authError = null
                                    onClearAccessDenied()
                                    isLoading = true
                                    AuthHelper.onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess("OWNER")
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            authError = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("owner_google_sign_in_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyDeep, strokeWidth = 2.5.dp)
                                } else {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        text = if (strings.isHindi) "मालिक Google साइन इन" else "Verify Owner with Google",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    GoldOutlineCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.customerLoginLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = LuminousGold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.signInSubhead,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CreamIvory,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    authError = null
                                    onClearAccessDenied()
                                    isLoading = true
                                    AuthHelper.onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess("CUSTOMER")
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            authError = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isLoading = false
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyDeep, strokeWidth = 2.5.dp)
                                } else {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        text = strings.signInWithGoogle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = strings.noAdvancePaymentNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSlate,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerRegistrationDialog(
    userProfile: UserProfile?,
    defaultName: String,
    defaultEmail: String,
    allowedLocalities: List<String>,
    isSubmitting: Boolean,
    strings: AppStrings,
    onSaveRegistration: (String, String, String, FulfillmentType, String, String, () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(userProfile) { mutableStateOf(userProfile?.displayName?.ifBlank { defaultName } ?: defaultName) }
    var mobile by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var email by remember(userProfile) { mutableStateOf(userProfile?.email?.ifBlank { defaultEmail } ?: defaultEmail) }
    var preference by remember(userProfile) {
        mutableStateOf(FulfillmentType.fromRaw(userProfile?.preferredFulfillment))
    }
    var locality by remember(userProfile, allowedLocalities) {
        mutableStateOf(
            userProfile?.defaultLocality?.ifBlank { allowedLocalities.firstOrNull() ?: "Khatu Shyam Ji" }
                ?: (allowedLocalities.firstOrNull() ?: "Khatu Shyam Ji")
        )
    }
    var address by remember(userProfile) { mutableStateOf(userProfile?.defaultAddress ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RoyalGold,
        unfocusedBorderColor = AntiqueGold.copy(alpha = 0.45f),
        focusedLabelColor = LuminousGold,
        unfocusedLabelColor = MutedSlate,
        focusedTextColor = CreamIvory,
        unfocusedTextColor = CreamIvory
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        titleContentColor = LuminousGold,
        textContentColor = CreamIvory,
        icon = {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(32.dp))
        },
        title = { Text(strings.completeRegistrationTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.completeRegistrationSubtitle, style = MaterialTheme.typography.bodySmall, color = MutedSlate)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.customerNameLabel) },
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text(strings.contactPhoneLabel) },
                    placeholder = { Text("10-digit Indian mobile") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Preferred Order Type:", style = MaterialTheme.typography.labelLarge, color = CreamIvory)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preference == FulfillmentType.PICKUP,
                        onClick = { preference = FulfillmentType.PICKUP },
                        label = { Text(strings.pickupLabel) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                    )
                    FilterChip(
                        selected = preference == FulfillmentType.DELIVERY,
                        onClick = { preference = FulfillmentType.DELIVERY },
                        label = { Text(strings.deliveryLabel) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                    )
                }

                if (preference == FulfillmentType.DELIVERY) {
                    Text(strings.deliveryLocalityLabel, style = MaterialTheme.typography.labelMedium, color = CreamIvory)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allowedLocalities.forEach { loc ->
                            FilterChip(
                                selected = locality == loc,
                                onClick = { locality = loc },
                                label = { Text(loc) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalGold, selectedLabelColor = NavyDeep)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(strings.deliveryAddressLabel) },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                validationError?.let { err ->
                    Text(err, style = MaterialTheme.typography.bodySmall, color = StatusClosedRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.trim().length < 2 -> validationError = "Please enter your full name."
                        !DomainCalculators.isValidMobileNumber(mobile) -> validationError = "Enter a valid 10-digit Indian mobile number."
                        preference == FulfillmentType.DELIVERY && address.trim().length < 4 ->
                            validationError = "Please enter your Khatu Shyam Ji delivery address."
                        else -> {
                            validationError = null
                            onSaveRegistration(name.trim(), mobile.trim(), email.trim(), preference, address.trim(), locality) {
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                modifier = Modifier.testTag("reg_save_button")
            ) {
                Text(strings.saveLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelLabel, color = MutedSlate)
            }
        }
    )
}
