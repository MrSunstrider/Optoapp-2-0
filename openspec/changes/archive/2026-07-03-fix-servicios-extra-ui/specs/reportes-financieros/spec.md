# Delta for Reportes Financieros

## ADDED Requirements

### Requirement: Servicios Extra in Detail List

`ReportesScreen` SHALL render servicios extra alongside dispensaciones in the "Detalle de Ventas" `LazyColumn`. The screen MUST collect `allServiciosDelPeriodo` from the ViewModel and merge both lists chronologically. Each item MUST show date, description, montoTotal, and payment status.

#### Scenario: Period has both dispensaciones and servicios extra

- GIVEN a period with 2 dispensaciones and 1 servicio extra
- WHEN `ReportesScreen` renders the detail list
- THEN all 3 items MUST appear in the `LazyColumn`
- AND servicios extra MUST NOT be absent from the rendered list

#### Scenario: Period has only servicios extra

- GIVEN a period with 0 dispensaciones and 3 servicios extra
- WHEN `ReportesScreen` renders the detail list
- THEN all 3 servicios extra MUST appear

### Requirement: Servicios Extra in PDF Report

`ReporteFinancieroPdfGenerator.generate()` SHALL accept a `serviciosExtra: List<ServicioExtra>` parameter. The PDF detail section MUST render servicios extra rows alongside dispensación rows.

#### Scenario: PDF with servicios extra

- GIVEN a non-empty servicios extra list
- WHEN the PDF is generated
- THEN the detail section MUST contain servicios extra rows

#### Scenario: PDF with empty servicios extra

- GIVEN an empty servicios extra list
- WHEN the PDF is generated
- THEN the detail section MUST contain only dispensación rows
