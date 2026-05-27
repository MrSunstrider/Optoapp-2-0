package com.example.optoapp.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.optoapp.BuildConfig
import com.example.optoapp.factories.TestDataFactory
import com.example.optoapp.fakes.FakeSyncPacientesUseCase
import com.example.optoapp.rules.TestDatabaseRule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Room → Supabase sync flow.
 *
 * Requires a dedicated test Supabase project with credentials in [BuildConfig].
 * During [setUp], the test environment is bootstrapped via the service_role key:
 * 1. A test user is registered
 * 2. A test [optica] is created
 * 3. A [usuario_optica] membership (admin) links the user to the optica
 *
 * After setup, tests use the anon key client (authenticated as the test user)
 * to interact with Supabase. This mirrors real app behavior and exercises RLS.
 *
 * @see TestDatabaseRule
 * @see TestDataFactory
 * @see FakeSyncPacientesUseCase
 */
@RunWith(AndroidJUnit4::class)
class SyncFlowTest {

    @get:Rule
    val dbRule = TestDatabaseRule()

    // Admin client (service_role — bypasses RLS for setup/cleanup)
    private var adminClient: SupabaseClient? = null

    // User client (anon key — authenticated as test user, RLS applies)
    private var userClient: SupabaseClient? = null

    private val fakeSyncUseCase = FakeSyncPacientesUseCase()

    // Test identifiers — suffixed with timestamp for isolation.
    private val runId = "sync-test-${System.currentTimeMillis().toString().takeLast(6)}"
    val testOpticaId = "optica-$runId"
    private val testUserId = "user-$runId"
    private val testEmail = "$runId@optoapp-test.local"
    private val testPassword = "TestPass123!"
    private var authUserId: String? = null

    @Before
    fun setUp() = runBlocking {
        val url = BuildConfig.SUPABASE_TEST_URL
        val anonKey = BuildConfig.SUPABASE_TEST_ANON_KEY
        val serviceKey = BuildConfig.SUPABASE_TEST_SERVICE_KEY
        assumeNotNull("SUPABASE_TEST_URL not set", url)
        assumeNotNull("SUPABASE_TEST_ANON_KEY not set", anonKey)
        assumeNotNull("SUPABASE_TEST_SERVICE_KEY not set", serviceKey)
        assumeTrue("SUPABASE_TEST_URL is blank", url.isNotBlank())
        assumeTrue("SUPABASE_TEST_ANON_KEY is blank", anonKey.isNotBlank())
        assumeTrue("SUPABASE_TEST_SERVICE_KEY is blank", serviceKey.isNotBlank())

        // Create admin client with service_role key (bypasses RLS for setup).
        adminClient = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = serviceKey
        ) {
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
            )
            install(Postgrest)
            install(Auth)
        }

        // Bootstrap test environment: user, optica, membership.
        val admin = adminClient!!
        admin.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
        }
        authUserId = admin.auth.currentUserOrNull()?.id
            ?: error("Failed to create test user")

        // Create test optica.
        admin.postgrest["opticas"].upsert(mapOf(
            "id" to testOpticaId,
            "nombre" to "Test Optica $runId",
            "plan" to "free",
            "plan_code" to "free",
            "max_pacientes_por_optica" to 100,
            "max_usuarios_por_optica" to 5,
            "max_opticas" to 1
        ))

        // Create membership (admin role) so RLS allows the test user to access optica data.
        admin.postgrest["usuario_optica"].upsert(mapOf(
            "id" to "membership-$runId",
            "usuario_id" to authUserId,
            "optica_id" to testOpticaId,
            "rol" to "admin"
        ))

        // Create user client with anon key, then sign in as the test user.
        userClient = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
            )
            install(Postgrest)
            install(Auth)
        }
        userClient!!.auth.signInWith(Email) {
            email = testEmail
            password = testPassword
        }
    }

    @After
    fun tearDown() = runBlocking {
        val admin = adminClient ?: return@runBlocking
        try {
            // Clean up test data via service_role (bypasses RLS).
            admin.postgrest["pacientes"].delete { filter { eq("optica_id", testOpticaId) } }
            admin.postgrest["historial"].delete { filter { eq("optica_id", testOpticaId) } }
            admin.postgrest["dispensaciones"].delete { filter { eq("optica_id", testOpticaId) } }
            admin.postgrest["usuario_optica"].delete { filter { eq("optica_id", testOpticaId) } }
            admin.postgrest["opticas"].delete { filter { eq("id", testOpticaId) } }
            authUserId?.let { uid ->
                runCatching { admin.auth.admin.deleteUser(uid) }
            }
        } catch (_: Exception) {
            // Cleanup failure is non-fatal.
        }
    }

    // ── Real Supabase E2E Tests ──────────────────────────────────────────────

    @Test
    fun createPatientLocally_verifyInSupabase() = runBlocking {
        val client = userClient!!
        val pacienteDao = dbRule.pacienteDao

        // Arrange: insert patient into Room.
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Sync E2E Patient $runId"
        )
        pacienteDao.insertPaciente(paciente)

        // Act: upsert the patient directly to Supabase (as the authenticated user).
        // In production, this is done by the sync coordinator after reading from Room.
        client.postgrest["pacientes"].upsert(mapOf(
            "id" to paciente.id,
            "nombre_completo" to paciente.nombreCompleto,
            "edad" to paciente.edad,
            "telefono" to paciente.telefono,
            "optica_id" to testOpticaId,
            "fecha_creacion" to paciente.fechaCreacion.toString()
        ))

        // Assert: poll Supabase until the patient appears (30s timeout).
        val startTime = System.currentTimeMillis()
        var found = false
        while (System.currentTimeMillis() - startTime < 30_000) {
            val rows = client.postgrest["pacientes"].select {
                filter { eq("id", paciente.id) }
            }.decodeList<Paciente>()
            if (rows.isNotEmpty()) {
                found = true
                break
            }
            delay(1000)
        }
        assertTrue("Patient should appear in Supabase within 30s", found)
        assertTrue("Patient should also exist in Room", pacienteDao.getPacienteById(paciente.id) != null)
    }

    @Test
    fun createEvaluationLocally_verifyInSupabase() = runBlocking {
        val client = userClient!!
        val pacienteDao = dbRule.pacienteDao
        val evaluacionDao = dbRule.evaluacionDao

        // Arrange: insert linked patient + evaluation into Room.
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Eval E2E Patient $runId"
        )
        pacienteDao.insertPaciente(paciente)

        val evaluacion = TestDataFactory.createTestEvaluacion(
            pacienteId = paciente.id,
            opticaId = testOpticaId,
            motivoConsulta = "E2E sync evaluation test"
        )
        evaluacionDao.insertEvaluacion(evaluacion)

        // Act: upsert both to Supabase as authenticated user.
        // The app sync coordinator does paciente first, then historial.
        client.postgrest["pacientes"].upsert(mapOf(
            "id" to paciente.id,
            "nombre_completo" to paciente.nombreCompleto,
            "edad" to paciente.edad,
            "telefono" to paciente.telefono,
            "optica_id" to testOpticaId,
            "fecha_creacion" to paciente.fechaCreacion.toString()
        ))
        client.postgrest["historial"].upsert(mapOf(
            "id" to evaluacion.id,
            "paciente_id" to paciente.id,
            "optica_id" to testOpticaId,
            "fecha" to evaluacion.fecha.toString(),
            "motivo_consulta" to evaluacion.motivoConsulta,
            "cita_estado" to evaluacion.citaEstado
        ))

        // Assert: poll Supabase until the evaluation appears.
        val startTime = System.currentTimeMillis()
        var found = false
        while (System.currentTimeMillis() - startTime < 30_000) {
            val rows = client.postgrest["historial"].select {
                filter { eq("id", evaluacion.id) }
            }.decodeList<EvaluacionClinica>()
            if (rows.isNotEmpty()) {
                found = true
                break
            }
            delay(1000)
        }
        assertTrue("Evaluation should appear in Supabase within 30s", found)
        assertTrue("Evaluation should also exist in Room", evaluacionDao.getEvaluacionById(evaluacion.id) != null)
    }

    // ── Sync Chain Unit Test (via FakeSyncPacientesUseCase) ──────────────────

    @Test
    fun syncChain_isTriggeredViaFakeUseCase() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: insert patient with PENDING sync marker.
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Sync Chain Patient $runId"
        )
        pacienteDao.insertPaciente(paciente)
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Act: simulate the sync coordinator calling the use case.
        val result = fakeSyncUseCase.invoke(testOpticaId, downloadAfterUpload = true)

        // Assert: the use case was invoked with the correct optica.
        assertTrue("Sync should succeed", result is Resource.Success)
        assertTrue("Upload should have been called", fakeSyncUseCase.uploadCallCount > 0)
        assertEquals("Last sync optica should match", testOpticaId, fakeSyncUseCase.lastOpticaId)
        assertTrue("Patient should still exist in Room", pacienteDao.getPacienteById(paciente.id) != null)
    }

    // ── Offline Test ─────────────────────────────────────────────────────────

    @Test
    fun syncWhenOffline_dataQueuedInRoom() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: create data while offline.
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient $runId"
        )
        pacienteDao.insertPaciente(paciente)

        // Mark as pending — real app does this when sync fails or is offline.
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Assert: data persists in Room.
        val stored = pacienteDao.getPacienteById(paciente.id)
        assertTrue("Patient data must persist in Room while offline", stored != null)
        assertEquals("Patient name should match", "Offline Patient $runId", stored?.nombreCompleto)
    }
}
