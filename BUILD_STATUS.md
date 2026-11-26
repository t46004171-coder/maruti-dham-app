# MARUTI DHAM — Build & Feature Status

## Completed
- [x] Custom adaptive launcher icon (`ic_maruti_logo`) and density mipmap PNG fallbacks.
- [x] Devotional Royal Navy, Royal Gold & White Material 3 theme + bundled Google Fonts (`Cinzel`, `Plus Jakarta Sans`, `Noto Sans Devanagari`), dynamically driven by `/site_config/main`.
- [x] Animated MARUTI DHAM Splash Screen & Two-Option Login Portal (**1. Customer Login** / **2. Owner / Admin Login**) using Google Sign-In via Jetpack Credential Manager.
- [x] Strict Backend-Verified Owner RBAC: `/admins/{uid}` first-time owner bootstrap + Firestore Security Rules requiring exact Gmail (`tarunkumarjoshi11@gmail.com`) and `email_verified == true`; non-owners receive an Access Denied alert and are signed out automatically, with zero public owner registration.
- [x] Exact 5-tab Customer Navigation (`Home`, `Food Order`, `Rooms & Freshen Up`, `Food Delivery`, `Settings`) + `My Requests` isolation + working Customer Sign Out & Account Deletion request.
- [x] **Cash Only Payment Default** (`cashEnabled = true`, `upiEnabled = false`): UPI options, UPI IDs, and UTR fields stay completely hidden on both Android and Web until explicitly enabled by the owner.
- [x] **15-Tab Owner Admin Panel** on Android (and matching 9-subtab Owner Dashboard on Web):
  - Overview with separated Shop vs Delivery revenue & counts
  - Separated **Shop Orders** (`FOOD_REQUEST`), **Delivery Orders** (`FOOD_DELIVERY_REQUEST`), **Room Bookings** (`ROOM_REQUEST`), and **Freshen-Up Requests** (`FRESHEN_UP_REQUEST`)
  - **Customers** (search + pagination)
  - **Menu & Editable Categories** (`/categories`) & **Stock & Prices**
  - **Design & Theme Studio** (`/site_config/main`: `NAVY_ROYAL_GOLD`, `WHITE_GOLD`, `PREMIUM_DARK`, `CREAM_ROYAL_BLUE`, `MODERN_ROYAL_PURPLE`, `CUSTOM` hex colors, font/layout variants, logo/hero banner upload, home section toggles, Live Preview, Save Draft, Publish Theme)
  - **Shop & Payment** (Cash Only default / optional UPI toggle, timings, announcements)
  - **Rooms & Delivery**, **Reports & WhatsApp**, **Privacy & Security** (verified session card, deletion requests, Owner Sign Out), **MARUTI AI** (`Firebase.ai`), and **Activity & Alerts** (`/notifications` + immutable `/activity_logs`).
- [x] Responsive Website & Installable PWA (`/web/`) connected to the same Firebase project, `/site_config/main` live theme engine, two-option login gate, Cash Only checkout, and verified Owner Admin console.
- [x] Deployed hardened zero-trust `firestore.rules`, passing `/firestore.test.js` (6/6 subtests), passing `DomainCalculatorsTest` & `ExampleUnitTest`, and verified `compile_applet`.

## Exporting APK / AAB
- Debug APK: `gradle :app:assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`
- Signed Release APK: `gradle :app:assembleRelease` -> `app/build/outputs/apk/release/app-release.apk`
- Or export APK / AAB directly from the AI Studio Settings menu.

## Owner Optional Configuration
- Set Meta Cloud API secrets (`WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`) and deploy `/functions/` for automated server-side WhatsApp dispatch.
- Configure optional UPI ID, custom theme/branding in Design & Theme Studio, 1–5 hour room rates, or editable menu categories inside the Owner Admin Panel.
