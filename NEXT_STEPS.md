# MARUTI DHAM — Next Steps & APK Export

## First-Time Owner Setup (Inside the App)
1. Open the app and sign in with the authorized owner Google account (`gopisharma439291@gmail.com`).
2. Open **Settings -> Owner Admin Panel** (or tap **Open** on the Home screen Owner banner).
3. Under **Shop & Notices**, enter:
   - Shop Opening & Closing Timings (e.g., `06:00 AM` and `10:00 PM`).
   - Owner WhatsApp Number (with country code, e.g., `91XXXXXXXXXX`) so customer WhatsApp shares open your chat directly.
   - Public Contact Phone and Email.
4. Under **Rooms & Fresh**, optionally set rates for 1, 2, 3, 4, and 5-hour stays for AC and Non-AC rooms, and update available room counts.
5. Under **Menu** and **Delivery**, customize food items, HTTPS product image URLs, and allowed Khatu Shyam Ji localities.

## Generating an Installable APK / AAB
- **Via AI Studio**: Use the **Settings / Export** menu in AI Studio to generate a signed APK/AAB or export the complete project ZIP / push to GitHub.
- **Via Gradle CLI**:
  ```bash
  gradle :app:assembleDebug
  ```
  The installable debug APK will be generated at:
  `app/build/outputs/apk/debug/app-debug.apk`
  For a release bundle:
  ```bash
  gradle :app:bundleRelease
  ```

## Optional Production Enhancements
- **Firebase Storage**: Enable in Firebase Console to support direct image uploads from the Admin Panel across customer devices.
- **WhatsApp Cloud API**: Connect a Firebase Cloud Function + Meta WhatsApp Business Platform API for automated server-side notifications when a request document is created or updated.
