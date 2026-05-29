# Proposal: Virtual Try-On for Eyewear

## Intent

Enable opticians to show patients how eyeglass frames look on their face using a 2D overlay approach. The optician picks or captures a photo, selects a frame from inventory, and gets a scaled overlay with the frame rendered on the face. Result is shareable via WhatsApp. This addresses the patient decision point at prescription dispensing — "how will this look on me?"

## Scope

### In Scope
- Face landmark detection via MediaPipe Face Landmarker (static image from gallery or CameraX capture)
- 2D Canvas overlay of montura PNG (transparent background) onto detected face landmarks
- Known PD from patient `Evaluacion` record used as pixel-to-mm scale reference
- DIP/DNP measurement extraction from landmarks (for verification)
- Save composite image to device gallery
- Share via WhatsApp (`Intent.ACTION_SEND`)
- Montura data model extended with frame metadata: `ancho` (mm), `puente` (mm), `altura` (mm), `imagenUri` (local PNG path)

### Out of Scope
- Live camera preview with real-time AR overlay (future: ARCore 3D)
- Multiple frame comparison side-by-side
- Auto-recommend frames by face shape
- Cloud ML-based PD estimation

## Capabilities

### New Capabilities
- `virtual-try-on`: Face landmark detection + 2D frame overlay + measurement extraction + share workflow

### Modified Capabilities
- `montura-inventory`: Montura entity extended with `ancho`, `puente`, `altura`, `imagenUri` fields; existing fields unchanged

## Approach

**Android-only MVP** using 2D Canvas + MediaPipe.

**Flow**:
1. User selects patient → opens Try-On screen
2. Pick photo (gallery) or capture (CameraX) → load as `Bitmap`
3. MediaPipe Face Landmarker processes image → 468 face landmarks
4. Calculate `mmPerPixel` using known PD from patient's `Evaluacion` record
5. Compute eye center, frame scale, and rotation from landmarks
6. Load montura PNG → `Canvas.drawBitmap` with transform matrix
7. Save composite to `MediaStore`
8. Share via `Intent.ACTION_SEND` to WhatsApp

**Montura metadata**: store `ancho`, `puente`, `altura`, `imagenUri` as new columns on `monturas` table. PNG images stored locally, referenced by URI.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/data/` | Modified | Extend `Montura` entity with frame metadata fields |
| `optoapp/src/main/java/com/example/optoapp/data/` | Modified | Add Room migration for new columns + DAOs |
| `optoapp/src/main/java/com/example/optoapp/viewmodel/` | New | `VirtualTryOnViewModel` — face landmark processing, overlay logic, measurement extraction |
| `optoapp/src/main/java/com/example/optoapp/ui/` | New | `VirtualTryOnScreen` — Compose UI (photo picker, frame selector, result view, share) |
| `optoapp/src/main/java/com/example/optoapp/domain/` | New | `FaceLandmarkerUseCase`, `FrameOverlayUseCase`, `MeasurementExtractorUseCase` |
| `optoapp/build.gradle.kts` | Modified | Add `com.google.mediapipe:mediapipe-face-detection` or `mediapipe-tasks-vision` |
| `gradle/libs.versions.toml` | Modified | MediaPipe dependency version |
| `optoapp/src/main/AndroidManifest.xml` | Modified | `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` permissions |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| MediaPipe model size bloat (APK) | Medium | Use `mediapipe-tasks-vision` (dynamic download) or bundle smallest `.task` asset |
| Face not detected (profile shot, poor lighting) | Medium | Show error state; allow manual PD override |
| Frame metadata (mm) missing for existing monturas | Medium | Make fields optional in DB; prompt to fill in montura edit form |
| WhatsApp share intent fails on some launchers | Low | Fallback to generic `ACTION_SEND` |
| Rotation/pose of face causes bad overlay | Medium | Validate face yaw/pitch confidence from MediaPipe before overlay |

## Rollback Plan

1. **Disable the feature flag** — guard Try-On button with `BuildConfig.VIRTUAL_TRY_ON_ENABLED`
2. **Revert MediaPipe dependency** — remove from `libs.versions.toml` and `build.gradle.kts`
3. **Revert Room migration** — keep migration as non-destructive (add-only columns); existing rows get null/default values
4. **Revert Montura entity** — remove 4 new fields; DAOs fall back to old schema via column exclusion

## Dependencies

- MediaPipe Vision (face landmarking) — `com.google.mediapipe:mediapipe-tasks-vision` or `mediapipe-face-detection`
- CameraX (already in dependencies) — for photo capture
- Patient PD from existing `Evaluacion` entity — already populated

## Success Criteria

- [ ] Optician can pick/gallery photo and see frame overlaid on face within 3 seconds
- [ ] Overlay scales correctly using patient's known PD — frames of same PD look same size regardless of photo resolution
- [ ] Composite image saved and shareable to WhatsApp contact
- [ ] DIP/DNP measurements extracted and displayed for verification (within ±1mm of known PD)
- [ ] Existing Montura CRUD continues to work without migration failures
- [ ] APK size increase < 10 MB from MediaPipe bundle