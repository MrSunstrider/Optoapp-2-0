# Análisis Financiero — Fase 3 (multi-dispositivo y profundidad)

Objective: Sync remote feedback/seguimiento with RLS+LWW; add mix as fourth MoM driver; show plan cumplimiento hechas vs abiertas. Utilidad neta blocked until fiscal policy.  
Branch: `feat/analisis-financiero-fase3` (from `feat/analisis-financiero-fase2`)  
Delivery: `auto-chain` + `stacked-to-main`  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`  
Forecast: ~800 authored LOC  
Suite: `testDebugUnitTest` BUILD SUCCESSFUL (observed before work-unit commits)

## Specs

S1. User (verbatim): `Sync remoto de seguimiento (diseño ya preparado en Fase 1).`

S2. User (verbatim): `Mix como cuarto driver.`

S3. User (verbatim): `Cumplimiento del plan (hechas vs abiertas) y utilidades neta solo si más adelante hay política fiscal explícita.` — utilidad neta blocked; cumplimiento in scope.

S4. User (verbatim priority): Sync seguimiento Supabase + RLS = Imprescindible; Driver mix = Recomendable; Utilidad neta / IGV real = Opcional (bloqueado hasta política).

S5. User (verbatim source rule): `Vendido, COGS, márgenes/RO, ticket y cantidades: solo resumen_diario / rpc_analisis_mensual (misma fórmula online/offline). Todo filtrado por optica_id.`

S6. User (verbatim out of scope): `Reportes, tablas OT×OT, demografía, ranking inventario completo, PDF multisección, comparativos anuales, utilidad neta inventada, mix en la primera entrega de drivers, CRUD de gastos en Análisis.`

S7. Sync design (Fase 1 prepared): stable id `opticaId|periodo|recomendacionId`, `updatedAt` LWW, estados HECHO/EN_CURSO/NO_APLICA; remote must gain `estado`, `periodo`, `updated_at`, UPDATE/DELETE RLS, id as text for stable keys.

S8. Mix driver (authorized): prior-month `margen_por_categoria` from RPC; mix effect on gross margin `Σ (share1_i − share0_i) × gm0_i × V1` when V0>0 and categories align; fourth driver in `ResultadoDrivers` top list (top 3 still shown, or top 4 — show top by |S/| including MIX).

## Tasks

- [x] T1 S1 S7 — Remote schema + RLS for feedback seguimiento — route: inline — commit: 1363cfc6
- [x] T2 S1 S7 — DTO + SyncFinanzas upload/download LWW — route: inline — commit: 1363cfc6
- [x] T3 S2 S5 S8 — RPC prior categories + MIX driver + home UI — route: inline — commit: 173594fd
- [x] T4 S3 — Plan cumplimiento hechas vs abiertas on coach — route: inline — commit: 173594fd

## Log

L1. User (verbatim package): Fase 3 — Multi-dispositivo y profundidad; Sync remoto de seguimiento; Mix como cuarto driver; Cumplimiento del plan (hechas vs abiertas) y utilidades neta solo si política fiscal; fuente resumen_diario / rpc_analisis_mensual.

L2. Implemented T1–T4 on `feat/analisis-financiero-fase3`. Utilidad neta skipped (blocked). GGA required moving `uploadConfiguracionFinanciera` from `DownloadSyncCoordinator` to `UploadSyncCoordinator`. Full `testDebugUnitTest` green before commits.

L3. Work-unit commits: `1363cfc6` sync seguimiento LWW; `173594fd` mix driver + cumplimiento. Slice PRs still need approved GitHub issue when stacking to main.
