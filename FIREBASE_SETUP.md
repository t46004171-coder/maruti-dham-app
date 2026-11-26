# MARUTI DHAM — Firebase Setup Guide

## Provisioned Resources
Provisioned on Firebase project `gen-lang-client-0413146166`:
- **Cloud Firestore** (named database `ai-studio-android-marutidh-bc57197a-19de-4b00-a083-0583d44833e4` in `app/src/main/res/values/firebase_applet_config.xml`).
- **Firebase Authentication (Google Sign-In)** configured in `app/google-services.json` and Jetpack Credential Manager.
- **Firebase AI Logic (`Firebase.ai`)** for **MARUTI AI** (`gemini-3.8-flash`) inside the owner-only admin console.
- **Firebase App Check**:
  - Debug builds automatically initialize `DebugAppCheckProviderFactory` (plus `recaptcha_site_key` wired cleanly for Play Integrity / web protection).
  - For Play Store release builds, register your Play Integrity / App Check SHA-256 certificate in Firebase Console -> App Check.

## Collections Structure
- `/users/{userId}` — Customer/owner profile, mobile number, fulfillment preference, language (`en`/`hi`), `consentAccepted`, and `deletionRequested`.
- `/phone_registry/{normalizedPhone}` — Uniqueness index preventing duplicate accounts (`allow list: if false`).
- `/counters/orders` — Atomic transactional counter generating sequential `MD-000001` Order IDs.
- `/admins/{adminUid}` — Admin role documents checked by security rules alongside verified email `tarunkumarjoshi11@gmail.com`.
- `/shop_settings/main` — Public storefront settings (open/closed mode, timings, address, UPI ID, public support fields left blank by default so private phone/email never leak).
- `/owner_private_settings/main` — **Owner-only** collection (`allow get, list, write: if isAdmin()`) storing `ownerWhatsAppNumber` and `whatsappWebhookUrl`.
- `/announcements/{announcementId}` — Daily specials and announcements.
- `/products/{productId}` — 6-category food catalog with `description`, `priceOptions`, visibility, availability, and stock quantity.
- `/room_configs/{roomTypeId}` (`ac`, `non_ac`) — Room availability, status, 6h/12h and optional 1–5h rates.
- `/freshen_up_configs/{optionId}` (`only_fresh`, `bath_and_fresh`) — Per-person rates and availability.
- `/delivery_settings/main` — `deliveryEnabled`, `pickupEnabled`, `deliveryCharge`, `packagingCharge`, Khatu Shyam Ji localities, and minimum order amount.
- `/requests/{requestId}` — Customer orders & requests with `deliveryCharge`, `packagingCharge`, `grandTotal`, IST timestamps, and payment/WhatsApp tracking.
- `/activity_logs/{logId}` — **Immutable** owner & MARUTI AI change audit trail (`allow update, delete: if false`).
- `/notifications/{notifId}` — **Server-generated** unread order alerts (`allow create, delete: if false;`, owner can mark `read: true`).

## Deploying Security Rules, Indexes, Functions & Web PWA
1. **Verify Rules Locally**:
   ```bash
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```
2. **Deploy Rules & Indexes**:
   ```bash
   firebase deploy --only firestore:rules,firestore:indexes
   ```
3. **Deploy Cloud Functions (`/functions/index.js`)**:
   ```bash
   firebase functions:secrets:set WHATSAPP_ACCESS_TOKEN
   firebase functions:secrets:set WHATSAPP_PHONE_NUMBER_ID
   firebase deploy --only functions
   ```
   Cloud Functions automatically read the recipient WhatsApp number from `/owner_private_settings/main` (fallback `917850068566`), generate an unread `/notifications` alert, and update `whatsappStatus`.
4. **Deploy Responsive Website & PWA (`/web/`)**:
   ```bash
   firebase deploy --only hosting
   ```
   Ensure `gen-lang-client-0413146166.firebaseapp.com` and your custom domain are listed in Firebase Console -> Authentication -> Authorized Domains for Google Sign-In on web.
