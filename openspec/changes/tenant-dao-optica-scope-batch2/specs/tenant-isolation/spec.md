# Spec: tenant-isolation (batch 2)

## Requirement: costo-lookup-tenant

### Scenario: foreign optica cost ignored
- **WHEN** costos_productos / costos_biselado row exists for optica A matching business keys
- **AND** lookup is invoked with optica B
- **THEN** result is null

### Scenario: same optica cost returned
- **WHEN** matching vigente row for optica A
- **AND** lookup with optica A
- **THEN** that row is returned

## Requirement: pago-helper-tenant

### Scenario: sum ignores foreign tenant
- **WHEN** pagos for dispensacionId exist under optica A and B
- **AND** sumMontoByDispensacion(id, optica B)
- **THEN** only B montos are summed

### Scenario: credit/reverso scoped
- **WHEN** getCreditPagosByParent / getReversoByOriginalId called with opticaId
- **THEN** only matching optica rows return

## Requirement: parent-fk-tenant

### Scenario: items by dispensacion
- **WHEN** items residual under foreign optica_id
- **AND** getItemsListByDispensacion(dispId, sessionOptica)
- **THEN** empty or only session rows

### Scenario: reassign requires optica
- **WHEN** reassignEvaluacionesPaciente(from, to, opticaId)
- **THEN** UPDATE includes AND opticaId = :opticaId
