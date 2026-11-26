package com.example.data.repository

import android.util.Log
import com.example.data.model.DeliverySettings
import com.example.data.model.OwnerDashboardStats
import com.example.data.model.Product
import com.example.data.model.ShopSettings
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AiActionType {
    QUERY_REPORT,
    SET_SHOP_STATUS,
    SET_SHOP_TIMINGS,
    UPDATE_PRODUCT_PRICE,
    TOGGLE_PRODUCT_AVAILABILITY,
    TOGGLE_DELIVERY,
    UNSUPPORTED
}

data class AiProposedAction(
    val actionType: AiActionType,
    val replyHindiEnglish: String,
    val changeSummary: String,
    val requiresConfirmation: Boolean,
    val shopOpen: Boolean? = null,
    val openingTime: String? = null,
    val closingTime: String? = null,
    val targetProductId: String? = null,
    val targetProductName: String? = null,
    val newPrice: Long? = null,
    val productAvailable: Boolean? = null,
    val deliveryEnabled: Boolean? = null
)

class MarutiAiRepository {

    private companion object {
        const val TAG = "MarutiAiRepo"
    }

    private val actionSchema = Schema.obj(
        properties = mapOf(
            "actionType" to Schema.enumeration(
                values = AiActionType.entries.map { it.name },
                description = "Exact action category matching the owner command"
            ),
            "replyHindiEnglish" to Schema.string(
                description = "Helpful natural answer for Tarun Kumar (owner) in Hindi/English summarizing metrics or explaining the planned change"
            ),
            "changeSummary" to Schema.string(
                description = "Concise bullet summary of exact values that will change if confirmed, or 'Read-only query' if no mutation"
            ),
            "requiresConfirmation" to Schema.boolean(
                description = "True for any write/update action (shop open/close, timing, price, availability, delivery); false for read-only stats queries"
            ),
            "shopOpen" to Schema.boolean(description = "True to open shop, false to close shop", nullable = true),
            "openingTime" to Schema.string(description = "Opening time in format e.g. 7:00 AM", nullable = true),
            "closingTime" to Schema.string(description = "Closing time in format e.g. 10:00 PM", nullable = true),
            "targetProductId" to Schema.string(description = "Matching product document id from provided catalog", nullable = true),
            "targetProductName" to Schema.string(description = "Matching product display name", nullable = true),
            "newPrice" to Schema.integer(description = "Updated price in INR integer", nullable = true),
            "productAvailable" to Schema.boolean(description = "Updated product availability boolean", nullable = true),
            "deliveryEnabled" to Schema.boolean(description = "Updated local delivery enabled boolean", nullable = true)
        ),
        optionalProperties = listOf(
            "shopOpen",
            "openingTime",
            "closingTime",
            "targetProductId",
            "targetProductName",
            "newPrice",
            "productAvailable",
            "deliveryEnabled"
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = actionSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are MARUTI AI, the private operations assistant exclusively for Tarun Kumar, owner of MARUTI DHAM (Near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan).
                    Understand Hindi, Hinglish, and English commands accurately.
                    Examples:
                    - "Aaj ki shop band kar do" / "Close the shop" -> SET_SHOP_STATUS (shopOpen = false, requiresConfirmation = true)
                    - "Shop khol do" / "Open shop now" -> SET_SHOP_STATUS (shopOpen = true, requiresConfirmation = true)
                    - "Opening time 6:30 AM aur closing 10:30 PM karo" -> SET_SHOP_TIMINGS (openingTime, closingTime, requiresConfirmation = true)
                    - "Maggi ka price 60 rupay kar do" -> UPDATE_PRODUCT_PRICE (match targetProductId & targetProductName from the live catalog, newPrice = 60, requiresConfirmation = true)
                    - "Chole Kulche unavailable kar do" -> TOGGLE_PRODUCT_AVAILABILITY (targetProductId, productAvailable = false, requiresConfirmation = true)
                    - "Delivery band kar do" / "Enable local delivery" -> TOGGLE_DELIVERY (deliveryEnabled, requiresConfirmation = true)
                    - "Aaj kitne order aaye?" / "How many new customers?" / "Show today's sales" -> QUERY_REPORT (requiresConfirmation = false, answer directly from live stats)
                    Never invent fake orders or fake sales numbers; use only the live snapshot provided.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun interpretOwnerCommand(
        command: String,
        shopSettings: ShopSettings,
        deliverySettings: DeliverySettings,
        products: List<Product>,
        stats: OwnerDashboardStats
    ): AiProposedAction {
        val cleanCmd = command.trim()
        require(cleanCmd.isNotBlank()) { "Please enter an instruction for MARUTI AI." }

        val catalogLines = products.joinToString("\n") { p ->
            val prices = p.priceOptions.joinToString(", ") { "${it.label}: ₹${it.price}" }
            "- id=${p.id} | name=${p.name} (${p.nameHindi}) | category=${p.category} | available=${p.available} | prices=[$prices]"
        }

        val contextPrompt = """
            LIVE SHOP SNAPSHOT:
            - Shop Open: ${shopSettings.shopOpen} (Mode: ${shopSettings.shopControlMode})
            - Opening Time: ${shopSettings.openingTime}
            - Closing Time: ${shopSettings.closingTime}
            - Delivery Enabled: ${deliverySettings.deliveryEnabled} | Delivery Charge: ₹${deliverySettings.deliveryCharge} | Packaging Charge: ₹${deliverySettings.packagingCharge}
            - Today's Sales (IST): ₹${stats.todaysSales}
            - Total Orders: ${stats.totalOrders} (Pending: ${stats.pendingOrders}, Completed: ${stats.completedOrders}, Cancelled: ${stats.cancelledOrders})
            - Total Registered Customers: ${stats.totalCustomers} (New Signups Today: ${stats.newSignupsToday})

            MENU CATALOG:
            $catalogLines

            OWNER COMMAND:
            $cleanCmd
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(contextPrompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("MARUTI AI returned an empty response.")

            val obj = JSONObject(jsonText)
            val actionStr = obj.optString("actionType", AiActionType.UNSUPPORTED.name)
            val actionType = AiActionType.entries.firstOrNull { it.name == actionStr } ?: AiActionType.UNSUPPORTED

            return AiProposedAction(
                actionType = actionType,
                replyHindiEnglish = obj.optString("replyHindiEnglish", ""),
                changeSummary = obj.optString("changeSummary", ""),
                requiresConfirmation = obj.optBoolean("requiresConfirmation", actionType != AiActionType.QUERY_REPORT && actionType != AiActionType.UNSUPPORTED),
                shopOpen = if (obj.has("shopOpen") && !obj.isNull("shopOpen")) obj.getBoolean("shopOpen") else null,
                openingTime = obj.optString("openingTime", "").takeIf { it.isNotBlank() && it != "null" },
                closingTime = obj.optString("closingTime", "").takeIf { it.isNotBlank() && it != "null" },
                targetProductId = obj.optString("targetProductId", "").takeIf { it.isNotBlank() && it != "null" },
                targetProductName = obj.optString("targetProductName", "").takeIf { it.isNotBlank() && it != "null" },
                newPrice = if (obj.has("newPrice") && !obj.isNull("newPrice")) obj.getLong("newPrice") else null,
                productAvailable = if (obj.has("productAvailable") && !obj.isNull("productAvailable")) obj.getBoolean("productAvailable") else null,
                deliveryEnabled = if (obj.has("deliveryEnabled") && !obj.isNull("deliveryEnabled")) obj.getBoolean("deliveryEnabled") else null
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "MARUTI AI timed out", e)
            throw IllegalStateException("MARUTI AI timed out after 60 seconds. Please check connection and retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "MARUTI AI failed", e)
            throw IllegalStateException(e.localizedMessage ?: "MARUTI AI request failed.")
        }
    }
}
