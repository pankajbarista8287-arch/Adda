# ADDA – Setup, Deployment, Voice SDK, Payment & Admin Guide

## 1. Official Identity & Architecture
- **App Name**: `ADDA` (Android Jetpack Compose + Material Design 3, Kotlin)
- **Package / Application ID**: `com.aistudio.addavoice.qkrmzx`
- **Default Language**: Hindi (`hi`) with instant in-app switching to English (`en`)
- **Admin ID**: `AKKI_ADMIN` (Server-verified via `/admins/{uid}` in Firestore)

## 2. Firebase Setup Guide
1. The project is already provisioned with `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` (`firestore_database_id`).
2. `firestore.rules` enforces Zero-Trust schema validation, PII isolation (`/users/{uid}/private/account`), and anti-fraud wallet balance protection.
3. Optional Firebase services to enable manually in Firebase Console if desired:
   - **Firebase Cloud Storage**: For uploading custom user/room avatar JPEGs from Photo Picker URIs.
   - **Firebase Cloud Messaging (FCM)**: For background push notifications when the app is closed.
   - **Phone Auth**: Enable Phone Sign-In in Firebase Console -> Authentication if you want SMS OTP login alongside Google Sign-In.

## 3. Live Voice SDK Setup Guide (Agora RTC / WebRTC)
- The app currently uses Android's `AudioRecord` + `AudioManager` in `VoiceAudioEngine.kt` for real-time microphone capture, speaking-ring amplitude telemetry, speakerphone routing, and gift/level-up sound FX, synchronized across users via Firestore `/voice_rooms/{roomId}/seats`.
- To enable full cross-device low-latency audio streaming via **Agora RTC SDK**:
  1. Create an Agora project at `console.agora.io` and obtain your `AGORA_APP_ID`.
  2. Add `AGORA_APP_ID=<your-id>` in the **AI Studio Secrets** panel (and `.env.example`).
  3. Add `implementation("io.agora.rtc:full-sdk:4.x.x")` to `app/build.gradle.kts`.
  4. Initialize `RtcEngine.create(context, BuildConfig.AGORA_APP_ID, handler)` inside `VoiceAudioEngine.kt` and call `joinChannel(token, roomId, uid, options)` when a user takes a mic seat.

## 4. Backend Deployment Guide (Cloud Functions)
1. Install Firebase CLI & deploy `backend/functions/index.js`:
   ```bash
   cd backend/functions && npm install firebase-admin firebase-functions
   firebase functions:secrets:set RAZORPAY_KEY_SECRET
   firebase deploy --only functions,firestore:rules
   ```

## 5. Admin Setup Guide (`AKKI_ADMIN`)
- Admin access is **never** unlocked via hardcoded passwords in the APK.
- Admin privileges are enforced server-side in `firestore.rules` (`isAdmin()` checks `/admins/{uid}` and verified admin email).
- Authorized admins see the **AKKI_ADMIN Panel** icon in the Home header and Wallet screen to verify INR recharge orders, ban/verify users, adjust gift prices, publish announcements, create events, and resolve reports.

## 6. Indian Payment Integration Guide (UPI & Razorpay)
- Recharge packages (`₹10` to `₹4,999`) launch standard `upi://pay` intents and create `PENDING` documents in `/recharge_orders/{orderId}`.
- Coins are **never** credited client-side on payment tap. They are credited strictly after verification by the Cloud Function (`verifyPaymentAndCreditCoins`) or `AKKI_ADMIN` verification in the Admin Dashboard.
- For production Razorpay Checkout, add your public `RAZORPAY_KEY_ID` to **AI Studio Secrets** and keep `RAZORPAY_KEY_SECRET` strictly in Firebase Functions Secrets.

## 7. Testing & APK Build Instructions
- **Security Rules Tests**:
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- **Unit / Robolectric Tests**:
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Build Debug & Release APK**:
  ```bash
  gradle :app:assembleDebug
  ```
  (Or export APK / AAB directly from the AI Studio Settings menu.)
