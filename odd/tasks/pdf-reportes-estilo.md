# PDF reportes estilo

Objective: Improve non-receta app PDFs (Reportes export + Inventario) with a commercial report look, distinct from clinical receta. Receta untouched.  
Branch: `feat/pdf-reportes-estilo`  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Specs

S1. User: improve how app PDFs look — not the same visual language as receta; leave receta as-is for a later redesign.

S2. Scope: Reportes PDF (`ReporteFinancieroPdfGenerator` + extras) and Inventario PDF (`InventarioMonturasPdfGenerator`). No other non-receta generators exist.

S3. Own `ReportPdfTheme` / canvas — do not wire through clinical `PdfStyle` used by receta.

## Tasks

- [x] T1 S3 — ReportPdfTheme + ReportPdfCanvas
- [x] T2 S1–S2 — Restyle financiero/reportes PDF
- [x] T3 S2 — Restyle inventario PDF
- [x] T4 — Tests + suite — BUILD SUCCESSFUL

## Log

L1. User: improve PDFs; not same as receta; leave receta; Reportes and other places too.
L2. Verified only 2 non-receta generators; Reportes uses financiero + extras.
L3. Implemented ReportPdfTheme/Canvas; restyled financiero + inventario; suite green.
