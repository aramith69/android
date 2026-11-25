# Troy's File Renamer for Android

Renames TV episode video files to `Show Name - S01E04.mkv`, with a preview before anything changes.
Same naming rules as the Windows version. Native Kotlin + Jetpack Compose.

**Status: written but not yet compiled or run.** It was written without access to an Android SDK, so expect to
fix a small build error or two on the first run. The naming rules were checked against the desktop app's results;
the Android UI and file access have not been tested on a device.

## Get an APK without installing anything (GitHub)
1. Create a free GitHub account and a new repository.
2. Upload everything in this folder (keep the `.github` folder).
3. Open the repository's **Actions** tab. The "Build Android APK" workflow runs by itself.
   It runs the unit tests, then builds the app.
4. When it finishes, open the run and download **TroysFileRenamer-debug-apk** at the bottom. Unzip it to get the `.apk`.
5. If the run fails, open the failed step, copy the red error text and send it to me. First-build errors are quick to fix.

## Install on your phone
1. Copy the `.apk` to your phone.
2. Open it. Android will ask you to allow installs from that app (for example Files or Chrome). Allow it for this one install.
3. Open **Troy's File Renamer**.

## Use it
1. Tap **+ Add folder** and choose the folder or drive that holds your shows (internal storage, SD card, USB drive).
   Android only lets an app change files in folders you give it access to.
2. Tap **SCAN**.
3. Untick any files you don't want changed, then tap **PREVIEW & RENAME**, check the list, and confirm.

Some cloud storage apps in the picker (for example Google Drive) don't allow renaming; those files show an error.

## Build in Android Studio instead
Open this folder in Android Studio (Ladybug or newer), let it sync, then **Run**. Or from a terminal with JDK 17 and
the Android SDK: `gradle assembleDebug` (the APK ends up in `app/build/outputs/apk/debug/`).

## Putting it on Google Play
- A debug APK is only for your own devices. Play needs a **signed release bundle (.aab)**.
- Create a signing key once (keep it safe, you cannot replace it later):
  `keytool -genkeypair -v -keystore release.jks -alias troy -keyalg RSA -keysize 2048 -validity 10000`
- In GitHub: Settings > Secrets > Actions, add `KEYSTORE_BASE64` (the .jks file encoded with `base64`),
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Then Actions > Build Android APK > **Run workflow**; the
  `release` job produces the `.aab`.
- Play Console needs a developer account (a one-time registration fee applies; check Google's current terms),
  the 512 px icon in `store/play_store_icon_512.png`, screenshots from your phone, a privacy policy URL
  (`store/PRIVACY_POLICY.html`, fill in your name and email, then host it), and the Data safety form
  (this app collects and shares no data).
- Google requires new apps to target a recent Android API level, and that requirement moves every year. This project
  targets API 35. Check Play Console's current requirement before you submit and raise `targetSdk` and
  `compileSdk` in `app/build.gradle.kts` if needed.
- New personal developer accounts may have to run a closed test with real testers before they can publish.
  Check the current rules in Play Console.

## Project layout
- `app/src/main/java/.../Naming.kt` season/episode and show-name rules (ported from the desktop app)
- `SafFiles.kt` folder scanning and renaming through Android's folder picker (Storage Access Framework)
- `MainViewModel.kt` app state, scanning and renaming
- `MainActivity.kt` screens (Renamer, Preview dialog, About)
- `app/src/test/.../NamingTest.kt` unit tests for the naming rules
