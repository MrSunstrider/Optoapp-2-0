# Delta Spec: Critical Test Coverage

## ADDED Requirements

### Requirement: Web — cierre-caja pure functions

The system MUST test all pure functions in `cierre-caja.ts` that handle money normalization, period resolution, payment method mapping, and role-based cierre authorization.

| Function | Behavior | Edge Cases |
|----------|----------|------------|
| `normalizeMoney(v)` | Round to 2 decimals; return 0 for non-finite | NaN, Infinity, -Infinity, 0, 0.005 |
| `toCents(v)` | `Math.round(normalizeMoney(v) * 100)` | Negative amounts, fractional cents |
| `fromCents(c)` | `Math.round(c) / 100` | Negative cents, non-integer input |
| `mapMedioPago(raw)` | Map raw string to one of 4 medios | null, empty, whitespace, "yape", "plin", "transferencia", "tarjeta de credito" |
| `resolveCierrePeriodo(fecha)` | Return `{fecha, from, toExclusive}` for YYYY-MM-DD | Invalid month/day parsing |
| `canReadCierre(role)` | true unless "invitado" or "lectura" | Mixed case, whitespace padding |
| `canCloseCierre(role)` | true for admin/gerente/cajero | All other roles |
| `canOverrideCierre(role)` | true for admin/gerente only | cajero must be false |

#### Scenario: normalizeMoney handles non-finite values

- GIVEN input is NaN, Infinity, or -Infinity
- WHEN normalizeMoney is called
- THEN result MUST be 0

#### Scenario: normalizeMoney rounds correctly

- GIVEN input is 10.555
- WHEN normalizeMoney is called
- THEN result MUST be 10.56

#### Scenario: toCents converts dollars to integer cents

- GIVEN input is 10.50
- WHEN toCents is called
- THEN result MUST be 1050

#### Scenario: mapMedioPago identifies mobile payments

- GIVEN input is "Yape", "Plin", "transferencia", or "móvil"
- WHEN mapMedioPago is called
- THEN result MUST be "Móvil/Trans"

#### Scenario: mapMedioPago defaults to Efectivo for null/empty

- GIVEN input is null, "", or "   "
- WHEN mapMedioPago is called
- THEN result MUST be "Efectivo"

#### Scenario: canReadCierre rejects invited and read-only roles

- GIVEN role is "invitado" or "lectura" (any casing/whitespace)
- WHEN canReadCierre is called
- THEN result MUST be false

#### Scenario: canOverrideCierre excludes cajero

- GIVEN role is "cajero"
- WHEN canOverrideCierre is called
- THEN result MUST be false (only admin/gerente can override)

### Requirement: Web — roles authorization

The system MUST test all role-checking functions in `roles.ts` covering ALL role variants: admin, especialista, gerente, asesor, asesora, ventas, invitado, lectura, and unknown roles.

| Function | True For | False For |
|----------|----------|-----------|
| `canViewBiAndReports` | admin, especialista, gerente | asesor, asesora, ventas, invitado, lectura, any other |
| `canAccessModule(role, "pacientes")` | all except invitado | invitado |
| `canAccessModule(role, "servicios-varios")` | all except invitado | invitado |
| `canAccessModule(role, "reportes")` | admin, especialista, gerente | asesor, asesora, ventas, invitado, lectura |
| `canAccessModule(role, "estadisticas")` | same as reportes | same as reportes |
| `canAccessModule(role, other)` | always true | never |
| `canReadPacientes` | all except invitado | invitado |
| `canManagePacientes` | all except invitado | invitado |
| `canManageFiscalConfig` | admin, gerente | all others |
| `canDeletePaciente` | admin, gerente | all others |

#### Scenario: canViewBiAndReports rejects commercial roles

- GIVEN role is "asesor", "asesora", or "ventas"
- WHEN canViewBiAndReports is called
- THEN result MUST be false

#### Scenario: canAccessModule delegates correctly per module

- GIVEN role is "asesor" and module is "reportes"
- WHEN canAccessModule is called
- THEN result MUST be false (delegates to canViewBiAndReports)

#### Scenario: canAccessModule allows unknown modules

- GIVEN module is "configuracion" (not pacientes/servicios-varios/reportes/estadisticas)
- WHEN canAccessModule is called with any role
- THEN result MUST be true

#### Scenario: All role functions handle whitespace and mixed case

- GIVEN role is "  Admin  " or "GERENTE"
- WHEN any role function is called
- THEN result MUST match the normalized lowercase behavior

### Requirement: Web — permissions module access

The system MUST test config module access control, read-only detection, and internal role identification.

| Function | True For | False For |
|----------|----------|-----------|
| `canAccessConfigModule(_, "plan-admin")` | superadmin, staff, interno | all others |
| `canAccessConfigModule(_, "gestion-datos")` | admin, gerente | all others |
| `canAccessConfigModule(_, "fiscal")` | everyone | never |
| `canAccessConfigModule(_, "seguridad")` | all except invitado/lectura | invitado, lectura |
| `isReadOnlyRole` | invitado, lectura | all others |
| `isInternalRole` | superadmin, staff, interno | all others |

#### Scenario: fiscal module is always accessible

- GIVEN module is "fiscal" and role is any value
- WHEN canAccessConfigModule is called
- THEN result MUST be true

#### Scenario: plan-admin requires internal role

- GIVEN role is "admin" (not internal) and module is "plan-admin"
- WHEN canAccessConfigModule is called
- THEN result MUST be false

### Requirement: Web — rate limiting

The system MUST test `checkRateLimit` with window expiry, max attempts, and stateful tracking across calls.

| Scenario | Expected |
|----------|----------|
| First call for a key | allowed=true, remaining=4 |
| 5th call within window | allowed=true, remaining=0 |
| 6th call within window | allowed=false, remaining=0 |
| Call after window expires | allowed=true, remaining=4 (reset) |
| Different keys are independent | each key has own counter |

#### Scenario: rate limit resets after window expiry

- GIVEN a key has been called 5 times
- WHEN Date.now() exceeds entry.resetAt
- THEN next call MUST return allowed=true, remaining=4

#### Scenario: different keys do not interfere

- GIVEN key "A" has 3 attempts
- WHEN checkRateLimit("B") is called for the first time
- THEN result MUST be allowed=true, remaining=4

### Requirement: Web — reportes-financieros pure functions

The system MUST test `resolveRange` and `labelForPeriodo` for all period types.

| Periodo | labelForPeriodo | resolveRange behavior |
|---------|-----------------|----------------------|
| dia | "Día" | today 00:00 to tomorrow 00:00 |
| semana | "Semana" | Monday of current week to next Monday |
| mes | "Mes" | 1st of month to 1st of next month |
| anio | "Año" | Jan 1 to Jan 1 next year |

#### Scenario: resolveRange for semana starts on Monday

- GIVEN current day is Wednesday
- WHEN resolveRange("semana") is called
- THEN start MUST be the Monday of the current week

#### Scenario: resolveRange for semana handles Sunday as day 0

- GIVEN current day is Sunday (getDay() === 0)
- WHEN resolveRange("semana") is called
- THEN diffToMonday MUST be 6 (go back to previous Monday)

### Requirement: Web — inventario summary and mapping

The system MUST test `computeInventarioSummary`, `mapMontura`, and `soles` formatting.

| Function | Behavior | Edge Cases |
|----------|----------|------------|
| `computeInventarioSummary` | Aggregate stock, cost, sale value, alerts | Empty array, stockActual=0, stockActual <= stockMinimo |
| `mapMontura` | Map DB row to MonturaItem with defaults | null fields, empty strings, non-finite numbers |
| `soles(n)` | Format as "S/ X.XX" | 0, negative, fractional |

#### Scenario: computeInventarioSummary with empty array

- GIVEN items is []
- WHEN computeInventarioSummary is called
- THEN all numeric fields MUST be 0, listed MUST be 0

#### Scenario: mapMontura defaults missing numeric fields

- GIVEN row has costo_unitario=null, stock_minimo=null
- WHEN mapMontura is called
- THEN costoUnitario MUST be 0, stockMinimo MUST be 2

#### Scenario: mapMontura uses ID as SKU fallback

- GIVEN row.sku is null or empty
- WHEN mapMontura is called
- THEN sku MUST be row.id.slice(0, 8)

### Requirement: Web — EvaluacionBuilder optical calculations

The system MUST test `transpose`, `calculateEE`, and `normalizeSphere` with boundary values including plano, zero, and negative cylinder.

| Function | Behavior | Edge Cases |
|----------|----------|------------|
| `normalizeSphere` | "plano"/"neutro"/"pl" → "0.00" | Already numeric, mixed case "PLANO" |
| `calculateEE(esf, cil)` | esf + cil/2 | Zero values, negative cylinder |
| `transpose(esf, cil, eje)` | Convert plus-cyl to minus-cyl | cil <= 0 (no transpose), eje > 90 (wrap), eje = 0 |

#### Scenario: transpose does nothing for non-positive cylinder

- GIVEN cil is "0.00" or "-1.50"
- WHEN transpose is called
- THEN nEsf, nCil, nEje MUST equal input values unchanged

#### Scenario: transpose wraps axis above 180

- GIVEN esf="0.00", cil="+2.00", eje="120"
- WHEN transpose is called
- THEN nEje MUST be "30" (120+90=210, 210-180=30)

#### Scenario: normalizeSphere handles case variations

- GIVEN input is "PLANO", "Plano", or "PL"
- WHEN normalizeSphere is called
- THEN result MUST be "0.00"

#### Scenario: calculateEE with negative cylinder

- GIVEN esf="-2.00", cil="-1.00"
- WHEN calculateEE is called
- THEN result MUST be -2.50

### Requirement: Android — ServicioRemoto.toEntity() money coercion

The system MUST test `ServicioRemoto.toEntity()` money field coercion ensuring montoTotal is non-negative and aCuenta is clamped to [0, montoTotal].

| Input montoTotal | Input aCuenta | Expected montoTotal | Expected aCuenta |
|-----------------|---------------|---------------------|------------------|
| -5.0 | 0.0 | 0.0 | 0.0 |
| 100.0 | 150.0 | 100.0 | 100.0 |
| 100.0 | -10.0 | 100.0 | 0.0 |
| 0.0 | 0.0 | 0.0 | 0.0 |
| 50.0 | 25.0 | 50.0 | 25.0 |

#### Scenario: aCuenta clamped to montoTotal when exceeding

- GIVEN montoTotal=100.0 and aCuenta=150.0
- WHEN toEntity is called
- THEN aCuenta MUST be 100.0 (coerceAtMost of normalized montoTotal)

#### Scenario: negative aCuenta clamped to zero

- GIVEN montoTotal=50.0 and aCuenta=-10.0
- WHEN toEntity is called
- THEN aCuenta MUST be 0.0

### Requirement: Android — normalizedOtForUnique

The system MUST test `normalizedOtForUnique()` handling null, empty, whitespace-only, and case normalization.

| Input | Expected Output |
|-------|-----------------|
| null | null |
| "" | null |
| "   " | null |
| "ot-123" | "OT-123" |
| "  Ot-456  " | "OT-456" |

#### Scenario: blank OT returns null

- GIVEN input is "" or "   "
- WHEN normalizedOtForUnique is called
- THEN result MUST be null (not used for deduplication)

### Requirement: Android — isTransientNetworkError

The system MUST test `isTransientNetworkError()` classification of HTTP errors and exception messages.

| Message Pattern | Expected |
|-----------------|----------|
| "timeout" | true |
| "timed out" | true |
| "connect failed" | true |
| "unable to resolve host" | true |
| "network is unreachable" | true |
| "connection reset" | true |
| "401 Unauthorized" | false |
| "500 Internal Server Error" | false |
| null message | false |

#### Scenario: server errors are NOT transient

- GIVEN exception message is "500 Internal Server Error"
- WHEN isTransientNetworkError is called
- THEN result MUST be false (server errors should not trigger retry)

#### Scenario: null message is not transient

- GIVEN exception.message is null
- WHEN isTransientNetworkError is called
- THEN result MUST be false

### Requirement: Test infrastructure and conventions

The system MUST establish test infrastructure, coverage configuration, and testing conventions for both Web and Android.

#### Web Infrastructure

| Change | Detail |
|--------|--------|
| Dependency | Add `@vitest/coverage-v8` as devDependency in `web/package.json` |
| Coverage config | Add `coverage` block to `web/vitest.config.ts` |
| Per-file threshold | 50% minimum for in-scope files |
| Global threshold | 0% (informational only, must not block CI) |
| Test location | Co-located `.test.ts` next to source files |
| Test pattern | `describe`/`it` blocks, one assertion per test |
| Zod tests | Use `safeParse` pattern, assert `result.success` |

#### Android Infrastructure

| Change | Detail |
|--------|--------|
| Dependency | Add `testImplementation(libs.kotlinx.coroutines.test)` to `app/build.gradle.kts` |
| Test location | Mirror package structure under `app/src/test/java/` |
| Naming | JUnit 4, descriptive camelCase method names |
| Existing tests | Do not modify existing `__tests__` or test files |

#### Test Data Factories

Create `web/src/test/factories/` with pure-function factories:
- `createMockMonturaItem(overrides)`: MonturaItem with sensible defaults
- `createMockPagoRow(overrides)`: PagoRow for cierre-caja tests
- `createMockDispensacionRemota(overrides)`: For Android sync tests
- `createMockServicioRemoto(overrides)`: For Android sync tests

#### Edge Case Convention

All tests MUST cover:
- Null inputs where the function accepts optional parameters
- Empty strings and whitespace-only strings
- Boundary values (0, negative numbers, max reasonable values)
- Pure functions tested WITHOUT mocking (no Supabase, no coroutines)
