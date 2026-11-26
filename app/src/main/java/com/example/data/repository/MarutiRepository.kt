package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ActivityLogEntry
import com.example.data.model.Announcement
import com.example.data.model.CustomerRequest
import com.example.data.model.DeliverySettings
import com.example.data.model.FreshenUpConfig
import com.example.data.model.FulfillmentType
import com.example.data.model.OwnerNotification
import com.example.data.model.OwnerPrivateSettings
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.RequestStatus
import com.example.data.model.RoomConfig
import com.example.data.model.ShopSettings
import com.example.data.model.SiteConfig
import com.example.data.model.ThemeConfig
import com.example.data.model.UserProfile
import com.example.data.model.WhatsAppDeliveryStatus
import com.example.util.DefaultCatalogData
import com.example.util.DomainCalculators
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MarutiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    init {
        configureOfflineCacheSafely(db)
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        private fun configureOfflineCacheSafely(db: FirebaseFirestore) {
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(50L * 1024L * 1024L)
                            .build()
                    )
                    .build()
                db.firestoreSettings = settings
            } catch (_: IllegalStateException) {
                // Already initialized with active listeners; safe to ignore
            }
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.handleSnapshotError(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeIsAdminDoc(uid: String): Flow<Boolean> = flow {
        require(uid.isNotBlank()) { "UID required" }
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snapshot -> snapshot.exists() }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(false)
                }
        )
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        require(uid.isNotBlank()) { "UID required" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .handleSnapshotError(uid, OperationType.GET, path)
        )
    }

    fun observeAllUsersForAdmin(pageLimit: Long = 50L): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageLimit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeShopSettings(): Flow<ShopSettings> = flow {
        val uid = requireUserId()
        val path = "shop_settings/main"
        emitAll(
            db.collection("shop_settings").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(ShopSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: DefaultCatalogData.defaultShopSettings
                }
                .handleSnapshotError(uid, OperationType.GET, path)
        )
    }

    fun observeSiteConfig(): Flow<SiteConfig> = flow {
        val uid = requireUserId()
        val path = "site_config/main"
        emitAll(
            db.collection("site_config").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(SiteConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: DefaultCatalogData.defaultSiteConfig
                }
                .handleSnapshotError(uid, OperationType.GET, path)
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection("announcements")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "products"
        emitAll(
            db.collection("products")
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isEmpty()) DefaultCatalogData.initialProducts
                    else docs.sortedBy { it.sortOrder }
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeRoomConfigs(): Flow<List<RoomConfig>> = flow {
        val uid = requireUserId()
        val path = "room_configs"
        emitAll(
            db.collection("room_configs")
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.toObjects(RoomConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isEmpty()) DefaultCatalogData.initialRoomConfigs
                    else docs.sortedBy { it.roomType }
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeFreshenUpConfigs(): Flow<List<FreshenUpConfig>> = flow {
        val uid = requireUserId()
        val path = "freshen_up_configs"
        emitAll(
            db.collection("freshen_up_configs")
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.toObjects(FreshenUpConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (docs.isEmpty()) DefaultCatalogData.initialFreshenUpConfigs
                    else docs.sortedBy { it.pricePerPerson }
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeDeliverySettings(): Flow<DeliverySettings> = flow {
        val uid = requireUserId()
        val path = "delivery_settings/main"
        emitAll(
            db.collection("delivery_settings").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(DeliverySettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: DefaultCatalogData.defaultDeliverySettings
                }
                .handleSnapshotError(uid, OperationType.GET, path)
        )
    }

    fun observeCustomerRequests(userId: String, pageLimit: Long = 30L): Flow<List<CustomerRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "requests"
        emitAll(
            db.collection("requests")
                .whereEqualTo("userId", userId)
                .limit(pageLimit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CustomerRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeAllRequestsForAdmin(pageLimit: Long = 60L): Flow<List<CustomerRequest>> = flow {
        val uid = requireUserId()
        val path = "requests"
        emitAll(
            db.collection("requests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageLimit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CustomerRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeOwnerPrivateSettings(): Flow<OwnerPrivateSettings> = flow {
        val uid = requireUserId()
        val path = "owner_private_settings/main"
        emitAll(
            db.collection("owner_private_settings").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(OwnerPrivateSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: DefaultCatalogData.defaultOwnerPrivateSettings
                }
                .handleSnapshotError(uid, OperationType.GET, path)
        )
    }

    fun observeActivityLogs(limitCount: Long = 80L): Flow<List<ActivityLogEntry>> = flow {
        val uid = requireUserId()
        val path = "activity_logs"
        emitAll(
            db.collection("activity_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limitCount)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ActivityLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    fun observeNotifications(limitCount: Long = 40L): Flow<List<OwnerNotification>> = flow {
        val uid = requireUserId()
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limitCount)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(OwnerNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleSnapshotError(uid, OperationType.LIST, path)
        )
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String,
        preferredLanguage: String
    ): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "User ID mismatch" }
            val safeName = displayName.trim().ifBlank { "Devotee" }.take(80)
            val safeLang = if (preferredLanguage == "hi") "hi" else "en"
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(120),
                    "phone" to "",
                    "preferredLanguage" to safeLang,
                    "registrationCompleted" to false,
                    "preferredFulfillment" to FulfillmentType.PICKUP.name,
                    "defaultAddress" to "",
                    "defaultLocality" to "Khatu Shyam Ji",
                    "consentAccepted" to true,
                    "deletionRequested" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to safeLang,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Registers or updates a customer profile while atomically enforcing unique mobile numbers
     * via /phone_registry/{normalizedPhone} to prevent duplicate customer accounts.
     */
    suspend fun registerCustomerProfile(
        uid: String,
        displayName: String,
        mobile: String,
        email: String,
        preferredFulfillment: FulfillmentType,
        defaultAddress: String,
        defaultLocality: String,
        preferredLanguage: String
    ): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "User ID mismatch" }

            val safeName = displayName.trim().take(80)
            require(safeName.length >= 2) { "Please enter your full name." }
            require(DomainCalculators.isValidMobileNumber(mobile)) {
                "Please enter a valid 10-digit Indian mobile number."
            }

            val normalizedPhone = DomainCalculators.normalizeIndianMobile(mobile)
            val safeEmail = email.trim().take(120)
            val safeAddress = defaultAddress.trim().take(300)
            val safeLocality = defaultLocality.trim().take(100)
            val safeLang = if (preferredLanguage == "hi") "hi" else "en"

            val userRef = db.collection("users").document(uid)
            val phoneRef = db.collection("phone_registry").document(normalizedPhone)

            db.runTransaction { transaction ->
                // Execute ALL reads before ANY writes (Firestore transaction requirement)
                val phoneSnap = transaction.get(phoneRef)
                val userSnap = transaction.get(userRef)

                if (phoneSnap.exists()) {
                    val ownerUid = phoneSnap.getString("userId")
                    if (ownerUid != null && ownerUid != uid) {
                        throw IllegalStateException(
                            "This mobile number is already registered with another account. Please sign in with your original Google account."
                        )
                    }
                } else {
                    transaction.set(
                        phoneRef,
                        mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (!userSnap.exists()) {
                    transaction.set(
                        userRef,
                        mapOf(
                            "userId" to uid,
                            "displayName" to safeName,
                            "email" to safeEmail,
                            "phone" to mobile.trim().take(20),
                            "preferredLanguage" to safeLang,
                            "registrationCompleted" to true,
                            "preferredFulfillment" to preferredFulfillment.name,
                            "defaultAddress" to safeAddress,
                            "defaultLocality" to safeLocality,
                            "consentAccepted" to true,
                            "deletionRequested" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.update(
                        userRef,
                        mapOf(
                            "displayName" to safeName,
                            "email" to safeEmail,
                            "phone" to mobile.trim().take(20),
                            "preferredLanguage" to safeLang,
                            "registrationCompleted" to true,
                            "preferredFulfillment" to preferredFulfillment.name,
                            "defaultAddress" to safeAddress,
                            "defaultLocality" to safeLocality,
                            "consentAccepted" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateLanguageOnly(uid: String, preferredLanguage: String): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "User ID mismatch" }
            val safeLang = if (preferredLanguage == "hi") "hi" else "en"
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredLanguage" to safeLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun requestAccountDeletion(uid: String): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "User ID mismatch" }
            db.collection("users").document(uid).update(
                mapOf(
                    "deletionRequested" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyAndBootstrapOwner(): Result<Boolean> {
        val currentUser = auth.currentUser ?: return Result.success(false)
        val uid = currentUser.uid
        val email = currentUser.email?.trim().orEmpty()
        if (!email.equals(AuthHelper.AUTHORIZED_OWNER_EMAIL, ignoreCase = true) || !currentUser.isEmailVerified) {
            return Result.success(false)
        }
        val docPath = "admins/$uid"
        return try {
            val adminRef = db.collection("admins").document(uid)
            val snap = adminRef.get().await()
            if (!snap.exists()) {
                adminRef.set(
                    mapOf(
                        "uid" to uid,
                        "email" to AuthHelper.AUTHORIZED_OWNER_EMAIL,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedInitialDataIfOwner(): Result<Unit> {
        return try {
            requireUserId()
            val shopRef = db.collection("shop_settings").document("main")
            if (!shopRef.get().await().exists()) {
                saveShopSettings(DefaultCatalogData.defaultShopSettings).getOrThrow()
            }
            val siteRef = db.collection("site_config").document("main")
            if (!siteRef.get().await().exists()) {
                saveSiteConfig(DefaultCatalogData.defaultSiteConfig).getOrThrow()
            }
            val privateRef = db.collection("owner_private_settings").document("main")
            if (!privateRef.get().await().exists()) {
                saveOwnerPrivateSettings(DefaultCatalogData.defaultOwnerPrivateSettings).getOrThrow()
            }
            val deliveryRef = db.collection("delivery_settings").document("main")
            if (!deliveryRef.get().await().exists()) {
                saveDeliverySettings(DefaultCatalogData.defaultDeliverySettings).getOrThrow()
            }
            val productsSnap = db.collection("products").limit(1).get().await()
            if (productsSnap.isEmpty) {
                DefaultCatalogData.initialProducts.forEach { product ->
                    saveProduct(product).getOrThrow()
                }
            }
            val roomsSnap = db.collection("room_configs").limit(1).get().await()
            if (roomsSnap.isEmpty) {
                DefaultCatalogData.initialRoomConfigs.forEach { room ->
                    saveRoomConfig(room).getOrThrow()
                }
            }
            val freshenSnap = db.collection("freshen_up_configs").limit(1).get().await()
            if (freshenSnap.isEmpty) {
                DefaultCatalogData.initialFreshenUpConfigs.forEach { cfg ->
                    saveFreshenUpConfig(cfg).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seedInitialDataIfOwner")
            Result.failure(e)
        }
    }

    suspend fun submitCustomerRequest(
        request: CustomerRequest,
        shopSettings: ShopSettings
    ): Result<CustomerRequest> {
        var docPath = "requests"
        return try {
            val uid = requireUserId()
            val name = request.customerName.trim().take(80)
            val phone = request.contactPhone.trim().take(25)
            val time = request.expectedTime.trim().take(60)
            require(name.isNotBlank()) { "Customer name is required" }
            require(phone.length >= 5) { "Valid contact phone number is required" }
            require(time.isNotBlank()) { "Expected arrival or delivery time is required" }

            val counterRef = db.collection("counters").document("orders")
            val docRef = db.collection("requests").document()
            docPath = docRef.path

            val itemMaps = request.items.take(30).map { item ->
                mapOf(
                    "name" to item.name.take(80),
                    "variantLabel" to item.variantLabel.take(40),
                    "unitPrice" to item.unitPrice.coerceAtLeast(0L),
                    "quantity" to item.quantity.coerceAtLeast(1L),
                    "lineTotal" to (item.unitPrice.coerceAtLeast(0L) * item.quantity.coerceAtLeast(1L))
                )
            }

            val foodTotal = request.foodTotal.coerceAtLeast(0L)
            val roomTotal = request.roomTotal.coerceAtLeast(0L)
            val freshenUpTotal = request.freshenUpTotal.coerceAtLeast(0L)
            val deliveryCharge = request.deliveryCharge.coerceAtLeast(0L)
            val packagingCharge = request.packagingCharge.coerceAtLeast(0L)
            val grandTotal = DomainCalculators.calculateGrandTotal(
                foodTotal = foodTotal,
                roomTotal = roomTotal,
                freshenUpTotal = freshenUpTotal,
                deliveryCharge = deliveryCharge,
                packagingCharge = packagingCharge
            )

            val safePaymentMethod = if (shopSettings.upiEnabled && request.paymentMethod == PaymentMethod.UPI.name) {
                PaymentMethod.UPI.name
            } else {
                PaymentMethod.CASH.name
            }
            val safePaymentStatus = if (safePaymentMethod == PaymentMethod.UPI.name) {
                PaymentStatus.UPI_PENDING_VERIFICATION.name
            } else {
                PaymentStatus.UNPAID.name
            }
            val safeUpiRef = if (safePaymentMethod == PaymentMethod.UPI.name) {
                request.upiTransactionRef?.trim()?.take(80)?.takeIf { it.isNotBlank() }
            } else null
            val initialWhatsAppStatus = if (shopSettings.whatsappWebhookUrl.isNotBlank()) {
                WhatsAppDeliveryStatus.PENDING_SERVER.name
            } else {
                WhatsAppDeliveryStatus.NOT_CONFIGURED.name
            }

            val generatedOrderNumber = db.runTransaction { transaction ->
                val counterSnap = transaction.get(counterRef)
                val nextNumber = if (counterSnap.exists()) {
                    ((counterSnap.getLong("lastNumber") ?: 0L) + 1L)
                } else {
                    1L
                }
                if (counterSnap.exists()) {
                    transaction.update(
                        counterRef,
                        mapOf(
                            "lastNumber" to nextNumber,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.set(
                        counterRef,
                        mapOf(
                            "lastNumber" to 1L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val orderNum = "MD-%06d".format(nextNumber)
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "orderNumber" to orderNum,
                    "category" to request.category,
                    "customerName" to name,
                    "contactPhone" to phone,
                    "customerEmail" to request.customerEmail?.trim()?.take(120)?.takeIf { it.isNotBlank() },
                    "items" to itemMaps,
                    "foodTotal" to foodTotal,
                    "roomTotal" to roomTotal,
                    "freshenUpTotal" to freshenUpTotal,
                    "deliveryCharge" to deliveryCharge,
                    "packagingCharge" to packagingCharge,
                    "grandTotal" to grandTotal,
                    "expectedTime" to time,
                    "fulfillmentType" to request.fulfillmentType,
                    "paymentMethod" to safePaymentMethod,
                    "paymentStatus" to safePaymentStatus,
                    "upiTransactionRef" to safeUpiRef,
                    "whatsappStatus" to initialWhatsAppStatus,
                    "whatsappRetryCount" to 0L,
                    "roomType" to request.roomType,
                    "numberOfRooms" to request.numberOfRooms,
                    "numberOfPeople" to request.numberOfPeople,
                    "roomDurationHours" to request.roomDurationHours,
                    "roomRateConfigured" to request.roomRateConfigured,
                    "freshenUpOption" to request.freshenUpOption,
                    "deliveryAddress" to request.deliveryAddress?.trim()?.take(300)?.takeIf { it.isNotBlank() },
                    "deliveryLocality" to request.deliveryLocality?.trim()?.take(100)?.takeIf { it.isNotBlank() },
                    "notes" to request.notes?.trim()?.take(300)?.takeIf { it.isNotBlank() },
                    "status" to RequestStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                transaction.set(docRef, payload)
                orderNum
            }.await()

            Result.success(
                request.copy(
                    id = docRef.id,
                    orderNumber = generatedOrderNumber,
                    userId = uid,
                    customerName = name,
                    contactPhone = phone,
                    expectedTime = time,
                    foodTotal = foodTotal,
                    roomTotal = roomTotal,
                    freshenUpTotal = freshenUpTotal,
                    deliveryCharge = deliveryCharge,
                    packagingCharge = packagingCharge,
                    grandTotal = grandTotal,
                    paymentStatus = safePaymentStatus,
                    whatsappStatus = initialWhatsAppStatus,
                    status = RequestStatus.PENDING.name
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markOrderManualWhatsAppShared(requestId: String): Result<Unit> {
        val docPath = "requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank())
            db.collection("requests").document(requestId).update(
                mapOf(
                    "whatsappStatus" to WhatsAppDeliveryStatus.MANUAL_SHARED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Owner Admin Operations ---

    suspend fun saveShopSettings(settings: ShopSettings): Result<Unit> {
        val docPath = "shop_settings/main"
        return try {
            requireUserId()
            val payload = mapOf(
                "shopName" to settings.shopName.trim().ifBlank { "MARUTI DHAM" }.take(100),
                "ownerName" to settings.ownerName.trim().ifBlank { "Tarun Kumar" }.take(80),
                "address" to settings.address.trim().ifBlank { DefaultCatalogData.defaultShopSettings.address }.take(300),
                "mapsQuery" to settings.mapsQuery.trim().ifBlank { DefaultCatalogData.defaultShopSettings.mapsQuery }.take(300),
                "shopOpen" to settings.shopOpen,
                "shopControlMode" to settings.shopControlMode,
                "openingTime" to settings.openingTime.trim().take(40),
                "closingTime" to settings.closingTime.trim().take(40),
                "whatsappNumber" to settings.whatsappNumber.trim().take(25),
                "contactPhone" to settings.contactPhone.trim().take(25),
                "contactEmail" to settings.contactEmail.trim().take(120),
                "upiId" to settings.upiId.trim().take(120),
                "cashEnabled" to settings.cashEnabled,
                "upiEnabled" to settings.upiEnabled,
                "whatsappWebhookUrl" to settings.whatsappWebhookUrl.trim().take(500),
                "aboutText" to settings.aboutText.trim().take(2000),
                "privacyPolicy" to settings.privacyPolicy.trim().take(3000),
                "termsText" to settings.termsText.trim().take(3000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("shop_settings").document("main").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun themeToMap(theme: ThemeConfig): Map<String, Any> = mapOf(
        "preset" to theme.preset.ifBlank { "NAVY_ROYAL_GOLD" }.take(40),
        "primaryColorHex" to theme.primaryColorHex.ifBlank { "#E5B94C" }.take(12),
        "backgroundColorHex" to theme.backgroundColorHex.ifBlank { "#07122B" }.take(12),
        "surfaceColorHex" to theme.surfaceColorHex.ifBlank { "#142652" }.take(12),
        "accentColorHex" to theme.accentColorHex.ifBlank { "#F7D774" }.take(12),
        "textColorHex" to theme.textColorHex.ifBlank { "#F8F6F0" }.take(12),
        "fontStyle" to theme.fontStyle.ifBlank { "DEVOTIONAL_SERIF" }.take(30),
        "layoutVariant" to theme.layoutVariant.ifBlank { "DEVOTIONAL_CARD" }.take(30)
    )

    suspend fun saveSiteConfig(config: SiteConfig): Result<Unit> {
        val docPath = "site_config/main"
        return try {
            requireUserId()
            val cleanCategories = config.categories
                .map { it.trim().take(50) }
                .filter { it.isNotBlank() }
                .distinct()
                .take(20)
                .ifEmpty { DefaultCatalogData.defaultSiteConfig.categories }
            val payload = mapOf(
                "logoUrl" to config.logoUrl.trim().take(2000),
                "heroBannerUrl" to config.heroBannerUrl.trim().take(2000),
                "showAnnouncementsSection" to config.showAnnouncementsSection,
                "showDailySpecialsSection" to config.showDailySpecialsSection,
                "showRoomsSection" to config.showRoomsSection,
                "showDeliverySection" to config.showDeliverySection,
                "categories" to cleanCategories,
                "draftTheme" to themeToMap(config.draftTheme),
                "publishedTheme" to themeToMap(config.publishedTheme),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("site_config").document("main").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(announcement: Announcement): Result<Unit> {
        val isNew = announcement.id.isBlank()
        val docRef = if (isNew) db.collection("announcements").document() else db.collection("announcements").document(announcement.id)
        return try {
            requireUserId()
            val title = announcement.title.trim().take(100)
            val message = announcement.message.trim().take(500)
            require(title.isNotBlank() && message.isNotBlank()) { "Title and message are required" }
            if (isNew || !docRef.get().await().exists()) {
                docRef.set(
                    mapOf(
                        "title" to title,
                        "message" to message,
                        "active" to announcement.active,
                        "dailySpecial" to announcement.dailySpecial,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to title,
                        "message" to message,
                        "active" to announcement.active,
                        "dailySpecial" to announcement.dailySpecial,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val docPath = "announcements/$announcementId"
        return try {
            requireUserId()
            require(announcementId.isNotBlank())
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        val isNew = product.id.isBlank()
        val docRef = if (isNew) db.collection("products").document() else db.collection("products").document(product.id)
        return try {
            requireUserId()
            val name = product.name.trim().take(80)
            require(name.isNotBlank()) { "Product name is required" }
            val options = product.priceOptions
                .filter { it.label.isNotBlank() && it.price >= 0L }
                .take(5)
            require(options.isNotEmpty()) { "At least one price option is required" }
            val optionMaps = options.map { mapOf("label" to it.label.trim().take(40), "price" to it.price) }

            val threshold = product.lowStockThreshold.coerceIn(0L, 10000L)
            val exists = !isNew && docRef.get().await().exists()
            if (!exists) {
                docRef.set(
                    mapOf(
                        "name" to name,
                        "nameHindi" to product.nameHindi.trim().take(80),
                        "description" to product.description.trim().take(240),
                        "category" to product.category.trim().ifBlank { "Snacks and Desserts" }.take(50),
                        "priceOptions" to optionMaps,
                        "imageUrl" to product.imageUrl.trim().take(2000),
                        "localImageKey" to product.localImageKey.trim().ifBlank { "food_platter" }.take(50),
                        "available" to product.available,
                        "visible" to product.visible,
                        "foodOrderEnabled" to product.foodOrderEnabled,
                        "deliveryEnabled" to product.deliveryEnabled,
                        "stockQuantity" to product.stockQuantity.coerceIn(0L, 100000L),
                        "lowStockThreshold" to threshold,
                        "trackStock" to product.trackStock,
                        "sortOrder" to product.sortOrder.coerceIn(0L, 10000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name,
                        "nameHindi" to product.nameHindi.trim().take(80),
                        "description" to product.description.trim().take(240),
                        "category" to product.category.trim().ifBlank { "Snacks and Desserts" }.take(50),
                        "priceOptions" to optionMaps,
                        "imageUrl" to product.imageUrl.trim().take(2000),
                        "localImageKey" to product.localImageKey.trim().ifBlank { "food_platter" }.take(50),
                        "available" to product.available,
                        "visible" to product.visible,
                        "foodOrderEnabled" to product.foodOrderEnabled,
                        "deliveryEnabled" to product.deliveryEnabled,
                        "stockQuantity" to product.stockQuantity.coerceIn(0L, 100000L),
                        "lowStockThreshold" to threshold,
                        "trackStock" to product.trackStock,
                        "sortOrder" to product.sortOrder.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank())
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveRoomConfig(config: RoomConfig): Result<Unit> {
        val docId = config.id.ifBlank { if (config.roomType == "AC") "ac" else "non_ac" }
        val docRef = db.collection("room_configs").document(docId)
        return try {
            requireUserId()
            val exists = docRef.get().await().exists()
            if (!exists) {
                val payload = mutableMapOf<String, Any?>(
                    "roomType" to config.roomType,
                    "title" to config.title.trim().take(80),
                    "titleHindi" to config.titleHindi.trim().take(80),
                    "available" to config.available,
                    "statusLabel" to config.statusLabel,
                    "availableCount" to config.availableCount.coerceIn(0L, 500L),
                    "imageUrl" to config.imageUrl.trim().take(2000),
                    "rate6Hours" to config.rate6Hours.coerceIn(0L, 100000L),
                    "rate12Hours" to config.rate12Hours.coerceIn(0L, 100000L),
                    "rate1Hour" to config.rate1Hour?.coerceIn(0L, 100000L),
                    "rate2Hours" to config.rate2Hours?.coerceIn(0L, 100000L),
                    "rate3Hours" to config.rate3Hours?.coerceIn(0L, 100000L),
                    "rate4Hours" to config.rate4Hours?.coerceIn(0L, 100000L),
                    "rate5Hours" to config.rate5Hours?.coerceIn(0L, 100000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "title" to config.title.trim().take(80),
                    "titleHindi" to config.titleHindi.trim().take(80),
                    "available" to config.available,
                    "statusLabel" to config.statusLabel,
                    "availableCount" to config.availableCount.coerceIn(0L, 500L),
                    "imageUrl" to config.imageUrl.trim().take(2000),
                    "rate6Hours" to config.rate6Hours.coerceIn(0L, 100000L),
                    "rate12Hours" to config.rate12Hours.coerceIn(0L, 100000L),
                    "rate1Hour" to (config.rate1Hour?.coerceIn(0L, 100000L) ?: FieldValue.delete()),
                    "rate2Hours" to (config.rate2Hours?.coerceIn(0L, 100000L) ?: FieldValue.delete()),
                    "rate3Hours" to (config.rate3Hours?.coerceIn(0L, 100000L) ?: FieldValue.delete()),
                    "rate4Hours" to (config.rate4Hours?.coerceIn(0L, 100000L) ?: FieldValue.delete()),
                    "rate5Hours" to (config.rate5Hours?.coerceIn(0L, 100000L) ?: FieldValue.delete()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveFreshenUpConfig(config: FreshenUpConfig): Result<Unit> {
        val docId = config.id.ifBlank { if (config.optionKey == "ONLY_FRESH") "only_fresh" else "bath_and_fresh" }
        val docRef = db.collection("freshen_up_configs").document(docId)
        return try {
            requireUserId()
            val exists = docRef.get().await().exists()
            if (!exists) {
                docRef.set(
                    mapOf(
                        "optionKey" to config.optionKey,
                        "title" to config.title.trim().take(100),
                        "titleHindi" to config.titleHindi.trim().take(100),
                        "pricePerPerson" to config.pricePerPerson.coerceIn(0L, 10000L),
                        "available" to config.available,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to config.title.trim().take(100),
                        "titleHindi" to config.titleHindi.trim().take(100),
                        "pricePerPerson" to config.pricePerPerson.coerceIn(0L, 10000L),
                        "available" to config.available,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveDeliverySettings(settings: DeliverySettings): Result<Unit> {
        val docRef = db.collection("delivery_settings").document("main")
        return try {
            requireUserId()
            val localities = settings.allowedLocalities
                .map { it.trim().take(80) }
                .filter { it.isNotBlank() }
                .take(30)
                .ifEmpty { listOf("Khatu Shyam Ji") }
            docRef.set(
                mapOf(
                    "deliveryEnabled" to settings.deliveryEnabled,
                    "pickupEnabled" to settings.pickupEnabled,
                    "deliveryCharge" to settings.deliveryCharge.coerceIn(0L, 10000L),
                    "packagingCharge" to settings.packagingCharge.coerceIn(0L, 10000L),
                    "serviceAreaName" to settings.serviceAreaName.trim().ifBlank { "Khatu Shyam Ji" }.take(100),
                    "allowedLocalities" to localities,
                    "minimumOrderAmount" to settings.minimumOrderAmount.coerceIn(0L, 100000L),
                    "deliveryNote" to settings.deliveryNote.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveOwnerPrivateSettings(settings: OwnerPrivateSettings): Result<Unit> {
        val docRef = db.collection("owner_private_settings").document("main")
        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "ownerWhatsAppNumber" to settings.ownerWhatsAppNumber.trim().take(25),
                    "whatsappWebhookUrl" to settings.whatsappWebhookUrl.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun logOwnerActivity(
        action: String,
        targetType: String,
        targetId: String,
        previousValue: String,
        newValue: String,
        viaAi: Boolean = false
    ) {
        val docRef = db.collection("activity_logs").document()
        try {
            val user = auth.currentUser ?: return
            val istText = DomainCalculators.formatIstDateTime(System.currentTimeMillis())
            docRef.set(
                mapOf(
                    "action" to action.trim().take(120),
                    "targetType" to targetType.trim().take(60),
                    "targetId" to targetId.trim().take(120),
                    "previousValue" to previousValue.trim().take(400),
                    "newValue" to newValue.trim().take(400),
                    "performedByEmail" to (user.email ?: "").trim().take(120),
                    "performedByUid" to user.uid.take(128),
                    "viaAi" to viaAi,
                    "istTimestampText" to istText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val docPath = "notifications/$notificationId"
        return try {
            requireUserId()
            require(notificationId.isNotBlank())
            db.collection("notifications").document(notificationId).update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatusAndPayment(
        requestId: String,
        newStatus: RequestStatus? = null,
        newPaymentStatus: PaymentStatus? = null,
        adminNote: String? = null
    ): Result<Unit> {
        val docPath = "requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank())
            val payload = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus != null) payload["status"] = newStatus.name
            if (newPaymentStatus != null) payload["paymentStatus"] = newPaymentStatus.name
            val cleanedNote = adminNote?.trim()?.take(300)
            if (!cleanedNote.isNullOrBlank()) {
                payload["adminNote"] = cleanedNote
            }
            db.collection("requests").document(requestId).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Calls the owner-configured Firebase Cloud Function HTTP webhook (if deployed)
     * with the user's Firebase ID token so the server sends the WhatsApp Cloud API message.
     * Never fakes a SENT result if unconfigured or failing.
     */
    suspend fun retryWhatsAppViaCloudFunction(
        requestId: String,
        webhookUrl: String
    ): Result<String> {
        val docPath = "requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank())
            val urlClean = webhookUrl.trim()
            if (urlClean.isBlank() || !urlClean.startsWith("https://")) {
                val msg = "Cloud Function endpoint URL is not configured in Shop Settings."
                db.collection("requests").document(requestId).update(
                    mapOf(
                        "whatsappStatus" to WhatsAppDeliveryStatus.NOT_CONFIGURED.name,
                        "whatsappError" to msg,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.failure(IllegalStateException(msg))
            }

            val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
                ?: return Result.failure(IllegalStateException("Missing auth token"))

            val responsePair = withContext(Dispatchers.IO) {
                val conn = (URL(urlClean).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
                val body = JSONObject().apply {
                    put("data", JSONObject().apply { put("requestId", requestId) })
                }.toString()
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
                val code = conn.responseCode
                val text = try {
                    (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (_: Exception) { "" }
                conn.disconnect()
                code to text
            }

            if (responsePair.first in 200..299) {
                db.collection("requests").document(requestId).update(
                    mapOf(
                        "whatsappStatus" to WhatsAppDeliveryStatus.SENT.name,
                        "whatsappError" to FieldValue.delete(),
                        "whatsappRetryCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success("WhatsApp bill delivered via server")
            } else {
                val errMsg = "HTTP ${responsePair.first}: ${responsePair.second.take(200)}"
                db.collection("requests").document(requestId).update(
                    mapOf(
                        "whatsappStatus" to WhatsAppDeliveryStatus.FAILED.name,
                        "whatsappError" to errMsg,
                        "whatsappRetryCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.failure(IllegalStateException(errMsg))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
