# Multi-Tenant Lint Guide — Room Queries

## The Rule

**Every Room `@Query` that accesses an entity with an `opticaId` field MUST include `opticaId = :opticaId` in its WHERE clause.**

Skipping this filter is a **data leak** — one user could read another optica's rows.

---

## Entities with `opticaId`

The following entities are multi-tenant and require the filter:

| Entity | Tenant Filter |
|--------|--------------|
| `PacienteEntity` | `opticaId = :opticaId` |
| `EvaluacionEntity` | `opticaId = :opticaId` |
| `DispensacionEntity` | `opticaId = :opticaId` |
| `ServicioExtraEntity` | `opticaId = :opticaId` |
| `PagoEntity` | `opticaId = :opticaId` |
| `MonturaEntity` | `opticaId = :opticaId` |
| `ProveedorEntity` | `opticaId = :opticaId` |
| `OrdenCompraEntity` | `opticaId = :opticaId` |
| `GastoOperativoEntity` | `opticaId = :opticaId` |
| `ConflictRecord` | `opticaId = :opticaId` |

---

## Enforcement

### Manual review checklist

- [ ] Every `@Query` references `opticaId` in WHERE
- [ ] No `@Query("SELECT * FROM ...")` without filter
- [ ] JOIN queries filter the tenant on the main entity

### Counterexample

```kotlin
// ❌ LEAK — returns rows from all opticas
@Query("SELECT * FROM pacientes WHERE nombre LIKE :query")
suspend fun searchByName(query: String): List<PacienteEntity>
```

### Correct

```kotlin
// ✅ scoped to the current optica
@Query("SELECT * FROM pacientes WHERE opticaId = :opticaId AND nombre LIKE :query")
suspend fun searchByName(opticaId: String, query: String): List<PacienteEntity>
```

---

## Room DAO test guide

When testing DAOs in isolation with `Room.inMemoryDatabaseBuilder`:

1. Every test row must set `opticaId` to the test's tenant ID.
2. Queries without `opticaId` in WHERE may silently return rows from other tenants set up by other tests — always verify the tenant filter in assertions.
3. Prefer parametrized DAO tests that pass `opticaId` explicitly rather than hardcoding it.

---

## Summary

> If the entity has `opticaId`, every `@Query` must filter by it.
> No exceptions unless the query is intentionally cross-tenant (e.g. admin tools).
