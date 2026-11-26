package com.example.ui.viewmodel

import com.example.data.model.DeliverySettings
import com.example.data.model.OwnerDashboardStats
import com.example.data.model.Product
import com.example.data.model.ShopControlMode
import com.example.data.model.ShopSettings
import com.example.data.repository.AiActionType
import com.example.data.repository.AiProposedAction
import com.example.data.repository.MarutiAiRepository
import com.example.data.repository.MarutiRepository
import com.example.util.DomainCalculators

sealed interface MarutiAiUiState {
    data object Idle : MarutiAiUiState
    data class Loading(val prompt: String) : MarutiAiUiState
    data class Proposal(val prompt: String, val action: AiProposedAction) : MarutiAiUiState
    data class ResultMessage(val prompt: String, val reply: String) : MarutiAiUiState
    data class Error(val message: String) : MarutiAiUiState
}

class MarutiAiCoordinator(
    private val aiRepository: MarutiAiRepository,
    private val repository: MarutiRepository
) {
    suspend fun interpret(
        command: String,
        shop: ShopSettings,
        delivery: DeliverySettings,
        products: List<Product>,
        stats: OwnerDashboardStats
    ): MarutiAiUiState {
        return try {
            val action = aiRepository.interpretOwnerCommand(
                command = command,
                shopSettings = shop,
                deliverySettings = delivery,
                products = products,
                stats = stats
            )
            val isMutation = action.actionType != AiActionType.QUERY_REPORT &&
                action.actionType != AiActionType.UNSUPPORTED
            if (isMutation && action.requiresConfirmation) {
                MarutiAiUiState.Proposal(prompt = command, action = action)
            } else {
                MarutiAiUiState.ResultMessage(prompt = command, reply = action.replyHindiEnglish)
            }
        } catch (e: Exception) {
            MarutiAiUiState.Error(e.localizedMessage ?: "MARUTI AI failed to process command.")
        }
    }

    suspend fun applyConfirmedAction(
        action: AiProposedAction,
        shop: ShopSettings,
        delivery: DeliverySettings,
        products: List<Product>
    ): Result<String> {
        return when (action.actionType) {
            AiActionType.SET_SHOP_STATUS -> {
                val targetOpen = action.shopOpen ?: !shop.shopOpen
                val nextMode = if (targetOpen) ShopControlMode.OPEN_NOW.name else ShopControlMode.CLOSED_NOW.name
                val updated = shop.copy(shopOpen = targetOpen, shopControlMode = nextMode)
                repository.saveShopSettings(updated).map {
                    repository.logOwnerActivity(
                        action = if (targetOpen) "AI: Opened Shop" else "AI: Closed Shop",
                        targetType = "shop_settings",
                        targetId = "main",
                        previousValue = "mode=${shop.shopControlMode}, open=${shop.shopOpen}",
                        newValue = "mode=$nextMode, open=$targetOpen",
                        viaAi = true
                    )
                    if (targetOpen) "Shop status updated to OPEN NOW." else "Shop status updated to SHOP CLOSED."
                }
            }

            AiActionType.SET_SHOP_TIMINGS -> {
                val nextOpen = action.openingTime?.takeIf { it.isNotBlank() } ?: shop.openingTime
                val nextClose = action.closingTime?.takeIf { it.isNotBlank() } ?: shop.closingTime
                val draft = shop.copy(
                    openingTime = nextOpen,
                    closingTime = nextClose,
                    shopControlMode = ShopControlMode.OPEN_AT_SCHEDULED.name
                )
                val evaluated = DomainCalculators.evaluateShopOpenStatus(draft)
                repository.saveShopSettings(draft.copy(shopOpen = evaluated.isOpen)).map {
                    repository.logOwnerActivity(
                        action = "AI: Updated Shop Timings",
                        targetType = "shop_settings",
                        targetId = "main",
                        previousValue = "${shop.openingTime} - ${shop.closingTime}",
                        newValue = "$nextOpen - $nextClose",
                        viaAi = true
                    )
                    "Timings updated to $nextOpen – $nextClose IST."
                }
            }

            AiActionType.UPDATE_PRODUCT_PRICE -> {
                val target = findTargetProduct(products, action.targetProductId, action.targetProductName)
                    ?: return Result.failure(IllegalStateException("Could not match menu item for price update."))
                val newPrice = action.newPrice
                    ?: return Result.failure(IllegalStateException("Missing new price in AI proposal."))
                if (newPrice <= 0L) {
                    return Result.failure(IllegalStateException("Price must be greater than ₹0."))
                }
                val oldPrices = target.priceOptions.joinToString { "${it.label}: ₹${it.price}" }
                val updatedOptions = if (target.priceOptions.isEmpty()) {
                    listOf(com.example.data.model.PriceOption("Regular", newPrice))
                } else {
                    target.priceOptions.mapIndexed { idx, opt -> if (idx == 0) opt.copy(price = newPrice) else opt }
                }
                val updated = target.copy(priceOptions = updatedOptions)
                repository.saveProduct(updated).map {
                    repository.logOwnerActivity(
                        action = "AI: Updated Price (${target.name})",
                        targetType = "products",
                        targetId = target.id,
                        previousValue = oldPrices,
                        newValue = updatedOptions.joinToString { "${it.label}: ₹${it.price}" },
                        viaAi = true
                    )
                    "${target.name} updated to ₹$newPrice."
                }
            }

            AiActionType.TOGGLE_PRODUCT_AVAILABILITY -> {
                val target = findTargetProduct(products, action.targetProductId, action.targetProductName)
                    ?: return Result.failure(IllegalStateException("Could not match menu item for availability toggle."))
                val nextAvail = action.productAvailable ?: !target.available
                val updated = target.copy(available = nextAvail)
                repository.saveProduct(updated).map {
                    repository.logOwnerActivity(
                        action = "AI: Toggled Availability (${target.name})",
                        targetType = "products",
                        targetId = target.id,
                        previousValue = "available=${target.available}",
                        newValue = "available=$nextAvail",
                        viaAi = true
                    )
                    "${target.name} marked ${if (nextAvail) "Available" else "Unavailable"}."
                }
            }

            AiActionType.TOGGLE_DELIVERY -> {
                val nextEnabled = action.deliveryEnabled ?: !delivery.deliveryEnabled
                val updated = delivery.copy(deliveryEnabled = nextEnabled)
                repository.saveDeliverySettings(updated).map {
                    repository.logOwnerActivity(
                        action = "AI: Toggled Local Delivery",
                        targetType = "delivery_settings",
                        targetId = "main",
                        previousValue = "deliveryEnabled=${delivery.deliveryEnabled}",
                        newValue = "deliveryEnabled=$nextEnabled",
                        viaAi = true
                    )
                    "Local delivery ${if (nextEnabled) "enabled" else "paused"}."
                }
            }

            AiActionType.QUERY_REPORT, AiActionType.UNSUPPORTED ->
                Result.success(action.replyHindiEnglish)
        }
    }

    private fun findTargetProduct(
        products: List<Product>,
        targetId: String?,
        targetName: String?
    ): Product? {
        if (!targetId.isNullOrBlank()) {
            products.firstOrNull { it.id.equals(targetId.trim(), ignoreCase = true) }?.let { return it }
        }
        if (!targetName.isNullOrBlank()) {
            val q = targetName.trim().lowercase()
            products.firstOrNull {
                it.name.lowercase().contains(q) || it.nameHindi.lowercase().contains(q)
            }?.let { return it }
        }
        return null
    }
}
