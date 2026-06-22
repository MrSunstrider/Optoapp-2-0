# Proposal: Complete Sync Conflict Protection

## Intent

Only 3 of 15 entity types are protected against sync conflict data loss (servicio_extra, dispensacion, pago). The other 12 silently overwrite local edits with remote data during download. Users lose clinical records, inventory adjustments, and financial data without warning. This extends the download-guard + bump + three-way merge pattern from `fix-servicios-conflictos` across ALL entity types, then replaces the timestamp-bump trick with proper three-way field-level merge using entity snapshots.

## Scope

**In Scope**
- Phase A: download guard + bump for all 12 unprotected entity types
- Phase B: three-way merge with base/local/remote JSON snapshots in conflict_records, field-level conflict UI, auto-merge non-conflicting fields
- Phase B: remove `bumpEntityUpdatedAt` trick (replaced by semantic merge)
- Child entity strategy: conflict record references parent entity type+ID for bump

**Out of Scope**
- Supabase trigger/server changes (timestamp semantics stay)
- Recovering pre-existing lost data
- Real-time conflict detection (sync-triggered only)

## Capabilities

**New**
- `sync-conflict-resolution`: full-coverage download guard, field-level three-way merge, snapshot-based conflict records (replaces fix-servicios-conflictos scope of 3 entities)

**Modified**
- None (prior sync-conflict-resolution spec was change-scoped, not in openspec/specs/)

## Approach

**Phase A — Full coverage (PR #1)**

Download guard: inject `ConflictDao` into every use case with download methods: SyncPacientesUseCase.download(), SyncHistorialUseCase.downloadEvaluaciones(), SyncInventarioUseCase.downloadMonturas/downloadMovimientos(), SyncProveedoresUseCase.downloadProveedores/downloadCategorias(), SyncOrdenesCompraUseCase.downloadOrdenesCompra/downloadItems(), SyncInventarioFisicoUseCase.downloadSessions/downloadDetalles(), DownloadSyncCoordinator.downloadDispensacionItems/downloadArqueos().

Bump: add `when` branches in `bumpEntityUpdatedAt()` for paciente, evaluacion, montura, proveedor, orden_compra, inventario_fisico (all have existing `update*` repo methods with auto-stamp). Child entities (montura_movimiento, orden_compra_item, categoria_montura, inventario_fisico_detalle, dispensacion_item): bump their **parent** entity.

**Phase B — Three-way merge (PR #2–3)**

Schema: migration v27→v28 adds `baseSnapshot`, `localData`, `remoteData` TEXT columns to conflict_records.

ConflictHelper.filterConflicts(): on conflict detection, serialize full Room entity (local) + full remote DTO to JSON via kotlinx.serialization. Store in 3 new columns. Read base snapshot from Room pre-upload.

Resolution: field-level auto-merge (local=base & remote=base → keep, local≠base & remote=base → apply local, local=base & remote≠base → apply remote, both changed → field conflict). UI shows per-field diffs. "Usar el mío"/"Usar nube" resolve field-level. Remove bump trick.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/sync/ConflictRecord.kt` | Modified | Add 3 JSON columns; schema migration |
| `domain/sync/ConflictHelper.kt` | Modified | Serialize full entity on conflict; field-level merge logic |
| `viewmodel/SyncViewModel.kt` | Modified | 12 new bump branches; remove bump in Phase B; three-way resolve |
| `ui/screens/ConflictosScreen.kt` | Modified | Field-level diff UI; TYPE_LABELS extended |
| `domain/DownloadSyncCoordinator.kt` | Modified | Guard dispensacion_item, arqueos |
| `domain/SyncPacientesUseCase.kt` | Modified | Download guard + bump support |
| `domain/SyncHistorialUseCase.kt` | Modified | Download guard + bump support |
| `domain/SyncInventarioUseCase.kt` | Modified | Download guard for monturas + movimientos |
| `domain/SyncProveedoresUseCase.kt` | Modified | Download guard + bump for categorias |
| `domain/SyncOrdenesCompraUseCase.kt` | Modified | Download guard + bump for OC + items |
| `domain/SyncInventarioFisicoUseCase.kt` | Modified | Download guard + bump for sessions + detalles |
| `data/OptoDatabase.kt` | Modified | Version 27→28 |
| Tests (12+ new files) | New | TDD per use case |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Room migration 27→28 fails on existing installs | Med | Test with real DB dumps; add migration test |
| JSON snapshot size bloats conflict_records | Low | Truncate long fields (text > 10KB → summary) |
| Child-entity parent resolution breaks on multi-child conflicts | Low | Group child conflicts by parent before bump |
| Phase B merge logic introduces subtle field-loss bugs | Med | TDD with exhaustive field-combination matrices |
| Hilt wiring breaks with new ConflictDao deps | Low | All affected classes already use @Inject constructors |

## Rollback Plan

Phase A: revert commits restore original download methods (no ConflictDao param), remove bump branches. No schema change — code-only rollback.

Phase B: revert migration via `fallbackToDestructiveMigration()` in dev, database backup restore in prod. Revert merge logic to original timestamp-bump.

## Delivery Strategy

3 chained PRs (400-line budget each):
1. **Phase A download guard** — ConflictDao injection into all download methods. TDD per use case.
2. **Phase A bump coverage** — 12 entity types in bumpEntityUpdatedAt + child-entity parent strategy. ConflictosScreen TYPE_LABELS extended.
3. **Phase B three-way merge** — Schema migration, snapshot serialization, field-level merge, new UI, remove bump trick.

## Success Criteria

- [ ] All 15 entity types have download guard (active conflicts skip download)
- [ ] resolveKeepMine works for all 15 entity types (bump → upload → resolve)
- [ ] "Usar el mío para todos" clears all conflict types
- [ ] Phase B: non-conflicting fields auto-merge without user action
- [ ] Phase B: conflict UI shows per-field diffs, not just timestamps
- [ ] Phase B: bump trick removed; merged entities upload once, conflict never reappears
- [ ] 0 regressions: `./gradlew :optoapp:testDebugUnitTest` passes all existing tests
- [ ] New TDD tests fail before implementation, pass after
