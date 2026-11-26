const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-marutidh-bc57197a-19de-4b00-a083-0583d44833e4";
const AUTHORIZED_OWNER_EMAIL = "tarunkumarjoshi11@gmail.com";
const DEFAULT_OWNER_WHATSAPP = "917850068566";

// Server-side secrets stored via `firebase functions:secrets:set` (NEVER in client code)
const whatsappAccessToken = defineSecret("WHATSAPP_ACCESS_TOKEN");
const whatsappPhoneNumberId = defineSecret("WHATSAPP_PHONE_NUMBER_ID");

function formatIstDateTime(timestamp) {
  const date = timestamp && typeof timestamp.toDate === "function" ? timestamp.toDate() : new Date();
  const dateStr = new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
  const timeStr = new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(date);
  return { dateStr, timeStr };
}

function buildWhatsAppOrderMessage(orderId, data) {
  const { dateStr, timeStr } = formatIstDateTime(data.createdAt);
  const subtotal = (data.foodTotal || 0) + (data.roomTotal || 0) + (data.freshenUpTotal || 0) || (data.grandTotal || 0);
  const deliveryCharge = data.deliveryCharge || 0;
  const packagingCharge = data.packagingCharge || 0;
  const lines = [
    "MARUTI DHAM – NEW ORDER",
    "",
    `Order ID: ${data.orderNumber || orderId}`,
    `Customer Name: ${data.customerName || ""}`,
    `Customer Mobile: ${data.contactPhone || ""}`,
    `Order Date: ${dateStr}`,
    `Order Time: ${timeStr} IST`,
    `Mode: ${data.fulfillmentType || "PICKUP"} (${data.expectedTime || "ASAP"})`,
    `Payment: ${data.paymentMethod || "CASH"} (${data.paymentStatus || "UNPAID"})`,
  ];

  if (data.deliveryAddress) {
    lines.push(`Delivery Address: ${data.deliveryLocality || ""} - ${data.deliveryAddress}`);
  }

  if (Array.isArray(data.items) && data.items.length > 0) {
    lines.push("", "ITEM | QTY | RATE | TOTAL");
    for (const item of data.items) {
      const variant = item.variantLabel ? ` (${item.variantLabel})` : "";
      lines.push(`• ${item.name}${variant} | ${item.quantity} | ₹${item.unitPrice} | ₹${item.lineTotal}`);
    }
  }

  lines.push("", `Subtotal: ₹${subtotal}`);
  if (deliveryCharge > 0) lines.push(`Delivery Charge: ₹${deliveryCharge}`);
  if (packagingCharge > 0) lines.push(`Packaging Charge: ₹${packagingCharge}`);
  lines.push(
    `Grand Total: ₹${data.grandTotal || 0}`,
    "",
    "MARUTI DHAM • Near Manda Chauraha, Khatu Shyam Ji"
  );
  return lines.join("\n");
}

async function resolveOwnerWhatsAppDigits(db) {
  const privateDoc = await db.collection("owner_private_settings").doc("main").get();
  if (privateDoc.exists && privateDoc.data().ownerWhatsAppNumber) {
    const digits = String(privateDoc.data().ownerWhatsAppNumber).replace(/\D/g, "");
    if (digits.length >= 10) return digits;
  }
  const shopDoc = await db.collection("shop_settings").doc("main").get();
  if (shopDoc.exists && shopDoc.data().whatsappNumber) {
    const digits = String(shopDoc.data().whatsappNumber).replace(/\D/g, "");
    if (digits.length >= 10) return digits;
  }
  return DEFAULT_OWNER_WHATSAPP;
}

async function dispatchWhatsAppCloudMessage({ recipientDigits, messageText, token, phoneNumberId }) {
  if (!token || !phoneNumberId) {
    return {
      ok: false,
      status: "NOT_CONFIGURED",
      error: "WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_NUMBER_ID secret is not configured on the server.",
    };
  }

  const endpoint = `https://graph.facebook.com/v20.0/${phoneNumberId}/messages`;
  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      messaging_product: "whatsapp",
      recipient_type: "individual",
      to: recipientDigits,
      type: "text",
      text: {
        preview_url: false,
        body: messageText,
      },
    }),
  });

  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const errMsg = payload?.error?.message || `WhatsApp API HTTP ${response.status}`;
    return { ok: false, status: "FAILED", error: errMsg };
  }

  return {
    ok: true,
    status: "SENT",
    messageId: payload?.messages?.[0]?.id || null,
  };
}

/**
 * Automatically creates an unread owner notification alert and dispatches the formatted bill
 * to the owner's private WhatsApp number via Meta Cloud API when a new order arrives.
 */
exports.onOrderCreatedNotifyOwner = onDocumentCreated(
  {
    document: "requests/{requestId}",
    database: DATABASE_ID,
    secrets: [whatsappAccessToken, whatsappPhoneNumberId],
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const db = getFirestore(DATABASE_ID);
    const data = snap.data();

    await db.collection("notifications").add({
      title: `New Order ${data.orderNumber || snap.id}`,
      message: `${data.customerName || "Customer"} • ₹${data.grandTotal || 0} (${data.fulfillmentType || "PICKUP"})`,
      orderNumber: data.orderNumber || snap.id,
      requestId: snap.id,
      read: false,
      createdAt: FieldValue.serverTimestamp(),
    });

    const recipientDigits = await resolveOwnerWhatsAppDigits(db);
    const messageText = buildWhatsAppOrderMessage(snap.id, data);
    const result = await dispatchWhatsAppCloudMessage({
      recipientDigits,
      messageText,
      token: whatsappAccessToken.value(),
      phoneNumberId: whatsappPhoneNumberId.value(),
    });

    await snap.ref.update({
      whatsappStatus: result.status,
      whatsappError: result.ok ? FieldValue.delete() : (result.error || "Delivery failed").slice(0, 400),
      whatsappRetryCount: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });
  }
);

/**
 * Owner-only callable function to retry sending WhatsApp notification for an order.
 */
exports.retryWhatsAppNotification = onCall(
  {
    secrets: [whatsappAccessToken, whatsappPhoneNumberId],
  },
  async (request) => {
    const auth = request.auth;
    if (!auth || auth.token.email !== AUTHORIZED_OWNER_EMAIL || auth.token.email_verified !== true) {
      throw new HttpsError("permission-denied", "Only the authorized owner can retry WhatsApp notifications.");
    }

    const requestId = request.data && request.data.requestId;
    if (!requestId || typeof requestId !== "string") {
      throw new HttpsError("invalid-argument", "requestId is required.");
    }

    const db = getFirestore(DATABASE_ID);
    const orderRef = db.collection("requests").doc(requestId);
    const orderSnap = await orderRef.get();
    if (!orderSnap.exists) {
      throw new HttpsError("not-found", "Order not found.");
    }

    const recipientDigits = await resolveOwnerWhatsAppDigits(db);
    const messageText = buildWhatsAppOrderMessage(orderSnap.id, orderSnap.data());
    const result = await dispatchWhatsAppCloudMessage({
      recipientDigits,
      messageText,
      token: whatsappAccessToken.value(),
      phoneNumberId: whatsappPhoneNumberId.value(),
    });

    await orderRef.update({
      whatsappStatus: result.status,
      whatsappError: result.ok ? FieldValue.delete() : (result.error || "Retry failed").slice(0, 400),
      whatsappRetryCount: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });

    return {
      success: result.ok,
      status: result.status,
      error: result.error || null,
    };
  }
);
