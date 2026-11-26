# MARUTI DHAM — Privacy Policy

Effective Date: 2026  
Business Name: **MARUTI DHAM**  
Owner: **Tarun Kumar**  
Location: **Near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India**

## 1. Information We Collect
When you sign in with Google and place an order or request via the MARUTI DHAM Android app or website, we collect only the details necessary to fulfill your service:
- **Account Identifiers**: Firebase Authentication UID, Google display name, and verified email address.
- **Contact & Delivery Details**: Your full name, 10-digit Indian mobile number (indexed under `/phone_registry` to prevent duplicate accounts), preferred language (`en`/`hi`), and optional Khatu Shyam Ji delivery address/locality.
- **Order & Request Records**: Selected food items, room or freshen-up options, expected arrival time, payment method (`CASH` or `UPI`), optional UPI transaction reference, and generated digital receipts.

## 2. How We Use Information
- To prepare, confirm, and fulfill Food Pickup, Food Delivery, Room, and Freshen Up requests.
- To prevent duplicate spam accounts and verify genuine customer orders.
- To allow you to view your live order status and download/print digital receipts.
- To notify the authorized shop owner of new incoming orders.

## 3. Data Privacy, Owner Isolation & Security
- **Strict Customer Isolation**: Customers can read and write only their own profile (`/users/{uid}`) and their own orders (`/requests` filtered by `userId == uid`). No customer can access another customer's mobile number, address, or order history.
- **Owner Personal Privacy**: The shop owner's personal email and private WhatsApp notification settings are stored in server-protected / owner-only collections (`/owner_private_settings/main`) and are never exposed on public storefront screens or default customer receipts.
- **Zero-Trust Security Rules**: All database reads and writes are enforced by Cloud Firestore Security Rules and Firebase App Check.
- **AI Admin Assistant Isolation**: **MARUTI AI** runs strictly inside the owner-authenticated admin console and never exposes customer personal records externally.

## 4. Data Retention & Account Deletion
- Order and profile records are retained to maintain your order history and shop accounting records.
- You may request deletion of your account and profile data at any time from **Settings → Request Account Deletion** inside the Android app or website (`deletionRequested: true` on `/users/{uid}`), or request assistance at the shop counter near Manda Chauraha, Khatu Shyam Ji.
