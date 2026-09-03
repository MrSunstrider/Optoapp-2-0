# Tasks: fix-dashboard-fab-nav-inset

## WU-1 — RED characterization test

- [x] Add test asserting `OperacionHoyScreen.kt` source: within `floatingActionButton` block, `navigationBarsPadding` is present.
- [x] Run focused test → expect FAIL before code change.

## WU-2 — GREEN apply FAB padding

- [x] On `OperacionHoyScreen` FAB `Column`, set `modifier = Modifier.navigationBarsPadding()`.
- [x] Re-run focused test → PASS.

## WU-3 — Verify

- [x] `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.ui.screens.OperacionHoyScreenTest`
- [x] `./gradlew :optoapp:assembleDebug`
- [x] ADB: deferred — debug APK signature incompatible with installed release; documented in verify-report.

## WU-4 — Archive

- [x] Merge delta into `openspec/specs/system-insets/spec.md`.
- [x] Write `verify-report.md` + `archive-report.md`.
- [x] Move change to `openspec/changes/archive/2026-09-03-fix-dashboard-fab-nav-inset/`.
