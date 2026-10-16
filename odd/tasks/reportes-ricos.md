# Reportes ricos (Must #5)

Objective: Evidence and export — period KPIs, rich report families, and **operational calculations** (e.g. sales by seller → commissions/bonuses). Not a coach; that is Análisis.  
Branch: `feat/reportes-ricos` (from `main`)  
Delivery: `auto-chain` + `stacked-to-main`  
Cycle: ODD → Strict TDD → GGA → RDD when high risk (PDF/PII, payroll math, Room migration) → Judgment Day opt-in  
Runner: `./gradlew :optoapp:testDebugUnitTest --stacktrace`

## Product job (user intent)

- **Does**: show what happened; compare periods; export PDF; compute owed amounts (commissions) from rules + attributed sales.
- **Does not**: prioritize “qué hacer mañana” / coaching copy (Análisis).

## Specs

S1. SRS RF-REP-03 (verbatim): `Reportes de pacientes (altas, retención, demografía)` — F (Must #5).

S2. SRS RF-REP-04 (verbatim): `Reportes de ventas detallados y cuadros comparativos entre periodos` — F (Must #5).

S3. SRS RF-REP-05 (verbatim): `Reportes de inventario (rotación, stock, monturas)` — F (Must #5).

S4. SRS RF-REP-06 (verbatim): `Estadísticas clínicas (emetropías y diagnósticos, lentes, monturas asociadas)` — F (Must #5).

S5. Roadmap §10 Must #5 (verbatim): `Reportes ricos` · ancla `RF-REP-03..06` · nota `Pacientes, ventas, inventario, comparativos, emetropías, monturas, lentes`.

S6. User (2026-10-15, verbatim intent): `lo que quiero con los reportes es ademas de lo que ya esta sugerido, ejemplo se pueda ver la venta de los vendedores y poder sacar sus comisiones, bonos y cosas asi, aunque no se si va en esta seccion`.

S7. Preserve RF-REP-01/02: period selector + basic PDF; rich modules as tabs/sections. OpenSpec period windows stay authoritative.

S8. Sales reports use real costs / `costoIncompleto` flags (Must #3) — no fake zero COGS.

S9. **Seller = óptica membership user** (not a free-text name). Owner/gerente already create users and assign roles (`AppRoles`: admin, gerente, especialista, asesor/asesora, ventas). **Especialistas MAY sell** (they already can create dispensaciones) in addition to evaluating. Attribution field: `vendedorUserId` (auth/membership uid) on dispensación/servicio; default = current user at create; admin/gerente may reassign. Stats: sales by user, mix by role, commission/bonus inputs. See “Commissions path”.

S10. Out of scope for this feature’s first slices: Analista UX; inventing commission product rules without user confirmation; OptoWeb; RF-FAC.

S11. UX/UI (Must #5): evidence-first IA — see **UX/UI plan** below. Visually distinct from Analista coach (tables, filters, exports OK here).

S12. User (2026-10-16, verbatim): `todo` — authorize full Must #5 apply: UX tabs + real-data modules + seller attribution + Equipo/comisiones path (commission % stays **configurable**, default 0 until dueño sets rules — do not invent fixed % without confirmation).

## Commissions path (analysis)

**Today (code):** no SRS RF for commissions. OT has `updatedBy` only; `Pago.userId` = who recorded payment, not seller of the sale. Roles already support multi-seller workforce via membership (`canManageUsers` for admin/gerente; `especialista` in `ROLES_ESCRITURA_CLINICA` + evaluaciones).

**Product decision (user 2026-10-15):** seller is the **membership user**; specialists sell too; stats include per-user (and optionally per-role) performance.

**Seller pool for attribution / stats / commissions**

| Rol | Puede vender (OT/servicio) | En stats de ventas |
|-----|----------------------------|--------------------|
| admin, gerente | yes | yes (if they registered sales) |
| especialista | yes (+ evaluar) | yes |
| asesor / asesora / ventas | yes | yes |

**Where it belongs**

| Layer | Role |
|-------|------|
| **Reportes** | Ventas por usuario/vendedor, por rol, liquidación comisión/bono, PDF — **yes** |
| **Config** | Commission/bonus rules keyed by user and/or role — **config** |
| **Análisis** | Optional coaching from aggregates (“concentración en un usuario”) — **not** the calculator |

**Suggested implementation order**

1. **Attribution:** `vendedorUserId` on dispensación + servicio_extra; default = session user; admin/gerente can change; legacy → null / “Sin atribuir”.
2. **Directory:** resolve display name from membership list (same users dueño already manages).
3. **Rules:** config % / bonos by role and optional per-user overrides (default 0%; no invented %).
4. **Reportes slice:** “Equipo” — ventas por usuario, filtros por rol, comisión del periodo + PDF.
5. **Stats reuse:** same aggregates feed Reportes charts and later Analista tips.

## UX/UI plan (Reportes)

### Job of the screen
Answer: “¿Qué pasó en el periodo y qué puedo exportar / liquidar?” — evidence, not coaching.

### Current UI (gap) — verified 2026-10-16
- Period dropdown + date picker + KPI cards (vendido/cobrado/margen/ticket…) + movimiento list + basic PDF.
- No tabs for pacientes / comparativos / inventario / clínicas / equipo.
- KPIs are REAL (Room + `CostoVentaCalculator`); UX tabs not implemented.

### Target IA
1. **Period chrome** — keep Diario/Semanal/Mensual/Anual/Total + date + PDF action.
2. **Module tabs** (or segmented control): Resumen | Ventas | Pacientes | Inventario | Clínico | Equipo.
3. **Resumen** — KPI strip + optional chart (reuse OptoBar/Line); not recommendation cards.
4. **Ventas** — detail + period-over-period compare; `costoIncompleto` badges.
5. **Equipo** — by membership user / role; commission due; export liquidación PDF.
6. **Others** — aggregate tables; drill-down optional; minimize PII in default PDF.

### Components
| Piece | Notes |
|-------|-------|
| OptoKpiCard, MoneyFormatter, OptoDatePicker | existing |
| Chart wrappers | from costos |
| New: `ReportModuleTabs`, `EquipoComisionesTable`, compare period picker | this feature |

### Anti-patterns
- Recommendation / “qué hacer ahora” blocks (belongs in Analista).
- Hiding export behind Análisis navigation.

### Accessibility / copy
- Tú-neutral Spanish `strings.xml`; clear empty states per tab.

## Tasks

| ID | Specs | Route | Status | Evidence |
|----|-------|-------|--------|----------|
| T0 | S11 | inline | done | UX/UI plan added (L5) |
| T1 | S6–S9,S11 | inline | done | KPIs REAL; UX tabs missing; no vendedorUserId (L7) |
| T-attr | S9,S12 | inline | done | Room v57 `vendedorUserId` + create defaults + sync + supabase mig |
| T-tabs | S7,S11,S12 | inline | done | `ReportModule` tabs; Resumen KPIs / Ventas list / placeholders |
| T2 | S2,S7,S8 | inline | pending | Ventas detallados + comparativos periodo |
| T3 | S1 | inline | pending | Pacientes altas/retención/demografía |
| T4 | S3 | inline | pending | Inventario rotación/stock/monturas |
| T5 | S4 | inline | pending | Clínicas emetropías/diagnósticos/lentes/monturas |
| T-equipo | S6,S9,S12 | inline | pending | Ventas por usuario + comisión % configurable (default 0) |
| T6 | S7,S12 | inline | pending | PDF upgrade path (sectioned rich export) |
| T7 | all | inline | pending | GGA; RDD/JD on migration + export+PII |

**Merge note:** Analista stack also plans Room 56→57 (seguimiento). Before merging both to main, rebase so Reportes becomes 57→58 (or combine migrations). Do not ship two conflicting 56→57 definitions.

## Log

L1. User (2026-10-15): same request as analista-negocio — analyze/plan Análisis + Reportes ricos against SRS with full Gentle AI cycle.

L2. Exploration: Must #5 is distinct from Must #4; current Reportes = period KPIs + basic PDF + real costs on movements; RF-REP-03..06 missing.

L3. Plan: ship after Must #4 unless user prioritizes exports first; share chart/MoneyFormatter foundations from costos.

L4. User (2026-10-15): Reportes should also support seller sales + commissions/bonuses; unsure of section. Analysis: belongs in Reportes (+ config rules), not Análisis; needs seller attribution first — no dedicated vendedor field today.

L5. User (2026-10-15): Seller should be the membership **user** (dueño/gerente create users + roles); especialistas also sell besides evaluating; add to statistics. Locked: `vendedorUserId` = membership uid; seller pool includes especialista + comerciales + gestores who sell.

L6. User (2026-10-15): Include UX/UI in general plan for both features; apply starts with Analista. Reportes UX plan recorded (tabs + Equipo; no coach blocks).

L7. User (2026-10-16): `ahora reportes ricos, mejorar uxui creo qye ya lo as hecho y que los datos sean reales, verificalo.` T1 verify: (1) UX/UI plan written in this doc only — **not implemented**. (2) Current financial KPIs/movimientos are **REAL**. (3) `vendedorUserId` / Equipo / RF-REP-03..06 still absent.

L8. User (2026-10-16): `todo` — full Must #5 apply authorized. Branch `feat/reportes-ricos` from main. Commission % configurable default 0 (S12). Start T-attr then T-tabs → T2…T7.

L9. T-attr done (2026-10-16): `vendedorUserId` on dispensaciones + servicios_extra; MIGRATION_56_57; supabase `20261016150000_vendedor_user_id.sql`; sync DTOs; create defaults via `VendedorAttribution` + auth uid; edits preserve seller. GREEN `VendedorAttributionTest` + `Migration56To57Test`. Next: T-tabs.

L10. T-tabs done (2026-10-16): `ReportModule` + ScrollableTabRow; Resumen = KPIs/cobros/composición; Ventas = detalle list (real costs); Pacientes/Inventario/Clínico/Equipo placeholders. GREEN `ReportModuleOrderTest`. Next: T2 comparativos.
