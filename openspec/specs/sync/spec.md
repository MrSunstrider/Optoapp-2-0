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

### Requirement: SyncFinanzasUseCase static Resource.Error messages

`SyncFinanzasUseCase` MUST return user-facing `Resource.Error` messages as static, user-friendly strings. `e.localizedMessage`, `e.message`, JSON bodies, and other raw exception text MUST NOT appear in the `Resource.Error` message. Full exception detail MUST be logged via `Log.e` (or the existing sync logger) for diagnostics. Partial-upload static messaging already in place MUST remain static.

#### Scenario: IOException yields static error

- GIVEN an `IOException` during finanzas sync upload or download
- WHEN `SyncFinanzasUseCase` catches the failure
- THEN the returned `Resource.Error` message MUST be static text
- AND the message MUST NOT contain the exception's `localizedMessage` or raw `message`
- AND diagnostics MUST still capture the original exception

#### Scenario: Generic Exception yields static error

- GIVEN a non-IO `Exception` during finanzas sync
- WHEN `SyncFinanzasUseCase` catches the failure
- THEN the returned `Resource.Error` message MUST be static text
- AND the message MUST NOT embed raw exception text
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
