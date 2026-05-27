# Technical Design: Android E2E Tests

## Goal

Implement comprehensive E2E testing infrastructure for OptoApp Android covering 4 P0 user flows (Login+PIN, Patient Creation, Evaluation, Dispensación) via Compose UI Tests and Supabase integration tests.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Test Architecture Layers                      │
├─────────────────────────────────────────────────────────────────┤
│  Level 1: Compose UI Tests (androidTest)                        │
│  - LoginScreenTest.kt          • Fake Repository                │
│  - PacienteScreenTest.kt         In-Memory Room                 │
│  - EvaluacionScreenTest.kt       No network calls               │
│  - DispensacionScreenTest.kt                                      │
│  - NavigationTest.kt                                              │
├─────────────────────────────────────────────────────────────────┤
│  Level 2: Instrumented Supabase Tests (androidTest)             │
│  - SupabaseAuthTest.kt           Real Supabase project          │
│  - SyncFlowTest.kt                 BuildConfig credentials      │
│  - OfflineSyncTest.kt              Test data cleanup            │
├─────────────────────────────────────────────────────────────────┤
│  Shared Test Infrastructure                                     │
│  - TestTags.kt                   • @TestTag constants           │
│  - TestDataFactory.kt            • Inline data factories        │
│  - FakeSupabaseClient.kt         • Mock implementations         │
│  - TestDatabaseRule.kt           • Room lifecycle management    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. Compose UI Tests Architecture

### 1.1 Test Structure

**Decision**: One test class per screen/flow, not per individual screen component.

**Rationale**: 
- Tests user JOURNEYS, not isolated components
- Matches P0 flow definition in proposal
- Reduces test duplication (navigation tested once per flow)
- Easier to maintain when UI changes

```
optoapp/src/androidTest/java/com/example/optoapp/ui/
├── LoginFlowTest.kt          // Login → PIN → Main
├── PacienteFlowTest.kt       // Dashboard → Nuevo Paciente → Lista
├── EvaluacionFlowTest.kt     // Paciente selected → Nueva Evaluación → Guardar
└── DispensacionFlowTest.kt   // Paciente selected → Nueva Dispensación → OT → Pago
```

### 1.2 Test Rule Setup

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<MainActivity>()

@get:Rule
val databaseRule = TestDatabaseRule() // Custom @TestRule

@Before
fun setup() {
    // Inject fake repository into MainActivity's Hilt graph
    // OR use direct instantiation without Hilt (Phase 1)
}
```

**TestDatabaseRule Implementation**:
```kotlin
class TestDatabaseRule : TestRule {
    private lateinit var database: OptoDatabase
    
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // BEFORE
                database = Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    OptoDatabase::class.java
                ).allowMainThreadQueries().build()
                
                try {
                    base.evaluate() // Run test
                } finally {
                    // AFTER
                    database.close()
                }
            }
        }
    }
    
    fun getDatabase(): OptoDatabase = database
}
```

### 1.3 Test Data Factories

**Location**: `TestDataFactory.kt`

```kotlin
object TestDataFactory {
    
    fun createPaciente(
        id: String = generateId(),
        nombre: String = "Test Paciente $id",
        edad: Int = 30,
        telefono: String = "555-0100",
        email: String? = null,
        direccion: String? = null,
        opticaId: String = "test-optica"
    ): Paciente = Paciente(
        id = id,
        nombreCompleto = nombre,
        edad = edad,
        telefono = telefono,
        email = email,
        direccion = direccion,
        fechaCreacion = LocalDate.now(),
        opticaId = opticaId
    )
    
    fun createEvaluacion(
        id: String = generateId(),
        pacienteId: String,
        opticaId: String = "test-optica",
        motivoConsulta: String = "Control general",
        diagnostico: String = "Miopía leve"
    ): EvaluacionClinica = EvaluacionClinica(
        id = id,
        pacienteId = pacienteId,
        fecha = LocalDate.now(),
        opticaId = opticaId,
        motivoConsulta = motivoConsulta,
        diagnostico = diagnostico,
        citaEstado = "completada"
    )
    
    fun createDispensacion(
        id: String = generateId(),
        pacienteId: String,
        opticaId: String = "test-optica",
        montoTotal: Double = 150.0,
        montoPagado: Double = 100.0,
        ot: String = "OT-TEST-${System.currentTimeMillis()}"
    ): DispensacionOptica = DispensacionOptica(
        id = id,
        pacienteId = pacienteId,
        fecha = LocalDate.now(),
        opticaId = opticaId,
        montoTotal = montoTotal,
        montoPagado = montoPagado,
        estadoEntrega = "pendiente",
        ot = ot,
        monturaId = null,
        tratamientos = emptyList()
    )
    
    private fun generateId(): String = "test-${UUID.randomUUID().toString().take(8)}"
}
```

**Decision**: Inline factories over external seed scripts.

**Rationale**:
- No external dependencies (files, network)
- Tests are self-contained and reproducible
- Follows existing pattern from `DaoTest.kt`
- Faster execution (no I/O)

### 1.4 Async Handling

**Pattern**: `composeTestRule.waitUntil()` with explicit timeouts

```kotlin
@Test
fun login_navigatesToPinScreen() {
    // Arrange
    fillEmail("test@example.com")
    fillPassword("correcthorsebatterystaple")
    
    // Act
    onNodeWithTag(TestTags.LOGIN_BUTTON).performClick()
    
    // Assert - wait for navigation
    composeTestRule.waitUntil(timeoutMillis = 5000) {
        try {
            onNodeWithTag(TestTags.PIN_SCREEN_ROOT).fetchSemanticsNode()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}
```

**Decision**: 5-second default timeout for UI operations.

**Rationale**:
- Balances between flakiness and fast failure
- Emulator can be slower than physical device
- Long enough for Room operations, short enough to catch hangs

---

## 2. Test Tag Strategy

### 2.1 Naming Convention

Format: `screen_element_action` or `screen_element_type`

```kotlin
object TestTags {
    // Login Screen
    const val LOGIN_EMAIL_FIELD = "login_email_field"
    const val LOGIN_PASSWORD_FIELD = "login_password_field"
    const val LOGIN_BUTTON = "login_submit_button"
    const val LOGIN_ERROR_MESSAGE = "login_error_message"
    
    // PIN Screen
    const val PIN_INPUT = "pin_input_field"
    const val PIN_SCREEN_ROOT = "pin_screen_root"
    
    // Paciente Screen
    const val PACIENTE_NOMBRE_FIELD = "paciente_nombre_field"
    const val PACIENTE_EDAD_FIELD = "paciente_edad_field"
    const val PACIENTE_TELEFONO_FIELD = "paciente_telefono_field"
    const val PACIENTE_GUARDAR_BTN = "paciente_guardar_btn"
    const val PACIENTE_LISTA = "paciente_lista"
    
    // Evaluacion Screen
    const val EVALUACION_DIP_FIELD = "evaluacion_dip_field"
    const val EVALUACION_ESFERA_OD = "evaluacion_esfera_od"
    const val EVALUACION_CILINDRO_OD = "evaluacion_cilindro_od"
    const val EVALUACION_GUARDAR_BTN = "evaluacion_guardar_btn"
    const val EVALUACION_AUTO_DIAGNOSTICO = "evaluacion_auto_diagnostico"
    
    // Dispensacion Screen
    const val DISPENSACION_AGREGAR_ITEM_BTN = "dispensacion_agregar_item_btn"
    const val DISPENSACION_OT_FIELD = "dispensacion_ot_field"
    const val DISPENSACION_MONTO_PAGADO = "dispensacion_monto_pagado"
    const val DISPENSACION_GUARDAR_BTN = "dispensacion_guardar_btn"
    
    // Navigation
    const val NAV_BOTTOM_PACIENTES = "nav_bottom_pacientes"
    const val NAV_BOTTOM_EVALUACIONES = "nav_bottom_evaluaciones"
    const val NAV_BOTTOM_DISPENSACIONES = "nav_bottom_dispensaciones"
    const val NAV_DRAWER_TOGGLE = "nav_drawer_toggle"
}
```

### 2.2 Where to Declare Test Tags

**Decision**: Dual declaration strategy

1. **Production code**: `@TestTag` annotation on composables
2. **Test code**: `TestTags.kt` object with constants

```kotlin
// In LoginScreen.kt (production)
@Composable
fun LoginScreen(...) {
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        modifier = Modifier.testTag(TestTags.LOGIN_EMAIL_FIELD),
        // ...
    )
}

// In TestTags.kt (shared)
object TestTags {
    const val LOGIN_EMAIL_FIELD = "login_email_field"
}
```

**Location for TestTags.kt**: `optoapp/src/main/java/com/example/optoapp/testing/TestTags.kt`

**Rationale**:
- Single source of truth (tests import from main)
- Compile-time safety (typos caught early)
- No runtime reflection needed
- Follows Android testing best practices

---

## 3. Supabase Instrumented Tests Architecture

### 3.1 Test Project Setup

**Decision**: Separate free-tier Supabase project for tests.

**Configuration**:
```properties
# local.properties (gitignored)
supabase.test.url=https://test-project.supabase.co
supabase.test.anon.key=eyJhbGc...test-key
supabase.test.service.key=eyJhbGc...service-key
supabase.redirect.scheme=optoapp
supabase.redirect.host=auth
```

**BuildConfig fields** (added to `build.gradle.kts`):
```kotlin
val supabaseTestUrl = escapeForBuildConfigField(
    localProperties.getProperty("supabase.test.url", "")
)
val supabaseTestAnonKey = escapeForBuildConfigField(
    localProperties.getProperty("supabase.test.anon.key", "")
)
val supabaseTestServiceKey = escapeForBuildConfigField(
    localProperties.getProperty("supabase.test.service.key", "")
)

buildConfigField("String", "SUPABASE_TEST_URL", "\"$supabaseTestUrl\"")
buildConfigField("String", "SUPABASE_TEST_ANON_KEY", "\"$supabaseTestAnonKey\"")
buildConfigField("String", "SUPABASE_TEST_SERVICE_KEY", "\"$supabaseTestServiceKey\"")
```

### 3.2 Test User Lifecycle

**Sequence Diagram**:

```
┌─────────┐         ┌──────────────┐         ┌──────────┐         ┌──────────┐
│  Test   │         │ SupabaseAuth │         │  Room DB │         │  Cleanup │
└────┬────┘         └──────┬───────┘         └────┬─────┘         └────┬─────┘
     │                     │                       │                    │
     │ createTestUser()    │                       │                    │
     │────────────────────>│                       │                    │
     │                     │                       │                    │
     │                     │ POST /auth/v1/signup  │                    │
     │                     │──────────────────────>│                    │
     │                     │                       │                    │
     │ Session returned    │                       │                    │
     │<────────────────────│                       │                    │
     │                     │                       │                    │
     │ insertTestData()    │                       │                    │
     │────────────────────────────────────────────>│                    │
     │                     │                       │                    │
     │ runTestAssertions() │                       │                    │
     │────────────────────────────────────────────>│                    │
     │                     │                       │                    │
     │ cleanup()           │                       │                    │
     │─────────────────────────────────────────────────────────────────>│
     │                     │                       │                    │
     │                     │ DELETE /admin/users   │ DELETE from Room   │
     │                     │──────────────────────>│───────────────────>│
     │                     │                       │                    │
     │ complete            │                       │                    │
     │<─────────────────────────────────────────────────────────────────│
```

### 3.3 Data Cleanup Strategy

**Approach**: Per-test-run cleanup using Supabase Admin API

```kotlin
abstract class SupabaseInstrumentedTest {
    
    protected lateinit var supabase: SupabaseClient
    protected lateinit var testUserId: String
    protected val testIdentifier = "test-${System.currentTimeMillis()}"
    
    @Before
    fun setup() {
        supabase = createSupabaseClient(
            BuildConfig.SUPABASE_TEST_URL,
            BuildConfig.SUPABASE_TEST_ANON_KEY
        )
    }
    
    @After
    fun cleanup() = runBlocking {
        // 1. Delete test user from Supabase Auth
        if (::testUserId.isInitialized) {
            deleteTestUser(testUserId)
        }
        
        // 2. Delete synced data via RPC (if any)
        cleanupSyncedData(testIdentifier)
    }
    
    private suspend fun deleteTestUser(userId: String) {
        val serviceKeyClient = createSupabaseClient(
            BuildConfig.SUPABASE_TEST_URL,
            BuildConfig.SUPABASE_TEST_SERVICE_KEY
        )
        
        serviceKeyClient.auth.admin.deleteUser(userId)
    }
    
    private suspend fun cleanupSyncedData(testIdentifier: String) {
        // Use Supabase RPC function for bulk cleanup
        supabase.postgrest
            .from("pacientes")
            .delete()
            .eq("optica_id", testIdentifier)
    }
}
```

**Decision**: Timestamp-suffixed test identifiers for isolation.

**Rationale**:
- Prevents collisions between concurrent test runs
- Easy to identify and clean up orphaned test data
- No need for complex transaction management

### 3.4 Connection Handling

```kotlin
class SupabaseAuthTest : SupabaseInstrumentedTest() {
    
    @Test
    fun register_new_user_returns_session() = runBlocking {
        val email = "test-${System.currentTimeMillis()}@example.com"
        val password = "TestPassword123!"
        
        // Act
        val result = supabase.auth.signUpWith(
            Email,
            credentials {
                email = email
                password = password
            }
        )
        
        // Assert
        assertNotNull(result.session)
        assertNotNull(result.user)
        testUserId = result.user!!.id
    }
    
    @Test
    fun login_with_wrong_password_throws_error() = runBlocking {
        // Arrange
        val email = "test-${System.currentTimeMillis()}@example.com"
        val password = "TestPassword123!"
        
        supabase.auth.signUpWith(Email, credentials {
            email = email
            password = password
        })
        
        // Act & Assert
        val exception = assertFailsWith<AuthException> {
            supabase.auth.signInWith(Password, credentials {
                email = email
                password = "wrong-password"
            })
        }
        
        assertEquals("Invalid login credentials", exception.message)
    }
}
```

---

## 4. CI Pipeline Design

### 4.1 GitHub Actions Workflow Structure

```yaml
# .github/workflows/android-ci.yml
name: Android CI

on:
  push:
    branches: [main, version-saas]
  pull_request:
    branches: [main, version-saas]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run unit tests
        run: ./gradlew :optoapp:testDebugUnitTest
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: unit-test-results
          path: optoapp/build/test-results/testDebugUnitTest/

  android-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'push'  # Skip on PRs
    timeout-minutes: 20
    strategy:
      matrix:
        api-level: [34]
    steps:
      - uses: actions/checkout@v4
      
      - name: Enable KVM group perms
        run: |
          echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' | \
            sudo tee /etc/udev/rules.d/99-kvm4all.rules
          sudo udevadm control --reload-rules
          sudo udevadm trigger --name-match=kvm
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Configure Supabase test credentials
        run: |
          echo "supabase.test.url=${{ secrets.SUPABASE_TEST_URL }}" >> local.properties
          echo "supabase.test.anon.key=${{ secrets.SUPABASE_TEST_ANON_KEY }}" >> local.properties
          echo "supabase.test.service.key=${{ secrets.SUPABASE_TEST_SERVICE_KEY }}" >> local.properties
      
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          target: google_apis
          arch: x86_64
          profile: pixel_6
          script: ./gradlew :optoapp:connectedDebugAndroidTest
      
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: android-test-results
          path: |
            optoapp/build/outputs/androidTest-results/
            optoapp/build/reports/androidTests/
      
      - name: Upload emulator logs
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: emulator-logs
          path: |
            ${{ github.workspace }}/android-emulator-logcat.txt
```

### 4.2 Credential Injection

**GitHub Secrets Required**:
| Secret | Purpose |
|--------|---------|
| `SUPABASE_TEST_URL` | Test Supabase project URL |
| `SUPABASE_TEST_ANON_KEY` | Anonymous key for client operations |
| `SUPABASE_TEST_SERVICE_KEY` | Service role key for cleanup (admin operations) |

**Security**:
- Credentials injected at runtime via `local.properties`
- Never logged or printed
- `local.properties` is gitignored
- Service key only used in `@After` cleanup phase

### 4.3 Parallel Execution Strategy

```
┌──────────────────┐    ┌──────────────────┐
│  unit-tests job  │    │ android-test job │
│  (runs on PRs)   │    │ (main only)      │
├──────────────────┤    ├──────────────────┤
│ testDebugUnitTest│    │ Emulator boot    │
│ ~5 minutes       │    │ ~5 minutes       │
│                  │    │                  │
│                  │    │ connectedAndroid │
│                  │    │ ~15 minutes      │
└──────────────────┘    └──────────────────┘
```

**Decision**: Run unit tests and androidTest in separate jobs.

**Rationale**:
- Unit tests are faster, run on every PR
- androidTest is slow (emulator boot), only on main
- Independent failure domains
- Can retry androidTest without re-running unit tests

---

## 5. File Structure

```
optoapp/
├── src/
│   ├── main/
│   │   └── java/com/example/optoapp/
│   │       └── testing/
│   │           └── TestTags.kt                    # Shared test tag constants
│   │
│   └── androidTest/
│       └── java/com/example/optoapp/
│           ├── ui/
│           │   ├── LoginFlowTest.kt
│           │   ├── PacienteFlowTest.kt
│           │   ├── EvaluacionFlowTest.kt
│           │   ├── DispensacionFlowTest.kt
│           │   └── NavigationTest.kt
│           ├── data/
│           │   ├── SupabaseAuthTest.kt
│           │   ├── SyncFlowTest.kt
│           │   └── OfflineSyncTest.kt
│           ├── rules/
│           │   └── TestDatabaseRule.kt            # Custom JUnit rule for Room
│           ├── fakes/
│           │   ├── FakeSupabaseClient.kt
│           │   ├── FakeOptoRepository.kt
│           │   └── FakeSessionManager.kt
│           └── factories/
│               └── TestDataFactory.kt             # Inline data factories
```

---

## 6. Dependencies

### 6.1 Build Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Existing androidTest dependencies (keep)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // Room testing (keep)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    
    // NEW: IdlingResource for async operations
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.5.1")
    
    // NEW: Test rules
    androidTestImplementation("androidx.test:rules:1.5.0")
}
```

### 6.2 Production Code Changes

Minimal changes required:

1. **Add `@TestTag` annotations** to key composables (4 screens)
2. **Export `TestTags.kt`** from main source set
3. **Add BuildConfig fields** for test Supabase credentials

No business logic changes. No production dependencies added.

---

## 7. Sequence Diagrams

### 7.1 Compose UI Test Flow

```
┌─────────┐    ┌──────────────┐    ┌──────────┐    ┌─────────────┐    ┌──────────┐
│  Test   │    │ ComposeRule  │    │  Room DB │    │ FakeRepo    │    │  Screen   │
└────┬────┘    └──────┬───────┘    └────┬─────┘    └──────┬──────┘    └────┬─────┘
     │                │                  │                 │                 │
     │ createDb()     │                  │                 │                 │
     │───────────────>│                  │                 │                 │
     │                │                  │                 │                 │
     │                │ inMemoryBuilder()│                 │                 │
     │                │─────────────────>│                 │                 │
     │                │                  │                 │                 │
     │ launchApp()    │                  │                 │                 │
     │───────────────>│                  │                 │                 │
     │                │                  │                 │                 │
     │                │ startActivity()  │                 │                 │
     │                │──────────────────────────────────────────────────────>│
     │                │                  │                 │                 │
     │ clickButton()  │                  │                 │                 │
     │───────────────>│                  │                 │                 │
     │                │                  │                 │                 │
     │                │ performClick()   │                 │                 │
     │                │──────────────────────────────────────────────────────>│
     │                │                  │                 │                 │
     │                │                  │ insert()        │                 │
     │                │                  │────────────────>│                 │
     │                │                  │                 │                 │
     │ waitUntil()    │                  │                 │                 │
     │───────────────>│                  │                 │                 │
     │                │                  │                 │                 │
     │                │ poll condition   │                 │                 │
     │                │<─────────────────────────────────────────────────────│
     │                │                  │                 │                 │
     │ assertVisible()│                  │                 │                 │
     │───────────────>│                  │                 │                 │
     │                │                  │                 │                 │
     │ closeDb()      │                  │                 │                 │
     │───────────────>│                  │                 │                 │
     │                │                  │                 │                 │
     │                │ close()          │                 │                 │
     │                │─────────────────>│                 │                 │
```

### 7.2 Supabase Sync Test Flow

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌───────────┐    ┌─────────┐
│  Test   │    │ Room DB  │    │  SyncMgr │    │ Supabase  │    │ Cleanup │
└────┬────┘    └────┬─────┘    └────┬─────┘    └─────┬─────┘    └────┬────┘
     │              │                │                 │               │
     │ insert()     │                │                 │               │
     │─────────────>│                │                 │               │
     │              │                │                 │               │
     │ triggerSync()│                │                 │               │
     │──────────────────────────────>│                 │               │
     │              │                │                 │               │
     │              │                │ uploadPending() │               │
     │              │                │────────────────>│               │
     │              │                │                 │               │
     │              │                │ POST /rest/v1/  │               │
     │              │                │────────────────────────────────>│
     │              │                │                 │               │
     │              │                │ 201 Created     │               │
     │              │                │<────────────────────────────────│
     │              │                │                 │               │
     │ verifySynced()               │                 │               │
     │────────────────────────────────────────────────>│               │
     │              │                │                 │               │
     │              │                │                 │ SELECT count  │
     │              │                │                 │──────────────>│
     │              │                │                 │               │
     │              │                │                 │ count = 1     │
     │              │                │                 │<──────────────│
     │              │                │                 │               │
     │ cleanup()    │                │                 │               │
     │────────────────────────────────────────────────────────────────>│
     │              │                │                 │               │
     │              │ DELETE         │ DELETE          │ DELETE user   │
     │              │───────────────>│────────────────>│──────────────>│
```

---

## 8. Architecture Decisions

### 8.1 Hilt in Tests

**Decision**: Phase 1 uses direct instantiation without Hilt. Phase 2 may add `@UninstallModules`.

**Rationale**:
- Existing `DaoTest.kt` doesn't use Hilt
- Hilt adds complexity to test setup
- Direct instantiation is simpler for Compose UI tests
- Can migrate gradually as needed

### 8.2 Test Data Strategy

**Decision**: Inline factories, no external seed scripts.

**Rationale**:
- Tests are self-contained and reproducible
- No file I/O dependencies
- Follows existing `DaoTest.kt` pattern
- Faster execution

### 8.3 Supabase Test Project

**Decision**: Separate free-tier project, not shared with dev/prod.

**Rationale**:
- Complete isolation from production data
- Can delete users/tables freely
- Free tier is sufficient for test load
- RLS policies can be tested independently

### 8.4 CI Execution Strategy

**Decision**: Skip androidTest on PRs, run only on main/version-saas pushes.

**Rationale**:
- Emulator boot adds 5-10 minutes to CI
- PR feedback should be fast (<5 minutes)
- Main branch gets full test coverage
- Can manually trigger androidTest on PRs if needed

---

## 9. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Flaky async tests | Medium | High | Use `waitUntil()` with generous timeouts; add IdlingResource |
| Emulator timeout in CI | Medium | High | Use `reactivecircus/android-emulator-runner`; 20-min budget |
| Test data pollution | Low | Medium | Timestamp-suffixed identifiers; aggressive cleanup in `@After` |
| Supabase credentials leak | Low | Critical | GitHub Secrets only; never commit `local.properties` |
| Hilt module conflicts | Medium | Medium | Phase 1 avoids Hilt; Phase 2 uses `@UninstallModules` |
| Test execution time >15min | Medium | Medium | Parallel test execution; split into multiple test classes |

---

## 10. Rollback Plan

All changes are **additive**:
- Test files in `androidTest/` don't affect production APK
- `@TestTag` annotations are no-op in production
- BuildConfig fields are unused if tests don't run

**If tests break CI**:
1. Temporarily comment out `android-test` job in `android-ci.yml`
2. Keep test files on disk for local execution
3. Fix tests locally, re-enable CI job when ready

**No production code rollback needed**.

---

## 11. Success Criteria

- [ ] 4 Compose UI flow tests pass locally on emulator
- [ ] 3 Supabase instrumented tests pass with real Supabase project
- [ ] `connectedAndroidTest` step runs in CI on main branch
- [ ] Total androidTest execution time < 15 minutes
- [ ] Zero production code changes beyond test infrastructure
- [ ] Test coverage report generated (JaCoCo for androidTest)

---

## 12. Implementation Phases

### Phase 1: Test Infrastructure (Week 1)
- [ ] Create `TestTags.kt` with all constants
- [ ] Create `TestDataFactory.kt`
- [ ] Create `TestDatabaseRule.kt`
- [ ] Add BuildConfig fields for test credentials
- [ ] Update `build.gradle.kts` with test dependencies

### Phase 2: Compose UI Tests (Week 2)
- [ ] Add `@TestTag` annotations to LoginScreen
- [ ] Write `LoginFlowTest.kt`
- [ ] Add `@TestTag` annotations to NuevoPacienteScreen
- [ ] Write `PacienteFlowTest.kt`
- [ ] Add `@TestTag` annotations to NuevaEvaluacionScreen
- [ ] Write `EvaluacionFlowTest.kt`
- [ ] Add `@TestTag` annotations to NuevaDispensacionScreen
- [ ] Write `DispensacionFlowTest.kt`

### Phase 3: Supabase Tests (Week 3)
- [ ] Provision test Supabase project
- [ ] Create `SupabaseAuthTest.kt`
- [ ] Create `SyncFlowTest.kt`
- [ ] Create `OfflineSyncTest.kt`
- [ ] Implement cleanup logic

### Phase 4: CI Pipeline (Week 4)
- [ ] Add GitHub Secrets
- [ ] Update `android-ci.yml` with emulator step
- [ ] Configure artifact uploads
- [ ] Run full pipeline on main branch

---

## 13. Cross-Platform Impact

**Android Only**: This change has **zero web impact**.

- Web E2E tests are out of scope (future work)
- No Supabase schema changes required
- No API contract changes
- Test infrastructure is Android-specific (Compose, Room, androidTest)

---

## 14. Testing the Tests

Before merging, verify:

```bash
# Local execution on emulator
./gradlew :optoapp:connectedDebugAndroidTest

# Run specific test class
./gradlew :optoapp:connectedDebugAndroidTest \
  --tests "com.example.optoapp.ui.LoginFlowTest"

# Generate test report
./gradlew :optoapp:createDebugAndroidTestCoverageReport

# Verify BuildConfig fields are populated
./gradlew :optoapp:assembleDebugAndroidTest
# Inspect: optoapp/build/generated/source/buildConfig/debug/com/example/optoapp/BuildConfig.java
```

---

## 15. Notes for Implementation

1. **Start with LoginFlowTest** — it's the simplest and validates the test infrastructure
2. **Use `composeTestRule.onIdle()`** before assertions to ensure UI is stable
3. **Add logcat logging** in tests for debugging (`Log.d("TEST", "...")`)
4. **Run tests on physical device first** — faster iteration than emulator
5. **Document flaky tests** with `@FlakyTest` annotation for future stabilization

---

## Key Learnings

**What**: Comprehensive E2E test architecture for Android Compose + Supabase integration.

**Why**: Proposal requires verification of 4 P0 user flows end-to-end without relying solely on unit tests.

**Where**: `optoapp/src/androidTest/`, `optoapp/src/main/java/.../testing/`, `.github/workflows/android-ci.yml`.

**Learned**: 
- In-memory Room databases must use `allowMainThreadQueries()` for Compose tests
- Supabase test credentials require separate BuildConfig fields
- CI emulator setup needs KVM permissions on GitHub-hosted runners
- Test tags should live in main source for compile-time safety
