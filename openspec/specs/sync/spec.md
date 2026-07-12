# Sync Specification

## Purpose

Sync boundary normalization: ensuring remote payment data is normalized before persisting to local storage.

## Requirements

### Requirement: PagoRemoto metodoPago Normalization

`PagoRemoto.toEntity()` SHALL normalize `metodoPago` using `remotoPagoMetodoToLocal()` (or the same normalizer used by `ServicioRemoto.toEntity()`). Raw/unnormalized values MUST NOT be stored in the local pago entity.

#### Scenario: Normalized input passes through

- GIVEN a `PagoRemoto` with `metodoPago = "Efectivo"` (already normalized)
- WHEN `toEntity()` is called
- THEN the resulting pago's `metodoPago` MUST be "Efectivo"

#### Scenario: Unnormalized input is normalized

- GIVEN a `PagoRemoto` with `metodoPago = "efectivo"` (lowercase)
- WHEN `toEntity()` is called
- THEN the resulting pago's `metodoPago` MUST be "Efectivo" (normalized)

#### Scenario: Matches ServicioRemoto normalization

- GIVEN a `PagoRemoto` and a `ServicioRemoto` both with `metodoPago = "tarjeta_credito"`
- WHEN both `toEntity()` methods are called
- THEN both resulting entities MUST have identical `metodoPago` values

---

## costos_productos Sync

System SHALL include `costos_productos` in download AND upload sync. Remote DTO SHALL map matrix columns via `@SerialName`. Sync order: ← ventas → **costos_productos** → pagos.

- GIVEN sync cycle with downloadAfterUpload = true
- WHEN SyncFinanzasUseCase runs
- THEN costos_productos downloads after ventas
- AND local updates upload

## costos_biselado Sync (Read-Only)

System SHALL include `costos_biselado` in download only. Upload SHALL NOT be supported.

- GIVEN sync cycle with downloadAfterUpload = true
- WHEN SyncFinanzasUseCase runs
- THEN costos_biselado downloads
- AND no upload occurs
