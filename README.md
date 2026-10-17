# عمرو وجودي – Amr & Judy Kids App

Kotlin + Jetpack Compose Android app (offline-first, no ads, no subscriptions).

## Get the APK (no Android Studio needed)
1. Push this folder to a GitHub repository (branch `main`).
2. Open the repo → **Actions** → **Build Android APK** → open the latest run.
3. Download the **AmrJudyKids-apk** artifact, unzip it, and install `app-debug.apk` on the phone
   (allow "install unknown apps" when asked).

## Build locally
Open the folder in Android Studio and press Run, or run `gradle assembleDebug`.

## Notes
- Default parent PIN: `1234` (change it in the parent area).
- Speech recognition uses the phone's Arabic (ar-EG) engine; offline works if the Arabic offline pack is installed
  in Google app settings. Arabic text-to-speech needs an Arabic voice installed on the phone.
- Auto-pause after 8 minutes of play.
