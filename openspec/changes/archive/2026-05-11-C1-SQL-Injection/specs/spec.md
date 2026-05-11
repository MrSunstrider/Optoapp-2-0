# Delta Spec: SQL Injection in SupabasePacienteRepository

## MODIFIED Requirements

### Requirement: Web — Escape search input in SupabasePacienteRepository.getAll()

The `SupabasePacienteRepository.getAll()` method MUST escape user-provided search input before interpolating it into the PostgREST `.or()` filter string. The vulnerability at line 17 allows an attacker to inject PostgREST operators (e.g. `%,id.neq.null`) that bypass the intended `ilike` semantics and potentially read data across tenant boundaries.

#### Scenario: Normal search returns matching patients unchanged

- GIVEN a valid search term like "juan" or "perez"
- WHEN `getAll()` is called with `options.search = "juan"`
- THEN the query MUST return all patients whose `nombre_completo`, `telefono`, or `historia_optometrica` contains "juan" (case-insensitive)
- AND the result set MUST be identical to the current behavior before the fix

#### Scenario: Malicious input with PostgREST operators is neutralized

- GIVEN a search term like `%,id.neq.null` or `%'); DROP TABLE pacientes; --`
- WHEN `getAll()` is called with that search term
- THEN the `%` and `_` characters MUST be escaped so they are treated as literal characters by the `ilike` operator
- AND the query MUST NOT interpret injected PostgREST operators as filter clauses
- AND the result MUST be an empty set (no patient matches the literal string)

#### Scenario: Search with literal percent and underscore characters works correctly

- GIVEN a search term containing `%` or `_` (e.g. "100%", "test_case")
- WHEN `getAll()` is called with that search term
- THEN the `%` MUST be escaped to `\%` and `_` MUST be escaped to `\_` in the filter
- AND the search MUST match patients whose fields contain the literal characters `%` or `_`
- AND the search MUST NOT treat them as SQL wildcards

#### Scenario: Search with backslash characters is escaped

- GIVEN a search term containing `\` (e.g. "path\name" or "100\%")
- WHEN `getAll()` is called with that search term
- THEN the `\` MUST be escaped to `\\` before any other escaping occurs
- AND the resulting filter MUST NOT allow the backslash to escape subsequent characters

#### Scenario: Empty or whitespace-only search is handled

- GIVEN `options.search` is `""` or `"   "`
- WHEN `getAll()` is called
- THEN the `.or()` clause MUST NOT be appended (same as current behavior after `.trim()`)
- AND all patients for the given `opticaId` MUST be returned (subject to pagination)

#### Scenario: RLS tenant isolation is preserved

- GIVEN a user authenticated to óptica A
- WHEN `getAll()` is called with any search term (including malicious)
- THEN the `eq("optica_id", opticaId)` filter MUST remain effective
- AND no data from other ópticas MUST be returned

### Requirement: Web — Reuse or co-locate the escape utility

The escape function for `ilike` fragments MUST be shared between `lib/pacientes.ts` and `SupabasePacienteRepository.ts` to prevent drift. The repository MUST NOT define its own duplicate escape logic.

#### Scenario: Single source of truth for escape logic

- GIVEN the escape function exists in one location
- WHEN `SupabasePacienteRepository` needs to escape a search term
- THEN it MUST import the function from the shared location
- AND there MUST NOT be two separate implementations of the same escape logic

### Requirement: Web — Preserve existing search behavior

The fix MUST NOT change the observable behavior of `getAll()` for any valid, non-malicious input. The search MUST continue to work across the same three fields (`nombre_completo`, `telefono`, `historia_optometrica`) with the same case-insensitive matching.

#### Scenario: Phone number search works unchanged

- GIVEN a patient with `telefono = "999123456"`
- WHEN `getAll()` is called with `options.search = "999"`
- THEN the patient MUST be included in the results

#### Scenario: Historia optométrica search works unchanged

- GIVEN a patient with `historia_optometrica = "miopía severa"`
- WHEN `getAll()` is called with `options.search = "miopia"`
- THEN the patient MUST be included in the results (PostgREST `ilike` is case-insensitive)

## Acceptance Criteria

| # | Criterion | Verification |
|---|-----------|-------------|
| AC1 | `SupabasePacienteRepository.ts` line 17 no longer interpolates raw user input | Code review: `options.search` is passed through `escapeIlikeFragment()` before template literal |
| AC2 | The escape function is imported from a shared location, not duplicated | Code review: no inline `.replace()` chain in the repository file |
| AC3 | No `any` types introduced | TypeScript compilation with `strict: true` passes |
| AC4 | No catch-and-silence patterns introduced | Code review: no empty `catch {}` blocks |
| AC5 | Existing tests (if any) continue to pass | `npm test` in `web/` passes |
| AC6 | The `.or()` filter string structure is preserved (same 3 fields) | Code review: filter string matches `nombre_completo.ilike.%${t}%,telefono.ilike.%${t}%,historia_optometrica.ilike.%${t}%` |
