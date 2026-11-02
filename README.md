# Finance Manage – mobile

Personal finance app for **Android and iOS**, built with Kotlin Multiplatform and Compose Multiplatform, connected to
the Finance Manage backend.

- Architecture, conventions and commands: [CLAUDE.md](CLAUDE.md)
- Roadmap and feature status: [docs/PLAN.md](docs/PLAN.md)
- Feature blueprint (F01–F22): [docs/Claude.md](docs/Claude.md)
- Backend API: [docs/MOBILE_API_GUIDE.en.md](docs/MOBILE_API_GUIDE.en.md), [docs/api/openapi.json](docs/api/openapi.json)

## Requirements

- JDK 17+, Android Studio (Ladybug or newer) with Android SDK 35
- macOS + Xcode 16 for iOS
- The backend running locally (see API guide §1)

## Run

```bash
./gradlew :composeApp:installDebug   # Android emulator (backend at http://10.0.2.2:8080)
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode, set your team in `iosApp/Configuration/Config.xcconfig` if you run on a
device, and run the `iosApp` scheme (backend at `http://localhost:8080`).

## Test

```bash
./gradlew testDebugUnitTest ktlintCheck detekt
```
