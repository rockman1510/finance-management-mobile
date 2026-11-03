# Release guide – Finance Manage mobile

Covers F20 (push setup) and F22 (release). Secrets never go into git: `google-services.json`,
`GoogleService-Info.plist`, keystores and `.p8` keys are listed in `.gitignore`.

## 1. Backend URL

| Build | Android | iOS |
|---|---|---|
| Debug | `http://10.0.2.2:8080` (emulator → host) | `http://localhost:8080` (simulator) |
| Release | Gradle property `financemanage.apiBaseUrl.release` | `PRODUCTION_API_BASE_URL` in `composeApp/.../PlatformConfig.kt` |

Release builds must use **HTTPS**: cleartext traffic is disabled for Android release builds, and iOS only allows local
networking over HTTP.

## 2. Push notifications (FCM, API §14.1)

The app builds and runs without Firebase; push is then simply off.

### Firebase project
1. Create a Firebase project; add the Android app `com.financemanage.mobile` and the iOS app (same bundle id as the
   Xcode target).
2. iOS: upload an **APNs Authentication Key** (.p8) under Project settings → Cloud Messaging.
3. Backend: `FCM_ENABLED=true` + the service-account key file (see the backend's `docs/DEPLOYMENT.md`).

### Android
1. Download `google-services.json` into `composeApp/` (ignored by git). The Google Services Gradle plugin is applied
   automatically when the file exists.
2. CI: store the file as a base64 secret and decode it before building (see §4).

What the app does: creates the `default` notification channel, fetches the FCM token, registers it after sign-in
(`POST /notifications/devices`, provider `FCM`, platform `android`), re-registers on `onNewToken`, unregisters on
sign-out. Pushes received in the foreground are shown by the app and refresh the list/badge; tapping a push opens
the screen of `referenceType`/`referenceId`. The notification permission (Android 13+) is requested after the first
budget or bill is created, and from the notification list/settings.

### iOS
1. In Xcode: File → Add Package Dependencies → `https://github.com/firebase/firebase-ios-sdk`, product
   **FirebaseMessaging**.
2. Add `GoogleService-Info.plist` to the `iosApp` target (ignored by git).
3. Signing & Capabilities: add **Push Notifications** and **Background Modes → Remote notifications**
   (`UIBackgroundModes` is already in `Info.plist`).
4. In `iosApp/iosApp/iOSApp.swift`, add `import FirebaseCore` / `import FirebaseMessaging` and uncomment the lines
   marked for Firebase (`FirebaseApp.configure()`, the messaging delegate, `apnsToken`). The token reaches the shared
   code through `IosPush.shared.onNewToken(token:)`.

### Deep links
`financemanage://<referenceType>/<id>` (`bill`, `budget`, `saving_goal`, `deposit`, `loan`) and
`financemanage://quick-add` open the matching screen on both platforms. Try them with:

```bash
adb shell am start -a android.intent.action.VIEW -d "financemanage://bill/<id>"
xcrun simctl openurl booted "financemanage://bill/<id>"
```

## 3. Signing

### Android (AAB)
1. Create an upload keystore (keep it outside the repository):
   `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. Add a `signingConfigs.release` reading the path/passwords from Gradle properties or environment variables, and use
   it in `buildTypes.release`.
3. `./gradlew :composeApp:bundleRelease` → `composeApp/build/outputs/bundle/release/composeApp-release.aab`.
4. Upload to Play Console → Internal testing.

### iOS
1. Set the team and bundle id in the `iosApp` target; automatic signing is fine for TestFlight.
2. Product → Archive → Distribute App → App Store Connect → TestFlight.

## 4. CI (suggested release jobs)

- Android: decode `GOOGLE_SERVICES_JSON` and the keystore from secrets, run `bundleRelease`, upload with
  `r0adkll/upload-google-play` to the internal track.
- iOS: decode `GoogleService-Info.plist`, use an App Store Connect API key with `xcodebuild archive` /
  `xcodebuild -exportArchive`, upload with `xcrun altool` or fastlane `pilot`.

## 5. Release checklist (docs/Claude.md F22)

- [ ] Every feature F01–F21 is "done" in `docs/PLAN.md`; CI green on Android + iOS.
- [ ] No `Double`/`Float` for money; `vi-VN` formatting correct.
- [ ] Expired / revoked refresh token → Sign in, no loops.
- [ ] Flaky network creates no duplicate transactions (Idempotency-Key).
- [ ] PENDING actions show the right state and update themselves.
- [ ] Push arrives on real devices, opens the right screen, stops after sign-out.
- [ ] No secrets in git; every production request uses HTTPS.
- [ ] No Google button on iOS (or Sign in with Apple is offered); in-app account deletion exists.
- [ ] Privacy policy URL in both stores; permission texts (camera, photos, notifications, Face ID) reviewed.
