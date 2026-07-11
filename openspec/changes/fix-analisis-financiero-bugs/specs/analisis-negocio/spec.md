# Delta Spec: GastosViewModel Test Coverage & Diagnostics

## Context

This delta adds test coverage and diagnostic logging for the gastos CRUD flow within the `analisis-negocio` capability (spec defined in `openspec/specs/analisis-negocio/spec.md`).

Exploration found the CRUD code (R12 `GastoOperativoEntity`/DAO, R30 category values, ViewModel state) is correct. The likely cause of Bug 1 (gastos not appearing) is empty Room data for the user's `opticaId`. These tests prove the `save()`→`allGastos`→UI flow works and add diagnostics to help future investigation.

---

## Requirements

### REQ-GASTOS-TEST-1: save() Emits New Expense Through allGastos

The `GastosViewModel.save()` method MUST update the `allGastos` StateFlow within one emission after the Room DAO write completes.

#### Scenario: Save emits new expense immediately

```
GIVEN a GastosViewModel with a fake GastoOperativoDao that records upsert calls
 WHEN save(nuevoGasto) is called with a valid GastoOperativoEntity
 THEN the DAO upsert SHALL be called exactly once with that entity
  AND allGastos SHALL emit a list that contains the new entity
  AND the emission SHALL occur before the coroutine completes
```

#### Scenario: Save does not affect other gastos

```
GIVEN allGastos initially emits [gastoA, gastoB]
 WHEN save(gastoC) completes successfully
 THEN allGastos emits a list containing [gastoA, gastoB, gastoC]
  AND gastoA and gastoB are unchanged
```

---

### REQ-GASTOS-TEST-2: Saved Expense Appears in Monthly Filter

After `save()` completes, the gastos list filtered by the same month MUST include the new expense.

#### Scenario: New expense in same month appears in filtered list

```
GIVEN the current month filter is "2026-07"
 WHEN save() succeeds with a gasto whose fecha is in "2026-07"
 THEN the filtered gastos list emitted by the ViewModel SHALL include that new gasto
```

#### Scenario: New expense in different month does not appear

```
GIVEN the current month filter is "2026-07"
 WHEN save() succeeds with a gasto whose fecha is in "2026-08"
 THEN the filtered gastos list SHALL NOT include that gasto
  AND the gasto SHALL appear when the month filter changes to "2026-08"
```

---

### REQ-GASTOS-TEST-3: Diagnostic Logging on Empty Gastos

`GastosViewModel` initialization SHALL log a diagnostic message at DEBUG level when `allGastos` is empty.

#### Scenario: Empty gastos logs diagnostic

```
GIVEN a GastosViewModel is created for an opticaId that has zero gastos in Room
 WHEN the ViewModel initializes and collects allGastos
 THEN a Log.d message SHALL be written containing "GastosViewModel"
  AND the message SHALL contain the opticaId value
  AND the message SHALL contain "0 gastos" or similar zero-count indicator
```

#### Scenario: Non-empty gastos does not log diagnostic

```
GIVEN a GastosViewModel is created for an opticaId that has 5 gastos in Room
 WHEN the ViewModel initializes and collects allGastos
 THEN no DEBUG diagnostic log SHALL be written about empty gastos
```

---

## Out of Scope

- No changes to `GastoOperativoEntity`, `GastoOperativoDao`, Room migrations, or the CRUD logic in `GastosViewModel`.
- No changes to sync coordinator, Supabase, or Supabase RLS.
- No Compose UI tests — these are ViewModel-level unit tests (Robolectric with in-memory Room).

---

## Test Type

All scenarios SHALL be verified via **unit tests** (Robolectric + `Room.inMemoryDatabaseBuilder`) with a real `GastoOperativoDao`. Diagnostic logging SHALL be verified via a Logcat spy or shadow Log.
