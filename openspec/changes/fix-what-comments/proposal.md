## Proposal: Fix WHAT-comments across codebase

### Intent

~250 Kotlin files contain KDoc blocks and inline comments that restate WHAT the code does (e.g. `// ─── Section Header ────`, `/** Persiste los datos de sesión SaaS en DataStore */`). AGENTS.md rule: *"No comments explaining WHAT — only WHY. Code should be self-documenting."* Remove WHAT-comments project-wide; keep only comments that explain rationale, constraints, gotchas, or design intent.

### Scope

**In Scope:**
- All `*.kt` under `optoapp/src/main/` and `optoapp/src/test/`
- KDoc class-level blocks that paraphrase the class name
- Section header comment blocks (`// ─── Some Section ────`)
- Method-level KDoc that repeats the method signature
- Test class KDoc listing what tests cover (paraphrases test name)

**Out of Scope:**
- `build/generated/` code — not human-authored
- Public API KDoc that documents contract (parameters, return values, exceptions)
- Valid WHY comments (design rationale, ordering constraints, gotchas, bug workarounds)
- Supabase schema, RLS, edge functions — no DB changes
- `docs/`, `*.md`, Gradle files — only `.kt` sources

### Capabilities

- **New**: None — pure cleanup, no new behavior
- **Modified**: None — existing AGENTS.md rule is being enforced, no spec-level requirements change

### Approach

1. **Pattern inventory**: grep for comment patterns (KDoc blocks, `// ───` headers, class-level `/**` that starts with a verb or noun describing the class)
2. **Bulk removal script**: `sed`/regex over all `.kt` files — strip KDoc blocks that match WHAT patterns but preserve `@param`/`@return`/`@throws` in public API KDoc; strip section headers
3. **Manual review pass**: for each file changed, verify removals; convert borderline WHAT→WHY where intent can be inferred but the comment has value
4. **Guard**: add `detekt` or `ktlint` rule to flag new WHAT-comments (future-proof)
5. **One commit per package**: keep review budget under 400 lines per PR where possible

### Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/**/*.kt` | Modified | Remove WHAT-comments, preserve WHY |
| `optoapp/src/test/java/com/example/optoapp/**/*.kt` | Modified | Remove test KDoc that paraphrases test names |

### Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Over-removing valid public API KDoc | Medium | Script preserves `@param`/`@return`/`@throws` KDoc; manual review catches misses |
| Removing WHY comments with value | Low | Manual review pass; borderline cases reviewed per file |
| Large diff makes review painful | High | Split into one branch/slice per package; use chained PRs |

### Rollback

Revert the commit(s). One commit per package slice means partial rollback is possible.

### Dependencies

None.

### Success Criteria

- [ ] No `// ───` section headers remain in any `.kt` file under `optoapp/src/`
- [ ] No class-level KDoc restating the class name remains
- [ ] All public API KDoc with `@param`/`@return`/`@throws` preserved
- [ ] All genuine WHY comments preserved
- [ ] `./gradlew :optoapp:assembleDebug` succeeds (no syntax breakage from missing blocks)
- [ ] `./gradlew :optoapp:testDebugUnitTest` passes
