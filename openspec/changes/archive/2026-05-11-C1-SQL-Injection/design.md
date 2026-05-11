# Design: Fix SQL Injection in Paciente Search

## Technical Approach

Apply `escapeIlikeFragment()` to user input before interpolation into PostgREST `.or()` filter. Extract the function from `lib/pacientes.ts` (where it exists module-private) to a shared `lib/sanitize.ts` to prevent duplication. The repository imports it and escapes the search term identically to the existing `applySearchOr()` pattern in `lib/pacientes.ts`.

## Architecture Decisions

### Decision: Escaping over Parameterization

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Parameterized `.or()` | Not supported by Supabase PostgREST | Rejected |
| Escaping via `escapeIlikeFragment()` | Works today; standard pattern in codebase | **Chosen** |

**Rationale**: Supabase PostgREST `.or()` is a string-based filter with no parameter binding. Escaping `\`, `%`, and `_` (PostgREST `ilike` special chars) neutralizes injection while preserving all normal search behavior.

### Decision: Extract to Shared Utility vs Export from `lib/pacientes.ts`

| Option | Tradeoff | Decision |
|--------|----------|----------|
| A: Export from `lib/pacientes.ts` | Fewer files touched; couples repos to a domain module | Rejected |
| B: Extract to `lib/sanitize.ts` | Clean separation; follows `lib/supabase/db-error.ts` precedent; new file | **Chosen** |

**Rationale**: `escapeIlikeFragment` is a general PostgREST utility, not pacientes-specific. Extracting prevents future duplication when other repositories need `ilike` escaping. Follows the existing pattern of `lib/supabase/` for shared DB concerns. The spec requires a single shared location — exporting from a domain module doesn't meet that as cleanly.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `web/src/lib/sanitize.ts` | **Create** | Exported `escapeIlikeFragment(s: string): string` |
| `web/src/lib/pacientes.ts` | **Modify** | Remove module-private `escapeIlikeFragment` (lines 31-33); import from `@/lib/sanitize` |
| `web/src/data/repositories/SupabasePacienteRepository.ts` | **Modify** | Import `escapeIlikeFragment`; escape search term on line 17; skip `.or()` for empty strings |

## Exact Code Changes

### 1. Create `web/src/lib/sanitize.ts`

```ts
/** Escapes \, %, and _ for safe use inside PostgREST ilike patterns. */
export function escapeIlikeFragment(s: string): string {
  return s.replace(/\\/g, "\\\\").replace(/%/g, "\\%").replace(/_/g, "\\_");
}
```

### 2. Modify `SupabasePacienteRepository.ts`

Replace lines 15-17:

```diff
     if (options?.search) {
       const s = options.search.trim();
-      query = query.or(`nombre_completo.ilike.%${s}%,telefono.ilike.%${s}%,historia_optometrica.ilike.%${s}%`);
+      if (s.length > 0) {
+        const escaped = escapeIlikeFragment(s);
+        query = query.or(`nombre_completo.ilike.%${escaped}%,telefono.ilike.%${escaped}%,historia_optometrica.ilike.%${escaped}%`);
+      }
     }
```

Add import at top:
```ts
+import { escapeIlikeFragment } from "@/lib/sanitize";
```

### 3. Modify `lib/pacientes.ts`

Remove lines 31-33 (module-private `escapeIlikeFragment`). Add import:
```ts
+import { escapeIlikeFragment } from "@/lib/sanitize";
```

`applySearchOr()` continues using the imported function unchanged.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `escapeIlikeFragment` escapes `\`, `%`, `_` in correct order | Vitest test in `lib/__tests__/sanitize.test.ts` |
| Integration | Normal search returns same results as before | Run existing test suite against real Supabase |
| Security | Malicious payloads neutralized | Test inputs: `%,id.neq.null`, `test_case`, `100\%` |

## Open Questions

None.
