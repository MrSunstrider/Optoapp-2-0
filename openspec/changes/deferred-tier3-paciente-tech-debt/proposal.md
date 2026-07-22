# Proposal: Deferred Tier 3 Paciente Tech Debt (Items 15-22)

## Intent

8 deferred tech-debt items from the archived `refactor-paciente-tech-debt`. Pure cleanup: fix risky CSV serialization, replace in-memory MAX loop with SQL, create a reusable empty-state composable, surface orphaned tag data, move sort logic out of composables, and fix an infinite spinner. No new features.

## Scope

### In Scope (6 items, 3 groups)

- **Group A — Quick wins**: Item 15 (JSON array for `ultimasEtiquetas` sync), Item 18 (SQL MAX for `suggestNextHistoriaOptometrica`), Item 21 (move composable-side sort to ViewModels — GastosScreen, MonturasScreen)
- **Group B — UI polish**: Item 19 (create reusable `EmptyState` composable → apply to DetallePacienteScreen 3 tabs), Item 20 (display-only tag chips in PacienteInfoHeader), Item 22 (fix infinite spinner on DetallePacienteScreen + add timeout/error state)

### Out of Scope

- Items 16 (`Resource.Empty`) & 17 (`firstOrNull` safety) — deferred. Use convention for empty, fix only actual crash sites if found in review.
- Full CRUD for patient tags (separate feature change).
- DAO-level `ORDER BY` for every sort column (over-engineered for current data volume).

## Capabilities

### New Capabilities

None. Pure tech debt — refactoring and UI polish, no spec-worthy new capabilities.

### Modified Capabilities

None. No existing spec requirements change. Sync serialization (Item 15) changes only the wire format of `ultimasEtiquetas` — the Supabase column stays TEXT; no spec contract breaks.

## Approach

Execute in two independent groups, Group C deferred:

1. **Group A**: Fix sync serialization (`SyncPacientesUseCase.toRemoto()` → `Json.encodeToString`, `toEntity()` → `Json.decodeFromString` with CSV fallback for existing rows). Same fix in `SyncHistorialUseCase`. Replace in-memory MAX loop via `PacienteDao` SQL `MAX(SUBSTR(...))` query. Move GastosScreen and MonturasScreen `sortedByDescending` into their respective ViewModels.
2. **Group B**: Create `EmptyState` composable (`ui/components/common/`). Apply to DetallePacienteScreen tab content areas (evaluaciones, dispensaciones, servicios extra). Add tag chips to `PacienteInfoHeader`. Add loading timeout + error+retry state to DetallePacienteScreen (replaces infinite `CircularProgressIndicator`).
3. **Group C (deferred)**: Document `Resource.Empty` convention. Fix 2-3 actual `list.first()` crash sites if code review identifies them.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/SyncPacientesUseCase.kt` | Modified | CSV→JSON serialization for ultimasEtiquetas |
| `domain/SyncHistorialUseCase.kt` | Modified | Same CSV→JSON in orphan upload |
| `data/PacienteDao.kt` | Modified | Add `getMaxHistoriaNum(year)` SQL query |
| `data/PacienteRepository.kt` | Modified | Replace in-memory MAX loop → DAO call |
| `ui/components/common/EmptyState.kt` | **New** | Reusable empty-state composable |
| `ui/components/paciente/PacienteInfoHeader.kt` | Modified | Add tag chip display |
| `ui/screens/DetallePacienteScreen.kt` | Modified | EmptyState in 3 tabs + fix infinite spinner |
| `viewmodel/GastosViewModel.kt` | Modified | Absorb sort from GastosScreen |
| `viewmodel/MonturasViewModel.kt` | Modified | Absorb sort from MonturasScreen |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Item 15 CSV→JSON breaks existing Supabase rows | Low | Try JSON first; CSV `split(",")` fallback. Add TODO to remove after all clients sync. |
| Item 18 `SUBSTR` offset wrong for `HO-YYYY-NNNN` | Low | `HO-YYYY-` = 8 chars, verified against established format. |
| Item 22 fix misses other screens with infinite spinners | Medium | Scoped to DetallePacienteScreen only. Other screens flagged for separate pass. |

## Rollback Plan

- **Item 15**: Revert `toRemoto()`/`toEntity()` in both sync use cases. Fallback code handles existing rows — no data loss on revert.
- **Items 18, 21**: Revert DAO query and VM changes. Trivial `git revert`.
- **Items 19, 20, 22**: Revert composable/VM changes. Pure additive UI — no data impact. No schema or RLS changes anywhere.

**Supabase schema/RLS**: NOT affected. Column stays `TEXT`. No new tables or policies.

## Success Criteria

- [ ] All existing unit tests pass (`./gradlew :optoapp:testDebugUnitTest`)
- [ ] `suggestNextHistoriaOptometrica` returns correct `HO-YYYY-NNNN` verified via DAO test
- [ ] Sync round-trip: paciente with tags survives upload → download with correct values
- [ ] DetallePacienteScreen shows empty state on tabs (no data) instead of blank area
- [ ] DetallePacienteScreen shows error + retry when patient load fails (no infinite spinner)
- [ ] PacienteInfoHeader shows tag chips when paciente has `ultimasEtiquetas`
- [ ] GastosScreen / MonturasScreen sort order unchanged from pre-change behavior
