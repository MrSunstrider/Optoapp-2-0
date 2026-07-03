# Design: Fix servicios extra UI rendering and sync normalization

## Technical Approach

Six targeted fixes across UI rendering, PDF generation, and sync normalization. No data model changes. Each bug maps to a single file modification with minimal blast radius. The fix extracts a shared normalizer for `metodoPago` at the sync boundary and propagates normalized values downstream.

## Architecture Decisions

### Decision: 3-way label in TransactionItem

**Choice**: `when` block checking `dispensacionId` then `servicioExtraId` then fallback to "Pago"
**Alternatives considered**: Sealed class for transaction types (over-engineered for a label)
**Rationale**: Matches existing pattern; `Pago` already carries both optional IDs

### Decision: Reuse `remotoServicioExtraMetodoToLocal()` for pago normalization

**Choice**: Call existing normalizer in `PagoRemoto.toEntity()` instead of creating a new function
**Alternatives considered**: New `remotoPagoMetodoToLocal()` — duplicate logic; shared `normalizeMetodoPago()` extension
**Rationale**: `remotoServicioExtraMetodoToLocal()` already handles the `"Sin especificar" → ""` mapping. Reusing it avoids divergence. A rename to `normalizeMetodoPago()` is optional but not required for correctness.

### Decision: Merge lists in Compose, not ViewModel

**Choice**: Combine `dispensaciones` and `allServiciosDelPeriodo` in `ReportesScreen` using a wrapper type
**Alternatives considered**: New combined Flow in ViewModel; sealed interface for unified list
**Rationale**: Keeps ViewModel changes minimal; the merge is presentation-only logic. Existing `ReportesViewModel` already exposes both flows independently.

## Data Flow

```
PagoRemoto.toEntity()                  CierreCajaViewModel
  │ metodoPago.normalized()              │ getTotalesPorMetodo()
  ▼                                      ▼
Room Pago ──→ TransactionItem        groupBy { normalized }
             (3-way label)           (consistent keys)
                                        │
ReportesScreen                         ▼
  │ collect both flows              ResumenCard row
  ▼
merge + chronologically sort
  │
  ▼
LazyColumn (disp + serv items)
  │
  ▼
PDF generator (both lists)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `TransactionItem.kt` | Modify | 3-way `when` label: "Dispensación" / "Servicio Extra" / "Pago" |
| `CierreCajaScreen.kt` | Modify | Add servicios extra section: count, individual items, total |
| `ReportesScreen.kt` | Modify | Collect `allServiciosDelPeriodo`, merge into detail LazyColumn, pass to PDF |
| `ReporteFinancieroPdfGenerator.kt` | Modify | Add `serviciosExtra` parameter, render rows in detail section |
| `SyncFinanzasDto.kt` | Modify | Normalize `metodoPago` in `PagoRemoto.toEntity()` via existing normalizer |
| `CierreCajaViewModel.kt` | Modify | Normalize `metodoPago` before `groupBy` in `getTotalesPorMetodo()` |

## Interfaces / Contracts

No new interfaces. Signature changes:

```kotlin
// ReporteFinancieroPdfGenerator.kt
fun generate(
    context: Context,
    dispensaciones: List<DispensacionOptica>,
    serviciosExtra: List<ServicioExtra>,  // NEW
    periodo: String,
    totalVendido: Double,
    porCobrar: Double,
    ticketPromedio: Double
): File
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | TransactionItem label 3-way | Compose test: verify text for each ID combination |
| Unit | getTotalesPorMetodo normalization | ViewModel test: raw "efectivo" + "EFECTIVO" grouped together |
| Unit | PagoRemoto.toEntity() normalization | DTO test: unnormalized input → normalized output |
| Unit | PDF generator servicios extra | Assert PDF byte stream contains servicios rows |
| Unit | CierreCajaViewModel totalGeneral | ViewModel test: totalGeneral = totalVentasHoy + totalServiciosExtra |

## Migration / Rollout

No migration required. All fixes are corrective — no schema changes, no data backfill. Existing unnormalized pagos in Room will display with raw keys until next sync re-downloads and normalizes them.

## Open Questions

- None
