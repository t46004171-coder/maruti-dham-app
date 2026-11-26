const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_ALICE = "customer_alice";
const CUSTOMER_BOB = "customer_bob";
const OWNER_UID = "owner_tarun";
const OWNER_EMAIL = "tarunkumarjoshi11@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function ownerDb() {
  return testEnv
    .authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true })
    .firestore();
}

function spoofedOwnerDb() {
  return testEnv
    .authenticatedContext("spoof_uid", { email: OWNER_EMAIL, email_verified: false })
    .firestore();
}

function customerDb(uid = CUSTOMER_ALICE) {
  return testEnv
    .authenticatedContext(uid, { email: `${uid}@example.com`, email_verified: true })
    .firestore();
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("products").get());
  await assertFails(unauth.collection("shop_settings").doc("main").get());
  await assertFails(unauth.collection("requests").get());
  await assertFails(unauth.collection("users").doc(CUSTOMER_ALICE).get());
  await assertFails(unauth.collection("phone_registry").doc("919876543210").get());
  await assertFails(unauth.collection("counters").doc("orders").get());
});

test("Customer registration & duplicate mobile prevention via phone_registry", async () => {
  const alice = customerDb(CUSTOMER_ALICE);
  const bob = customerDb(CUSTOMER_BOB);
  const owner = ownerDb();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    alice.collection("users").doc(CUSTOMER_ALICE).set({
      userId: CUSTOMER_ALICE,
      displayName: "Alice Devotee",
      email: "alice@example.com",
      phone: "9876543210",
      preferredLanguage: "en",
      registrationCompleted: true,
      preferredFulfillment: "PICKUP",
      defaultAddress: "",
      defaultLocality: "Khatu Shyam Ji",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Claim unique normalized phone
  await assertSucceeds(
    alice.collection("phone_registry").doc("919876543210").set({
      userId: CUSTOMER_ALICE,
      createdAt: now,
    })
  );

  // Bob cannot claim Alice's mobile number or list phone_registry
  await assertFails(
    bob.collection("phone_registry").doc("919876543210").set({
      userId: CUSTOMER_BOB,
      createdAt: now,
    })
  );
  await assertFails(bob.collection("phone_registry").get());

  // Bob cannot list /users (private customer DB), only owner can
  await assertFails(bob.collection("users").get());
  await assertSucceeds(owner.collection("users").get());

  // Shadow escalation rejected
  await assertFails(
    alice.collection("users").doc(CUSTOMER_ALICE).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner-only catalog, shop control, and stock management", async () => {
  const owner = ownerDb();
  const spoof = spoofedOwnerDb();
  const alice = customerDb(CUSTOMER_ALICE);
  const now = new Date(Date.now() - 2000);

  const productPayload = {
    name: "Desi Tea",
    nameHindi: "देसी चाय",
    category: "Tea & Beverages",
    priceOptions: [{ label: "Small", price: 10 }, { label: "Kulhad", price: 20 }],
    imageUrl: "",
    localImageKey: "food_platter",
    available: true,
    visible: true,
    foodOrderEnabled: true,
    deliveryEnabled: true,
    stockQuantity: 40,
    trackStock: true,
    sortOrder: 1,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(alice.collection("products").doc("desi_tea").set(productPayload));
  await assertFails(spoof.collection("products").doc("desi_tea").set(productPayload));

  await assertSucceeds(owner.collection("products").doc("desi_tea").set(productPayload));
  await assertSucceeds(
    owner.collection("products").doc("desi_tea").update({
      stockQuantity: 15,
      available: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const shopPayload = {
    shopName: "MARUTI DHAM",
    ownerName: "Tarun Kumar",
    address: "Near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
    mapsQuery: "Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India",
    shopOpen: true,
    shopControlMode: "OPEN_AT_SCHEDULED",
    openingTime: "07:00 AM",
    closingTime: "10:00 PM",
    whatsappNumber: "+91 7850068566",
    contactPhone: "+91 7850068566",
    contactEmail: "tarunkumarjoshi11@gmail.com",
    upiId: "marutidham@upi",
    cashEnabled: true,
    upiEnabled: false,
    whatsappWebhookUrl: "",
    aboutText: "Small devotional food stall near Manda Chauraha.",
    privacyPolicy: "Privacy policy",
    termsText: "Terms",
    updatedAt: now,
  };
  await assertFails(alice.collection("shop_settings").doc("main").set(shopPayload));
  await assertSucceeds(owner.collection("shop_settings").doc("main").set(shopPayload));
});

test("Atomic order counter, Cash-only default vs owner-enabled UPI, and owner lifecycle updates", async () => {
  const owner = ownerDb();
  const alice = customerDb(CUSTOMER_ALICE);
  const bob = customerDb(CUSTOMER_BOB);
  const now = new Date(Date.now() - 2000);

  // Default shop settings has upiEnabled: false (Cash Only)
  await assertSucceeds(
    owner.collection("shop_settings").doc("main").set({
      shopName: "MARUTI DHAM",
      ownerName: "Tarun Kumar",
      address: "Near Manda Chauraha, Khatu Shyam Ji",
      mapsQuery: "Manda Chauraha",
      shopOpen: true,
      shopControlMode: "OPEN_NOW",
      openingTime: "07:00 AM",
      closingTime: "10:00 PM",
      whatsappNumber: "",
      contactPhone: "",
      contactEmail: "",
      upiId: "",
      cashEnabled: true,
      upiEnabled: false,
      whatsappWebhookUrl: "",
      aboutText: "Food stall",
      privacyPolicy: "Privacy",
      termsText: "Terms",
      updatedAt: now,
    })
  );

  // When upiEnabled is false, UPI order creation MUST fail, CASH order succeeds
  await assertFails(
    alice.collection("requests").doc("ord_upi_blocked").set({
      userId: CUSTOMER_ALICE,
      orderNumber: "MD-000000",
      category: "FOOD_REQUEST",
      customerName: "Alice",
      contactPhone: "9876543210",
      items: [{ name: "Desi Tea", variantLabel: "Small", unitPrice: 10, quantity: 1, lineTotal: 10 }],
      foodTotal: 10,
      roomTotal: 0,
      freshenUpTotal: 0,
      grandTotal: 10,
      expectedTime: "10:00 AM",
      fulfillmentType: "PICKUP",
      paymentMethod: "UPI",
      paymentStatus: "UPI_PENDING_VERIFICATION",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("requests").doc("ord_cash_ok").set({
      userId: CUSTOMER_ALICE,
      orderNumber: "MD-000000",
      category: "FOOD_REQUEST",
      customerName: "Alice",
      contactPhone: "9876543210",
      items: [{ name: "Desi Tea", variantLabel: "Small", unitPrice: 10, quantity: 1, lineTotal: 10 }],
      foodTotal: 10,
      roomTotal: 0,
      freshenUpTotal: 0,
      grandTotal: 10,
      expectedTime: "10:00 AM",
      fulfillmentType: "PICKUP",
      paymentMethod: "CASH",
      paymentStatus: "UNPAID",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner enables UPI for future payment support
  await assertSucceeds(
    owner.collection("shop_settings").doc("main").update({
      upiEnabled: true,
      upiId: "marutidham@upi",
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Counter starts at 1, increments by +1 only
  await assertFails(
    alice.collection("counters").doc("orders").set({ lastNumber: 99, updatedAt: now })
  );
  await assertSucceeds(
    alice.collection("counters").doc("orders").set({ lastNumber: 1, updatedAt: now })
  );
  await assertSucceeds(
    bob.collection("counters").doc("orders").update({
      lastNumber: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bob.collection("counters").doc("orders").update({
      lastNumber: 5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const validOrder = {
    userId: CUSTOMER_ALICE,
    orderNumber: "MD-000001",
    category: "FOOD_REQUEST",
    customerName: "Alice",
    contactPhone: "9876543210",
    customerEmail: "alice@example.com",
    items: [{ name: "Desi Tea", variantLabel: "Small", unitPrice: 10, quantity: 2, lineTotal: 20 }],
    foodTotal: 20,
    roomTotal: 0,
    freshenUpTotal: 0,
    grandTotal: 20,
    expectedTime: "10:15 AM",
    fulfillmentType: "PICKUP",
    paymentMethod: "UPI",
    paymentStatus: "UPI_PENDING_VERIFICATION",
    upiTransactionRef: "UTR4123456789",
    whatsappStatus: "PENDING_SERVER",
    whatsappRetryCount: 0,
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
  };

  // Alice cannot pre-mark her UPI payment as PAID or status as CONFIRMED
  await assertFails(
    alice.collection("requests").doc("ord_bad_paid").set({ ...validOrder, paymentStatus: "PAID" })
  );
  await assertFails(
    alice.collection("requests").doc("ord_bad_status").set({ ...validOrder, status: "CONFIRMED" })
  );

  // Valid PENDING order creation succeeds
  await assertSucceeds(alice.collection("requests").doc("ord_1").set(validOrder));

  // Alice can mark manual WhatsApp share only
  await assertSucceeds(
    alice.collection("requests").doc("ord_1").update({
      whatsappStatus: "MANUAL_SHARED",
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Alice CANNOT verify her own UPI payment or change order status
  await assertFails(
    alice.collection("requests").doc("ord_1").update({
      paymentStatus: "PAID",
      updatedAt: new Date(Date.now() - 1200),
    })
  );
  await assertFails(
    alice.collection("requests").doc("ord_1").update({
      status: "CONFIRMED",
      updatedAt: new Date(Date.now() - 1200),
    })
  );

  // Bob cannot read or list Alice's orders
  await assertFails(bob.collection("requests").doc("ord_1").get());
  await assertFails(bob.collection("requests").get());
  await assertSucceeds(
    alice.collection("requests").where("userId", "==", CUSTOMER_ALICE).get()
  );

  // Owner verifies UPI payment & progresses order lifecycle
  await assertSucceeds(owner.collection("requests").orderBy("createdAt", "desc").limit(40).get());
  await assertSucceeds(
    owner.collection("requests").doc("ord_1").update({
      status: "PREPARING",
      paymentStatus: "PAID",
      adminNote: "UPI verified, preparing tea",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    owner.collection("requests").doc("ord_1").update({
      status: "READY",
      updatedAt: new Date(Date.now() - 800),
    })
  );
  await assertSucceeds(
    owner.collection("requests").doc("ord_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner private settings, immutable activity logs, notifications, and charges validation", async () => {
  const owner = ownerDb();
  const spoof = spoofedOwnerDb();
  const alice = customerDb(CUSTOMER_ALICE);
  const now = new Date(Date.now() - 2000);

  // 1. owner_private_settings strictly isolated to verified owner
  const privatePayload = {
    ownerWhatsAppNumber: "+91 7850068566",
    whatsappWebhookUrl: "https://us-central1-gen-lang-client-0413146166.cloudfunctions.net/retryWhatsAppNotification",
    updatedAt: now,
  };
  await assertFails(alice.collection("owner_private_settings").doc("main").set(privatePayload));
  await assertFails(spoof.collection("owner_private_settings").doc("main").set(privatePayload));
  await assertSucceeds(owner.collection("owner_private_settings").doc("main").set(privatePayload));
  await assertFails(alice.collection("owner_private_settings").doc("main").get());
  await assertSucceeds(owner.collection("owner_private_settings").doc("main").get());

  // 2. activity_logs is owner-only & strictly immutable
  const logPayload = {
    action: "SHOP_STATUS_CHANGED",
    targetType: "shop_settings",
    targetId: "main",
    previousValue: "OPEN_NOW",
    newValue: "CLOSED_NOW",
    performedByEmail: OWNER_EMAIL,
    performedByUid: OWNER_UID,
    viaAi: true,
    istTimestampText: "15/03/2026 • 10:00 AM IST",
    createdAt: now,
  };
  await assertFails(alice.collection("activity_logs").doc("log_1").set(logPayload));
  await assertSucceeds(owner.collection("activity_logs").doc("log_1").set(logPayload));
  await assertSucceeds(owner.collection("activity_logs").doc("log_1").get());
  await assertFails(alice.collection("activity_logs").get());
  // Immutability: even owner cannot update or delete an activity log
  await assertFails(owner.collection("activity_logs").doc("log_1").update({ newValue: "TAMPERED" }));
  await assertFails(owner.collection("activity_logs").doc("log_1").delete());

  // 3. notifications cannot be created or deleted by clients; owner can mark read
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("notifications").doc("notif_1").set({
      title: "New Order MD-000001",
      message: "Alice • ₹120 (DELIVERY)",
      orderNumber: "MD-000001",
      requestId: "ord_deliv",
      read: false,
      createdAt: now,
    });
  });
  await assertFails(
    owner.collection("notifications").doc("notif_2").set({
      title: "Fake",
      message: "Fake",
      orderNumber: "MD-999",
      requestId: "x",
      read: false,
      createdAt: now,
    })
  );
  await assertFails(alice.collection("notifications").get());
  await assertSucceeds(owner.collection("notifications").get());
  await assertFails(alice.collection("notifications").doc("notif_1").update({ read: true }));
  await assertSucceeds(owner.collection("notifications").doc("notif_1").update({ read: true }));
  await assertFails(owner.collection("notifications").doc("notif_1").delete());

  // 4. product description + delivery_settings charges/pickupEnabled
  await assertSucceeds(
    owner.collection("products").doc("kulhad_tea").set({
      name: "Kulhad Tea",
      nameHindi: "कुल्हड़ चाय",
      description: "Served hot in earthen kulhad cup.",
      category: "Tea & Drinks",
      priceOptions: [{ label: "Cup", price: 20 }],
      imageUrl: "",
      localImageKey: "food_platter",
      available: true,
      visible: true,
      foodOrderEnabled: true,
      deliveryEnabled: true,
      stockQuantity: 30,
      trackStock: false,
      sortOrder: 5,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    owner.collection("delivery_settings").doc("main").set({
      deliveryEnabled: true,
      pickupEnabled: true,
      deliveryCharge: 20,
      packagingCharge: 10,
      serviceAreaName: "Khatu Shyam Ji",
      allowedLocalities: ["Manda Chauraha", "Toran Dwar"],
      minimumOrderAmount: 100,
      deliveryNote: "Khatu local only",
      updatedAt: now,
    })
  );

  // 5. requests with deliveryCharge & packagingCharge must match grandTotal exactly
  const deliveryOrder = {
    userId: CUSTOMER_ALICE,
    orderNumber: "MD-000002",
    category: "FOOD_DELIVERY_REQUEST",
    customerName: "Alice",
    contactPhone: "9876543210",
    items: [{ name: "Poha", variantLabel: "Full", unitPrice: 30, quantity: 3, lineTotal: 90 }],
    foodTotal: 90,
    roomTotal: 0,
    freshenUpTotal: 0,
    deliveryCharge: 20,
    packagingCharge: 10,
    grandTotal: 120,
    expectedTime: "01:00 PM",
    fulfillmentType: "DELIVERY",
    deliveryLocality: "Toran Dwar",
    deliveryAddress: "Gali 2, Khatu Shyam Ji",
    paymentMethod: "CASH",
    paymentStatus: "UNPAID",
    whatsappStatus: "NOT_CONFIGURED",
    whatsappRetryCount: 0,
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(
    alice.collection("requests").doc("ord_tampered").set({ ...deliveryOrder, grandTotal: 90 })
  );
  await assertSucceeds(alice.collection("requests").doc("ord_deliv").set(deliveryOrder));

  // 6. user self consent & deletion request update
  await assertSucceeds(
    alice.collection("users").doc(CUSTOMER_ALICE).set({
      userId: CUSTOMER_ALICE,
      displayName: "Alice",
      email: "alice@example.com",
      phone: "9876543210",
      preferredLanguage: "en",
      registrationCompleted: true,
      preferredFulfillment: "PICKUP",
      defaultAddress: "",
      defaultLocality: "Khatu Shyam Ji",
      consentAccepted: true,
      deletionRequested: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(CUSTOMER_ALICE).update({
      deletionRequested: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Strict Owner /admins bootstrap & /site_config Theme Customization security", async () => {
  const owner = ownerDb();
  const spoof = spoofedOwnerDb();
  const alice = customerDb(CUSTOMER_ALICE);
  const now = new Date(Date.now() - 2000);

  // Non-owner or unverified email cannot create /admins doc
  await assertFails(
    alice.collection("admins").doc(CUSTOMER_ALICE).set({
      uid: CUSTOMER_ALICE,
      email: "customer_alice@example.com",
      createdAt: now,
    })
  );
  await assertFails(
    alice.collection("admins").doc(CUSTOMER_ALICE).set({
      uid: CUSTOMER_ALICE,
      email: OWNER_EMAIL,
      createdAt: now,
    })
  );
  await assertFails(
    spoof.collection("admins").doc("spoof_uid").set({
      uid: "spoof_uid",
      email: OWNER_EMAIL,
      createdAt: now,
    })
  );

  // Only verified tarunkumarjoshi11@gmail.com can bootstrap own /admins/{uid}
  await assertSucceeds(
    owner.collection("admins").doc(OWNER_UID).set({
      uid: OWNER_UID,
      email: OWNER_EMAIL,
      createdAt: now,
    })
  );

  // SiteConfig & Theme customization: owner-only write, signed-in read
  const themeObj = {
    preset: "CREAM_ROYAL_BLUE",
    primaryColorHex: "#1E3A8A",
    backgroundColorHex: "#FFFDF5",
    surfaceColorHex: "#F3EFE0",
    accentColorHex: "#C89B2B",
    textColorHex: "#0F1E46",
    fontStyle: "DEVOTIONAL_SERIF",
    layoutVariant: "DEVOTIONAL_CARD",
  };
  const sitePayload = {
    logoUrl: "",
    heroBannerUrl: "",
    showAnnouncementsSection: true,
    showDailySpecialsSection: true,
    showRoomsSection: true,
    showDeliverySection: true,
    categories: ["Tea & Drinks", "Breakfast", "Maggi", "Fast Food", "Indian Food"],
    draftTheme: themeObj,
    publishedTheme: themeObj,
    updatedAt: now,
  };

  await assertFails(alice.collection("site_config").doc("main").set(sitePayload));
  await assertFails(spoof.collection("site_config").doc("main").set(sitePayload));
  await assertSucceeds(owner.collection("site_config").doc("main").set(sitePayload));
  await assertSucceeds(alice.collection("site_config").doc("main").get());
});

