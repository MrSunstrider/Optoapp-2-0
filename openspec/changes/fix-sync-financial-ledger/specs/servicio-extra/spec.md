# Delta for servicio-extra

## ADDED Requirements

### Requirement: Servicios Extra Estado Domain Includes Anulado

Remote `servicios_extra.estado` MUST accept `Anulado` in addition to existing delivery states so cancelled servicios sync without CHECK 23514. Local writers MUST set `Anulado` on cancel. Active-sale and debt queries MUST exclude `estado = 'Anulado'`.

#### Scenario: Anulado sync succeeds

- GIVEN a local servicio extra with `estado=Anulado` and valid remaining fields
- WHEN finanzas upload upserts it
- THEN remote CHECK MUST accept the row
- AND no 23514 MUST be raised for estado

#### Scenario: Anulado excluded from active sales and debt

- GIVEN servicios with estados `Pendiente`, `Entregado`, and `Anulado` for the same optica
- WHEN active-sale or debt listings/aggregates run
- THEN Anulado rows MUST be excluded
- AND Pendiente/Entregado rows MUST remain

#### Scenario: Negative control — unknown estado still rejected

- GIVEN an upsert with `estado='Cancelado'` (not in domain)
- WHEN the write hits remote CHECK
- THEN the operation MUST fail
- AND the row MUST NOT be marked remotely OK

### Requirement: Dispensaciones Estado Domain Includes Anulado and Reclamada

Remote `dispensaciones.estado_entrega` MUST accept `Anulado` and `Reclamada` so cancelled/claimed dispensaciones sync without CHECK 23514. Active-sale and debt queries MUST exclude both.

#### Scenario: Anulado and Reclamada sync succeed

- GIVEN local dispensaciones with `estado_entrega` in {`Anulado`, `Reclamada`}
- WHEN finanzas upload upserts them
- THEN remote CHECK MUST accept both
- AND no 23514 MUST be raised for estado_entrega

#### Scenario: Active-sale and debt exclusions

- GIVEN dispensaciones including Pendiente, Entregado, Anulado, and Reclamada
- WHEN active-sale or debt queries run for that optica
- THEN Anulado and Reclamada MUST be excluded
- AND Pendiente/Entregado MUST remain eligible per existing rules

#### Scenario: Multi-tenant isolation on estado updates

- GIVEN optica A cancels a dispensacion to Anulado
- WHEN sync and listings run for optica B
- THEN optica A’s Anulado row MUST NOT appear in optica B results
