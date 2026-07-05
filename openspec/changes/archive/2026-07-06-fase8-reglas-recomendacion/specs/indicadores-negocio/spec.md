# Delta Spec: indicadores-negocio — Fase 8 Reglas de Recomendación

## MODIFIED Requirements

### Requirement: AnalisisMensual gains gastosMes field

The `AnalisisMensual` domain model SHALL add:

```kotlin
val gastosMes: Double = 0.0
```

And the `fromJson` parser SHALL read the key `gastos_mes` from the RPC response JSONB via `obj.optDouble("gastos_mes")`.

Full existing text at `openspec/specs/indicadores-negocio/spec.md`.

#### Scenario: RPC response with gastos_mes is parsed correctly

- GIVEN an RPC JSONB response with `"gastos_mes": 3900.0`
- WHEN `AnalisisMensual.fromJson(json)` is called
- THEN `gastosMes == 3900.0`

#### Scenario: RPC response without gastos_mes defaults to 0

- GIVEN an RPC JSONB response missing the `gastos_mes` key
- WHEN `AnalisisMensual.fromJson(json)` is called
- THEN `gastosMes == 0.0` (no crash)
