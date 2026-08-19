# Archive report: fix-auth-login-onboarding-recovery

Archived: 2026-08-19
Change: fix-auth-login-onboarding-recovery
Mode: hybrid (Engram + OpenSpec)
rdd_mode: disabled/unmanaged
reviewGate: ABSENT (kill switch off; archive under ordinary repository policy)
Verify verdict (authoritative): PASS WITH WARNINGS. CRITICAL=0. Archive allowed.

## Final state at close

All implementation tasks **1.1–8.1** are checked on filesystem `tasks.md` (10/10). Stacked PRs merged to main: WU1 #67, WU2 #68, WU3 #69, WU4 #70, WU5 #71, WU6 #72, WU7 #73, verify coverage #74.

Remote SQL applied: `harden_create_optica_return_id`, `user_profiles_select_scoped`.

Intentional remaining product facts (not incomplete work):

- PIN remains optional (C3 not merged).
- Postgres `invitaciones` remains unused by Android.
- `createAdditionalOptica` remains unwired in Compose.

Verify warnings at close (not CRITICAL, not blockers):

- Hosted GoTrue dashboard is still a human operator step; in-repo record `docs/gotrue-hosted-password-policy.md` exists and is greppable.
- SQL Docker harness was not run (`dockerDesktopLinuxEngine` unavailable at verify time). Catalog files exist: `supabase/tests/verify_create_optica_bootstrap.sql`, `supabase/tests/verify_user_profiles_select_rls.sql`.
- apply-progress.md TDD table exists as an intermediate snapshot; final task completion is filesystem `tasks.md`.

## Source ranking notes

- Filesystem `tasks.md` is the Task Completion Gate authority (all boxes `[x]`).
- Engram `sdd/fix-auth-login-onboarding-recovery/tasks` observation **#1822** is a stale earlier revision (unchecked 4.1–8.1 in stored content). That snapshot is **not** current state. No archive-time checkbox repair was required on disk.
- `verify-report` observation **#1827** and filesystem `verify-report.md` record PASS WITH WARNINGS, CRITICAL=0, 19/19 requirements, 41/41 scenarios.

## Specs synced to main

New full specs (no ADDED/MODIFIED/REMOVED/RENAMED delta sections). Mechanical `cp` + empty `diff -r` for each domain:

| Domain | Action | Details |
|--------|--------|---------|
| android-auth-onboarding | Created | 6 requirements (owner create, keep session, fetch error≠empty, blank rol, skip selector, assign-by-email) |
| android-auth | Created | 4 requirements (cold start, Google Idle, empty PIN invalid, CreatePin iff required&&unset) |
| supabase-optica-bootstrap | Created | 5 requirements (ignore client id, admin iff INSERT, RETURNS text, no direct INSERT, no RPC max cap) |
| supabase-user-profiles-rls | Created | 2 requirements (own profile SELECT, peer SELECT shared óptica) |
| gotrue-password-policy | Created | 2 requirements (local toml, hosted policy recorded in repo) |

Destructive merge: none (no REMOVED requirements). `login-screen` and `optica-config-settings` main specs were not modified.

## Mechanical copy readback

Step 2 (each domain): `diff -r` source spec vs temp copy — empty (no differences).

Step 3: `diff -r` pre-move snapshot vs `openspec/changes/archive/2026-08-19-fix-auth-login-onboarding-recovery` — empty (no differences).

This `archive-report.md` is additive after that comparison.

## Archive destination

`openspec/changes/archive/2026-08-19-fix-auth-login-onboarding-recovery/`

Contents: proposal.md, exploration.md, design.md, tasks.md (10/10 complete), specs/, apply-progress.md, verify-report.md, archive-report.md (this file).

Active path `openspec/changes/fix-auth-login-onboarding-recovery/` is gone.

## Engram observation IDs read

| Artifact | Observation ID | Topic |
|----------|----------------|-------|
| proposal | 1819 | sdd/fix-auth-login-onboarding-recovery/proposal |
| spec | 1820 | sdd/fix-auth-login-onboarding-recovery/spec |
| design | 1821 | sdd/fix-auth-login-onboarding-recovery/design |
| tasks | 1822 | sdd/fix-auth-login-onboarding-recovery/tasks |
| verify-report | 1827 | sdd/fix-auth-login-onboarding-recovery/verify-report |

## SDD cycle

Planned, implemented (WU1–WU7 + coverage), verified (pass with warnings), archived.
