package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppStrings
import com.example.data.model.CustomerRequest
import com.example.data.model.FreshenUpConfig
import com.example.data.model.RequestCategory
import com.example.data.model.RoomConfig
import com.example.data.model.RoomStatusLabel
import com.example.data.model.RoomType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AnimatedSuccessCheckmark
import com.example.ui.components.DevotionalScreenHeader
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.OrderSummarySuccessBanner
import com.example.ui.components.ProductOrServiceImage
import com.example.ui.components.QuantityStepper
import com.example.ui.components.ScrollToTopIndicator
import com.example.ui.components.StatusPill
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.util.DefaultCatalogData
import com.example.util.DomainCalculators
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomsAndFreshenUpScreen(
    roomConfigsState: UiState<List<RoomConfig>>,
    freshenUpConfigsState: UiState<List<FreshenUpConfig>>,
    userProfile: UserProfile?,
    defaultUserName: String,
    isSubmitting: Boolean,
    recentSuccessOrder: CustomerRequest? = null,
    strings: AppStrings,
    onSubmitRoomRequest: (RoomConfig, Int, Int, Int, String, String, String, String) -> Unit,
    onSubmitFreshenUpRequest: (FreshenUpConfig, Int, String, String, String, String) -> Unit
) {
    val rooms = (roomConfigsState as? UiState.Success)?.data ?: DefaultCatalogData.initialRoomConfigs
    val freshenOptions = (freshenUpConfigsState as? UiState.Success)?.data ?: DefaultCatalogData.initialFreshenUpConfigs

    var selectedRoomType by remember { mutableStateOf(RoomType.AC.name) }
    val selectedRoom = remember(rooms, selectedRoomType) {
        rooms.firstOrNull { it.roomType == selectedRoomType } ?: rooms.first()
    }
    var selectedDurationHours by remember { mutableIntStateOf(6) }
    var numberOfRooms by remember { mutableIntStateOf(1) }
    var numberOfPeopleRooms by remember { mutableIntStateOf(2) }
    var roomArrivalTime by remember { mutableStateOf("") }
    var roomNotes by remember { mutableStateOf("") }

    var selectedFreshenKey by remember { mutableStateOf(freshenOptions.firstOrNull()?.optionKey ?: "ONLY_FRESH") }
    val selectedFreshen = remember(freshenOptions, selectedFreshenKey) {
        freshenOptions.firstOrNull { it.optionKey == selectedFreshenKey } ?: freshenOptions.first()
    }
    var numberOfPeopleFreshen by remember { mutableIntStateOf(1) }
    var freshenArrivalTime by remember { mutableStateOf("") }
    var freshenNotes by remember { mutableStateOf("") }

    var customerName by remember { mutableStateOf(defaultUserName) }
    var contactPhone by remember { mutableStateOf("") }

    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            if (customerName.isBlank() && profile.displayName.isNotBlank()) customerName = profile.displayName
            if (contactPhone.isBlank() && profile.phone.isNotBlank()) contactPhone = profile.phone
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = RoyalGold,
        unfocusedBorderColor = AntiqueGold.copy(alpha = 0.45f),
        focusedLabelColor = LuminousGold,
        unfocusedLabelColor = MutedSlate,
        focusedTextColor = CreamIvory,
        unfocusedTextColor = CreamIvory
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DevotionalScreenHeader(
            title = strings.navRoomsFreshenUp,
            subtitle = strings.subtitleLocation
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Continue without booking a room shortcut
            item {
                OutlinedButton(
                    onClick = { scope.launch { listState.animateScrollToItem(4) } },
                    border = BorderStroke(1.dp, RoyalGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skip_room_booking_button")
                ) {
                    Icon(Icons.Default.Shower, contentDescription = null, tint = RoyalGold)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.skipRoomBookingBtn, color = RoyalGold)
                }
            }

            item {
                Text(
                    text = strings.roomsSectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = LuminousGold
                )
            }

            // AC and Non-AC Room Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rooms.forEach { room ->
                        val isSelected = room.roomType == selectedRoomType
                        val statusEnum = RoomStatusLabel.fromRaw(room.statusLabel)
                        val isRoomAvailable = room.available && statusEnum == RoomStatusLabel.AVAILABLE
                        val (statusText, statusColor) = when {
                            !room.available || statusEnum == RoomStatusLabel.CLOSED -> strings.closedLabel to StatusClosedRed
                            statusEnum == RoomStatusLabel.FULLY_BOOKED -> strings.fullyBookedLabel to StatusPendingAmber
                            else -> "${strings.availableLabel} (${room.availableCount})" to StatusOpenGreen
                        }

                        GoldOutlineCard(
                            containerColor = if (isSelected) NavyElevated else com.example.ui.theme.NavyCard,
                            modifier = Modifier.clickable { selectedRoomType = room.roomType }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductOrServiceImage(
                                    imageUrl = room.imageUrl,
                                    localFallbackRes = R.drawable.img_guest_room,
                                    contentDescription = room.title,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (room.roomType == RoomType.AC.name) Icons.Default.AcUnit else Icons.Default.WindPower,
                                            contentDescription = null,
                                            tint = RoyalGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (strings.isHindi && room.titleHindi.isNotBlank()) "${room.titleHindi} (${room.title})" else room.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CreamIvory
                                        )
                                    }
                                    StatusPill(text = statusText, color = statusColor)
                                    Text(
                                        text = "6h: ₹${room.rate6Hours}  •  12h: ₹${room.rate12Hours}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LuminousGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRoomAvailable) "Tap to select ${room.title}" else "Currently unavailable for booking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSlate
                                )
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRoomType = room.roomType },
                                    label = { Text(if (isSelected) "Selected" else "Select") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalGold,
                                        selectedLabelColor = NavyDeep
                                    ),
                                    modifier = Modifier.testTag("select_room_${room.roomType.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Room Booking Form
            item {
                val unitRate = DomainCalculators.resolveRoomRateForDuration(selectedRoom, selectedDurationHours)
                val roomTotal = DomainCalculators.calculateRoomTotal(unitRate, numberOfRooms)
                val selectedRoomOpen = selectedRoom.available && selectedRoom.statusLabel == RoomStatusLabel.AVAILABLE.name
                val showRoomSuccess = recentSuccessOrder?.category == RequestCategory.ROOM_REQUEST.name
                val roomBorderColor by animateColorAsState(
                    targetValue = if (showRoomSuccess) StatusOpenGreen else RoyalGold.copy(alpha = 0.45f),
                    animationSpec = tween(durationMillis = 320),
                    label = "room_summary_border"
                )

                GoldOutlineCard(
                    containerColor = NavyElevated,
                    borderColor = roomBorderColor
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Hotel, contentDescription = null, tint = LuminousGold)
                        Text("${strings.roomTypeLabel}: ${selectedRoom.title}", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                    }
                    OrderSummarySuccessBanner(
                        visible = showRoomSuccess,
                        orderNumber = recentSuccessOrder?.orderNumber,
                        isHindi = strings.isHindi,
                        testTag = "room_order_success_banner"
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(strings.durationLabel, style = MaterialTheme.typography.labelLarge, color = CreamIvory)
                    Spacer(Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DomainCalculators.supportedRoomDurations.forEach { hrs ->
                            val rate = DomainCalculators.resolveRoomRateForDuration(selectedRoom, hrs)
                            val label = if (rate != null) "${hrs}h (₹$rate)" else "${hrs}h (Owner rate)"
                            FilterChip(
                                selected = selectedDurationHours == hrs,
                                onClick = { selectedDurationHours = hrs },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalGold,
                                    selectedLabelColor = NavyDeep
                                ),
                                modifier = Modifier.testTag("duration_${hrs}h")
                            )
                        }
                    }

                    if (unitRate == null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.rateConfiguredByOwnerNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusPendingAmber
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.roomsCountLabel, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                        QuantityStepper(
                            quantity = numberOfRooms.toLong(),
                            onDecrement = { if (numberOfRooms > 1) numberOfRooms-- },
                            onIncrement = { if (numberOfRooms < 20) numberOfRooms++ },
                            testTagPrefix = "room_count"
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.peopleCountLabel, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                        QuantityStepper(
                            quantity = numberOfPeopleRooms.toLong(),
                            onDecrement = { if (numberOfPeopleRooms > 1) numberOfPeopleRooms-- },
                            onIncrement = { if (numberOfPeopleRooms < 50) numberOfPeopleRooms++ },
                            testTagPrefix = "room_people"
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.roomTotalLabel, style = MaterialTheme.typography.titleMedium, color = CreamIvory)
                        Text(
                            text = if (unitRate != null) "₹$roomTotal" else "Owner will confirm",
                            style = MaterialTheme.typography.titleLarge,
                            color = LuminousGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(strings.customerNameLabel) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("room_customer_name")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(strings.contactPhoneLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("room_contact_phone")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roomArrivalTime,
                        onValueChange = { roomArrivalTime = it },
                        label = { Text(strings.expectedArrivalTimeLabel) },
                        placeholder = { Text("e.g., Today 04:00 PM") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = RoyalGold) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("room_arrival_time")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roomNotes,
                        onValueChange = { roomNotes = it },
                        label = { Text(strings.optionalNotesLabel) },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.roomInspectNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Spacer(Modifier.height(10.dp))
                    val roomBtnColor by animateColorAsState(
                        targetValue = if (showRoomSuccess) StatusOpenGreen else RoyalGold,
                        animationSpec = tween(durationMillis = 280),
                        label = "room_submit_btn_color"
                    )
                    Button(
                        onClick = {
                            onSubmitRoomRequest(
                                selectedRoom,
                                selectedDurationHours,
                                numberOfRooms,
                                numberOfPeopleRooms,
                                roomArrivalTime,
                                customerName,
                                contactPhone,
                                roomNotes
                            )
                        },
                        enabled = !isSubmitting && selectedRoomOpen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = roomBtnColor,
                            contentColor = if (showRoomSuccess) Color.White else NavyDeep
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_room_request_button")
                    ) {
                        val btnState = when {
                            isSubmitting -> "LOADING"
                            showRoomSuccess -> "SUCCESS"
                            else -> "IDLE"
                        }
                        AnimatedContent(targetState = btnState, label = "room_submit_btn_content") { state ->
                            when (state) {
                                "LOADING" -> CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyDeep, strokeWidth = 2.dp)
                                "SUCCESS" -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedSuccessCheckmark(modifier = Modifier.size(22.dp), tint = Color.White)
                                    Text(
                                        text = if (strings.isHindi) "कमरा अनुरोध दर्ज!" else "Booking Request Sent",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> Text(
                                    text = if (selectedRoomOpen) strings.submitRoomRequest else "${selectedRoom.title} Unavailable",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: FRESHEN UP (Always available without room booking)
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.freshenUpSectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = LuminousGold
                )
                Text(
                    text = strings.freshenUpNoRoomNeededNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate
                )
            }

            item {
                val freshenTotal = DomainCalculators.calculateFreshenUpTotal(
                    selectedFreshen.pricePerPerson,
                    numberOfPeopleFreshen
                )
                val showFreshenSuccess = recentSuccessOrder?.category == RequestCategory.FRESHEN_UP_REQUEST.name
                val freshenBorderColor by animateColorAsState(
                    targetValue = if (showFreshenSuccess) StatusOpenGreen else RoyalGold.copy(alpha = 0.45f),
                    animationSpec = tween(durationMillis = 320),
                    label = "freshen_summary_border"
                )

                GoldOutlineCard(borderColor = freshenBorderColor) {
                    OrderSummarySuccessBanner(
                        visible = showFreshenSuccess,
                        orderNumber = recentSuccessOrder?.orderNumber,
                        isHindi = strings.isHindi,
                        testTag = "freshen_order_success_banner"
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductOrServiceImage(
                            imageUrl = "",
                            localFallbackRes = R.drawable.img_freshen_up,
                            contentDescription = "Freshen Up Suite",
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (strings.isHindi) "ताज़गी एवं स्नान सुविधा" else "Clean Freshen Up Facilities",
                                style = MaterialTheme.typography.titleMedium,
                                color = CreamIvory
                            )
                            Text(
                                text = strings.freshenUpNoRoomNeededNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        freshenOptions.forEach { option ->
                            val isChosen = option.optionKey == selectedFreshen.optionKey
                            val title = if (strings.isHindi && option.titleHindi.isNotBlank()) {
                                "${option.titleHindi} (${option.title})"
                            } else {
                                option.title
                            }
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChosen) NavyElevated else NavyDeep,
                                border = BorderStroke(
                                    width = if (isChosen) 1.5.dp else 1.dp,
                                    color = if (isChosen) RoyalGold else AntiqueGold.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFreshenKey = option.optionKey }
                                    .testTag("freshen_option_${option.optionKey.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall, color = CreamIvory)
                                        Text(
                                            text = "₹${option.pricePerPerson} ${strings.perPersonLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LuminousGold
                                        )
                                    }
                                    StatusPill(
                                        text = if (option.available) strings.availableLabel else strings.unavailableLabel,
                                        color = if (option.available) StatusOpenGreen else StatusClosedRed
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.peopleCountLabel, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                        QuantityStepper(
                            quantity = numberOfPeopleFreshen.toLong(),
                            onDecrement = { if (numberOfPeopleFreshen > 1) numberOfPeopleFreshen-- },
                            onIncrement = { if (numberOfPeopleFreshen < 50) numberOfPeopleFreshen++ },
                            testTagPrefix = "freshen_people"
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.freshenUpTotalLabel, style = MaterialTheme.typography.titleMedium, color = CreamIvory)
                        Text(
                            text = "₹$freshenTotal",
                            style = MaterialTheme.typography.titleLarge,
                            color = LuminousGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text(strings.customerNameLabel) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text(strings.contactPhoneLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = freshenArrivalTime,
                        onValueChange = { freshenArrivalTime = it },
                        label = { Text(strings.expectedArrivalTimeLabel) },
                        placeholder = { Text("e.g., 07:30 AM") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = RoyalGold) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth().testTag("freshen_arrival_time")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = freshenNotes,
                        onValueChange = { freshenNotes = it },
                        label = { Text(strings.optionalNotesLabel) },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    val freshenBtnColor by animateColorAsState(
                        targetValue = if (showFreshenSuccess) StatusOpenGreen else RoyalGold,
                        animationSpec = tween(durationMillis = 280),
                        label = "freshen_submit_btn_color"
                    )
                    Button(
                        onClick = {
                            onSubmitFreshenUpRequest(
                                selectedFreshen,
                                numberOfPeopleFreshen,
                                freshenArrivalTime,
                                customerName,
                                contactPhone,
                                freshenNotes
                            )
                        },
                        enabled = !isSubmitting && selectedFreshen.available,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = freshenBtnColor,
                            contentColor = if (showFreshenSuccess) Color.White else NavyDeep
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_freshen_request_button")
                    ) {
                        val btnState = when {
                            isSubmitting -> "LOADING"
                            showFreshenSuccess -> "SUCCESS"
                            else -> "IDLE"
                        }
                        AnimatedContent(targetState = btnState, label = "freshen_submit_btn_content") { state ->
                            when (state) {
                                "LOADING" -> CircularProgressIndicator(modifier = Modifier.size(22.dp), color = NavyDeep, strokeWidth = 2.dp)
                                "SUCCESS" -> Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimatedSuccessCheckmark(modifier = Modifier.size(22.dp), tint = Color.White)
                                    Text(
                                        text = if (strings.isHindi) "फ्रेश-अप अनुरोध दर्ज!" else "Request Sent",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> Text(strings.submitFreshenUpRequest, fontWeight = FontWeight.Bold)
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
                testTag = "rooms_scroll_to_top"
            )
        }
    }
}
