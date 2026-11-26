# MARUTI DHAM — Test & Verification Checklist

## Automated Verification
- [x] **Android Build (`compile_applet`)**:
  - Compiled full Kotlin / Jetpack Compose app (`com.aistudio.marutidham.kxmpzq`) after all upgrades with zero errors.
- [x] **Firestore Security Rules (`firestore.test.js`)** on local Firestore emulator (port `8085`, 6/6 subtests passing, and rules deployed):
  - Unauthenticated reads/writes blocked across all collections.
  - Customer registration, duplicate mobile prevention via `/phone_registry/{normalizedPhone}`, consent (`consentAccepted`), and `deletionRequested` update verified; role/privilege escalation rejected.
  - `/admins/{uid}` first-time owner bootstrap and owner-only RBAC verified (`tarunkumarjoshi11@gmail.com` + `email_verified == true` required; non-owners denied).
  - `/owner_private_settings/main` read/write isolated strictly to verified owner.
  - `/site_config/main` public read allowed for live app/website theming; write restricted strictly to verified owner with schema validation (including optional `logoUrl`, `heroBannerUrl`, `heroTitle`, `heroSubtitle`).
  - `/categories` public read and verified owner CRUD verified.
  - `/activity_logs` owner-only creation/read verified; `update` and `delete` rejected even for owner (immutable audit trail).
  - `/notifications` client creation/deletion blocked; owner `read: true` toggle allowed; customers denied.
  - Cash Only default enforced (`cashEnabled: true`, `upiEnabled: false` default; UPI payments rejected when `upiEnabled == false`).
- [x] **JVM Unit Tests (`DomainCalculatorsTest`, `ExampleUnitTest`)**:
  - Initial menu pricing & inactive suggested catalog templates.
  - Food total, AC/Non-AC room rates, Freshen Up per-person calculations, Delivery + Packaging `grandTotal`, and Khatu Shyam Ji delivery validation.
  - All 4 IST shop open/closed modes (`OPEN_NOW`, `CLOSED_NOW`, `OPEN_AT_SCHEDULED`, `CLOSE_AT_SCHEDULED`).
  - Separated Shop Orders (`FOOD_REQUEST`), Delivery Orders (`FOOD_DELIVERY_REQUEST`), Room Bookings (`ROOM_REQUEST`), and Freshen-Up Requests (`FRESHEN_UP_REQUEST`) KPIs, `Most Popular Product` aggregation, reports, and customer name / mobile number search filtering (`filterAdminOrders`).
  - 4-column `ITEM | QTY | RATE | TOTAL` Digital Receipt & WhatsApp bill formatting with Subtotal, Delivery Charge, Packaging Charge, and Grand Total.
  - Exact 5 bottom navigation tab titles in both English and Hindi.
- [x] **Web PWA Syntax Check (`node --check web/app.js`)**:
  - Verified zero syntax errors across updated Web portal, login gate, Cash Only checkout flow, Design Studio, and 9-subtab Owner Admin Dashboard.
- [x] **Auth & Privacy Audit (`grep`)**:
  - Zero `signInAnonymously`, `signInWithEmailAndPassword`, or hardcoded client IDs.
  - Zero customer-facing exposure of owner's personal Gmail or mobile number in Compose UI or Web customer views.
- [ ] **Robolectric Resource Test (`ExampleRobolectricTest`)**:
  - Not executed inside this offline container because Robolectric downloads `org.robolectric:android-all` from Maven Central at runtime; run locally with internet access via `gradle :app:testDebugUnitTest`.

## Manual Verification on Device / Streaming Emulator & Web PWA
- [ ] On app/web open, verify the Two-Option Entry Gate (**1. Customer Login** and **2. Owner / Admin Login**) renders cleanly in Royal Navy + Gold without showing owner email/phone or public owner signup.
- [ ] Select **Owner / Admin Login** with a non-owner Gmail account -> verify immediate Access Denied alert and automatic sign-out.
- [ ] Sign in as Authorized Owner (`tarunkumarjoshi11@gmail.com`) -> verify `/admins/{uid}` bootstrap, all 15 Android Admin tabs (separated Shop Orders, Delivery Orders, Rooms, Freshen-Up, Customers, Menu & Categories, Stock & Prices, Design & Theme Studio, Shop & Payment, Rooms & Delivery, Reports & WhatsApp, Privacy & Security, MARUTI AI, Activity & Alerts), and working Owner Sign Out.
- [ ] Sign in as Customer -> verify **Cash Only** checkout (all UPI/UTR options hidden while `upiEnabled == false`), working Customer Sign Out in Settings, and `My Requests` isolation.
- [ ] Test **Design & Theme Studio** (`Save Draft`, `Live Preview`, `Publish Theme`, preset & custom hex colors, home section toggles, logo/banner upload) across both Android and `/web/index.html`.
