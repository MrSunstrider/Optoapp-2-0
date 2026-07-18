# Tasks: Unify Optical Catalog

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~130 (90 additions + 40 deletions) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Write Failing Tests

- [ ] 1.1 Create `OpticalCatalogTest.kt` at `optoapp/src/test/java/com/example/optoapp/domain/` with plain JUnit assertions for MATERIALES (exactly 4 values: Resina, Cristal, Policarbonato, Trivex; no "CR39" or "Alto Indice")
- [ ] 1.2 Add tests for TIPO_LENTE (contains "Multifocal", no "Progresivo"; "Lentes de Contacto" last of 5)
- [ ] 1.3 Add tests for TRATAMIENTOS (no empty/blank entries; "Antireflejo" present, "Antireflex" absent)
- [ ] 1.4 Add tests for TIPO_ARO (3 map entries with correct display labels) and SERIES (3 entries: "1ra"→1, "2da"→2, "3ra"→3)
- [ ] 1.5 Run `testDebugUnitTest` — confirm compilation fails (RED state, `OpticalCatalog` does not exist yet)

## Phase 2: GREEN — Implement OpticalCatalog

- [ ] 2.1 Create `domain/OpticalCatalog.kt` at `optoapp/src/main/java/com/example/optoapp/domain/` as Kotlin `object` in package `com.example.optoapp.domain`
- [ ] 2.2 Add 5 canonical properties: `MATERIALES: List<String>`, `TIPO_LENTE: List<String>`, `TRATAMIENTOS: List<String>`, `TIPO_ARO: Map<String, String>`, `SERIES: Map<String, Int?>`
- [ ] 2.3 Run `testDebugUnitTest` — all OpticalCatalog tests pass (GREEN)

> **⚠ Design note**: TRATAMIENTOS has a known open question (spec: 9 entries matching LenteForm line 103; design: 13 entries). Implement spec values. Resolve discrepancy before merging.

## Phase 3: GREEN — Wire Consumers

- [ ] 3.1 In `CostosYGastosViewModel.kt`: remove companion vals `MATERIALES_OPTICOS`, `TIPOS_LENTE`, `TRATAMIENTOS` (lines 100–102)
- [ ] 3.2 Same file: repoint `materialesOpticos` → `OpticalCatalog.MATERIALES`; `tiposLente` → `OpticalCatalog.TIPO_LENTE.filter { it != "Lentes de Contacto" }`; `tratamientos` → `OpticalCatalog.TRATAMIENTOS`
- [ ] 3.3 In `LenteForm.kt` line 67: replace `listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")` with `OpticalCatalog.TIPO_LENTE.filter { it != "Lentes de Contacto" }`
- [ ] 3.4 In `LenteForm.kt` line 98: replace `listOf("Resina", "Policarbonato", "Cristal", "Trivex")` with `OpticalCatalog.MATERIALES`
- [ ] 3.5 In `LenteForm.kt` line 103: replace `listOf("Ninguno", ...)` with `listOf("Ninguno") + OpticalCatalog.TRATAMIENTOS`
- [ ] 3.6 In `LenteForm.kt` line 221: replace `listOf("Aro Completo", "Semi al aire", "Al aire")` with `OpticalCatalog.TIPO_ARO.keys.toList()`

## Phase 4: VERIFY — Regression

- [ ] 4.1 Run `./gradlew :optoapp:testDebugUnitTest --stacktrace` — confirm all tests pass, 0 regressions
