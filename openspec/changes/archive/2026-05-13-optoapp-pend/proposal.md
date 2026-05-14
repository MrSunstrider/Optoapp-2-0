# Proposal: optoapp-pend

## Intent

Complete remaining Android cleanup after A1-A19 refactoring by addressing critical test gaps and large files needing extraction.

## Scope

### In Scope
- **Critical** (no tests — create base first):
  - PostSaveSyncScheduler (139 lines)
  - SubscriptionViewModel (68 lines)
  - SubscriptionManager (111 lines)
- **High** (files 300-564 lines needing split):
  - EvaluacionViewModel.kt (564 lines) — extract DiagnosticoCalculator, DipParser
  - EvaluacionFormSections.kt (542 lines) — extract per-section composables
  - MembershipRepository.kt (479 lines) — extract DTOs
  - MainDrawerScreen.kt (443 lines) — extract DrawerContent
  - NuevaDispensacionScreen.kt (440 lines) — extract LenteForm, MonturaForm, PagosSection
  - RecetaPdfBuilder.kt (435 lines) — extract RefraccionTableBuilder
  - ConfiguracionScreen.kt (395 lines) — already partially modularized
  - SyncFinanzasUseCase.kt (375 lines) — extract uploaders
  - NuevoPacienteScreen.kt (352 lines) — extract PacienteFormSections
  - OptoRepository.kt (341 lines) — facade, keep as is
  - NuevoServicioScreen.kt (312 lines) — extract ServicioForm
  - MonturasScreen.kt (306 lines) — extract MonturaList, MonturaForm
  - PacienteEvaluacionesTab.kt (300 lines) — extract EvaluacionListItem
- **Medium**: Compose BOM update (2024.02.02 → 2024.12.01)

### Out of Scope
- Web-side changes
- Supabase migrations
- New features

## Capabilities

### New Capabilities
None — pure refactor, no new capabilities

### Modified Capabilities
None — no spec-level behavior changes

## Approach

Priority-order execution with strict TDD:
1. **Critical**: Write tests for three untested components
2. **High**: Split large files into smaller modules following existing extraction patterns
3. **Medium**: Update Compose BOM version
4. All extractions follow patterns from A1-A19

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/sync/PostSaveSyncScheduler.kt` | Modified | Add unit tests |
| `presentation/subscription/SubscriptionViewModel.kt` | Modified | Add unit tests |
| `data/repository/SubscriptionManager.kt` | Modified | Add unit tests |
| `presentation/evaluacion/EvaluacionViewModel.kt` | Modified | Extract DiagnosticoCalculator, DipParser |
| `presentation/evaluacion/EvaluacionFormSections.kt` | Modified | Extract per-section composables |
| `data/repository/MembershipRepository.kt` | Modified | Extract DTOs |
| `presentation/main/MainDrawerScreen.kt` | Modified | Extract DrawerContent |
| `presentation/dispensacion/NuevaDispensacionScreen.kt` | Modified | Extract LenteForm, MonturaForm, PagosSection |
| `data/pdf/RecetaPdfBuilder.kt` | Modified | Extract RefraccionTableBuilder |
| `presentation/configuracion/ConfiguracionScreen.kt` | Modified | Complete modularization |
| `domain/sync/SyncFinanzasUseCase.kt` | Modified | Extract uploaders |
| `presentation/paciente/NuevoPacienteScreen.kt` | Modified | Extract PacienteFormSections |
| `presentation/servicio/NuevoServicioScreen.kt` | Modified | Extract ServicioForm |
| `presentation/monturas/MonturasScreen.kt` | Modified | Extract MonturaList, MonturaForm |
| `presentation/paciente/PacienteEvaluacionesTab.kt` | Modified | Extract EvaluacionListItem |
| `app/build.gradle.kts` | Modified | Update Compose BOM |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Regression from splits | Med | Atomic per-item commits, run full test suite after each |
| Test gaps remain | Low | Strict TDD: no code without test first |

## Rollback Plan

Per-item atomic commits. Revert specific commit if issue arises: `git revert <commit-hash>`

## Dependencies

- None

## Success Criteria

- [ ] All Critical items have tests
- [ ] All High-priority files split into smaller modules
- [ ] Compose BOM updated
- [ ] All existing tests pass
- [ ] Build succeeds