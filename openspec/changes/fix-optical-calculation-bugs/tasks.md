# Tasks: Fix Optical Calculation Bugs

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 100–150 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: RED — Update Tests, Confirm They Fail

- [ ] 1.1 Update `determineTipoLente` in test to accept `(esfera: Double, cilindro: Double?)`, add `|cil| > 6.00 → fabricacion` check
- [ ] 1.2 Update `determineSeriePorCilindro` in test to accept `(cilindro: Double?)`, return `1` for `null`
- [ ] 1.3 Add test: `costCalc_serieByCilindro_null_returns1` — `determineSeriePorCilindro(null)` → `1`
- [ ] 1.4 Add test: `costCalc_fabricacion_byHighCylinder` — `determineTipoLente(-2.0, -7.0)` → `"fabricacion"`
- [ ] 1.5 Add test-local `normalizeTipoAro` + tests for `"Semi al aire"` → `"semi_aire"`, `"ranurado"` → `"aro_completo"`, `"taladro"` → `"aro_completo"`
- [ ] 1.6 Update all existing test callers: pass cylinder param to `determineTipoLente`, handle nullable cylinder in `determineSeriePorCilindro`
- [ ] 1.7 Run tests → confirm failures

## Phase 2: GREEN — Fix Functions + Callers

- [ ] 2.1 Fix `determineSeriePorCilindro(cilindro: Double?)`: nullable param, return `1` for `null`
- [ ] 2.2 Fix `determineTipoLente(esfera: Double, cilindro: Double?)`: add cylinder param, return `"fabricacion"` when `|cil| > 6.00`
- [ ] 2.3 Fix `normalizeTipoAro`: remove `ranurado`/`taladro` branches, map `"Semi"` → `"semi_aire"`
- [ ] 2.4 Update OD caller (line 767–768): remove `tipo == "stock" && odCil != null` guard, pass `odCil` to `determineTipoLente` and `determineSeriePorCilindro`
- [ ] 2.5 Update OI caller (line 780–781): same as 2.4 for `oiCil`
- [ ] 2.6 Update biselado callers (lines 812–813): pass cylinder param to `determineTipoLente`

## Phase 3: VERIFY — Compile + Test

- [ ] 3.1 `./gradlew :optoapp:compileDebugKotlin --stacktrace`
- [ ] 3.2 `./gradlew :optoapp:testDebugUnitTest --tests "OpticalCatalogTest" --stacktrace`
- [ ] 3.3 All 6 success criteria pass
