# Análisis Financiero — Fase 2 (por qué cambió el resultado)

Objective: MoM drivers volumen+ticket+costo on coach home with illustrative S/; unified editable alert thresholds in ConfiguracionFinanciera; DSO/aging CxC when deudores data allows. Mix and seguimiento sync deferred.  
Branch: `feat/analisis-financiero-fase2` (from `feat/analisis-financiero-fase1`)  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
Forecast: ~700 authored LOC (add+del, excl. generated)

## Specs

S1. User (verbatim): `Extender RPC / agregados MoM: volumen, ticket, costo (no mix todavía).`

S2. User (verbatim): `Top drivers en home con S/ ilustrativos derivados de esos tres.`

S3. User (verbatim): `Umbrales de alerta restantes en ConfiguracionFinanciera.`

S4. User (verbatim): `DSO / aging CxC en coach (si los datos de deudores alcanzan).`

S5. User (verbatim priority): Drivers volumen + ticket + costo = Imprescindible; Umbrales editables unificados = Recomendable; DSO / aging en home = Recomendable; Mix por categoría = Opcional (Fase 2.5 / 3).

S6. User (verbatim source rule): `Vendido, COGS, márgenes/RO, ticket y cantidades: solo resumen_diario / rpc_analisis_mensual (misma fórmula online/offline). Todo filtrado por optica_id.`

S7. User (verbatim out of scope): `Reportes, tablas OT×OT, demografía, ranking inventario completo, PDF multisección, comparativos anuales, utilidad neta inventada, mix en la primera entrega de drivers, CRUD de gastos en Análisis.`

S8. Bridge math (authorized plan): `T0=V0/Q0`, `T1=V1/Q1`; Volumen `(Q1−Q0)×T0`; Ticket `Q1×(T1−T0)`; Costo efecto `−(C1−C0)`; prior qty 0 → sin base MoM; top 3 by `|S/|`.

S9. DSO (authorized plan): aging 0–30 / 31–60 / 61–90 / 90+ from `List<Deudor>`; DSO ilustrativo `saldo_pendiente/(ventas_mes/días_del_mes)` when ventas>0; secondary weighted days; empty → empty state.

S10. Thresholds to promote from hardcoded (authorized plan): `margenBajoAlertaPct` 10, `margenAltoAlertaPct` 35, `contribucionAltaRatio` 0.25, `gastoFueraRangoRatio` 0.4; editor for these + existing caída/deuda/stock/minVentas.

## Tasks

- [ ] T1 S1 S6 S8 — RPC + AnalisisMensual + offline Room MoM priors (qty/ticket/costo) — route: inline — commit:
- [ ] T2 S2 S8 — ResultadoDrivers calculator + home top drivers UI — route: inline — commit:
- [ ] T3 S3 S10 — Config thresholds migration + editor UI + wire GenerarRecomendaciones — route: inline — commit:
- [ ] T4 S4 S9 — Aging buckets + DSO ilustrativo on coach from List&lt;Deudor&gt; — route: inline — commit:

## Log

L1. User request (verbatim package): Fase 2 — Por qué cambió el resultado; Extender RPC / agregados MoM: volumen, ticket, costo (no mix todavía). Top drivers en home con S/ ilustrativos derivados de esos tres. Umbrales de alerta restantes en ConfiguracionFinanciera. DSO / aging CxC en coach (si los datos de deudores alcanzan). Mix deferred. Fuente de verdad resumen_diario / rpc_analisis_mensual.
