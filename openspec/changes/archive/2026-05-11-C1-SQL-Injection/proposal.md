# Proposal: Fix SQL Injection in Paciente Search

## Intent

Fix a SQL injection vulnerability in `SupabasePacienteRepository.getAll()` where raw user input is interpolated into a Supabase `.or()` PostgREST filter string. An attacker can bypass RLS-filtered queries and read data from other ópticas by crafting a search string like `%,id.neq.null`.

## Scope

### In Scope
- Escape `search` input in `SupabasePacienteRepository.ts` using the existing `escapeIlikeFragment()` pattern from `lib/pacientes.ts`
- Remove duplication by reusing or co-locating the escape utility

### Out of Scope
- Changes to Android or Supabase backend
- Changes to `lib/pacientes.ts` (already correct)
- New RLS policies or schema migrations

## Capabilities

### New Capabilities
None

### Modified Capabilities
None

## Approach

Adopt the existing `escapeIlikeFragment()` pattern from `lib/pacientes.ts` (lines 31–33) which escapes `\`, `%`, and `_` characters before interpolation. This is a minimal, proven fix that maintains backward compatibility.

Alternative considered: parameterized queries via `.or()` — Supabase PostgREST `.or()` does not support parameterization, so escaping is the correct approach.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `web/src/data/repositories/SupabasePacienteRepository.ts` | Modified | Escape search term before `.or()` interpolation |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Escaping too aggressively breaks valid searches with `%` or `_` | Low | `%` and `_` are wildcard characters in `ilike`; escaping them is correct behavior. Users don't intentionally use SQL wildcards in patient search. |
| Duplicate `escapeIlikeFragment()` definitions drift | Low | Export from `lib/pacientes.ts` or move to shared utility; address in apply phase. |

## Rollback Plan

Revert the single commit. The change is localized to one file and has no schema or dependency impact.

## Dependencies

None

## Success Criteria

- [ ] `SupabasePacienteRepository.ts` escapes the search term before `.or()` interpolation
- [ ] Existing search functionality continues to work for normal inputs
- [ ] No new `any` types or catch-and-silence patterns introduced
