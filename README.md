# MARUTI DHAM — Android & Responsive Web Application

Official platform for **MARUTI DHAM** (Owner: Tarun Kumar), located near Manda Chauraha, Khatu Shyam Ji, Sikar, Rajasthan, India.

## Architecture & Applications
1. **Native Android App (`/app/`)** — Kotlin, Jetpack Compose Material 3, Firebase Auth (Credential Manager Google Sign-In), Cloud Firestore, Firebase App Check, and Firebase AI Logic (**MARUTI AI**).
2. **Responsive Website & Installable PWA (`/web/`)** — HTML5 / CSS3 / Modular Firebase JS Web App mirroring the two-option Customer/Owner login gate, live `/site_config/main` theming, customer ordering experience, and verified Owner Admin console.
3. **Firebase Backend (`/firestore.rules`, `/functions/index.js`)** — Zero-trust Firestore rules, `/admins/{uid}` owner RBAC, `/site_config/main` live Design Studio config, `/categories`, `/owner_private_settings`, immutable `/activity_logs`, server-generated `/notifications`, and Meta WhatsApp Cloud API notifications.

## Separate Customer & Owner Entry
- **Two-Option Portal**: Every session starts with two clear choices: **1. Customer Login** and **2. Owner / Admin Login**.
- **Strict Owner Security**: Only `tarunkumarjoshi11@gmail.com` (Google Sign-In with `email_verified == true` + `/admins/{uid}` verification and Firestore rules) can enter the Owner Admin Panel. Non-owners are denied and automatically signed out; public owner registration is disabled, and owner personal contact details are never exposed to customers.

## Core Customer Features
- **Live Theme & Devotional Design**: Custom adaptive launcher icon, animated splash screen, Khatu Shyam Ji artwork, bundled Google Fonts (`Cinzel`, `Plus Jakarta Sans`, `Noto Sans Devanagari`), live theme updates from Design Studio, and full **English & Hindi** toggle.
- **Strict 5-Tab Customer Navigation**:
  1. **Home**: Shop Open/Closed status, IST timings, Google Maps directions, daily specials/announcements, and service shortcuts.
  2. **Food Order**: Owner-editable categories (`Tea & Drinks`, `Breakfast`, `Maggi`, `Fast Food`, `Indian Food`, `Snacks and Desserts`, plus custom categories), item descriptions, multi-price options, packaging charge transparency, pickup availability check, and arrival time.
  3. **Rooms & Freshen Up**: AC (`₹1,200 / 6h`, `₹2,000 / 12h`) and Non-AC (`₹800 / 6h`, `₹1,500 / 12h`) rooms, owner-configurable 1–5h rates, *"Continue Without Booking a Room"*, and standalone Freshen Up (`Only Fresh ₹50/person`, `Bath + Fresh ₹100/person`).
  4. **Food Delivery**: Strictly limited to Khatu Shyam Ji localities with minimum order enforcement, delivery charge, and packaging charge breakdown.
  5. **Settings**: Language toggle, `My Requests` order history, Digital Receipt (`ITEM | QTY | RATE | TOTAL`, Print/Save PDF, Share PNG, Copy), `Request Account Deletion`, Privacy Policy (`/PRIVACY_POLICY.md`), and clean **Sign Out** back to the login portal.
- **Cash Only Default**: Checkout defaults strictly to Cash Only (`cashEnabled = true`, `upiEnabled = false`). All UPI options, UPI IDs, and UTR fields stay completely hidden unless enabled by the owner.

## Private Owner Admin Panel (`tarunkumarjoshi11@gmail.com`)
- **15 Dedicated Admin Tabs (Android) / 9 Subtabs (Web)**:
  - **Overview** (separated Shop vs Delivery sales & counts), **Shop Orders**, **Delivery Orders**, **Room Bookings**, **Freshen-Up Requests**, **Customers** (search & pagination), **Menu & Categories** (add/edit/delete/toggle categories & items), **Stock & Prices**, **Design & Theme Studio** (presets + custom hex colors, fonts/layouts, logo/hero banner upload, home section toggles, Live Preview, Save Draft, Publish Theme), **Shop & Payment** (Cash Only / optional UPI toggle, timings, announcements), **Rooms & Delivery**, **Reports & WhatsApp** (daily/weekly/monthly breakdown + CSV), **Privacy & Security** (verified session card, deletion requests, Sign Out), **MARUTI AI** (confirmation-gated commands), and **Activity & Alerts** (unread bell + immutable audit trail).

## Build, Test & Deployment Commands
```bash
# 1. Build Debug & Release APKs
gradle :app:assembleDebug
gradle :app:assembleRelease

# 2. Run Domain JVM Unit Tests
gradle :app:testDebugUnitTest --tests "com.example.DomainCalculatorsTest" --tests "com.example.ExampleUnitTest"

# 3. Run Firestore Security Rules Emulator Tests (port 8085)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

# 4. Syntax-check Web App JS
node --check web/app.js

# 5. Deploy Firebase Rules, Indexes, Functions & Web Hosting
firebase deploy --only firestore:rules,firestore:indexes,functions,hosting
```
