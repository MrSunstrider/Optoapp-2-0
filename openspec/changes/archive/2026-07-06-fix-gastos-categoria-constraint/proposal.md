# Proposal: Fix Gastos Operativos Categoria Constraint Mismatch

## Intent

Android's gastos operativos sync upload fails because the app sends category values (`"Local"`, `"Sueldos"`, etc.) that don't match the PostgreSQL CHECK constraint. The DB allows only `'alquiler', 'servicios', 'personal', 'proveedores', 'insumos', 'marketing', 'impuestos', 'otro'`. Fix the app side — adapt Android to match the DB constraint. No DB migration needed.

## Scope

### In Scope
- Update `GastosViewModel.kt` `categorias` list (line 97) to use DB CHECK values
- Update `GastosUiState` default `categoria` (line 22) from `"Local"` to `"alquiler"`
- Update test data in `GastosRecurrentesTest.kt` and `OptoRepositoryFinanzasTest.kt` to use DB-compatible categories
- Verify existing tests pass with new values

### Out of Scope
- No DB schema or CHECK constraint changes
- No migration files
- No UI layout changes
- No new features or category reordering

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `analisis-negocio`: Category values in Android `GastosUiState` and `GastosViewModel.categorias` change from arbitrary labels to DB CHECK constraint values. The behavior contract from the spec (R3: categoria CHECK values) is already correct — the Android code was out of sync.

## Approach

Replace the Android-side category labels with exact DB CHECK values:

| DB Constraint | Old Android Value | New Android Value |
|---------------|------------------|-------------------|
| `alquiler` | `"Local"` | `"alquiler"` |
| `servicios` | `"Servicios"` | `"servicios"` |
| `personal` | `"Sueldos"` | `"personal"` |
| `proveedores` | `"Mantenimiento"` | `"proveedores"` |
| `insumos` | `"Insumos"` | `"insumos"` |
| `marketing` | `"Marketing"` | `"marketing"` |
| `impuestos` | `"Impuestos"` | `"impuestos"` |
| `otro` | `"Otro"` | `"otro"` |

Default in `GastosUiState`: `"Local"` → `"alquiler"`.

All test data using now-invalid category strings (`"Local"`, `"Sueldos"`, `"Planilla"`, `"Reparacion"`) will use DB-compatible values instead. Category is just a `String` field in Room — no type change needed.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/.../viewmodel/GastosViewModel.kt` | Modified | Lines 22 and 97 — category list + default |
| `optoapp/.../viewmodel/GastosRecurrentesTest.kt` | Modified | Test data uses old values (`"Planilla"`, `"Reparacion"`, `"Local"`) |
| `optoapp/.../data/OptoRepositoryFinanzasTest.kt` | Modified | Test data uses `"Sueldos"` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Existing saved gastos with old category values in Room will fail upload on next sync | High | They will fail until user edits them with new values. Acceptable — no data loss, error is already happening today |
| UI labels change (user sees `"alquiler"` instead of `"Local"`) | High | This is intentional — the keys are the display values. Future improvement could add a display-name mapping layer |

## Rollback Plan

Revert the `categorias` list and default in `GastosViewModel.kt`. Tests revert alongside. No DB changes to roll back.

## Dependencies

- None

## Success Criteria

- [ ] `categorias` list in `GastosViewModel` contains exactly the 8 DB CHECK values
- [ ] Default `categoria` in `GastosUiState` is `"alquiler"`
- [ ] All unit tests pass: `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [ ] No DB migration or schema change required
