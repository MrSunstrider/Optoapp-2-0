# Apply Progress — Virtual Try-On Phase 3 (UI & Integration)

**Date**: 2026-05-29
**Mode**: Strict TDD (hybrid persistence)
**PR**: 3/4 (feature-branch-chain → PR 2 branch)

## Completed Tasks — Phase 3

| Task | Status | Notes |
|------|--------|-------|
| 3.1 | ✅ Done | `VirtualTryOnViewModel.kt` — `@HiltViewModel` with `TryOnState` sealed class (6 states), `FaceMeasurements` data class, 10 methods including `loadPatientPD`, `selectPhoto`, `selectMontura`, `adjustOverlay`, `saveComposite`, `shareComposite`, `saveMeasurementsToEvaluacion`, `loadMonturas`, `resetState`, `setSegmentType`. Bitmap decoding with max 2048px sampling, MediaStore save for Q+, FileProvider fallback for pre-Q. |
| 3.2 | ✅ Done | `VirtualTryOnScreen.kt` — Compose host screen with Scaffold + TopAppBar "Prueba Virtual", state-dependent rendering for all 6 TryOnState variants. Permission handling (READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE), Gallery picker via `ActivityResultContracts.GetContent()`, camera stub with snackbar. Montura list loading via `TryOnEntryPoint` Hilt accessor. |
| 3.3 | ✅ Done | `PhotoPickerSection.kt` — Two buttons (Galería + Cámara) with Material 3 OutlinedButton, icons, Spanish labels. Camera shows "Próximamente" snackbar for MVP. |
| 3.4 | ✅ Done | `MonturaSelectorBottomSheet.kt` — ModalBottomSheet with search, tipoAro filter chips, LazyColumn of monturas with thumbnail + metadata display. Takes `List<Montura>` + loading state as parameters (Hilt-agnostic design). |
| 3.5 | ✅ Done | `FacePreviewCanvas.kt` — Composable rendering different content per state: PhotoSelected (image + "Detectando rostro..."), FaceDetected (text info), OverlayReady (composite image). Key landmark dots rendered via Canvas. |
| 3.6 | ✅ Done | `MeasurementDisplay.kt` — Card with DIP/DNP OD/DNP OI/Altura values, Material 3 typography, PD warning banner with Warning icon. |
| 3.7 | ✅ Done | `ResultScreen.kt` — Composite preview + Save/Share/Save Measurements/Retry buttons. Snackbar feedback handled by parent screen. |
| 3.8 | ✅ Done | Added `virtual_try_on/{pacienteId}` route to `MainDrawerScreen.kt` NavHost (inner navigation, matching existing screen pattern). Null-safe pacienteId argument handling with popBackStack on invalid. |
| 3.9 | ✅ Done | Added "Prueba Virtual" drawer item in `DrawerSections.kt` under PROGRAMACIÓN section, after "Inventario". Uses `Icons.Default.Visibility`. For MVP, navigates to pacientes list (patient selection). |

## Completed Tasks — Phase 4 (Complete)

| Task | Status | Notes |
|------|--------|-------|
| 4.1 | ✅ Done | `VirtualTryOnViewModelTest.kt` — 27 tests: TryOnState types, FaceMeasurements data class, ViewModel method signatures, state StateFlow accessors, EvaluacionClinica PD field contracts, Montura metadata field contracts. |
| 4.2 | ✅ Done | `VirtualTryOnScreenTest.kt` — 22 tests: State-to-UI mapping rules, UI string labels in Spanish, measurement format strings, Montura field contracts for display/filter. |
| 4.3 | ✅ Done | `FrameOverlayUseCaseGoldenTest.kt` — 1 golden image test: generates `standard_overlay.png` golden file, pixel-by-pixel comparison with <1% tolerance. Robolectric-based, synthetic test data (face + montura bitmaps + 468 landmarks). Golden file at `src/test/resources/golden-images/standard_overlay.png`. |

## Completed Tasks — Phase 5 (Complete)

| Task | Status | Notes |
|------|--------|-------|
| 5.1 | ✅ Done | Verified `FileShareUtils.shareFile()` — already uses `FileProvider.getUriForFile()` with `Intent.FLAG_GRANT_READ_URI_PERMISSION` and `Intent.ACTION_SEND`. No changes needed. |
| 5.2 | ✅ Done | Removed 3 unused imports from `VirtualTryOnViewModel.kt`: `withContext`, `LocalDateTime`, `DateTimeFormatter`. Other Phase 3 files were clean. |
| 5.3 | ✅ Done | **KNOWN ISSUE FIX**: Added `originalBitmap: Bitmap` to `TryOnState.FaceDetected` data class. Updated `FacePreviewCanvas.kt` to display the original face bitmap with landmark dots overlaid instead of the text placeholder. Updated `VirtualTryOnViewModelTest.kt` to include `originalBitmap` in test state creation. |

## Files Created (Phase 4+5)

| File | Description |
|------|-------------|
| `optoapp/src/test/java/com/example/optoapp/domain/FrameOverlayUseCaseGoldenTest.kt` | Golden image regression test with synthetic landmarks + pixel comparison |
| `optoapp/src/test/resources/golden-images/standard_overlay.png` | Generated golden reference image (240 KB) |

## Files Modified (Phase 4+5)

| File | Action | What Was Done |
|------|--------|---------------|
| `optoapp/src/main/java/com/example/optoapp/viewmodel/VirtualTryOnViewModel.kt` | Modified | Added `originalBitmap` to `FaceDetected` state (line 75). Passes `bitmap` param in `detectFace()`. Removed 3 unused imports (`withContext`, `LocalDateTime`, `DateTimeFormatter`). |
| `optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/FacePreviewCanvas.kt` | Modified | `FaceDetected` branch now shows `state.originalBitmap` with landmark dots via `FaceImageWithLandmarks` instead of text placeholder. |
| `optoapp/src/test/java/com/example/optoapp/viewmodel/VirtualTryOnViewModelTest.kt` | Modified | Updated `TryOnState FaceDetected` test — added `originalBitmap` parameter, asserts bitmap dimensions. |
| `optoapp/src/test/java/com/example/optoapp/domain/FrameOverlayUseCaseGoldenTest.kt` | Created | Golden image test with `GENERATE_GOLDEN` toggle, synthetic 200x300 face + 120x40 montura bitmaps, 468 landmarks, pixel-by-pixel comparison. |

## TDD Cycle Evidence (Phase 4+5)

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 4.3 | `FrameOverlayUseCaseGoldenTest.kt` | Unit (Robolectric) | N/A (new) | ✅ Written | ✅ Passed | ➖ Single (deterministic) | ✅ Clean |
| 5.1 | — (verification only) | N/A | ✅ 864/864 | N/A | N/A | N/A | ✅ No changes |
| 5.2 | — (dead code removal) | N/A | ✅ 864/864 | N/A | N/A | N/A | ✅ 3 imports removed |
| 5.3 | `VirtualTryOnViewModelTest.kt` | Unit | ✅ 864/864 | ✅ Written (approval) | ✅ Passed | ✅ 1 test updated | ✅ Clean |

## Test Summary

- **Total tests written in Phase 4+5**: 1 (golden image)
- **Total tests updated**: 1 (FaceDetected state test — added `originalBitmap` assertion)
- **All 864 tests passing**: 0 failures, 0 errors across all suites
- **Existing tests preserved**: Yes — safety net confirmed, no regressions
- **Edge cases covered by golden test**: Transparent montura overlay, face landmark positioning, synthesized bitmap composition

## Deviations from Design

- **Task 5.3 scope**: `tasks.md` originally listed "Update IMPROVEMENT-PLAN.md". Implementation follows the orchestrator's task list: fix `FacePreviewCanvas` to show original bitmap in `FaceDetected` state. This was the known issue from Phase 3.
- No other deviations from Phase 4+5 design.

## Issues Found

- **FacePreviewCanvas text placeholder (FIXED)**: `FaceDetected` state showed "Rostro detectado (N landmarks)" text instead of the actual face bitmap. Root cause: `FaceDetected` data class lacked `originalBitmap` field. Fixed by adding `originalBitmap: Bitmap` to the state and updating `detectFace()` to pass the bitmap.
- **Unused imports in ViewModel (FIXED)**: `withContext`, `LocalDateTime`, `DateTimeFormatter` were imported but never used in `VirtualTryOnViewModel.kt`. Probably left over from early development.

## All Phases Complete — Cumulative Summary

### Phase 1 (Foundation) — 9/9 ✅
1.1-1.9: Gradle deps, Room migration (v21→v22), Montura entity fields, MediaPipeModule, manifest permissions, MonturaForm updates

### Phase 2 (Core Domain) — 5/5 ✅
2.1-2.5: FaceLandmarkerUseCase, FrameOverlayUseCase, FaceMeasurementExtractor, plus 38 unit tests

### Phase 3 (UI & Integration) — 9/9 ✅
3.1-3.9: ViewModel, 5 UI components, navigation, drawer item, plus 49 tests

### Phase 4 (Testing) — 3/3 ✅
4.1-4.3: 27 ViewModel tests, 22 UI tests, 1 golden image test

### Phase 5 (Cleanup) — 3/3 ✅
5.1-5.3: FileShareUtils verified, unused imports removed, FacePreviewCanvas bitmap fix

## Overall Project Metrics

- **Total new files**: 18 (9 Phase 1, 3 Phase 2, 7 Phase 3, 2 Phase 4+5)
- **Total modified files**: 10
- **Total tests added**: 88 (19 FaceMeasurement, 19 FrameOverlay, 27 ViewModel, 22 Screen, 1 Golden)
- **All 864 tests passing** (including 800+ existing)
- **Golden image generated**: `standard_overlay.png` (240 KB, 200×300)
- **Branch**: `feature/virtual-try-on` — all 4 PRs complete

## Remaining Tasks

**NONE** — All Phase 1-5 tasks are complete. Ready for verification and archive.

## Workload / PR Boundary

- Mode: auto-chain (feature-branch-chain) — PR 4/4 (final)
- Current work unit: Phase 4+5 — Golden Tests & Cleanup
- Boundary: Phase 3 UI layer → final cleanup, golden test, and known-issue fix
- Estimated review budget: ~200 lines (small, focused cleanup batch)
