# Tasks: Virtual Try-On for Eyewear

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~900–1200 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Base Branch | Notes |
|------|------|-------------|-------|
| 1 | Dependencies + Room migration + asset | feature/virtual-try-on | Gradle, manifest, migration, entity, Hilt shell |
| 2 | Domain layer (detection + overlay + measurements) | PR 1 branch | Unit tests included (Robolectric) |
| 3 | UI + ViewModel + navigation + form | PR 2 branch | Compose screens, integration tests |
| 4 | Sharing polish + golden tests + cleanup | PR 3 branch | MediaStore save, WhatsApp intent, dead code removal |

## Phase 1: Foundation

- [ ] 1.1 Add `mediapipe = "0.10.14"` and library alias to `gradle/libs.versions.toml`
- [ ] 1.2 Add `implementation(libs.mediapipe.tasks.vision)` to `optoapp/build.gradle.kts`
- [ ] 1.3 Copy `face_landmarker.task` into `optoapp/src/main/assets/`
- [ ] 1.4 Add `READ_MEDIA_IMAGES` and `READ_EXTERNAL_STORAGE` to `optoapp/src/main/AndroidManifest.xml`
- [ ] 1.5 Add `anchoMm`, `puenteMm`, `alturaMm`, `imagenUri` to `Montura` in `optoapp/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt`
- [ ] 1.6 Write `MIGRATION_21_22` in `optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt`
- [ ] 1.7 Bump `OptoDatabase` version 21→22 and register migration in `optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt`
- [ ] 1.8 Create `optoapp/src/main/java/com/example/optoapp/di/MediaPipeModule.kt` Hilt module
- [ ] 1.9 Update `MonturaEditForm` in `optoapp/src/main/java/com/example/optoapp/ui/components/monturas/MonturaForm.kt` to input 4 new fields

## Phase 2: Core Domain

- [ ] 2.1 Create `optoapp/src/main/java/com/example/optoapp/domain/FaceLandmarkerUseCase.kt` with `detect(Bitmap) → Result`
- [ ] 2.2 Create `optoapp/src/main/java/com/example/optoapp/domain/FaceMeasurementExtractor.kt` for DIP/DNP/segment height
- [ ] 2.3 Create `optoapp/src/main/java/com/example/optoapp/domain/FrameOverlayUseCase.kt` with `compose(OverlayConfig) → Bitmap`
- [ ] 2.4 Write `FaceMeasurementExtractorTest.kt` (DIP, DNP asymmetry, missing PD fallback)
- [ ] 2.5 Write `FrameOverlayUseCaseTest.kt` (scale computation, transparency preserved)

## Phase 3: UI & Integration

- [ ] 3.1 Create `optoapp/src/main/java/com/example/optoapp/viewmodel/VirtualTryOnViewModel.kt` with `TryOnState` sealed class
- [ ] 3.2 Create `optoapp/src/main/java/com/example/optoapp/ui/screens/VirtualTryOnScreen.kt` host screen
- [ ] 3.3 Create `optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/PhotoPickerSection.kt` (gallery + CameraX)
- [ ] 3.4 Create `optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/MonturaSelectorBottomSheet.kt`
- [ ] 3.5 Create `optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/FacePreviewCanvas.kt`
- [ ] 3.6 Create `optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/MeasurementDisplay.kt`
- [ ] 3.7 Create `optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/ResultScreen.kt` with save/share actions
- [ ] 3.8 Add `virtual_try_on/{pacienteId}` route to `optoapp/src/main/java/com/example/optoapp/MainActivity.kt`
- [ ] 3.9 Add "Prueba Virtual" button to `optoapp/src/main/java/com/example/optoapp/ui/components/MainDrawerContent.kt`

## Phase 4: Testing

- [ ] 4.1 Write `VirtualTryOnViewModelTest.kt` — state machine transitions
- [ ] 4.2 Write `VirtualTryOnScreenTest.kt` — picker, overlay, result flows
- [ ] 4.3 Add golden image test `FrameOverlayUseCaseGoldenTest.kt` against `src/test/resources/golden-images/`

## Phase 5: Cleanup

- [ ] 5.1 Verify `FileShareUtils` in `optoapp/src/main/java/com/example/optoapp/util/FileShareUtils.kt` handles `content://` URI grant for `ACTION_SEND`
- [ ] 5.2 Remove unused imports / dead code from Phase 3 if any
- [ ] 5.3 Update `IMPROVEMENT-PLAN.md` if relevant items were addressed
