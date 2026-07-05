# Proposal: Fase 8 — 6 Reglas de Recomendación

## Intent

Generar recomendaciones específicas, priorizadas y accionables para el dueño de la óptica, basadas en los indicadores de Fase 7. Cada recomendación dice exactamente qué hacer, con qué cliente/producto, y cuánta plata implica.

## Scope

- **In-scope**: `GenerarRecomendacionesUseCase` (6 reglas), `Recomendacion` domain model, `RecomendacionTipo` enum, `Prioridad` enum, `DatosAccion` data class, `FeedbackRecomendacionUseCase` (guardar 👍/👎), tests unitarios (1 por regla).
- **Out-of-scope**: UI (Fase 9), persistencia de recomendaciones en Room, RPC de Supabase nuevo (evaluación 100% local), feedback loop avanzado (modelo de relevancia — Fase futura).

## Dependencies on Fase 7

| Dependency | What it provides | Used by |
|-----------|-----------------|---------|
| `ObtenerAnalisisMensualUseCase` + `AnalisisMensual` | All 8 indicators incl. `variacionVentasPct`, `margenPorCategoria`, `stockEstancado`, `ventasMes`, `ventasMesAnterior` | All rules |
| `ObtenerDeudoresUseCase` + `List<Deudor>` | Individual debtor list with `saldo`, `diasDeuda`, `pacienteNombre`, `pacienteTelefono` | R1 |
| `ConfiguracionFinancieraDao` + `ConfiguracionFinancieraEntity` | All thresholds (deuda, caída, stock estancado, min ventas) | R1-R5 |

### Extension needed to AnalisisMensual

The RPC `rpc_analisis_mensual` already returns `gastos_mes`, but `AnalisisMensual.kt` doesn't parse it. Add:

```kotlin
val gastosMes: Double = 0.0
```

Parse in `fromJson`:
```kotlin
gastosMes = obj.optDouble("gastos_mes")
```

This is required for R6 ("Gastos que podrías reducir").

### Extension needed to MargenCategoria

`MargenCategoria` currently lacks `unidadesVendidas`. For R2/R4 to check `minVentasParaRecomendar` (an integer count from config):

**Option A (recommended)**: Add `unidadesVendidas: Int = 0` to `MargenCategoria` and update the Supabase RPC to include per-category counts. This matches the plan's examples ("Vendiste 8 monturas").

**Option B**: Use `ventas > 0` as proxy (simpler but imprecise — a category with S/ 1,200 in sales might be 1 expensive frame or 12 cheap ones).

## Architecture Approach

### No new Supabase RPC — pure local evaluation

All 6 rules evaluate locally in the Android UseCase. The engine takes three inputs:
- `AnalisisMensual` (from `ObtenerAnalisisMensualUseCase`)
- `List<Deudor>` (from `ObtenerDeudoresUseCase`)
- `ConfiguracionFinancieraEntity` (from `ConfiguracionFinancieraDao`)

### Data Flow

```
BIViewModel / BIScreen (Fase 9)
       │
       ▼ invoke(opticaId, mes)
┌───────────────────────────────────────┐
│  GenerarRecomendacionesUseCase        │
│                                       │
│  1. val analisis = analisisUC(opticaId, mes)
│  2. val deudores = deudoresUC(opticaId)
│  3. val config = configDao.getByOpticaId(opticaId)
│  4. Run 6 rule functions              │
│  5. Sort + take(5)                    │
│  6. Return Resource<List<Recomendacion>>
└───────────────────────────────────────┘
```

### New domain models

```kotlin
data class Recomendacion(
    val id: String,                  // hash único para feedback
    val tipo: RecomendacionTipo,
    val titulo: String,              // una frase, lenguaje llano
    val detalle: String,             // 2-3 oraciones con nombres y montos concretos
    val impactoEstimado: String?,    // "Impacto estimado: +S/ 320 este mes"
    val prioridad: Prioridad,        // ALTA, MEDIA, BAJA
    val accion: String?,             // qué hacer, paso a paso
    val datosAccion: DatosAccion?   // ids, montos, teléfonos para botones de acción
)

enum class RecomendacionTipo {
    COBRAR, MEJORAR_PRECIO, LIQUIDAR_STOCK,
    VENDER_MAS_DE, REDUCIR_GASTO, ALERTA_CAIDA
}

enum class Prioridad { ALTA, MEDIA, BAJA }

data class DatosAccion(
    val pacienteIds: List<String>? = null,
    val productoIds: List<String>? = null,
    val montoTotal: Double? = null
)
```

### UseCase contracts

```kotlin
class GenerarRecomendacionesUseCase @Inject constructor(
    private val obtenerAnalisisMensual: ObtenerAnalisisMensualUseCase,
    private val obtenerDeudores: ObtenerDeudoresUseCase,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao
) {
    suspend operator fun invoke(opticaId: String, mes: LocalDate): Resource<List<Recomendacion>>
}

class FeedbackRecomendacionUseCase @Inject constructor(
    // Future: guarda feedback 👍/👎 en tabla feedback_recomendaciones
)
```

### Rule evaluation — private functions per rule

Each rule is a private function inside the UseCase:

| Rule | Function | Input | Trigger |
|------|----------|-------|---------|
| R1 | `evaluarReglaCobrar()` | deudores, config | deudaTotal > monto OR any(diasDeuda > diasAlerta) |
| R2 | `evaluarReglaPerdida()` | margenPorCategoria, config | margenPct < 10% AND ventas >= minVentasParaRecomendar |
| R3 | `evaluarReglaStock()` | stockEstancado, config | diasSinVenta > stockEstancadoAlertaDias |
| R4 | `evaluarReglaEstrella()` | margenPorCategoria, config | margenPct > 35% AND contribucion > 25% del total |
| R5 | `evaluarReglaCaida()` | analisis | variacionVentasPct < -caidaVentasAlertaPct |
| R6 | `evaluarReglaGastos()` | analisis | gastosMes/ventasMes > 0.4 |

### Priority sorting

1. ALTA: R1 (deuda), R5 (caída ventas), R2 (margen bajo)
2. MEDIA: R3 (stock estancado), R4 (estrella), R6 (gastos altos)
3. BAJA: (future)
- Max 5 visible. If more than 5 ALTA+MEDIA, trim lowest-priority MEDIA.

## Dependencies / Side Effects

| Change | File | Type |
|--------|------|------|
| Add `gastosMes` to domain model | `AnalisisMensual.kt` | Modify (add field + parse) |
| Add `unidadesVendidas` to domain model (Option A) | `AnalisisMensual.kt` + RPC SQL | Modify (add field + extend RPC output) |
| Create `Recomendacion`, `RecomendacionTipo`, `Prioridad`, `DatosAccion` | `domain/Recomendacion.kt` | New file |
| Create `GenerarRecomendacionesUseCase` | `domain/GenerarRecomendacionesUseCase.kt` | New file |
| Create `FeedbackRecomendacionUseCase` | `domain/FeedbackRecomendacionUseCase.kt` | New file |
| Tests: 1 per rule + edge cases | `test/domain/GenerarRecomendacionesUseCaseTest.kt` | New file |

No Room migration (no schema changes). No OptoRepository changes.

## Size Estimate

| Component | Estimated lines |
|-----------|----------------|
| `Recomendacion.kt` (all models) | ~50 |
| `GenerarRecomendacionesUseCase.kt` | ~250 (6 rules + sort + constructor) |
| `FeedbackRecomendacionUseCase.kt` | ~30 |
| AnalisisMensual extension (gastosMes + unidadesVendidas) | ~5 |
| Tests | ~250 |
| **Total** | **~585** |

## Open Questions

1. **unidadesVendidas en MargenCategoria**: ¿Agregamos el campo y extendemos el RPC (Opción A) o usamos ventas monetarias como proxy (Opción B)? La Opción A es más precisa pero requiere tocar el RPC de Supabase.
2. **FeedbackRecomendacionUseCase**: ¿Guardamos en una tabla Room local, o directamente a Supabase? La propuesta original dice `feedback_recomendaciones` — decidir en design si es local-first con sync o directo.
