# CLAUDE.md – Finance Manage mobile (KMP)

Personal finance app for **Android + iOS**, built with Kotlin Multiplatform + Compose Multiplatform, talking to the
Finance Manage REST backend. UI language: **Vietnamese** (default resources) with English prepared.

## Read first

- `docs/Claude.md` – master prompt + blueprint of every feature (F01–F22), architecture and mandatory conventions.
- `docs/MOBILE_API_GUIDE.en.md` – how to use the API, business rules, error codes ("API §n" = section n).
- `docs/api/openapi.json` – exact request/response types. Never guess the API: look it up.
- `docs/PLAN.md` – architecture overview and the phased roadmap with the status of each feature.

## Commands

```bash
./gradlew :composeApp:assembleDebug          # Android debug APK
./gradlew :composeApp:installDebug           # install on a running emulator/device
./gradlew testDebugUnitTest                  # shared + Android unit tests (runs commonTest on the JVM)
./gradlew iosSimulatorArm64Test              # shared tests on the iOS simulator (macOS only)
./gradlew allTests                           # every KMP test task available on this machine
./gradlew ktlintCheck detekt                 # lint (ktlintFormat to auto-fix)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

Run tests and lint before reporting work as done. CI (`.github/workflows/ci.yml`) runs the Android build + unit
tests, ktlint + detekt, and the iOS simulator tests + Xcode build.

Backend for manual testing (API §1): Android emulator → `http://10.0.2.2:8080`, iOS simulator → `http://localhost:8080`.
In the backend's `dev` profile, `GET /api/v1/dev/mailbox?to=<email>` returns e-mail codes.

## Module layout

| Module | Role |
|---|---|
| `composeApp` | Android/iOS entry point (single `MainActivity`, `MainViewController`), NavHost, theme, Koin start-up |
| `core/model` | `Money`, `CurrencyCode`, `AppError`, `AppResult`, `AsyncStatus`, `PollResult`, `IdempotencyKey(Holder)`, `MoneySerializer`, `MoneyFormatter` |
| `core/domain` | Shared domain: models (`Cached`, `UserPreferences`, `SessionState`, `PendingWrite`, `NotificationLink`), repository interfaces (profile, wallets, categories, exchange rates, settings, session, outbox, push, `NetworkMonitor`) and their use cases. Pure Kotlin |
| `core/network` | `HttpClient` (auth + single-use refresh), `apiCall`/`ErrorMapper`, `Idempotency-Key` header, `PendingPoller` |
| `core/data` | Implementations of the `core/domain` repositories (`…RepositoryImpl`), Room `FinanceDatabase`, DataStore settings, `SecureTokenStore` (Keystore/Keychain), `SessionManager`, outbox, push bridge; Koin `dataModule` + `domainModule` (shared use cases) |
| `core/ui` | Design system: `FinanceTheme`, `MoneyText`, `AmountKeypad`/`AmountInput`, `EmptyState`, `ErrorState`, skeletons, `ConfirmDialog`, `OfflineBanner`, `AppError.userMessage()`; `Resource`/`ResourceHolder` (ViewModel helpers); shared string resources |
| `core/testing` | Fakes (`FakeTokenStore`, `FakeNetworkMonitor`, `InMemoryPreferencesDataStore`), `mockHttpClient`, `SampleJson` |
| `feature/*` | One module per feature (`financemanage.kmp.feature` plugin) with `data` / `domain` (`model`, `repository`, `usecase`) / `presentation` / `di` packages |
| `iosApp` | Xcode project hosting the `ComposeApp` framework |
| `build-logic` | Convention plugins: `financemanage.kmp.library`, `.kmp.compose`, `.kmp.feature` |

Rules:
- **Call chain: UI → ViewModel → UseCase → Repository.** Composables only talk to their ViewModel. A ViewModel's
  constructor takes **use cases only** – never a repository, DAO, `HttpClient`, `SessionManager` or `PushBridge`.
  Use cases call repository **interfaces**; implementations live in `data`.
- Clean architecture per feature:
  - `domain/model` (models, inputs), `domain/repository` (`XxxRepository` interface), `domain/usecase` (one class
    per action, `operator fun invoke`; business rules combining repositories go here). Domain code never imports
    `data`, `core.data`, `core.network`, Ktor or Room.
  - `data`: DTOs, mappers, `XxxRepositoryImpl : XxxRepository`, cache.
  - `presentation`: immutable `UiState`, `Event`, ViewModel, Screen.
  - `di`: `single<XxxRepository> { XxxRepositoryImpl(…) }`, `factoryOf(::XxxUseCase)`, `viewModelOf(::XxxViewModel)`.
- Shared data (profile, wallets, categories, rates, settings, session, outbox, push) is reached through the
  `core/domain` use cases (`ObserveWalletsUseCase`, `GetTodayUseCase`, `GetDefaultCurrencyUseCase`…).
- Keep ViewModel constructors under detekt's limit (≤ 9 parameters): combine use cases into a domain use case when
  that is a real business operation, or inject a `XxxUseCases` bundle (see `HomeUseCases`).
- **Features never depend on each other.** Shared things go to `core/*`. Cross-feature navigation goes through
  `composeApp`.
- Room entities/DAOs live in `core/data` (`FinanceDatabase` needs them all at compile time); bump the DB version when
  the schema changes (cache only – destructive migration is allowed).
- Offline-first: the UI observes Room (single source of truth); repositories refresh from the network in the
  background and write to Room.
- No API calls inside composables. Screens get state from a ViewModel (`koinViewModel()`), collected with
  `collectAsStateWithLifecycle()`.
- Every screen has loading (skeleton), empty (illustration + action) and error (retry) states; pull to refresh;
  refresh when the app returns to the foreground.

## Mandatory conventions

- **Money**: `com.ionspin.kotlin.bignum.decimal.BigDecimal` wrapped in `Money(amount, currency)`. Never `Double`/`Float`.
  DTO fields: `@Serializable(with = MoneySerializer::class) val amount: BigDecimal`. Send amounts with the currency's
  decimals (`Money.rounded()` / `toPlainString()`; VND 0, USD 2) – more decimals → `422 INVALID_AMOUNT`. Display with
  `MoneyText` / `MoneyFormatter` (`1.250.000 ₫`, `12,50 US$`); negative red, positive green (`colorBySign = true`).
- **Dates**: `kotlinx.datetime.LocalDate` (`yyyy-MM-dd` on the wire). "Today" = `today(profile.timeZone)`; display
  `dd/MM/yyyy` (`toDisplayString()`), grouped "Hôm nay" / "Hôm qua".
- **JSON**: always `NetworkJson` (`ignoreUnknownKeys`, `explicitNulls = false`, `coerceInputValues`). Enums use
  `FallbackEnumSerializer` with an `UNKNOWN` value. `PATCH` bodies: `null` fields are omitted = unchanged.
- **HTTP calls**: relative paths via `api("wallets")`; wrap every call in `apiCall { … }` → `AppResult<T>`. Never catch
  `CancellationException`.
- **Auth**: access token 15 min, refresh token **single use**. Refresh is handled by the `HttpClient`; a rejected
  refresh clears tokens and emits `SessionEvent.SignedOut` → navigate to Sign in. After sign-in/sign-out, call
  `HttpClient.resetCachedTokens()`.
- **Errors**: `AppError.Api(status, code, serverMessage, fieldErrors)`. The UI shows text based on `code` (feature
  strings first, then `AppError.userMessage()`), puts `fieldErrors` under each input; `409` → reload and retry;
  `429` → show the waiting time; offline → banner.
- **Idempotency-Key**: `IdempotencyKeyHolder` per form – `current()` when Save is tapped, reused for every retry,
  `reset()` after success. Required for `POST /transactions`, `/transfers`, `/loans/{id}/repayments`,
  `/loans/{id}/prepayments`.
- **PENDING** (API §2.1): after `202`/`PENDING` show "Đang xử lý…", then `PendingPoller.poll` (1.5 s, up to 10 s);
  `RECORDED` → success + refresh wallets; `FAILED` → show `failureReason`; timeout → "Sẽ cập nhật trong giây lát".
- **Strings**: every user-facing string in `composeResources/values/strings.xml` (Vietnamese) **and**
  `values-en/strings.xml` (English). Shared strings live in `core/ui` (`com.financemanage.mobile.core.ui.resources.Res`).
- **Accessibility**: dark mode, dynamic font size, `contentDescription` on icons that carry meaning.
- **Secrets**: never commit `google-services.json`, `GoogleService-Info.plist`, keystores or `.p8` keys.
- **Style**: ktlint official style (`.editorconfig`), detekt (`config/detekt/detekt.yml`), max line 120.
- **Tests**: `commonTest` with `kotlin.test` + `kotlinx-coroutines-test`; network code with `mockHttpClient` (Ktor
  `MockEngine`); ViewModels with `Dispatchers.setMain(UnconfinedTestDispatcher())`, built from real use cases over
  `…RepositoryImpl`s on a `MockBackend`.

## Adding a feature module

1. `settings.gradle.kts`: `include(":feature:wallet")`.
2. `feature/wallet/build.gradle.kts`: `plugins { alias(libs.plugins.financemanage.kmp.feature) }`.
3. Packages `com.financemanage.mobile.feature.wallet.{data,domain.model,domain.repository,domain.usecase,presentation}`
   + a Koin `walletModule` (repository binding, use cases, ViewModels).
4. Register the Koin module in `composeApp/.../di/AppModules.kt` and the routes in `navigation/MainScaffold.kt`.
5. Strings in `feature/wallet/src/commonMain/composeResources/values{,-en}/strings.xml`.
6. Update the feature's status in `docs/PLAN.md`.
