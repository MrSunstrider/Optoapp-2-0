# Delta for sync — parent balance upload floor

## ADDED Requirements

### Requirement: Parent balance upload floor

Finanzas upload MUST send `monto_pagado` / `a_cuenta` as `max(0, SUM(pago_effect))` on the parent upsert payload so remote CHECK constraints pass. Local ledger sums MUST NOT be rewritten; only the upload snapshot is floored.

#### Scenario: Negative net dispensación uploads parent with zero cache

- GIVEN dispensación D with pagos netting to `-50` via PagoEffect
- WHEN `uploadDispensaciones` runs
- THEN the remote upsert MUST use `monto_pagado = 0`
- AND MUST NOT mark D with `quarantine:constraint:dispensaciones_monto_pagado_chk`
- AND pagos for D MAY upload afterward (individual poison rows may still quarantine)

#### Scenario: Non-negative net unchanged

- GIVEN pagos net `150`
- WHEN upload runs
- THEN `monto_pagado = 150`
