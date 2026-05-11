# Skill Registry — OptoApp SaaS

Generated: 2026-05-11

## Agent Instructions (project-level)

### AGENTS.md
- **Path**: `C:\Users\usuario\Desktop\Programacion\OptoAppSaaS\Optoapp-2-0\Optoapp-2-0-saas\AGENTS.md`
- **Compact Rules**:
  - No Co-Authored-By or AI attribution; conventional commits only
  - No comments explaining *what*, only *why*
  - Android: Hilt DI scoped per feature, Compose for all UI, Kotlin Serialization, Clean Architecture (data/domain/presentation), Coroutines+Flow, Repository pattern, ViewModel+StateFlow
  - Web: App Router only, Server Components by default, Zod for API validation, Supabase SSR, Tailwind only, `@/` → `src/`
  - Supabase: migrations only, RLS policies always, `supabase/sql/` for ad-hoc queries
  - Type safety: no `any`, no unchecked casts; no catch-and-silence errors; no unnecessary recompositions; validate all inputs

### .cursorrules
- **Path**: `C:\Users\usuario\Desktop\Programacion\OptoAppSaaS\Optoapp-2-0\Optoapp-2-0-saas\.cursorrules`
- **Compact Rules**:
  - Android native Kotlin app for optometry; Jetpack Compose, MVVM, Room, Hilt, coroutines
  - Offline-first Supabase sync (supabase-kt, Ktor, kotlinx.serialization)
  - Credentials in `local.properties` → BuildConfig; multi-tenant via `optica_id`
  - Automatic OD/OI diagnosis from sphere/cylinder; presbyopia if ADD > 0; anisometropia ≥ 2.00D SE difference; amblyopia ≥ 2 line AV difference
  - Contact lens suggestion by |K1-K2| thresholds; remaining balance in S/., red if > 0
  - Delivery status: only Pendiente and Entregado (no Parcial)
  - Snake_case in PostgreSQL columns and @SerialName; camelCase in Kotlin vars/functions; PascalCase in classes
  - Sync error priority: NOT NULL violations, FK ordering, JWT/RLS authorization

## User-Level Skills

### branch-pr
- **Path**: `C:\Users\usuario\.claude\skills\branch-pr\SKILL.md`
- **Trigger**: Creating, opening, or preparing PRs for review
- **Rules**: Issue-first checks; verify issue exists + linked before PR; gh CLI for all GitHub ops; one squash commit per work unit; conventional commit message; present PR in review-friendly format

### chained-pr
- **Path**: `C:\Users\usuario\.claude\skills\chained-pr\SKILL.md`
- **Trigger**: PRs over 400 lines, stacked PRs, review slices
- **Rules**: Split >400 line changes into chained PRs; each PR must be independently reviewable; base each on previous PR branch; use `@gentle-ai/agentic-chains` app; never split across broken tests; add chain manifest at bottom of each PR body

### cognitive-doc-design
- **Path**: `C:\Users\usuario\.claude\skills\cognitive-doc-design\SKILL.md`
- **Trigger**: Writing guides, READMEs, RFCs, onboarding, architecture, or review-facing docs
- **Rules**: Every doc must solve a reader question before writing; front-load answers in title+first paragraph; one idea per paragraph; eliminate all weasel words; prefer flat hierarchy; use mermaid for non-trivial flows; never start a doc by describing the doc itself

### comment-writer
- **Path**: `C:\Users\usuario\.claude\skills\comment-writer\SKILL.md`
- **Trigger**: PR feedback, issue replies, reviews, Slack messages, or GitHub comments
- **Rules**: Warm + direct; start with context acknowledgment; use constructive structure; back opinions with code evidence; never dismiss — always explain motivation and tradeoffs; end with open question or offer

### go-testing
- **Path**: `C:\Users\usuario\.claude\skills\go-testing\SKILL.md`
- **Trigger**: Go tests, go test coverage, Bubbletea teatest, golden files
- **Rules**: Table-driven tests; prefer `github.com/stretchr/testify/require`; `go vet` before push; coverage threshold 80%; golden files for marshaling/output; Bubbletea tests with teatest; no `_test` pkg for unit tests

### issue-creation
- **Path**: `C:\Users\usuario\.claude\skills\issue-creation\SKILL.md`
- **Trigger**: Creating GitHub issues, bug reports, or feature requests
- **Rules**: Verify issue doesn't exist first; search open+closed before creating; use `gh issue create`; bug template: symptoms → reproduction → expected → actual → environment; feature template: problem → proposed solution → alternatives; add labels and project

### judgment-day
- **Path**: `C:\Users\usuario\.claude\skills\judgment-day\SKILL.md`
- **Trigger**: judgment day, dual review, adversarial review, juzgar
- **Rules**: Blind dual review — Reviewer A (strict) + Reviewer B (pragmatic) on same code without seeing each other; meet to reconcile; fix confirmed issues; re-judge; document final verdict

### skill-creator
- **Path**: `C:\Users\usuario\.claude\skills\skill-creator\SKILL.md`
- **Trigger**: New skills, agent instructions, documenting AI usage patterns
- **Rules**: Required frontmatter (name, description, trigger, rules); keep description ≤250 chars, one physical line, trigger-first; body 180-450 tokens; move examples/schemas/edge cases into local `references/`; output contract must state exact return format

### skill-registry
- **Path**: `C:\Users\usuario\.claude\skills\skill-registry\SKILL.md`
- **Trigger**: Create or update project skill registry; update skills
- **Rules**: Scan `~/.claude/skills/`, `~/.config/opencode/skills/`, project `.claude/skills/`, project skills/ dir; skip sdd-*, _shared, skill-registry; deduplicate by name (project wins); extract compact rules (5-15 lines per skill); scan AGENTS.md, CLAUDE.md, .cursorrules, GEMINI.md, copilot-instructions.md

### work-unit-commits
- **Path**: `C:\Users\usuario\.claude\skills\work-unit-commits\SKILL.md`
- **Trigger**: Implementation, commit splitting, chained PRs, keeping tests/docs with code
- **Rules**: One concern per commit; tests and docs travel with code; reviewable commits only (no WIP); group by logical work unit in PRs; conventional commit format; commit body explains WHY not WHAT
