# Design: Finanzas Oleada C — export cierre + Operación Hoy continuity

**Change**: `finanzas-oleada-c-cierre` · **Issue**: #107 · **RDD**: `rdd_mode=disabled/unmanaged`  
**Delivery**: `auto-chain`, WUs ≤400 · **Schema**: none · **PagoEffect**: untouched

## Technical Approach

Dedicated day-close PDF/CSV from aggregated `CierreCajaUiState`; Reportes-style share via `FileShareUtils`; optional `fecha` nav arg from Operación Hoy; counted-cash session/prefs only. Delta `cierre-caja`.

## Architecture Decisions

| Decision | Options | Choice | Rationale |
|----------|---------|--------|-----------|
| PDF shape | Reuse Reportes generator | **New `CierreCajaPdfGenerator`** | Day-close layout ≠ period report |
| CSV | Skip; embed in PDF only | **New `CierreCajaCsvExporter`** | #107 requires CSV; greenfield OK |
| Export inputs | Re-query pagos | **Pass UI/VM aggregates + lists** | Prevents PagoEffect drift |
| Share | New MIME helpers | **`FileShareUtils.shareFile` + optional `shareCsv`** | `shareFile` already exists |
| Fecha route | Path vs query | **`cierre_caja?fecha={iso}` optional query** | Optional without breaking deep links |
| Counted cash | Revive arqueo; Room-only table | **VM state + prefs `(opticaId,fecha)`** | Matches remove-arqueo OUT |
| Role | Widen export roles | **Keep `canExportCierreCaja` ≡ view set** | Fail-closed; no DrawerSections edits |

## Data Flow

```
OperacionHoy(fecha) ──navigate(?fecha=)──► CierreCaja VM setFecha
CierreCajaUiState (PagoEffect aggregates)
        │
        ├─► CierreCajaPdfGenerator.generate(state) ─► FileShareUtils.open/sharePdf
        └─► CierreCajaCsvExporter.export(state) ─► shareFile(text/csv)
Optional: contado ─► diferencia = contado − metodoEfectivoNet
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `util/CierreCajaPdfGenerator.kt` | Create | Day-close PDF |
| `util/CierreCajaCsvExporter.kt` | Create | UTF-8 CSV + BOM |
| `util/FileShareUtils.kt` | Modify | Optional `shareCsv` |
| `viewmodel/CierreCajaViewModel.kt` | Modify | fecha from SavedStateHandle; contado state |
| `ui/screens/CierreCajaScreen.kt` | Modify | Export menu; cash field; role gate |
| `ui/navigation/Route.kt` | Modify | Optional fecha |
| `ui/screens/MainDrawerScreen.kt` | Modify | Nav arg |
| `ui/screens/OperacionHoyScreen.kt` | Modify | Pass fecha |
| Tests: exporters, VM fecha/cash, role | Create/Modify | Strict TDD |

**Untouched**: `PagoEffect.kt`; sync; arqueo migrations; Reportes PDF body; DrawerSections.

## Interfaces / Contracts

```kotlin
object CierreCajaPdfGenerator {
  fun generate(context: Context, state: CierreCajaUiState, contado: Double?): File
}
object CierreCajaCsvExporter {
  fun toCsv(state: CierreCajaUiState, contado: Double?): String
}
// Route: "cierre_caja?fecha={fecha}" with fecha nullable
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | PDF/CSV totals = fixture aggregates | Pure JUnit fixtures |
| Unit | Role deny / allow export | UiPolicy / Screen gate tests |
| Unit | Nav fecha + default today | VM + SavedStateHandle |
| Unit | diferencia Contado − Efectivo | VM |

## Threat Matrix

N/A — Compose UI + file share intents only (existing FileProvider pattern). No new shell/VCS boundaries.

## Migration / Rollout

Chain: **WU-Export → WU-UI-Export → WU-Date → WU-Cash**. No DB migration. Rollback = revert PR slice.

## Open Questions

- [x] Prefs vs memory for cash → prefs by `(opticaId,fecha)` so rotate survives process death
- [ ] Confirm FileProvider already covers cache CSV path (reuse PDF cache dir)
