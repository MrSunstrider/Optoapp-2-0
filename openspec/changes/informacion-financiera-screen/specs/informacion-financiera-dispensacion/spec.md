# InformacionFinancieraDispensacion Specification

## Purpose

Pantalla dedicada para la gestión financiera de una dispensación. Desacopla monto total, pagos CRUD, saldo reactivo y cambio de estado del formulario de producción. Accesible vía ruta `informacion_financiera/{dispensacionId}`.

## Requirements

### Requirement: FINANCIAL_SCREEN_DISPLAY

The system MUST render a scrollable screen with a sticky header showing OT number, patient name, date, and description for a given dispensación.

- GIVEN a dispensación exists with financial data
- WHEN the user navigates to `informacion_financiera/{dispensacionId}`
- THEN a sticky header SHALL display OT, patient, date, and description
- AND the content area SHALL be scrollable

### Requirement: TOTAL_AMOUNT_EDIT

The system MUST allow viewing and editing the dispensación total amount. On "Guardar", changes SHALL persist locally via Venta upsert and trigger `postSaveSyncScheduler.scheduleFinanzasSync()`.

- GIVEN the financial screen is displayed with montoTotal = 15000
- WHEN the user edits the amount to 18000 and taps "Guardar"
- THEN the new total SHALL be persisted via `repository.upsertVenta()`
- AND `scheduleFinanzasSync()` SHALL be called

### Requirement: PAYMENT_CRUD

The system MUST support creating, reading, updating, and deleting payments for the dispensación. Deletions SHALL register anulación en caja via `deletePagoRegistrandoAnulacionEnCaja()`.

- GIVEN the financial screen is displayed
- WHEN the user adds a payment via AbonoDialog
- THEN the payment SHALL appear in the list and the remaining balance SHALL update

- GIVEN a payment exists in the list
- WHEN the user edits it via AbonoDialog
- THEN the payment SHALL be updated locally

- GIVEN a payment exists in the list
- WHEN the user deletes it
- THEN the payment SHALL be removed with anulación registrada en caja

- GIVEN the dispensación has no payments
- WHEN the financial screen opens
- THEN an empty state SHALL be displayed

### Requirement: REMAINING_BALANCE

The system MUST compute remaining balance as `montoTotal - SUM(pagos.monto)` and update reactively on any payment or amount change.

- GIVEN montoTotal = 10000 and existing payments sum = 4000
- WHEN a payment of 3000 is added
- THEN remaining balance SHALL display 3000

- GIVEN montoTotal = 10000 with no payments
- WHEN the screen opens
- THEN remaining balance SHALL display 10000

- GIVEN remaining balance is negative after changes
- WHEN saldo < 0
- THEN the balance SHALL display in red text

### Requirement: STATUS_CHANGE

The system MUST allow changing dispensación status between "Pendiente" and "Entregado" via a dropdown, persisting on "Guardar".

- GIVEN estado is "Pendiente"
- WHEN the user selects "Entregado" and taps "Guardar"
- THEN the new estado SHALL be persisted locally

### Requirement: NAVIGATION_REFACTOR

The system MUST replace the inline `FinancieraInfoSection` in `NuevaDispensacionScreen` with a summary Card (monto total, saldo, estado) and a "Gestionar Pagos" button.

- GIVEN `NuevaDispensacionScreen` is displayed
- THEN a Card SHALL show montoTotal, saldo, and estado
- WHEN the user taps "Gestionar Pagos"
- THEN navigation SHALL occur to `informacion_financiera/{dispensacionId}`

### Requirement: NAVIGATION_ROUTE

The system MUST register `informacion_financiera/{dispensacionId}` in the `MainDrawerScreen` NavHost with a guard returning null for non-existent IDs.

- GIVEN dispensacionId "abc-123" exists
- WHEN the route `informacion_financiera/abc-123` is invoked
- THEN the financial screen SHALL display

- GIVEN dispensacionId is invalid or null
- WHEN the route is invoked
- THEN the NavHost SHALL return null (no-op navigation)

### Requirement: SAVE_VENTA_SYNC

When "Guardar" is tapped after any financial change, the system SHALL upsert a `Venta` record and call `postSaveSyncScheduler.scheduleFinanzasSync()`.

- GIVEN financial changes have been made (amount, payment, or status)
- WHEN the user taps "Guardar"
- THEN `repository.upsertVenta()` SHALL be called
- AND `scheduleFinanzasSync()` SHALL be called
- AND the UI SHALL show a success confirmation
