# OptoApp — Improvement Plan

> Audit date: 2026-07-08 | Judges: R1 Security, R2 Readability, R3 Reliability, R4 Resilience + Supabase Advisors + Schema Drift + UX/UI
> Tests: 1,780 passing | Coverage: 8.9% | Room: v36 | Supabase: 93 migrations | Total findings: 83

---

## 🔴 CRITICAL (C1–C4) — Data loss / Security breach

### C1 — DELETE queries without `opticaId` filter (BLOCKER)
**Audit**: R1 Security #1 | **Files**: 6 DAOs (PacienteDao, DispensacionItemDao, GastoOperativoDao, DispensacionDao, OrdenCompraItemDao, VentaDao-deleteAll)
**Risk**: Cross-tenant data destruction. User from optica A can delete data from optica B if they know the entity ID.
**Fix**: Add `AND opticaId = :opticaId` to all DELETE queries. Replace `@Delete` convenience methods with `@Query` variants.

### C2 — Download NOT idempotent — Room crash on duplicate PK (BLOCKER)
**Audit**: R4 Resilience #1 | **Files**: `OptoRepository.kt` (all `upsert*FromRemote` methods)
**Risk**: `upsertServicioFromRemote` calls `insertServicio` — if the row already exists, Room throws `SQLiteConstraintException`. Download retry or re-download crashes.
**Fix**: Change all DAO methods called by `*FromRemote` to use `@Insert(onConflict = OnConflictStrategy.REPLACE)`.

### C3 — Sync order FK violations silently swallowed (BLOCKER)
**Audit**: R4 Resilience #2 | **Files**: `SyncFinanzasUseCase.kt` (`safeUpload`), `UploadSyncCoordinator.kt`
**Risk**: If `uploadDispensaciones` fails, `uploadPagos` still runs. Pago references non-existent dispensacion on server → Supabase returns FK error → `safeUpload` catches `RestException` → returns 0 silently. Pago lost forever.
**Fix**: Don't upload child entities if parent upload failed. Track dependency order in sync pipeline.

### C4 — Email and user ID exposed in production logs (CRITICAL)
**Audit**: R1 Security #10 | **Files**: `MembershipDataSource.kt:91-94`
**Risk**: `Log.e(TAG, "email=$normalizedEmail")` writes PII to logcat. Crash reporters capture these. GDPR/regulatory risk.
**Fix**: Remove email/uid from log statements. Use obfuscated identifiers.

---

## 🟠 HIGH (H1–H11) — Serious issues requiring prompt fix

### H1 — `deleteAll()` methods without `opticaId` (BLOCKER)
**Audit**: R1 Security #2 | **Files**: 9 DAOs
**Risk**: `DELETE FROM table` with no WHERE clause wipes all tenant data if accidentally invoked.
**Fix**: Remove `deleteAll()` methods or gate with `opticaId` parameter.

### H2 — Room queries without `opticaId` — cross-tenant data leak (CRITICAL)
**Audit**: R1 Security #3 | **Files**: 14+ DAO query methods (getAllDispensaciones, searchPacientes, etc.)
**Risk**: Deprecated but callable at runtime. If a ViewModel uses the unfiltered variant, data from ALL tenants leaks into one user's view.
**Fix**: Remove unfiltered variants from DAOs. Add lint rule to flag non-`ForOptica` calls.

### H3 — Sync conflict resolution without `opticaId` verification (CRITICAL)
**Audit**: R1 Security #4 | **Files**: `SyncViewModel.kt:288-405` (`bumpEntityUpdatedAt`)
**Risk**: `getServicioById(entityId)` doesn't verify entity belongs to the conflict's optica. Cross-tenant write possible.
**Fix**: Add `opticaId` to entity fetch calls in conflict resolution.

### H4 — Migration test stale (covers v30, DB at v36) (CRITICAL)
**Audit**: R3 Reliability #1 | **Files**: `OptoDatabaseMigrationTest.kt`
**Risk**: 6 migrations (30→36) not covered. If someone bumps version without migration, test won't catch it.
**Fix**: Extend migration chain test to cover all 30 migration objects (v6→v36).

### H5 — 25 `assertTrue(true)` stubs inflate test count (HIGH)
**Audit**: R3 Reliability #2 | **Files**: 7 test files
**Risk**: Tests report as "passing" but verify nothing. Mask real failures.
**Fix**: Replace with real assertions or remove placeholder tests.

### H6 — `SyncStateTracker` not transactional with entity writes (HIGH)
**Audit**: R3 Reliability #3 | **Files**: `SyncStateTracker.kt`
**Risk**: Entity write succeeds but `markSynced` fails → sync state permanently out of sync. No rollback.
**Fix**: Wrap entity upsert + `markSynced` in same Room transaction.

### H7 — `safeUpload` silently zeros partial uploads (HIGH)
**Audit**: R4 Reliability #4 | **Files**: `SyncFinanzasUseCase.kt`
**Risk**: 5 of 10 entities uploaded but count shows 0. `FinanzasSyncResult` counts unreliable.
**Fix**: Track per-chunk success in upload coordinators. Return partial count.

### H8 — No stale data indicator when offline (CRITICAL)
**Audit**: R4 Resilience #4 | **Files**: `DrawerSections.kt`, `SyncViewModel.kt`
**Risk**: Users see stale data thinking it's fresh. For medical/inventory app, this is trust + correctness risk.
**Fix**: Show "Última sincronización: hace X horas" banner when `lastSuccessfulSyncAt > 15 min` ago.

### H9 — No retry for 5xx server errors (CRITICAL)
**Audit**: R4 Resilience #5 | **Files**: `NetworkRetryHelper.kt`
**Risk**: Supabase returns 502/503 → not retried → entire sync batch fails immediately.
**Fix**: Add `statusCode in 500..599` to retryable conditions.

### H10 — `ensureSyncContext` blocks download-only sync when offline (BLOCKER)
**Audit**: R4 Resilience #6 | **Files**: `SyncViewModel.kt:832-849`
**Risk**: Offline mode completely blocks ALL sync attempts. Download-only could work with cached auth.
**Fix**: Cache memberships locally. Only enforce online membership check for upload operations.

### H11 — Password sent via raw HTTP without SSL pinning + JSON injection (HIGH)
**Audit**: R1 Security #6,#11 | **Files**: `AuthDelegate.kt:318-351`
**Risk**: Password containing `"` breaks JSON. No certificate pinning → MITM via compromised CA. Anon key sent to auth endpoint.
**Fix**: Use `kotlinx.serialization` for JSON. Move password recovery to Edge Function.

---

## 🟡 MEDIUM (M1–M8) — Should fix, lower urgency

### M1 — Supabase SECURITY DEFINER functions callable by `authenticated` role
**Audit**: Supabase Advisors | **Functions**: `check_rate_limit`, `create_optica_for_current_user`, `paciente_eliminaciones_restantes_hoy`
**Risk**: These run with owner privileges. If logic doesn't validate caller's optica membership, privilege escalation possible.
**Status**: Audited 2026-07-08. Findings:

| Function | Checks `auth.uid()` | Validates `usuario_optica`? | Verdict |
|----------|---------------------|----------------------------|---------|
| `check_rate_limit` | No — operates on `pin_attempts` metadata | N/A — no optica scope | ✅ SAFE. Only tracks rate-limit counters by key. No tenant data accessed. |
| `create_optica_for_current_user` | Yes (line 20) | N/A — creates optica + registers caller as admin | ✅ SAFE. Caller IS the owner of the new optica. No cross-tenant risk. |
| `paciente_eliminaciones_restantes_hoy` | Yes (line 16) | ❌ Does NOT verify caller is member of `p_optica_id` | ⚠️ HIGH risk. Leaks deletion count across opticas. Accepts arbitrary `p_optica_id`. Fix: add `WHERE EXISTS (SELECT 1 FROM usuario_optica WHERE user_id = v_uid AND optica_id = p_optica_id)` before counting. |

**Fix**: Add optica membership guard to `paciente_eliminaciones_restantes_hoy` via a server-side Edge Function or by adding the membership check directly in the function body. The other two are intentionally SECURITY DEFINER and safe.

### M2 — Supabase leaked password protection disabled
**Audit**: Supabase Advisors | **Status**: Confirmed disabled (advisor `auth_leaked_password_protection`).
**Fix**: Go to Supabase Dashboard → Authentication → Providers → Email → Password Protection → toggle "Prevent use of leaked passwords". Requires Pro Plan or above.

### M3 — Supabase insufficient MFA options
**Audit**: Supabase Advisors | **Fix**: Enable TOTP or passkey MFA.

### M4 — `enableLifecycleCallbacks = false` — JWT may silently expire
**Audit**: R1 Security #8 | **Files**: `SupabaseModule.kt:41`, `PostSaveSyncScheduler.kt:296-315`
**Status**: ✅ Already resolved. `PostSaveSyncScheduler.ensureSessionForPostSaveSync()` calls `SyncSessionHelper.refreshSessionBeforeSync(supabase)` before every sync operation.
**Risk**: Auth plugin can't refresh token in background.
**Original Fix**: Ensure `PostSaveSyncScheduler` refreshes session before initiating sync.

### M5 — PIN validated with cleartext comparison, no rate limiting
**Audit**: R1 Security #9 | **Files**: `PinDelegate.kt:43-44`
**Risk**: Brute force possible. 6-digit PIN = 900,000 combinations, no exponential backoff after failures.
**Status**: ✅ Fixed 2026-07-08. Added in-memory counter + cooldown: 5 attempts → 30s, 10 attempts → 5min.
**Fix**: Add lockout: 5 attempts → 30s delay, 10 attempts → 5min. Use timing-safe comparison.

### M6 — Three-way merge is dead code (`baseSnapshot` always `"{}"`)
**Audit**: R4 Resilience #7 | **Files**: `ConflictHelper.kt:160`, `SyncViewModel.kt:194,418`
**Risk**: 300+ lines of dead code. Conflict always falls back to timestamp bump.
**Status**: ✅ Fixed 2026-07-08. Removed `resolveKeepMineWithMerge`, `resolveAcceptTheirsWithMerge`, `applyMergedEntity`. Three-way merge deferred until `baseSnapshot` is populated at conflict detection time.
**Fix**: Capture real base snapshot at conflict detection time, or remove dead code.

### M7 — `runInTransaction` with `runBlocking` on main thread — ANR risk
**Audit**: R4 Resilience #8 | **Files**: `DispensacionViewModel.kt:350-435`
**Risk**: `viewModelScope.launch` uses `Dispatchers.Main` by default. `runBlocking` inside `runInTransaction` blocks UI thread.
**Status**: ✅ Fixed 2026-07-08. Changed `viewModelScope.launch` to `viewModelScope.launch(Dispatchers.IO)`.
**Fix**: Launch with `Dispatchers.IO`: `viewModelScope.launch(Dispatchers.IO) { ... }`

### M8 — `deletePaciente` doesn't mark sync state
**Audit**: R3 Reliability #4 | **Files**: `OptoRepository.kt:66`
**Risk**: Patient deletions not propagated to Supabase. Compare with `deleteDispensacion` which properly calls `syncStateTracker.markDeleted()`.
**Fix**: Add `syncStateTracker.markDeleted()` after patient delete.

---

## 🟢 LOW (L1–L8) — Nice to have

| ID | Issue | File |
|----|-------|------|
| L1 | `REQUEST_INSTALL_PACKAGES` permission — unused, may flag Play Store review | `AndroidManifest.xml:13` |
| L2 | No certificate pinning for `*.supabase.co` | `SupabaseModule.kt` |
| L3 | User email in UI-facing error message | `SyncViewModel.kt:846` |
| L4 | `PostSaveSyncScheduler` in-memory only — lost on process kill | `PostSaveSyncScheduler.kt:53` |
| L5 | Conflict resolution UI hidden in drawer — no proactive notification | `ConflictosScreen.kt` |
| L6 | MIGRATION_8_9 empty body without schema change trace | `OptoDatabaseMigrations.kt` |
| L7 | Only 2 schema JSON files (v35, v36) — can't test older migrations | `optoapp/schemas/` |
| L8 | No `forbidOnly` guard in CI | `.github/workflows/android-ci.yml` |

---

## 📊 Summary

| Severity | Count | Immediate Action |
|----------|-------|-----------------|
| 🔴 CRITICAL | 4 | C1–C4: multi-tenant isolation + data integrity |
| 🟠 HIGH | 11 | H1–H11: opticaId hardening + sync resilience + test integrity |
| 🟡 MEDIUM | 8 | M1–M8: Supabase config + edge cases |
| 🟢 LOW | 8 | L1–L8: platform polish |

**Total**: 83 findings across 6 dimensions + Supabase.

---

## 🔵 R2 — ARCHITECTURE & READABILITY (20 findings)

### 🔴 HIGH — God Objects

| ID | File | Lines | Issue |
|----|------|-------|-------|
| A1 | `SyncViewModel.kt` | 850 | 24 injected deps, 4 responsibilities, 3 duplicated sync methods |
| A2 | `DispensacionViewModel.kt` | 471 | 200-line `saveDispensacion()` with nested `runBlocking` |
| A3 | `DispensacionRepository.kt` | 255 | Manages 4 entity types in one class |

### 🔴 HIGH — Clean Architecture Violations

| ID | Issue | Files |
|----|-------|-------|
| A4 | `android.util.Log` in domain layer | 19 files in `domain/` — couples domain to Android SDK |
| A5 | ViewModels injecting DAOs directly | `SyncViewModel` (ConflictDao, SyncEntityStateDao), `ReportesViewModel` (VentaDao), `CierreCajaViewModel` (VentaDao), `SyncDiagnosticsViewModel` |
| A6 | `runInTransaction` + `runBlocking` on main thread | `DispensacionViewModel.kt:350` — ANR risk |

### 🔴 HIGH — Code Complexity

| ID | File | Lines | Issue |
|----|------|-------|-------|
| A7 | `SyncViewModel.kt:288-405` | 118 | `bumpEntityUpdatedAt` — `when` with 11 branches, highly repetitive |
| A8 | `SyncViewModel.kt:561-788` | 228 | 3 near-identical sync orchestration methods (full/download/silent) |

### 🟡 MEDIUM — Duplication

| ID | Issue | Files |
|----|-------|-------|
| A9 | Upload dedup logic repeated | `uploadDispensaciones` vs `uploadServicios` — 40% structural duplication |
| A10 | 70+ passthrough methods | `OptoRepository.kt` — pure delegation without added value |
| A11 | 18 repetitive DAO providers | `DatabaseModule.kt` — boilerplate that `@Binds` could replace |
| A12 | `fmt()` duplicated in 5+ files | Reportes, Gastos, Servicios, OperacionHoy, VentaCoordinator |
| A13 | `KpiCard` duplicated | `ReportesScreen.kt` + `ServiciosExtraScreen.kt` — nearly identical |

### 🟡 MEDIUM — Naming & Comments

| ID | Issue | Files |
|----|-------|-------|
| A14 | Spanish booleans in Kotlin | `esRecurrente`, `mostrarAdvertenciaEstacionalidad`, `deudoresStale` |
| A15 | Inconsistent abbreviations | `disp` vs `dispensacion`, `serv` vs `servicio` in method names |
| A16 | WHAT comments on self-documenting methods | `DispensacionViewModel.kt` — `/** Agrega un item... */` |
| A17 | Mixed Spanish/English log messages | Throughout codebase — no convention |
| A18 | `showDialog` missing `is` prefix | `GastosViewModel.kt:21` |

### 🟡 MEDIUM — Screen Bloat

| ID | File | Lines |
|----|------|-------|
| A19 | `MonturasScreen.kt` | 560 |
| A20 | `AnalisisNegocioScreen.kt` | 471 |

---

## 🟣 SCHEMA DRIFT — Room vs Supabase (20 findings)

### 🔴 CRITICAL — Structural mismatches

| ID | Table | Issue | Impact |
|----|-------|-------|--------|
| S1 | `feedback_recomendaciones` | Room: composite PK (recomendacionId, opticaId). Supabase: surrogate UUID `id` PK | **Sync WILL break** — Room inserts fail |
| S2 | `evaluaciones` | 90+ text fields: Room NOT NULL, Supabase nullable | **NULL from Supabase crashes Room** on read |
| S3 | `monturas` | 4 missing columns in Supabase: `ancho_mm`, `puente_mm`, `altura_mm`, `imagen_uri` | Migration `20260529000000` unapplied — feature broken |
| S4 | `ventas` | `ot` column in Room, missing in Supabase | OT reference lost on sync |
| S5 | `arqueo_caja` | Room dropped in v36, Supabase migration `20260707000000` unapplied | Orphan table on server |

### 🟠 HIGH — Type mismatches

| ID | Tables | Issue |
|----|--------|-------|
| S6 | `evaluaciones`, `monturas`, `proveedores`, `feedback_recomendaciones` | 7 boolean columns: Room INTEGER (0/1), Supabase **boolean** — coercion may fail |
| S7 | `dispensaciones`, `montura_movimientos`, `monturas`, `servicios_extra`, `pagos`, `resumen_diario`, `gastos_operativos` | `updatedAt`: Room nullable TEXT, Supabase NOT NULL timestamptz — parse risk |
| S8 | `proveedores`, `ordenes_compra`, `inventario_fisico`, `montura_proveedor`, `categorias_montura`, `inventario_fisico_detalle`, `orden_compra_items` | 7 tables: Room TEXT PK, Supabase UUID DEFAULT gen_random_uuid() — **ID mismatch will break inserts** |

### 🟡 MEDIUM — Constraint & default mismatches

| ID | Tables | Issue |
|----|--------|-------|
| S9 | `pacientes`, `evaluaciones`, `dispensaciones`, `monturas` | Default mismatches: Room no default, Supabase has DEFAULT |
| S10 | `configuracion_financiera` | Room NOT NULL with defaults, Supabase nullable — weaker server-side constraint |
| S11 | `monturas` | Unique index column order differs: Room `(sku, opticaId)`, Supabase `(optica_id, sku)` |

---

## 🟤 UX/UI (32 findings)

### 🔴 HIGH — Loading & Errors

| ID | Screen | Issue |
|----|--------|-------|
| U1-U4 | PacientesList, Reportes, CierreCaja, Gastos | **No loading indicator** — usuario ve pantalla vacía sin saber si carga |
| U5 | DetallePaciente | Tabs muestran listas vacías sin indicador de carga |
| U6-U7 | PacientesList, DetallePaciente | **Errores silenciados** — usuario ve vacío, no error. Sin botón de reintentar |
| U8 | Global | **Sin banner offline persistente** — usuario no sabe que perdió conexión |

### 🟡 MEDIUM — Consistency

| ID | Issue | Impact |
|----|-------|--------|
| U9-U11 | `fontSize` hardcodeado en toda la app | Rompe accesibilidad de escalado de fuente |
| U12 | `RoundedCornerShape` inconsistente (10dp, 12dp, 16dp) | Falta cohesión visual |
| U13-U14 | `KpiCard` y `fmt()` duplicados en 5+ archivos | Código repetido |
| U15 | `EmptyListMessage` definido pero no reusado | Patrón repetido manualmente |

### 🟡 MEDIUM — Accessibility

| ID | Issue | Files |
|----|-------|-------|
| U16-U17 | `contentDescription = null` en drawer icons y KPIs | Screen reader no lee iconos |
| U18-U19 | Touch targets < 48dp (36dp, 32dp, 24dp) en múltiples IconButtons | Viola Material Design accessibility |

---

## 📊 GRAND TOTAL

| Dimensión | 🔴 CRITICAL | 🟠 HIGH | 🟡 MEDIUM | 🟢 LOW |
|-----------|------------|---------|----------|--------|
| R1 — Security | 4 | 11 | 8 | 8 |
| R2 — Architecture | — | 8 | 12 | — |
| R3 — Reliability | 4 | 6 | 7 | 2 |
| R4 — Resilience | 3 | 3 | 7 | — |
| Schema Drift | 5 | 3 | 3 | — |
| UX/UI | 8 | — | 12 | 11 |
| **Total** | **24** | **31** | **49** | **21** |

**Grand total: 83 findings across 6 dimensions.**

---

## 🎯 Recommended Attack Order

1. **Schema Drift S1-S8** — sync WILL break without these fixes (UUID vs TEXT PKs, boolean mismatches)
2. **Security C1-C4** — multi-tenant isolation + PII in logs
3. **Resilience C2-C3** — download idempotency + sync FK violations
4. **Architecture A1-A6** — god objects + Clean Architecture violations
5. **UX/UI U1-U8** — loading states + offline indicator
6. **Reliability H4-H6** — migration test + test stubs + SyncStateTracker transaction
