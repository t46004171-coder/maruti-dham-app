# MARUTI DHAM – Firebase Cloud Functions Setup

This directory contains the server-side Firebase Cloud Functions for automatic owner WhatsApp order billing via the **Meta WhatsApp Business Cloud API**.

All API credentials stay exclusively on Google Cloud / Firebase servers (never inside the Android APK).

## Functions Included
1. **`onOrderCreatedNotifyOwner`**: Automatically triggers on `requests/{requestId}` creation in Firestore database `ai-studio-android-marutidh-bc57197a-19de-4b00-a083-0583d44833e4`, formats the IST `MARUTI DHAM – NEW ORDER` bill, sends it to the owner WhatsApp (`+91 7850068566`), and records `whatsappStatus` (`SENT`, `FAILED`, or `NOT_CONFIGURED`) on the order document.
2. **`retryWhatsAppNotification`**: Callable HTTPS function strictly restricted to verified owner email `tarunkumarjoshi11@gmail.com` to retry failed notifications from the Owner Dashboard.

## Deployment Steps
1. Upgrade the Firebase project (`gen-lang-client-0413146166`) to the Blaze plan in the Firebase Console.
2. Create a WhatsApp Business app in the Meta Developer Portal and copy your **Phone Number ID** and **Permanent System User Access Token**.
3. Configure secrets on Firebase (never commit them):
   ```bash
   firebase functions:secrets:set WHATSAPP_ACCESS_TOKEN
   firebase functions:secrets:set WHATSAPP_PHONE_NUMBER_ID
   ```
4. Deploy functions and Firestore indexes:
   ```bash
   cd functions && npm install
   firebase deploy --only functions,firestore:indexes --project gen-lang-client-0413146166
   ```
5. (Optional) Copy the HTTPS endpoint URL of `retryWhatsAppNotification` into **Owner Admin Panel → Shop Settings → WhatsApp Retry Webhook URL** if using an HTTP webhook trigger.
