# Virtual Try-On — Technical Design

## Executive Summary

2D virtual try-on for eyewear frames using MediaPipe Face Landmarker (468 landmarks) + Canvas overlay. Patient photo + montura selection → scaled overlay using known PD from Evaluacion → save/share via WhatsApp. **MVP: static image processing, no real-time AR**.

---

## Architecture Decisions

### 1. MediaPipe Model Delivery: **BUNDLED**

**Decision**: Bundle `.task` model asset in APK (`optoapp/src/main/assets/face_landmarker.task`)

**Rationale**:
- Offline-first architecture — cannot depend on network download at first-run
- CameraX already bundled, consistent with existing pattern
- Model size ~6-8MB, acceptable given APK size increase < 10MB constraint
- No dynamic download complexity (WorkManager, retry logic, storage management)

**Tradeoff**: +8MB APK vs download complexity. Chosen: simpler, offline-guaranteed.

**Implementation**:
```kotlin
// FaceLandmarkerUseCase.kt
val baseOptions = BaseOptions.Builder()
    .setModelAssetPath("face_landmarker.task")
    .build()
```

---

### 2. Image Processing: **Coroutine (IO Dispatcher)**

**Decision**: Use `viewModelScope.launch(Dispatchers.IO)` for face detection + overlay

**Rationale**:
- Processing time: ~500-1500ms per image (acceptable for MVP)
- WorkManager overkill for single-image, user-waiting workflow
- CameraX already uses coroutines for capture
- Simpler error handling + cancellation

**Tradeoff**: Blocks user flow if processing >2s. Mitigation: show progress indicator.

**Future**: If batch processing needed (multiple frames), migrate to WorkManager.

---

### 3. Face Measurement Service: **Standalone Domain Class**

**Decision**: `FaceMeasurementExtractor` as pure domain class (not repository extension)

**Rationale**:
- Single Responsibility: extracts mm measurements from landmarks + PD scale
- No database dependency — pure geometry + math
- Testable in isolation (no Room/Hilt needed)
- Reusable across use cases

**Location**: `optoapp/src/main/java/com/example/optoapp/domain/FaceMeasurementExtractor.kt`

---

### 4. Composite Image Storage: **Local File + MediaStore**

**Decision**: Save PNG to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (device gallery)

**Rationale**:
- User expects photo in gallery for sharing elsewhere
- Supabase Storage overkill for transient try-on results
- FileProvider already configured for sharing intents
- Existing `FileShareUtils` utility available

**Storage Path**: `MediaStore` (not app-private) for user accessibility

**Metadata**: Store composite metadata (patientId, monturaId, timestamp) in Room if needed for history (out of scope for MVP).

---

### 5. Sharing: **Direct Intent with Chooser**

**Decision**: `Intent.ACTION_SEND` with `Intent.createChooser()` (WhatsApp not guaranteed)

**Rationale**:
- WhatsApp may not be installed — fallback to generic share
- `Intent.createChooser()` shows all apps (WhatsApp, Telegram, Email, etc.)
- Existing `FileShareUtils` pattern in codebase
- Proposal says "WhatsApp as primary target" — achievable via chooser UI

**Implementation**:
```kotlin
val shareIntent = Intent.createChooser(
    Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    },
    "Compartir prueba virtual"
)
```

---

### 6. Montura Metadata: **NEW COLUMNS ON EXISTING TABLE**

**Decision**: Add 4 columns to `monturas` table via Room migration (v21→v22)

**Columns**:
- `anchoMm` REAL — frame width (lens + bridge)
- `puenteMm` REAL — bridge width (DIP reference)
- `alturaMm` REAL — lens height
- `imagenUri` TEXT — local PNG path (transparent background)

**Rationale**:
- Existing montura CRUD already manages this table
- New table would require JOINs for simple queries
- Migration is non-destructive (ADD COLUMN IF NOT EXISTS)
- Backward compatible: existing monturas get NULL values (optional fields)

**Migration**:
```sql
ALTER TABLE monturas ADD COLUMN anchoMm REAL
ALTER TABLE monturas ADD COLUMN puenteMm REAL
ALTER TABLE monturas ADD COLUMN alturaMm REAL
ALTER TABLE monturas ADD COLUMN imagenUri TEXT
```

**Entity Update**:
```kotlin
data class Montura(
    // ... existing fields ...
    val anchoMm: Double? = null,
    val puenteMm: Double? = null,
    val alturaMm: Double? = null,
    val imagenUri: String? = null
)
```

---

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User selects patient → VirtualTryOnScreen                    │
│    - NavController.navigate("virtual_try_on/{pacienteId}")      │
│    - ViewModel loads patient's latest Evaluacion (for PD)       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Photo Selection                                              │
│    - Option A: Gallery picker (ActivityResultContracts)         │
│    - Option B: CameraX capture → Bitmap                         │
│    - Load as Bitmap (max 2048px for performance)                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Face Landmark Detection (MediaPipe)                          │
│    - FaceLandmarkerUseCase.detect(Bitmap) → FaceLandmarksResult │
│    - Extract 468 landmarks (normalized 0.0-1.0 coords)          │
│    - Validate: face detected, confidence > 0.8                  │
│    - Error: "No se detectó rostro" → retry or manual PD         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. PD Scale Calculation                                         │
│    - Get known PD from Evaluacion: dipTotalMm / dnpOdMm+dnpOiMm │
│    - Calculate pixel distance between pupil landmarks           │
│    - mmPerPixel = knownPD_mm / pupilDistance_px                 │
│    - Fallback: default PD 63mm if missing (show warning)        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Montura Selection                                            │
│    - MonturaList bottom sheet (filter by tipoAro, material)     │
│    - User selects montura → load imagenUri (transparent PNG)    │
│    - Validate: has imagenUri + anchoMm metadata                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. Canvas Overlay Composition                                   │
│    - FrameOverlayUseCase.compose(faceBitmap, monturaPng, ...)   │
│    - Compute scale: monturaAnchoMm * mmPerPixel                 │
│    - Position: center on nose bridge (landmark index 1)         │
│    - Align: horizontal line through eye landmarks (362, 133)    │
│    - Draw: Canvas.drawBitmap(montura, matrix, null)             │
│    - Optional: user fine-tune position/scale (UI sliders)       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. Save Composite Image                                         │
│    - Canvas.toBitmap() → PNG                                    │
│    - MediaStore.insert → content:// URI                         │
│    - Store URI for sharing                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. Share Result                                                 │
│    - ResultScreen: "Guardar" + "Compartir" buttons              │
│    - Save: already done (step 7)                                │
│    - Share: Intent.createChooser(ACTION_SEND)                   │
│    - Disabled until rendering complete                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## File Changes

### New Files

```
optoapp/src/main/java/com/example/optoapp/domain/
├── FaceLandmarkerUseCase.kt          # MediaPipe wrapper, detect(Bitmap) → Result
├── FrameOverlayUseCase.kt            # Canvas composition logic
└── FaceMeasurementExtractor.kt       # Landmark → mm measurements (DIP/DNP)

optoapp/src/main/java/com/example/optoapp/viewmodel/
└── VirtualTryOnViewModel.kt          # StateFlow UI state, orchestration

optoapp/src/main/java/com/example/optoapp/ui/screens/
└── VirtualTryOnScreen.kt             # Compose UI (photo picker, selector, result)

optoapp/src/main/java/com/example/optoapp/ui/components/virtualtryon/
├── PhotoPickerSection.kt             # Gallery/Camera buttons
├── MonturaSelectorBottomSheet.kt     # Bottom sheet with montura grid
├── FacePreviewCanvas.kt              # Composable with Canvas overlay
├── ResultScreen.kt                   # Final view with save/share actions
└── MeasurementDisplay.kt             # DIP/DNP verification display

optoapp/src/main/java/com/example/optoapp/di/
└── MediaPipeModule.kt                # Hilt module for FaceLandmarker provider

optoapp/src/main/assets/
└── face_landmarker.task              # Bundled MediaPipe model
```

### Modified Files

```
optoapp/build.gradle.kts
├── Add: implementation("com.google.mediapipe:mediapipe-tasks-vision:0.10.14")

gradle/libs.versions.toml
├── Add: mediapipe = "0.10.14"
└── Add: mediapipe-tasks-vision = { group = "com.google.mediapipe", name = "mediapipe-tasks-vision", version.ref = "mediapipe" }

optoapp/src/main/java/com/example/optoapp/data/dispensacion/DispensacionEntity.kt
├── Add fields to Montura entity: anchoMm, puenteMm, alturaMm, imagenUri

optoapp/src/main/java/com/example/optoapp/data/OptoDatabaseMigrations.kt
└── Add: MIGRATION_21_22 (ADD COLUMN for 4 new fields)

optoapp/src/main/java/com/example/optoapp/data/OptoDatabase.kt
└── Update version: 21 → 22
    Add: .addMigrations(..., MIGRATION_21_22)

optoapp/src/main/AndroidManifest.xml
├── Add: READ_MEDIA_IMAGES (Android 13+)
└── Add: READ_EXTERNAL_STORAGE (Android 12 and below)

optoapp/src/main/java/com/example/optoapp/MainActivity.kt
└── Add navigation route: composable("virtual_try_on/{pacienteId}")

optoapp/src/main/java/com/example/optoapp/ui/components/MainDrawerContent.kt
└── Add navigation entry to MonturasScreen (existing) → add "Prueba Virtual" button
```

---

## Class Designs

### FaceLandmarkerUseCase

```kotlin
@Singleton
class FaceLandmarkerUseCase @Inject constructor() {
    private val faceLandmarker: FaceLandmarker by lazy {
        val baseOptions = BaseOptions.Builder()
            .setModelAssetPath("face_landmarker.task")
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setOutputFaceBlendshapes(false)
            .setOutputFacialTransformationMatrix(false)
            .setOutputTransferredSegmentationMask(false)
            .build()
        FaceLandmarker.createFromOptions(Context, options)
    }

    data class Result(
        val landmarks: List<List<NormalizedLandmark>>, // 468 landmarks per face
        val confidence: Float,
        val boundingBox: Rect
    )

    fun detect(bitmap: Bitmap): Result {
        val mpImage = bitmap.toMediaPipeImage()
        val results = faceLandmarker.detect(mpImage)
        // Extract landmarks, validate confidence
    }
}
```

### FrameOverlayUseCase

```kotlin
class FrameOverlayUseCase @Inject constructor() {
    data class OverlayConfig(
        val faceBitmap: Bitmap,
        val monturaBitmap: Bitmap,
        val landmarks: List<NormalizedLandmark>,
        val mmPerPixel: Float,
        val monturaAnchoMm: Float,
        val monturaPuenteMm: Float,
        val adjustmentX: Float = 0f,
        val adjustmentY: Float = 0f,
        val adjustmentScale: Float = 1f
    )

    fun compose(config: OverlayConfig): Bitmap {
        val canvas = Canvas(config.faceBitmap.copy(Bitmap.Config.ARGB_8888, true))
        val matrix = computeTransformMatrix(config)
        canvas.drawBitmap(config.monturaBitmap, matrix, null)
        return canvas.toBitmap()
    }

    private fun computeTransformMatrix(config: OverlayConfig): Matrix {
        // 1. Find nose bridge (landmark 1)
        // 2. Find eye centers (362, 133)
        // 3. Calculate scale: monturaAnchoMm * mmPerPixel
        // 4. Apply adjustments
        // 5. Return Matrix (translate + scale + rotate)
    }
}
```

### VirtualTryOnViewModel

```kotlin
sealed class TryOnState {
    object Idle : TryOnState()
    object LoadingPhoto : TryOnState()
    data class PhotoSelected(val bitmap: Bitmap) : TryOnState()
    data class FaceDetected(val landmarks: List<NormalizedLandmark>, val pd: Double) : TryOnState()
    data class OverlayReady(val compositeBitmap: Bitmap, val measurements: FaceMeasurements) : TryOnState()
    data class Error(val message: String) : TryOnState()
}

@HiltViewModel
class VirtualTryOnViewModel @Inject constructor(
    private val faceLandmarkerUseCase: FaceLandmarkerUseCase,
    private val frameOverlayUseCase: FrameOverlayUseCase,
    private val measurementExtractor: FaceMeasurementExtractor,
    private val pacienteRepository: PacienteRepository,
    private val monturaCoordinator: MonturaInventoryCoordinator,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<TryOnState>(Idle)
    val state: StateFlow<TryOnState> = _state

    private val _selectedMontura = MutableStateFlow<Montura?>(null)
    val selectedMontura: StateFlow<Montura?> = _selectedMontura

    fun loadPatientPD(pacienteId: String) { ... }
    fun selectPhoto(uri: Uri) { ... }
    fun selectMontura(montura: Montura) { ... }
    fun adjustOverlay(dx: Float, dy: Float, scale: Float) { ... }
    fun saveComposite(): Uri? { ... }
    fun shareComposite(uri: Uri) { ... }
}
```

---

## Testing Strategy

### Unit Tests (Robolectric)

**Location**: `optoapp/src/test/java/com/example/optoapp/domain/`

```kotlin
// FaceMeasurementExtractorTest.kt
@RunWith(RobolectricTestRunner::class)
class FaceMeasurementExtractorTest {
    @Test
    fun `DIP calculation from landmarks with known PD returns correct mmPerPixel`() { ... }

    @Test
    fun `DNP asymmetric calculation returns different Od vs Oi`() { ... }

    @Test
    fun `missing PD falls back to default 63mm`() { ... }
}

// FrameOverlayUseCaseTest.kt
@RunWith(RobolectricTestRunner::class)
class FrameOverlayUseCaseTest {
    @Test
    fun `overlay composition preserves transparency`() { ... }

    @Test
    fun `scale computation matches monturaAncho to PD ratio`() { ... }
}
```

### Integration Tests (AndroidTest)

**Location**: `optoapp/src/androidTest/java/com/example/optoapp/ui/screens/`

```kotlin
// VirtualTryOnScreenTest.kt
@RunWith(AndroidJUnit4::class)
class VirtualTryOnScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun photoPicker_showsGalleryAndCameraButtons() { ... }

    @Test
    fun faceDetected_showsMonturaSelectorButton() { ... }

    @Test
    fun monturaSelected_showsOverlayPreview() { ... }

    @Test
    fun resultScreen_saveAndShareButtonsEnabled() { ... }
}
```

### Golden Image Tests

**Location**: `optoapp/src/test/resources/golden-images/`

```kotlin
// Save expected overlay outputs as PNGs for visual regression
@Test
fun `overlay matches golden image for standard face`() {
    val result = frameOverlayUseCase.compose(testConfig)
    assertImageSimilar(result, loadGolden("standard_overlay.png"))
}
```

---

## Room Migration Script

```kotlin
// OptoDatabaseMigrations.kt
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE monturas ADD COLUMN anchoMm REAL")
        db.execSQL("ALTER TABLE monturas ADD COLUMN puenteMm REAL")
        db.execSQL("ALTER TABLE monturas ADD COLUMN alturaMm REAL")
        db.execSQL("ALTER TABLE monturas ADD COLUMN imagenUri TEXT")
    }
}
```

**Backward Compatibility**: Existing monturas get NULL values. MonturaEditForm must handle optional fields.

---

## UI Flow (Compose Navigation)

```
MainDrawerScreen
    └── [Prueba Virtual Button] → VirtualTryOnScreen(pacienteId)
            ├── PhotoPickerSection
            │       ├── Gallery → ActivityResultLauncher
            │       └── Camera → CameraX capture
            ├── FacePreviewCanvas (loading → detected)
            ├── MonturaSelectorButton (enabled after face detected)
            │       └── BottomSheet → MonturaSelectorBottomSheet
            │               └── MonturaGrid (thumbnail + metadata)
            ├── AdjustmentSliders (X, Y, Scale — optional, after overlay)
            └── ResultScreen (after composition)
                    ├── CompositeImage Preview
                    ├── Save Button → MediaStore
                    └── Share Button → Intent.createChooser()
```

---

## Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" /> <!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" /> <!-- Android 12 and below -->
```

**Runtime Permission Request**: Use `ActivityResultContracts.RequestMultiplePermissions()` in VirtualTryOnScreen.

---

## Error States

| Error | Trigger | UI Message | Recovery |
|-------|---------|------------|----------|
| No face detected | MediaPipe returns 0 faces | "No se detectó un rostro. Intenta con otra foto." | Retry or manual PD entry |
| Low confidence | Face confidence < 0.8 | "Rostro poco claro. Usa una foto con mejor iluminación." | Retry |
| Missing PD | Evaluacion has no dipTotalMm | "PD desconocido. Ingresá PD manual (opcional)." | Manual input or skip scaling |
| Missing montura image | Montura.imagenUri is null | "Esta montura no tiene imagen de prueba." | Select different montura |
| Missing montura metadata | anchoMm/puenteMm is null | "Faltan medidas de la montura." | Edit montura first |
| WhatsApp not installed | Intent.resolveActivity() returns null | "WhatsApp no está instalado." | Fallback to generic share |
| Storage full | MediaStore.insert fails | "No hay espacio para guardar la imagen." | Free space |

---

## Performance Considerations

1. **Bitmap Sizing**: Resize input to max 2048px before MediaPipe processing
2. **MediaPipe Initialization**: Lazy singleton (first call ~300ms, subsequent ~50ms)
3. **Canvas Composition**: ~100-200ms (acceptable)
4. **Memory**: Bitmap pooling not needed for single-image MVP
5. **Coroutine Cancellation**: `viewModelScope` auto-cancels on screen exit

---

## Future Enhancements (Out of Scope)

- **ARCore 3D overlay**: Real-time camera preview with depth
- **Multiple frame comparison**: Side-by-side UI
- **Face shape analysis**: Auto-recommend frames
- **Cloud PD estimation**: ML model for unknown PD
- **History gallery**: Save try-on sessions to patient record

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| MediaPipe model bloat | Use smallest `.task` asset, strip unused ops |
| Bad overlay alignment | Validate face yaw/pitch before overlay, show warning if >15° |
| Existing monturas missing metadata | Make fields optional, prompt to fill in edit form |
| Share intent fails | Fallback to generic ACTION_SEND |
| Migration failure | Test with existing production DB backup |

---

## Success Metrics

- **Performance**: Face detection + overlay < 3 seconds (p95)
- **Accuracy**: Overlay scale within ±1mm of known PD
- **APK Size**: Increase < 10MB
- **Coverage**: 70% unit test coverage on domain layer
- **UX**: 90% success rate (face detected + overlay rendered)

---

## Dependencies

```toml
# gradle/libs.versions.toml
[versions]
mediapipe = "0.10.14"

[libraries]
mediapipe-tasks-vision = { group = "com.google.mediapipe", name = "mediapipe-tasks-vision", version.ref = "mediapipe" }
```

```kotlin
// optoapp/build.gradle.kts
dependencies {
    // ... existing ...
    implementation(libs.mediapipe.tasks.vision)
}
```

---

## Rollback Plan

1. **Feature Flag**: Guard with `BuildConfig.VIRTUAL_TRY_ON_ENABLED = false`
2. **Remove Navigation**: Comment out route in MainActivity.kt
3. **Revert Dependency**: Remove MediaPipe from build.gradle.kts
4. **Migration**: Keep MIGRATION_21_22 (non-destructive, add-only columns)
5. **Revert Entity**: Remove 4 fields from Montura data class (Room ignores extra columns)

---

## Next Steps (Tasks Phase)

1. Add MediaPipe dependency + bundle model asset
2. Create Room migration (v21→v22)
3. Implement FaceLandmarkerUseCase
4. Implement FrameOverlayUseCase
5. Implement FaceMeasurementExtractor
6. Create VirtualTryOnViewModel
7. Create VirtualTryOnScreen + components
8. Add navigation route
9. Write unit tests (domain layer)
10. Write integration tests (UI layer)
11. Update MonturaEditForm for new fields
12. Test with real photos + monturas
