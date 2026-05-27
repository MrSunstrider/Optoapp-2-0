# Exploration: Android E2E Testing Setup

## Current State

### Test Architecture Summary

**Unit Tests (Robolectric)** - `optoapp/src/test/`
- 67 test files covering ViewModels, Repositories, DAOs, Utils, Sync logic
- Uses `Room.inMemoryDatabaseBuilder()` for Room testing
- MockK for mocking dependencies (SyncStateTracker, PostSaveSyncScheduler)
- JaCoCo configured with 5% minimum coverage threshold
- Pattern: Tests mirror main source structure (`data/`, `viewmodel/`, `ui/`, `sync/`, `domain/`)

**Instrumented Tests (androidTest)** - `optoapp/src/androidTest/`
- 4 test files total:
  - `MigrationTest.kt` - Tests Room migrations 6→19 using real database file
  - `DaoTest.kt` - DAO CRUD operations with in-memory DB
  - `SecurityManagerInstrumentedTest.kt` - EncryptedSharedPreferences tests
  - `AuthViewModelNavTest.kt` - PIN navigation state integration test
- Uses `AndroidJUnit4` runner, `ApplicationProvider.getApplicationContext()`
- Pattern: Direct instantiation of classes under test (no Hilt injection)

**Test Dependencies** (from `build.gradle.kts`):
```kotlin
// Unit tests
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.androidx.test.core)
testImplementation(libs.mockk)
testImplementation("org.robolectric:robolectric:4.14.1")
testImplementation("androidx.work:work-testing:2.9.0")

// Instrumented tests
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.ui.test.junit4)
debugImplementation(libs.androidx.ui.test.manifest)
androidTestImplementation(libs.androidx.room.testing)
androidTestImplementation(libs.kotlinx.coroutines.test)
```

**CI Pipeline** (`.github/workflows/android-ci.yml`):
- Runs `testDebugUnitTest` on Ubuntu
- Runs `assembleDebug`
- **No androidTest execution in CI** (requires emulator/device)

### Key User Flows Identified (Priority Ordered)

1. **Login Flow** (Auth + PIN) - P0
   - Entry: `LoginScreen.kt` → `AuthViewModel.login()` / `loginWithGoogle()`
   - PIN setup: `CreatePinScreen` → `SecurityManager.savePin()`
   - PIN verification: `PinScreen` → `SecurityManager.verifyPin()`
   - Navigation guard in `MainActivity.OptoAppNavigation`
   - Deep link handling for OAuth callback

2. **Patient Creation Flow** - P0
   - `NuevoPacienteScreen.kt` → `PacienteViewModel.savePaciente()`
   - Triggers `PostSaveSyncScheduler.schedulePacientesSync()`
   - Room insert via `PacienteRepository.insertPaciente()`

3. **Evaluation Flow** (Form + Auto-Diagnosis) - P0
   - `NuevaEvaluacionScreen.kt` → `EvaluacionViewModel.saveEvaluacion()`
   - Auto-diagnosis: `DiagnosticoCalculator` (tested in `DiagnosticoCalculatorTest.kt`)
   - DIP parsing: `DipParser` (tested in `DipParserTest.kt`)
   - Triggers `PostSaveSyncScheduler.scheduleHistorialSync()`

4. **Dispensación Flow** (Items + Payment) - P0
   - `NuevaDispensacionScreen.kt` → `DispensacionViewModel.saveDispensacion()`
   - OT suggestion, lens/mounture configuration
   - Payment registration via `insertPago()`
   - Triggers `PostSaveSyncScheduler.scheduleFinanzasSync()`

5. **Sync Flow** (Offline → Online) - P1
   - `SyncViewModel.performSilentSync()` on dashboard entry
   - `PostSaveSyncScheduler` for post-save background sync
   - `SyncGate` mutex prevents concurrent syncs
   - Entity-specific use cases: `SyncPacientesUseCase`, `SyncHistorialUseCase`, `SyncFinanzasUseCase`, `SyncInventarioUseCase`

6. **Navigation Flow** (Bottom Nav + Drawer) - P1
   - `MainDrawerScreen.kt` with nested `NavHost`
   - Role-based menu visibility via `AppRoles`
   - Drawer sections: Pacientes, Agenda, Dispensaciones, Monturas, Servicios, Configuración

### Supabase Client Setup

**`SupabaseModule.kt`** (Hilt Singleton):
```kotlin
@Provides
@Singleton
fun provideSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth) {
            host = BuildConfig.SUPABASE_REDIRECT_HOST
            scheme = BuildConfig.SUPABASE_REDIRECT_SCHEME
            defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
        }
    }
}
```

**BuildConfig fields** (from `local.properties`):
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_REDIRECT_SCHEME` (default: "optoapp")
- `SUPABASE_REDIRECT_HOST` (default: "auth")

**No existing test infrastructure for Supabase integration** - all tests mock or avoid Supabase calls.

## Affected Areas

- `optoapp/src/androidTest/` - New E2E test files will be added here
- `optoapp/build.gradle.kts` - May need additional test dependencies (e.g., test rules, IdlingResources)
- `optoapp/src/main/java/com/example/optoapp/di/` - May need test-specific DI modules
- `.github/workflows/android-ci.yml` - Would need emulator setup for androidTest in CI
- `local.properties` - Would need test Supabase credentials for Level 2 tests

## Approaches

### Approach 1: Level 1 - Compose UI Tests (Android Native Only)

**Description**: Write instrumented tests using `androidx.compose.ui.test.junit4` that test UI flows with mocked/fake backend.

**Pros**:
- Fast execution (~seconds per test)
- No external dependencies (Supabase, network)
- Can run on CI with emulator
- Tests actual UI interactions, not just logic
- Existing dependency already in `build.gradle.kts`

**Cons**:
- Doesn't verify real backend integration
- Requires maintaining fake data
- May miss integration bugs between layers

**Effort**: Medium (2-3 days for core flows)

**Test Structure Example**:
```kotlin
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun loginWithValidCredentials_navigatesToPinScreen() {
        composeTestRule.apply {
            onNodeWithText("Correo electrónico").performTextImeAction("test@optica.com")
            onNodeWithText("Contraseña").performTextImeAction("password123")
            onNodeWithText("ENTRAR AL SISTEMA").performClick()
            waitForIdle()
            onNodeWithText("Crear PIN de seguridad").assertExists()
        }
    }
}
```

### Approach 2: Level 2 - Instrumented Tests with Real Supabase Backend

**Description**: Full E2E tests hitting a test Supabase instance (Android → Supabase).

**Pros**:
- Catches real integration bugs
- Validates RLS policies
- Tests network error handling
- Closest to production behavior

**Cons**:
- Requires dedicated test Supabase project
- Slower execution (network calls)
- Flaky tests possible (network issues)
- Test data cleanup complexity
- Cannot run in CI without Supabase credentials
- Security risk if test credentials leak

**Effort**: High (5-7 days including infrastructure)

**Infrastructure Needed**:
1. Test Supabase project (separate from dev/prod)
2. Test user accounts seeded in test project
3. Test data cleanup strategy (transactions, teardown)
4. Secure credential storage (GitHub Secrets for CI, `local.properties` for local)
5. Custom test runner with `@Rule` for Supabase setup/teardown

**Test Structure Example**:
```kotlin
@RunWith(AndroidJUnit4::class)
class LoginE2ETest {
    @get:Rule
    val supabaseTestRule = SupabaseTestRule(testProjectCredentials)
    
    @Test
    fun loginWithRealBackend_navigatesToMainScreen() {
        val viewModel = AuthViewModel(
            supabaseClient = supabaseTestRule.client,
            // ... other deps
        )
        viewModel.login("test@optica.com", "password123")
        // Assert AuthState.Success
    }
}
```

### Approach 3: Hybrid - Level 1 + Selective Level 2

**Description**: Start with Level 1 for all flows, add Level 2 only for critical authentication and sync flows.

**Pros**:
- Best of both worlds
- Fast iteration for most tests
- Critical paths verified end-to-end
- Manageable infrastructure cost

**Cons**:
- Two test suites to maintain
- Need to decide which flows warrant Level 2

**Effort**: Medium-High (4-5 days)

## Recommendation

**Start with Approach 1 (Level 1 - Compose UI Tests)**, then selectively add Level 2 tests for:
1. Authentication flow (login, OAuth, PIN)
2. Sync flow (offline → online)

**Rationale**:
- Team has zero E2E test experience - Level 1 provides faster learning curve
- Existing test patterns are unit-test focused; gradual evolution is safer
- Most bugs will be caught at UI level without Supabase complexity
- Level 2 infrastructure (test Supabase, CI setup) can be built incrementally
- Compose UI Test dependency already exists and is unused

**Phase 1** (Week 1): Level 1 tests for Login, Patient Creation, Evaluation
**Phase 2** (Week 2): Level 1 tests for Dispensación, Navigation
**Phase 3** (Week 3): Level 2 infrastructure + Auth + Sync tests

## Risks

1. **CI Complexity**: androidTest requires emulator, which adds 5-10 minutes to CI pipeline. May need to run only on `main` branch, not PRs.

2. **Test Flakiness**: UI tests are inherently flaky (timing, animations). Need proper `waitUntil*` patterns and `ComposeTestRule` usage.

3. **Hilt Injection in Tests**: Current androidTest files don't use Hilt. For ViewModels with complex dependencies, may need `HiltAndroidRule` and `@UninstallModules`.

4. **Test Data Isolation**: Tests must not interfere with each other. Need proper `@Before`/`@After` cleanup, especially for Room database.

5. **Supabase Test Project Cost**: If using Level 2, test project needs to be kept separate. Free tier may suffice but requires monitoring.

6. **Credential Security**: Test Supabase credentials must never be committed. Need GitHub Secrets + `local.properties` gitignore verification.

## Ready for Proposal

**Yes** - Ready to proceed to proposal phase.

**Recommendation to User**:
Start with Level 1 (Compose UI Tests) for the 4 core flows:
1. Login + PIN setup/verification
2. Patient creation
3. Evaluation with auto-diagnosis
4. Dispensación with payment

This gives immediate E2E coverage without Supabase infrastructure overhead. Level 2 can be added later for auth and sync flows once Level 1 is stable.

**Estimated Timeline**:
- Level 1 (4 flows): 3-4 days
- Level 2 (auth + sync): 2-3 days additional
- CI setup for androidTest: 1 day

**Total**: 5-7 days for comprehensive E2E coverage.
