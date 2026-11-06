# 🛡️ SecureLock - Modern Android App Lock

A fully functional, battery-optimized, and secure App Lock application built from scratch for Android 10+ using **Kotlin**, **Jetpack Compose**, and **Material 3**.

---

## ✨ Features

- **🔐 Multi-Option Authentication**:
  - Secure 4-to-6 digit numeric PIN with encrypted verification.
  - Native Biometric integration (Fingerprint / Face Unlock).
- **📱 Installed Apps Dashboard**:
  - Clean Jetpack Compose UI that scans all installed user applications.
  - One-tap switch toggles to instantly protect or unprotect target apps.
- **⚡ Zero-Polling App Detection**:
  - Powered by Android's `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`).
  - Event-driven foreground detection with **0% background CPU drain**.
- **🪟 Instant Lock Screen Overlay**:
  - Directly rendered using `WindowManager` (`TYPE_APPLICATION_OVERLAY`).
  - Seamlessly covers locked apps without launch lag or Android 10+ background restriction errors.
  - Uses `FLAG_SECURE` to block unauthorized screenshots or screen recordings.
- **📸 Silent Intruder Catcher**:
  - Uses CameraX to silently snap a front-camera photo when an incorrect PIN is entered 2 or more times.
  - Saves evidence locally to private storage and logs metadata in a **Room Database**.

---

## 🛠️ Architecture & Tech Stack

- **UI**: Jetpack Compose & Material Design 3
- **Language**: Kotlin 1.9+
- **Security & Storage**: 
  - `EncryptedSharedPreferences` (AES-256-GCM / AES-256-SIV)
  - AndroidX Biometric API
  - Room SQLite Database for intruder logs
- **Camera**: Jetpack CameraX (Headless `ImageCapture`)
- **System Services**: `AccessibilityService`, `WindowManager`
- **CI/CD**: GitHub Actions cloud build pipeline

---

## 🚀 Automated Cloud Build (GitHub Actions)

This repository includes a continuous integration workflow (`.github/workflows/build.yml`) that builds the debug APK automatically in the cloud:

1. Push your changes to `main`.
2. Go to the **Actions** tab on GitHub.
3. Select the latest run of **Build Android Debug APK**.
4. Scroll to **Artifacts** at the bottom and download `SecureLock-Debug-APK.zip`.
5. Extract and install the `app-debug.apk` directly on your Android phone.

---

## 📱 Device Setup & Permissions

To allow SecureLock to function properly on modern Android (Android 10 - Android 14+), grant the following permissions on first run:

1. **Draw Over Other Apps (Overlay)**:
   - When prompted, toggle **Allow display over other apps** to **ON**.
2. **Accessibility Service (Foreground Detection)**:
   - Go to **Settings ➔ Accessibility ➔ Downloaded Apps / Installed Services ➔ SecureLock**.
   - Turn the service **ON**.
   *(Note for Android 13+: If the button is greyed out as a "Restricted Setting", open Settings ➔ Apps ➔ SecureLock ➔ tap the 3 dots in the top-right corner ➔ select **Allow restricted settings**).*
3. **Camera Permission**:
   - Tap **Allow** when the app requests camera access to enable the Intruder Catcher.

---

## 📄 License
This project is open-source and intended for personal and educational security research.
