# Business Audit: "Mi Negocio" Module — OptoApp

**Auditor**: Senior Software Architect, 15+ years financial/ERP systems  
**Date**: 2026-07-06  
**Scope**: Business-level audit (NOT code review). Product, metrics, data accuracy, UX.

---

## Executive Summary

"Mi Negocio" asks the right questions but delivers incomplete answers. The main screen gives a reasonable 30-second pulse (sales, collections, pending balance, margin), but **the detail screen is effectively broken** — 3 out of 5 sections are always empty because the backend RPC function never returns the required data. The recommendation engine has sensible categories but contains threshold values that will generate false positives for seasonality, and the "margin" label is ambiguous between gross and net. The module is a promising foundation, but **shipping it as-is would erode trust** because owners will see empty sections and question whether their data is being handled correctly.

---

## What Works Well

### 1. Main Screen: Good 30-Second Pulse
The `ResumenCard` shows exactly four numbers:
- **Vendiste** (total sales)
- **Cobraste** (total collections)
- **Saldo pendiente** (accounts receivable)
- **Margen** with a human-readable sentence

This is the right density. An optica owner can glance and understand: "Did I sell? Did I collect? What's still owed? Am I making money?"

### 2. Business Language in Labels
- "Vendiste" / "Cobraste" — second person, natural, what an owner would say
- "De cada S/100 que vendés, te quedan S/ X" — excellent translation of a margin ratio into plain language
- "Plata que entró y salió" — colloquial, clear

### 3. Recommendation Feedback Loop
The thumbs-up/thumbs-down on recommendations is smart. It captures real sentiment and enables future ML tuning. Most products at this scale don't have this.

### 4. Offline Fallback
When the network is down, `fallbackToRoom` degrades gracefully (limited data but not zero). The yellow banner "Datos limitados — sin conexión" is honest and well-placed.

### 5. Month Navigation
The left/right month picker is intuitive. The default to first-of-month is correct.

### 6. Role-Based Access Control
`canViewBiAndReports` check prevents unauthorized roles from seeing financial data. The lock screen is clear.

---

## What's Wrong or Misleading

### 🔴 P0: Detail Screen Sections Are Non-Functional (Data Pipeline Broken)

**The problem**: The Supabase RPC `rpc_analisis_mensual` returns exactly 10 fields:
```
ventas_mes, cobros_mes, costo_mes, gastos_mes, saldo_pendiente,
margen_neto_pct, ticket_promedio, cantidad_ventas,
ventas_mes_anterior, variacion_ventas_pct
```

But `AnalisisMensual.fromJson()` expects **15+ fields** — and critically, it parses:
- `margen_por_categoria` → **always empty** (key not in RPC response)
- `stock_estancado` → **always empty** (key not in RPC response)
- `proyeccion_caja` → **always null** (key not in RPC response)
- `valor_inventario` → **always 0.0** (key not in RPC response)

**Result**: When a user clicks "Ver análisis completo" they see:

| Section | Shows | Reality |
|---------|-------|---------|
| "Plata que entró y salió" | Costos = **S/ 0**, Ganancia = **inflated** | `costoDeVentas()` sums `margenPorCategoria.costos` = 0 because the list is empty. The RPC *does* return `costo_mes` but it's **never read** by the Kotlin parser. |
| "Lo que más te deja" | **"Sin datos de categorías"** | Always |
| "Productos sin vender" | **"Sin productos estancados"** | Always |
| "Plata que vas a tener" | **"Sin datos de proyección"** | Always |
| "Pacientes con deuda pendiente" | ✅ Works (separate RPC) | This is the only functional section |

**Business impact**: An optica owner who clicks "Ver análisis completo" sees mostly empty sections and incorrect cost/gain numbers. They will either:
- Think the system is broken (trust erosion), OR
- Think they have no stagnant stock, no category data, no cash flow projection (bad decisions)

The Ganancia bar is **mathematically wrong** — it shows `ventasMes - 0 - gastosMes` instead of `ventasMes - costoReal - gastosMes`.

### 🔴 P0: "Margen" Label Is Ambiguous (Gross vs Net)

The metric shown is `margenNetoPct = (ventas - costo - gastos) / ventas × 100`. This is **net margin** (after operating expenses).

The text says: *"De cada S/100 que vendés, te quedan S/ X"*

**The problem**: In an optica, "margen" almost always means **gross margin** (markup on product cost). The owner thinks in terms of: "I buy frames at S/50 and sell at S/150 = 66% margin". If they see a number like 8% (net), they'll panic — not realizing that's *after* rent, salaries, and utilities.

A typical optica has:
- Gross margin: 50-70% (lenses) / 30-50% (frames)
- Net margin: 10-20% (after all expenses)

Showing net margin without explicitly labeling it as "Margen neto (después de gastos)" is misleading.

### 🔴 P1: Seasonality Warning Is Miswired

Line 131 of the ViewModel:
```kotlin
mostrarAdvertenciaEstacionalidad = analisis?.esOffline == true
```

The warning says: *"Este cálculo se basa en pocos meses. Podría no ser preciso."*

**The bug**: The seasonality warning triggers when **offline**, not when there's little historical data. The field is named `mostrarAdvertenciaEstacionalidad` but the logic checks `esOffline`. These are completely different concerns. The warning about "pocos meses" should fire when, say, the optica has < 3 months of data — not when the phone has no signal.

### 🟡 P2: Monthly Comparison Without Seasonality Adjustment

`ALERTA_CAIDA` compares `ventasMes` vs `ventasMesAnterior` (month-over-month). The threshold is a 10% drop.

**The problem**: In an optica:
- December → January: sales naturally drop 20-40% (post-holiday)
- January → February: sales naturally climb (back-to-school exams)
- February → March: variable (Carnival effect in Peru)

A 10% MoM threshold will generate **false alarms** every January. The owner gets an ALTA priority alert saying "Caída en ventas" when it's just normal seasonality.

**Root cause**: No YoY comparison is implemented. The RPC computes `ventas_mes_anterior` but NOT `ventas_mes_ano_anterior`. Comparing "this January" to "last January" would eliminate 90% of false positives.

### 🟡 P2: "Saldo Pendiente" Is Actually Total AR

The saldo shown is the last daily snapshot of total accounts receivable across ALL outstanding sales. It's labeled as "Saldo pendiente" but it's really "Total por cobrar". 

**The issue**: If the current month had S/ 10,000 in sales but S/ 8,000 was collected immediately (patients paid at pickup), the "saldo pendiente" might show S/ 20,000 from older sales. The owner asks: *"But I only sold S/ 10,000 this month — why does it say I'm owed S/ 20,000?"*

It would be clearer to show both: "Por cobrar este mes" and "Deuda total".

### 🟡 P2: Ticket Promedio and Cantidad de Ventas Are Hidden

The RPC returns `ticket_promedio` and `cantidad_ventas` but the UI never displays them. These are **critical** metrics for an optica:
- Average ticket tells the owner if customers are buying premium or budget
- Sales count tells if they're getting foot traffic

### ⚪ P3: Offline Fallback Margin = 0.0

In `fallbackToRoom`, `margenNetoPct` is hardcoded to `0.0`:
```kotlin
AnalisisMensual(
    ...
    margenNetoPct = 0.0,
    ...
)
```

The main screen will show "De cada S/100 que vendés, te quedan S/ 0". An owner seeing this will think they're losing everything, not that the data is offline. The offline banner partially mitigates this, but a 0% margin displayed prominently is alarming.

---

## What's Missing

### 🔴 P0: Detail Screen — Data Pipeline Fix

The RPC must be extended to return `margen_por_categoria`, `stock_estancado`, `proyeccion_caja`, and `valor_inventario`. These tables exist in the schema (`margen_por_categoria`, `monturas`, `resumen_diario` has inventory data) but the RPC never queries them.

Alternatively, the Android client needs separate RPC calls for each section.

### 🟡 P2: Year-over-Year Comparison

An optica owner's #1 question: *"How is this month compared to the same month last year?"*

Example: "Vendiste S/ 12,000 this January — that's 15% MORE than last January (S/ 10,430)." This is infinitely more valuable than "Vendiste S/ 12,000 this month."

### 🟡 P2: Target vs Actual

The financial configuration already stores:
- `margen_neto_objetivo` (default 15%)
- `ticket_promedio_objetivo`

But these are never displayed. Imagine showing:
- Margen real: 12% | Meta: 15% | 🔴 Faltó 3%
- Ticket promedio: S/ 280 | Meta: S/ 320 | 🟡 Faltó S/ 40

This turns "Mi Negocio" from a passive dashboard into an active management tool.

### 🟡 P2: Expense Breakdown

The detail screen shows total "Gastos" but no breakdown. An optica owner needs to see:
- Alquiler: S/ 3,000 (40% of expenses)
- Personal: S/ 2,500 (33%)
- Servicios: S/ 800 (11%)
- Proveedores: S/ 600 (8%)
- Marketing: S/ 300 (4%)
- Otros: S/ 300 (4%)

Without this, "Reducir gastos operativos" is a vague scolding, not actionable advice.

### 🟡 P2: Top-Selling Products

An optica owner walks into their shop and asks: *"Which frames sold this month?"* This is table-stakes for any retail BI tool. The data exists in `ventas` with product references. Showing "Top 5 productos más vendidos" would be high-impact.

### 🟡 P2: Conversion Funnel

Opticas typically have a funnel:
1. Eye exams scheduled → completed
2. Exams → prescriptions written
3. Prescriptions → frames selected
4. Frames selected → purchased

A basic funnel would be revolutionary for most opticas: "De 100 pacientes que entraron, 80 compraron." This requires data from `pacientes`, `ventas`, and maybe `citas` (if that table exists).

### ⚪ P3: Payment Method Split

"How many pay with cash vs credit vs debit?" This matters for cash flow management and merchant fee optimization.

### ⚪ P3: New vs Returning Patients

"How many of this month's patients are new vs returning?" This signals brand health.

### ⚪ P3: Inventory Turnover Rate

"How many times per year does my inventory rotate?" A rate of <1 means you're sitting on stock. The data exists in `monturas`.

---

## Recommendation Engine Audit

### COBRAR (ALTA) ✅ Good
- Triggers when deuda > S/ 3,000 (configurable) OR any debtor > 30 days overdue
- Includes top 3 debtor names → highly actionable
- **Suggestion**: Also show aging buckets: 0-30, 31-60, 61-90, 90+

### MEJORAR_PRECIO (ALTA) ⚠️ Needs tuning
- Triggers when any category has margin <10% with ≥5 sales
- Target: 25% margin
- **Issue**: In an optica, "Servicios Extra" (adjustments, repairs) might naturally have low margins because they're labor-intensive. The recommendation should exclude service categories from this rule, or check if the category is a product (lente/montura) before suggesting price increases.

### LIQUIDAR_STOCK (MEDIA) ⚠️ Threshold concern
- 180 days (~6 months) is the default threshold
- **Issue**: For frame inventory, 180 days is too short for premium frames (which may take 9-12 months to sell) but fine for economy frames. The threshold should vary by category (premium = 365d, standard = 180d, economy = 90d). Also, the data never appears because the RPC doesn't return it.

### VENDER_MAS_DE (MEDIA) ✅ Good
- Identifies high-margin, high-contribution categories and suggests promotion
- The logic (margin >35% AND contribution >25% of total profit) is solid
- **Suggestion**: The action "Crear promocion destacada" could link to actual discount features in the app

### ALERTA_CAIDA (ALTA) 🔴 False positive risk
- Month-over-month comparison with 10% threshold
- **Issue**: As noted, seasonality causes false positives in every January
- **Fix**: Compare against same month last year, or use a 3-month rolling average comparison
- **Edge case**: First month of data → `ventas_mes_anterior` = 0 → `variacion_ventas_pct` = null → no alert. This is correctly handled.

### REDUCIR_GASTO (MEDIA) ⚠️ Needs expense breakdown
- Triggers when gastos/ventas > 40%
- **Issue**: Without an expense breakdown, the recommendation is not actionable. "Eliminar las no esenciales" is vague.
- **Better**: "Tus gastos de personal son 45% de las ventas (meta: <35%). Considera revisar horarios."

---

## UX Coherence

### Main Screen Flow
✅ Clear narrative: Month picker → Summary → Recommendations → CTA to detail

### Detail Screen Flow
🔴 Broken narrative:
- "Plata que entró y salió" — shows Costos = 0 (misleading), Ganancia = inflated
- "Lo que más te deja" — empty
- "Productos sin vender" — empty
- "Plata que vas a tener" — empty
- Only "Pacientes con deuda pendiente" works

### Expand/Collapse Pattern
✅ Good use of `ExpandableSection` — keeps the screen scannable

### Navigation
✅ Main → "Ver análisis completo" → detail
✅ Detail rows clickable → navigate to edit dispensación/servicio (for debtors)

---

## Competitive Gaps (vs Basic BI Tools)

| Feature | OptoApp | Google Data Studio | Power BI | QuickBooks |
|---------|---------|-------------------|----------|------------|
| 30-second pulse | ✅ | ❌ (too flexible) | ❌ | ✅ |
| Detail drill-down | 🔴 (broken) | ✅ | ✅ | ✅ |
| YoY comparison | ❌ | ✅ | ✅ | ✅ |
| Target vs actual | ❌ | ✅ | ✅ | ✅ |
| Expense breakdown | ❌ | ✅ | ✅ | ✅ |
| Custom date ranges | ❌ (month only) | ✅ | ✅ | ✅ |
| Export (PDF/CSV) | ❌ | ✅ | ✅ | ✅ |
| Trend lines | ❌ | ✅ | ✅ | ✅ |
| Top products | ❌ | ✅ | ✅ | ✅ |
| Employee performance | ❌ | ❌ | ✅ | ❌ |

The module is behind basic BI tools in drill-down depth and comparison features but ahead in simplicity. The fix priority should be: **make the existing data work correctly** before adding new features.

---

## Priority Recommendations

### P0 — CRITICAL (Fix before shipping)

| # | What | Why |
|---|------|-----|
| 1 | **Fix the RPC pipeline** — extend `rpc_analisis_mensual` to return `margen_por_categoria`, `stock_estancado`, `valor_inventario`, `proyeccion_caja` OR create separate RPCs per section | 3/5 detail sections are always empty; costos = 0 in the bar chart |
| 2 | **Use `costo_mes` from RPC** — read the existing `costo_mes` field in `AnalisisMensual.fromJson()` and use it for the bar chart's "Costos" row | Costos = 0 is mathematically wrong |
| 3 | **Fix seasonality warning wiring** — `mostrarAdvertenciaEstacionalidad` should check month count, not `esOffline` | Warning fires for wrong reason |

### P1 — HIGH (Fix soon)

| # | What | Why |
|---|------|------|
| 4 | **Rename "Margen" to "Margen neto"** — or show both gross and net | Ambiguity erodes trust when owners see 8% and think they're failing |
| 5 | **Add YoY comparison to ALERTA_CAIDA** — compare vs same month last year, not just previous month | False positives every January |
| 6 | **Show ticket promedio and cantidad_ventas** on main screen or detail | Hidden data that owners ask about daily |
| 7 | **Clarify "Saldo pendiente"** — label as "Total por cobrar" or add sub-label | Confuses owners when AR doesn't match monthly sales |

### P2 — MEDIUM (Valuable additions)

| # | What | Why |
|---|------|------|
| 8 | **Add target vs actual display** — use `configuracion_financiera` targets | Turns dashboard into management tool |
| 9 | **Add expense breakdown by category** — group gastos_operativos | Makes REDUCIR_GASTO actionable |
| 10 | **Add top 5 productos** from ventas data | Table-stakes retail metric |
| 11 | **Adjust stock alert thresholds by category** — premium ≠ economy frames | Wrong thresholds = bad decisions |
| 12 | **Add trend arrows** (↑/↓/→) next to Vendiste and Cobraste | Immediate direction understanding |

### P3 — LOW (Nice to have)

| # | What | Why |
|---|------|------|
| 13 | Payment method split | Cash flow planning |
| 14 | New vs returning patient ratio | Brand health indicator |
| 15 | Export to PDF/CSV | Owner wants to share with accountant |
| 16 | Inventory turnover rate | Capital efficiency metric |

---

## Edge Cases Summary

| Scenario | Current Behavior | Assessment |
|----------|-----------------|------------|
| **No data** | Shows "Sin datos para este mes" | ✅ Correct |
| **One month of data** | ALERTA_CAIDA won't fire (variacion = null). Seasonality warning NOT shown (offline check bug) | ⚠️ Warning should show |
| **Years of data** | Only previous month comparison; no YoY | ⚠️ Missed opportunity |
| **December → January** | False ALERTA_CAIDA | 🔴 Known issue |
| **Offline** | Limited data, yellow banner, margin=0% | ⚠️ 0% margin alarming |
| **Premium frames (slow sale)** | LIQUIDAR_STOCK after 180 days | ⚠️ Too aggressive |
| **New optica (first month)** | ventas_mes_anterior = 0 → variacion = null → no alert, no comparison | ✅ Safe |
| **No gastos_operativos entered** | gastosMes = 0 → REDUCIR_GASTO never triggers → Ganancia = ventas - 0 | ✅ Safe |
| **No deudores** | COBRAR not generated | ✅ Safe |
| **All deudores current (<30 days)** | COBRAR not generated if total < 3000 | ✅ Reasonable |

---

## Conclusion

"Mi Negocio" has the skeleton of a great business dashboard. The main screen answers "How is my business doing in 30 seconds?" correctly and with good business language. The recommendation engine covers the right categories.

**But the detail screen is non-functional for the majority of its sections.** This is not a minor bug — it's a data pipeline gap that makes the entire "Ver análisis completo" path misleading. The cost of goods sold shows as zero, making the gain calculation incorrect. Category analysis, stagnant stock, and cash flow projection show as "no data" despite having the underlying schema in place.

**Recommendation**: Fix the P0 data pipeline issues before any feature work. The schema is already there (margen_por_categoria table, monturas table, resumen_diario inventory columns) — the RPC just needs to query them. Once the detail screen works, prioritize YoY comparison and expense breakdown as the highest-value additions.
