# Tasks: Fix system insets (fix-system-insets)

## Review Workload Forecast

This is ~100 lines, Low budget risk, single PR.

## Phase 1: Tests

- [x] 1.1 Written and then removed — trivial unit tests (assertNotNull on strings) are meaningless. No Robolectric UI tests for insets (Robolectric reports 0 insets). Real validation requires instrumentation tests on device/emulator.

## Phase 2: Fix shared component

- [x] 2.1 Fix `OptoTopAppBar.kt` — replaced `WindowInsets(0,0,0,0)` with `TopAppBarDefaults.windowInsets`, removed unused WindowInsets import

## Phase 3: Fix Scaffold screens

- [x] 3.1 Fix `ConfiguracionScreen.kt` — removed `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from Scaffold
- [x] 3.2 Fix `AgendaScreen.kt` — removed `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from Scaffold

## Phase 4: Fix non-Scaffold screens

- [x] 4.1 Added `statusBarsPadding()` to `LoginScreen.kt` (root Box modifier chain)
- [x] 4.2 Added `statusBarsPadding()` to `PinScreen.kt` (root Box modifier chain)
- [x] 4.3 Added `statusBarsPadding()` to `CreatePinScreen.kt` (root Box modifier chain)
- [x] 4.4 Skipped — `RecoveryScreen.kt` uses Scaffold, fixed by root cause (Phase 2)
- [x] 4.5 Skipped — `NewPasswordScreen.kt` uses Scaffold, fixed by root cause (Phase 2)

## Phase 5: Verification

- [x] 5.1 Run `./gradlew :optoapp:compileDebugKotlin` — ✅ PASSED
- [x] 5.2 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — ✅ BUILD SUCCESSFUL
