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
