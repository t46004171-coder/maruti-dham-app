package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityLogEntry
import com.example.data.model.OwnerNotification
import com.example.ui.components.GoldOutlineCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AntiqueGold
import com.example.ui.theme.CreamIvory
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusCompletedTeal
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusPendingAmber
import com.example.ui.viewmodel.MarutiAiUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMarutiAiSection(
    aiState: MarutiAiUiState,
    onRunCommand: (String) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissProposal: () -> Unit
) {
    var commandText by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "Aaj ki shop band kar do",
        "Shop khol do",
        "Maggi ka price 60 rupay kar do",
        "Desi Tea ka price 15 rupay kar do",
        "Aaj kitne order aaye?",
        "Show today's sales summary"
    )

    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GoldOutlineCard(containerColor = NavyElevated) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LuminousGold)
                    Column {
                        Text(
                            text = "MARUTI AI • Owner Operations Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            color = LuminousGold
                        )
                        Text(
                            text = "Powered by Firebase AI Logic (Gemini). Understands Hindi, Hinglish & English. Every change asks your confirmation first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("Enter Hindi or English command...") },
                    placeholder = { Text("e.g., Aaj ki shop band kar do / Maggi price 60 karo") },
                    colors = adminTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("maruti_ai_input")
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onRunCommand(commandText) },
                    enabled = aiState !is MarutiAiUiState.Loading && commandText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = NavyDeep),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("maruti_ai_run_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ask MARUTI AI", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("Quick Hindi / English Commands:", style = MaterialTheme.typography.labelLarge, color = LuminousGold)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { sample ->
                    SuggestionChip(
                        onClick = {
                            commandText = sample
                            onRunCommand(sample)
                        },
                        label = { Text(sample, color = CreamIvory) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = NavyElevated),
                        border = BorderStroke(1.dp, AntiqueGold.copy(alpha = 0.5f))
                    )
                }
            }
        }

        item {
            when (aiState) {
                is MarutiAiUiState.Idle -> Unit

                is MarutiAiUiState.Loading -> {
                    GoldOutlineCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = RoyalGold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Text("MARUTI AI is analyzing live shop metrics...", color = CreamIvory)
                        }
                    }
                }

                is MarutiAiUiState.Error -> {
                    GoldOutlineCard {
                        Text("AI Notice", style = MaterialTheme.typography.titleSmall, color = StatusClosedRed)
                        Spacer(Modifier.height(4.dp))
                        Text(aiState.message, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                    }
                }

                is MarutiAiUiState.ResultMessage -> {
                    GoldOutlineCard(containerColor = NavyElevated) {
                        StatusPill(text = "MARUTI AI RESPONSE", color = StatusCompletedTeal)
                        Spacer(Modifier.height(8.dp))
                        Text(aiState.reply, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                    }
                }

                is MarutiAiUiState.Proposal -> {
                    GoldOutlineCard(containerColor = NavyElevated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(text = "CONFIRM CHANGE: ${aiState.action.actionType.name}", color = StatusPendingAmber)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(aiState.action.replyHindiEnglish, style = MaterialTheme.typography.bodyMedium, color = CreamIvory)
                        if (aiState.action.changeSummary.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Proposed Update: ${aiState.action.changeSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LuminousGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onDismissProposal,
                                border = BorderStroke(1.dp, MutedSlate),
                                modifier = Modifier.weight(1f).testTag("maruti_ai_cancel_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MutedSlate, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cancel", color = MutedSlate)
                            }
                            Button(
                                onClick = onConfirmAction,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusOpenGreen, contentColor = Color.White),
                                modifier = Modifier.weight(1f).testTag("maruti_ai_confirm_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Confirm & Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminActivityLogSection(
    notifications: List<OwnerNotification>,
    activityLogs: List<ActivityLogEntry>,
    onMarkNotificationRead: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = LuminousGold)
                Text(
                    text = "Order Alerts (${notifications.count { !it.read }} unread)",
                    style = MaterialTheme.typography.titleLarge,
                    color = LuminousGold
                )
            }
        }

        if (notifications.isEmpty()) {
            item {
                GoldOutlineCard {
                    Text("No server-generated order alerts yet.", color = MutedSlate)
                }
            }
        } else {
            items(notifications, key = { "notif_${it.id}" }) { notif ->
                GoldOutlineCard(containerColor = if (notif.read) NavyDeep else NavyElevated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = LuminousGold)
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                            if (notif.istTimestampText.isNotBlank()) {
                                Text(notif.istTimestampText, style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                            }
                        }
                        if (!notif.read) {
                            OutlinedButton(
                                onClick = { onMarkNotificationRead(notif.id) },
                                border = BorderStroke(1.dp, RoyalGold)
                            ) {
                                Text("Mark Read", color = RoyalGold)
                            }
                        } else {
                            StatusPill(text = "Read", color = MutedSlate)
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = AntiqueGold.copy(alpha = 0.35f))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = RoyalGold)
                Text("Immutable Owner Activity Log (${activityLogs.size})", style = MaterialTheme.typography.titleLarge, color = LuminousGold)
            }
        }

        if (activityLogs.isEmpty()) {
            item {
                GoldOutlineCard {
                    Text("Owner changes and MARUTI AI updates will appear here.", color = MutedSlate)
                }
            }
        } else {
            items(activityLogs, key = { "log_${it.id}" }) { entry ->
                GoldOutlineCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.action, style = MaterialTheme.typography.titleSmall, color = LuminousGold, modifier = Modifier.weight(1f))
                        if (entry.viaAi) {
                            StatusPill(text = "MARUTI AI", color = StatusCompletedTeal)
                        }
                    }
                    if (entry.istTimestampText.isNotBlank()) {
                        Text(entry.istTimestampText, style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    }
                    if (entry.previousValue.isNotBlank()) {
                        Text("Before: ${entry.previousValue}", style = MaterialTheme.typography.bodySmall, color = MutedSlate)
                    }
                    if (entry.newValue.isNotBlank()) {
                        Text("After: ${entry.newValue}", style = MaterialTheme.typography.bodySmall, color = CreamIvory)
                    }
                    if (entry.performedByEmail.isNotBlank()) {
                        Text("By: ${entry.performedByEmail}", style = MaterialTheme.typography.labelSmall, color = RoyalGold)
                    }
                }
            }
        }
    }
}
