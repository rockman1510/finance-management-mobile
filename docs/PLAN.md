# Plan – Finance Manage mobile (KMP)

Source of truth for scope: `docs/Claude.md` (blueprint F01–F22) and `docs/MOBILE_API_GUIDE.en.md` (API §1–§14).

## Architecture

```
composeApp ──► feature/* ──► core/ui ─────┐
    │              │                      ▼
    │              ├──────► core/data ─► core/network ─► core/model
    └──────────────┴──────────────────────────────────────────▲
core/testing (tests only) ─► core/data, core/network, core/model
```

- **Clean architecture inside each feature**: `presentation` (Compose screen + ViewModel, immutable state) →
  `domain` (models, repository interfaces, use cases) → `data` (Ktor API, DTOs, repository implementation, Room cache).
- **Offline-first, single source of truth**: screens observe Room; repositories fetch from the API and write to Room;
  writes that support it (transactions, transfers) go through an outbox with their Idempotency-Key (F21).
- **Navigation**: one Activity (Android) / one `UIViewController` (iOS) hosting a Compose `NavHost` with type-safe
  routes. Signed in: bottom bar Home · Transactions · ➕ · Plan · Assets.
- **DI**: Koin modules per core module and per feature, assembled in `composeApp/di/AppModules.kt`.
- **Platform specifics** (`expect`/`actual` or platform Koin bindings): HTTP engine (OkHttp/Darwin), secure token store
  (KVault: EncryptedSharedPreferences/Keychain), file paths for Room/DataStore, network monitor
  (ConnectivityManager/NWPathMonitor), later biometrics, camera, push.

## Roadmap

Legend: ✅ done (built + tested) · 🔬 built and unit-tested on CI, try-out against the real backend pending ·
🧪 built, compiles on CI, tests pending · ⏳ in progress · ⬜ not started.

| Phase | Feature | API | Status |
|---|---|---|---|
| 0 | Project scaffold: Gradle (version catalog, convention plugins), modules, CI, lint | – | ✅ Done |
| 0 | **F01** App foundation: HttpClient + auth refresh, ErrorMapper, Idempotency-Key, PendingPoller, TokenStore, NetworkMonitor, MoneySerializer/Formatter, design system, NavHost with 5 tabs, light/dark theme | §1–§3 | ✅ Done (see notes) |
| 1 | **F02** Sign up, sign in, 2FA, Google (Android), forgot/reset password | §4.1 | 🔬 Unit-tested, backend try-out pending |
| 1 | **F03** Onboarding (currency, first wallet, budget, optional 2FA) | §4.2, §5.1, §7 | 🔬 Unit-tested, backend try-out pending |
| 1 | **F04** Profile, settings, security (2FA, sessions, app lock, delete account) | §4.2 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F05** Wallets | §5.1 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F06** Categories (+ shared picker) | §5.2 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F07** Income/expense transactions (quick add, list, filter, detail) | §5.3 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F08** Transfers | §5.4 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F09** Recurring transactions | §5.5 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F10** Receipt photos | §5.6 | 🔬 Unit-tested, backend try-out pending |
| 2 | **F11** Currencies and exchange rates | §6 | 🔬 Unit-tested, backend try-out pending |
| 3 | **F12** Home | §5.1, §7–§9, §13, §14 | 🔬 Unit-tested, backend try-out pending |
| 3 | **F13** Budgets | §7 | 🔬 Unit-tested, backend try-out pending |
| 3 | **F14** Recurring bills | §8 | 🔬 Unit-tested, backend try-out pending |
| 3 | **F15** Saving goals | §9 | 🔬 Unit-tested, backend try-out pending |
| 4 | **F16** Bank deposits | §10 | 🔬 Unit-tested, backend try-out pending |
| 4 | **F17** Loans | §11 | 🔬 Unit-tested, backend try-out pending |
| 4 | **F18** Investments | §12 | 🔬 Unit-tested, backend try-out pending |
| 4 | **F19** Reports and net worth | §13 | 🔬 Unit-tested, backend try-out pending |
| 5 | **F20** Notifications and push (FCM) | §14 | 🔬 Unit-tested, backend try-out pending |
| 5 | **F21** Offline and sync (outbox) | §2 | 🔬 Unit-tested, backend try-out pending |
| 6 | **F22** Polish and release | – | ⏳ Release guide in `docs/RELEASE.md`; polish pass pending |
| 7 | Instrumentation / UI tests (Android `connectedAndroidTest`, iOS XCUITest) | – | ⬜ |

Each feature ends with: unit tests for its ViewModels/repositories, an Android + iOS build, and a try-out against the
real backend.

## F01 notes

Done:
- `core/model`: `Money` (bignum), `CurrencyCode` (minor units), `MoneySerializer` (raw JSON number → exact decimal),
  `MoneyFormatter` (`vi-VN`), `AppError`/`AppResult`, `AsyncStatus` + `FallbackEnumSerializer`, date helpers.
- `core/network`: `createHttpClient` (bearer auth, no token on `auth/*`, single-use refresh with sign-out on
  rejection, no refresh loop on `/auth/login` 401), `apiCall` + `ErrorMapper`, `IdempotencyKey(Holder)`,
  `PendingPoller`, Koin `networkModule`.
- `core/data`: `SecureTokenStore`, `AppSettingsRepository` (DataStore), `FinanceDatabase` (Room KMP, bundled SQLite),
  `NetworkMonitor` (Android/iOS), Koin modules.
- `core/ui`: theme (light/dark + finance colours), `MoneyText`, `AmountInput` + `AmountKeypad`, empty/error/skeleton
  states, offline banner, confirm dialog, error → message mapping, Vietnamese + English strings.
- `composeApp`: Android `FinanceApplication` + single `MainActivity`; iOS `MainViewController`; NavHost with Home,
  Transactions, ➕ sheet (Expense/Income/Transfer), Plan (Budgets/Bills/Savings tabs), Assets; placeholders for the
  features to come.
- Tests: MoneySerializer, MoneyFormatter, Money, AsyncStatus, dates, ErrorMapper (400 + fieldErrors, empty 401, 422,
  429 + Retry-After, network), auth refresh (single use, concurrent requests share one refresh, rejected refresh →
  signed out, no loop, login 401 does not refresh), Idempotency-Key, PendingPoller, AmountInput, AppViewModel.

Left for later features:
- `CategoryIcon`, `ProgressBar`, `DateRangePicker`, charts (Vico) → with F06/F13/F19.
- Production base URL: set `financemanage.apiBaseUrl.release` (Android) and `PRODUCTION_API_BASE_URL` (iOS).
- App icon artwork (placeholder vector today) → F22.

## F02–F21 notes (build-out)

All features are implemented following `docs/Claude.md`; tests are written after the build-out (task order agreed with
the product owner). Highlights:
- Feature modules: auth, onboarding, settings, wallet, category, transaction (incl. transfers, recurring, receipts,
  outbox screen), currency, home, budget, bill, saving, deposit, loan, investment, report, notification.
- Offline (F21): JSON cache in Room for every list (shown at once, "Updated at HH:mm"), outbox for transactions and
  transfers with their Idempotency-Key, offline banner, network-only actions disabled offline (`LocalIsOnline`).
- Push (F20): `PushBridge` / `PushRegistrar` in `core/data`, FCM service on Android, Swift hooks on iOS; Firebase is
  configured only when `google-services.json` / `GoogleService-Info.plist` are present (see `docs/RELEASE.md`).

## Tests (task after the build-out)

Every feature has `commonTest` tests for its ViewModels, run against the real repositories, `HttpClient` and JSON over
a fake backend (run on the JVM by `testDebugUnitTest` and on the iOS simulator by `iosSimulatorArm64Test`):
- `core/testing`: `MockBackend` (routes per method + path, request log, `offline()`), `FakeJsonCacheDao`,
  `FakeOutboxDao`, `testJsonCache()`, `testSessionManager()`, `runViewModelTest` (Main + mocked HTTP on the test
  dispatcher), `awaitValue`, `tracked()` (clears ViewModels that listen to `PushBridge`), body helpers.
- `core/data`: JSON cache, `ResourceHolder`, outbox sync (order, network vs business errors, retry with the same key),
  session sign-in/out/wipe, settings, push links and device registration.
- Features: validation, request bodies (amount decimals, omitted nulls), Idempotency-Key reuse on retry and renewal
  after an edit, offline queueing, PENDING polling (recorded / failed / timed out), error codes, caching.

Bugs found and fixed while writing the tests:
- A 401 from `/auth/*` was sent twice by Ktor's bearer plugin: each wrong password / 2FA code counted twice.
- Onboarding's monthly budget left out `period` and `repeat` (defaults are not encoded) → rejected by the backend.
- `2.000` typed for a USD amount was read as 2 instead of 2000.
- Home fetched the summary in VND on a first launch for non-VND users; Reports could show results of an older filter;
  a loan rate change without a date left the screen busy; delete account now requires the matching confirmation.
