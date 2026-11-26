package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.PaymentStatus
import com.example.data.model.RequestStatus
import com.example.data.model.ShopOpenEvaluation
import com.example.data.model.ShopSettings
import com.example.data.model.WhatsAppDeliveryStatus
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LocalMarutiPalette
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusCompletedTeal
import com.example.ui.theme.StatusConfirmedBlue
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.theme.StatusPreparingPurple
import com.example.ui.theme.StatusReadyCyan
import com.example.ui.theme.WarmSaffron
import com.example.util.DomainCalculators
import com.example.util.ExternalIntentHelper
import com.example.util.ReceiptAndReportExporter

@Composable
fun GoldOutlineCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalMarutiPalette.current.card,
    borderColor: Color = LocalMarutiPalette.current.primary.copy(alpha = 0.45f),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalMarutiPalette.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = palette.textPrimary
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AnimatedSuccessCheckmark(
    modifier: Modifier = Modifier.size(34.dp),
    tint: Color = StatusOpenGreen
) {
    val circleProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        circleProgress.snapTo(0f)
        checkProgress.snapTo(0f)
        circleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        )
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        )
    }

    Canvas(
        modifier = modifier.semantics { contentDescription = "Order confirmed checkmark" }
    ) {
        val strokeWidth = size.minDimension * 0.11f
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = tint.copy(alpha = 0.16f * circleProgress.value),
            radius = radius,
            center = center
        )

        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 360f * circleProgress.value,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val pStart = Offset(size.width * 0.28f, size.height * 0.52f)
        val pMid = Offset(size.width * 0.44f, size.height * 0.67f)
        val pEnd = Offset(size.width * 0.73f, size.height * 0.36f)

        val firstSegProgress = (checkProgress.value / 0.42f).coerceIn(0f, 1f)
        val secondSegProgress = ((checkProgress.value - 0.42f) / 0.58f).coerceIn(0f, 1f)

        if (firstSegProgress > 0f) {
            val midPt = Offset(
                x = pStart.x + (pMid.x - pStart.x) * firstSegProgress,
                y = pStart.y + (pMid.y - pStart.y) * firstSegProgress
            )
            drawLine(
                color = tint,
                start = pStart,
                end = midPt,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        if (secondSegProgress > 0f) {
            val endPt = Offset(
                x = pMid.x + (pEnd.x - pMid.x) * secondSegProgress,
                y = pMid.y + (pEnd.y - pMid.y) * secondSegProgress
            )
            drawLine(
                color = tint,
                start = pMid,
                end = endPt,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OrderSummarySuccessBanner(
    visible: Boolean,
    orderNumber: String?,
    isHindi: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "order_success_banner"
) {
    val palette = LocalMarutiPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + expandVertically(
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f)
        ),
        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StatusOpenGreen.copy(alpha = 0.16f),
            border = BorderStroke(1.2.dp, StatusOpenGreen.copy(alpha = 0.75f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedSuccessCheckmark(modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isHindi) "ऑर्डर सफलतापूर्वक दर्ज हो गया!" else "Order Confirmed!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusOpenGreen
                    )
                    Text(
                        text = buildString {
                            if (!orderNumber.isNullOrBlank()) {
                                append("$orderNumber • ")
                            }
                            append(
                                if (isHindi) "डिजिटल रसीद तैयार की जा रही है..."
                                else "Preparing your digital receipt..."
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.78f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RequestStatusPill(
    statusRaw: String,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val status = RequestStatus.fromRaw(statusRaw)
    val (label, color, icon) = when (status) {
        RequestStatus.PENDING -> Triple(strings.statusPending, StatusPendingAmber, Icons.Default.Schedule)
        RequestStatus.CONFIRMED -> Triple(strings.statusConfirmed, StatusConfirmedBlue, Icons.Default.CheckCircle)
        RequestStatus.PREPARING -> Triple(strings.statusPreparing, StatusPreparingPurple, Icons.Default.Restaurant)
        RequestStatus.READY -> Triple(strings.statusReady, StatusReadyCyan, Icons.Default.Verified)
        RequestStatus.REJECTED -> Triple(strings.statusRejected, StatusClosedRed, Icons.Default.Close)
        RequestStatus.COMPLETED -> Triple(strings.statusCompleted, StatusOpenGreen, Icons.Default.DoneAll)
    }
    StatusPill(text = label, color = color, leadingIcon = icon, modifier = modifier)
}

@Composable
fun PaymentStatusPill(paymentMethod: String, paymentStatusRaw: String) {
    val status = PaymentStatus.fromRaw(paymentStatusRaw)
    val (label, color) = when (status) {
        PaymentStatus.PAID -> "$paymentMethod • PAID" to StatusOpenGreen
        PaymentStatus.UPI_PENDING_VERIFICATION -> "$paymentMethod • VERIFY PENDING" to StatusPendingAmber
        PaymentStatus.REFUNDED -> "$paymentMethod • REFUNDED" to MutedSlate
        PaymentStatus.UNPAID -> "$paymentMethod • UNPAID" to WarmSaffron
    }
    StatusPill(text = label, color = color)
}

@Composable
fun WhatsAppStatusPill(statusRaw: String) {
    val status = WhatsAppDeliveryStatus.fromRaw(statusRaw)
    val (label, color) = when (status) {
        WhatsAppDeliveryStatus.SENT -> "WA: SENT" to StatusOpenGreen
        WhatsAppDeliveryStatus.MANUAL_SHARED -> "WA: SHARED" to StatusCompletedTeal
        WhatsAppDeliveryStatus.FAILED -> "WA: FAILED" to StatusClosedRed
        WhatsAppDeliveryStatus.NOT_CONFIGURED -> "WA: MANUAL" to StatusPendingAmber
        WhatsAppDeliveryStatus.PENDING_SERVER -> "WA: QUEUED" to RoyalGold
    }
    StatusPill(text = label, color = color)
}

@Composable
fun ShopClosedBanner(
    evaluation: ShopOpenEvaluation,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    if (evaluation.isOpen) return
    Card(
        modifier = modifier.fillMaxWidth().testTag("shop_closed_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusClosedRed.copy(alpha = 0.16f)),
        border = BorderStroke(1.5.dp, StatusClosedRed.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LockClock,
                contentDescription = strings.shopClosed,
                tint = StatusClosedRed,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.shopClosed,
                    style = MaterialTheme.typography.titleMedium,
                    color = StatusClosedRed,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.shopClosedOrderingDisabled,
                    style = MaterialTheme.typography.bodySmall,
                    color = CreamIvory
                )
                if (!evaluation.nextOpeningText.isNullOrBlank()) {
                    Text(
                        text = "${strings.nextOpeningLabel}: ${evaluation.nextOpeningText}",
                        style = MaterialTheme.typography.labelMedium,
                        color = LuminousGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun QuantityStepper(
    quantity: Long,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    enabled: Boolean = true,
    testTagPrefix: String = "qty"
) {
    val palette = LocalMarutiPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.elevated)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = enabled && quantity > 0L,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("${testTagPrefix}_dec")
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease quantity",
                tint = if (enabled && quantity > 0L) palette.primary else palette.textSecondary.copy(alpha = 0.4f)
            )
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            onClick = onIncrement,
            enabled = enabled,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("${testTagPrefix}_inc")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase quantity",
                tint = if (enabled) palette.primary else palette.textSecondary.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun ProductOrServiceImage(
    imageUrl: String,
    localFallbackRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (imageUrl.isNotBlank()) {
        val request = remember(imageUrl) {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(300)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            placeholder = painterResource(id = localFallbackRes),
            error = painterResource(id = localFallbackRes),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = localFallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

fun localDrawableForProductKey(key: String): Int = when (key.lowercase()) {
    "guest_room" -> R.drawable.img_guest_room
    "freshen_up" -> R.drawable.img_freshen_up
    "banner" -> R.drawable.img_khatu_shyam_banner
    else -> R.drawable.img_food_platter
}

@Composable
fun DevotionalScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalMarutiPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(palette.card, palette.elevated, palette.card)
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_maruti_logo),
                contentDescription = "MARUTI DHAM emblem",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.accent
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DigitalReceiptDialog(
    request: CustomerRequest,
    shopSettings: ShopSettings,
    strings: AppStrings,
    isNewSubmission: Boolean = true,
    targetWhatsAppOverride: String = "",
    onMarkWhatsAppShared: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveWhatsAppTarget = remember(targetWhatsAppOverride, shopSettings.whatsappNumber) {
        targetWhatsAppOverride.ifBlank { shopSettings.whatsappNumber }.trim()
    }
    val formattedWhatsAppMessage = remember(request, shopSettings) {
        DomainCalculators.formatWhatsAppRequestMessage(request, shopSettings)
    }
    val formattedReceipt = remember(request, shopSettings) {
        DomainCalculators.formatReceiptText(request, shopSettings)
    }
    val createdMillis = request.createdAt?.toDate()?.time ?: System.currentTimeMillis()
    val orderNum = request.orderNumber.ifBlank { request.id.take(10) }
    val subtotal = remember(request) { DomainCalculators.calculateRequestSubtotal(request) }
    val palette = LocalMarutiPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = LuminousGold,
        textContentColor = CreamIvory,
        icon = {
            if (isNewSubmission) {
                AnimatedSuccessCheckmark(
                    modifier = Modifier.size(42.dp),
                    tint = StatusOpenGreen
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = RoyalGold,
                    modifier = Modifier.size(38.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isNewSubmission) strings.requestSubmittedTitle else "Digital Receipt • $orderNum",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isNewSubmission) {
                    Text(
                        text = strings.requestSavedPendingNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = CreamIvory
                    )
                }

                // Crisp Digital Receipt Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NavyDeep,
                    border = BorderStroke(1.5.dp, AntiqueGold.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = shopSettings.shopName,
                            style = MaterialTheme.typography.titleMedium,
                            color = LuminousGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "FOOD ORDER RECEIPT",
                            style = MaterialTheme.typography.labelMedium,
                            color = CreamIvory,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Owner: ${shopSettings.ownerName} • Khatu Shyam Ji",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = AntiqueGold.copy(alpha = 0.35f)
                        )
                        ReceiptKeyValueRow("Order ID", orderNum, highlight = true)
                        ReceiptKeyValueRow("Date", DomainCalculators.formatIstDate(createdMillis))
                        ReceiptKeyValueRow("Time", DomainCalculators.formatIstTime(createdMillis))
                        ReceiptKeyValueRow("Customer", request.customerName)
                        ReceiptKeyValueRow("Mobile", request.contactPhone)
                        ReceiptKeyValueRow("Mode", "${request.fulfillmentType} (${request.expectedTime})")
                        ReceiptKeyValueRow("Payment", "${request.paymentMethod} • ${request.paymentStatus}")
                        if (!request.upiTransactionRef.isNullOrBlank()) {
                            ReceiptKeyValueRow("UPI Ref", request.upiTransactionRef)
                        }
                        if (!request.deliveryAddress.isNullOrBlank()) {
                            ReceiptKeyValueRow("Address", "${request.deliveryLocality ?: ""} ${request.deliveryAddress}".trim())
                        }

                        if (request.items.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = AntiqueGold.copy(alpha = 0.35f)
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ITEM", style = MaterialTheme.typography.labelSmall, color = RoyalGold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f))
                                Text("QTY", style = MaterialTheme.typography.labelSmall, color = RoyalGold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.45f), textAlign = TextAlign.Center)
                                Text("RATE", style = MaterialTheme.typography.labelSmall, color = RoyalGold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.65f), textAlign = TextAlign.End)
                                Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = RoyalGold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.75f), textAlign = TextAlign.End)
                            }
                            request.items.forEach { item ->
                                val itemLabel = if (item.variantLabel.isNotBlank()) "${item.name} (${item.variantLabel})" else item.name
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(itemLabel, style = MaterialTheme.typography.bodySmall, color = CreamIvory, modifier = Modifier.weight(1.6f))
                                    Text("${item.quantity}", style = MaterialTheme.typography.bodySmall, color = CreamIvory, modifier = Modifier.weight(0.45f), textAlign = TextAlign.Center)
                                    Text(
                                        text = if (item.unitPrice > 0L) "₹${item.unitPrice}" else "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedSlate,
                                        modifier = Modifier.weight(0.65f),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (item.lineTotal > 0L) "₹${item.lineTotal}" else "TBD",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LuminousGold,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(0.75f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = AntiqueGold.copy(alpha = 0.35f)
                        )
                        ReceiptKeyValueRow(strings.subtotalLabel, if (subtotal > 0L || request.grandTotal > 0L) "₹$subtotal" else "TBD")
                        if (request.deliveryCharge > 0L) {
                            ReceiptKeyValueRow(strings.deliveryChargeLabel, "₹${request.deliveryCharge}")
                        }
                        if (request.packagingCharge > 0L) {
                            ReceiptKeyValueRow(strings.packagingChargeLabel, "₹${request.packagingCharge}")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, color = LuminousGold, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (request.grandTotal > 0L) "₹${request.grandTotal}" else "Rate TBD",
                                style = MaterialTheme.typography.titleMedium,
                                color = LuminousGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = AntiqueGold.copy(alpha = 0.25f)
                        )
                        Text(
                            text = "THANK YOU FOR VISITING\nMARUTI DHAM",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = RoyalGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Export / Share Action Grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { ReceiptAndReportExporter.printOrSaveReceiptPdf(context, request, shopSettings) },
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier.testTag("receipt_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalGold)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.printPdfLabel, color = RoyalGold, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { ReceiptAndReportExporter.shareReceiptImage(context, request, shopSettings) },
                        border = BorderStroke(1.dp, RoyalGold),
                        modifier = Modifier.testTag("receipt_image_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalGold)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.shareImageLabel, color = RoyalGold, style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            ExternalIntentHelper.copyToClipboard(context, "MARUTI DHAM Receipt $orderNum", formattedReceipt)
                        },
                        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = CreamIvory)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.copyMessage, color = CreamIvory, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhatsAppStatusPill(statusRaw = request.whatsappStatus)
                    Text(
                        text = when (WhatsAppDeliveryStatus.fromRaw(request.whatsappStatus)) {
                            WhatsAppDeliveryStatus.SENT -> "Sent automatically to owner."
                            WhatsAppDeliveryStatus.MANUAL_SHARED -> "Opened share options."
                            WhatsAppDeliveryStatus.FAILED -> request.whatsappError ?: "Auto-send failed; tap below to share."
                            else -> if (effectiveWhatsAppTarget.isNotBlank()) {
                                "Tap below to send via WhatsApp."
                            } else {
                                "Tap below to share your bill on WhatsApp."
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (request.id.isNotBlank()) onMarkWhatsAppShared(request.id)
                    ExternalIntentHelper.launchWhatsAppShare(
                        context = context,
                        message = formattedWhatsAppMessage,
                        targetWhatsAppNumber = effectiveWhatsAppTarget
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusOpenGreen, contentColor = Color.White),
                modifier = Modifier.testTag("whatsapp_share_dialog_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.shareOnWhatsApp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeLabel, color = MutedSlate)
            }
        }
    )
}

@Composable
private fun ReceiptKeyValueRow(
    key: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) LuminousGold else CreamIvory,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ScrollToTopIndicator(
    listState: LazyListState,
    label: String = "Top",
    modifier: Modifier = Modifier,
    testTag: String = "scroll_to_top_button"
) {
    val palette = LocalMarutiPalette.current
    val scope = rememberCoroutineScope()
    val showIndicator by remember(listState) {
        derivedStateOf {
            listState.canScrollBackward && (
                listState.firstVisibleItemIndex >= 2 ||
                    (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 120) ||
                    listState.firstVisibleItemScrollOffset > 360
                )
        }
    }

    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.85f, animationSpec = spring()),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Surface(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            shape = RoundedCornerShape(999.dp),
            color = palette.elevated,
            contentColor = palette.accent,
            border = BorderStroke(1.5.dp, palette.primary),
            shadowElevation = 8.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
                .semantics { contentDescription = "Scroll to top" }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
