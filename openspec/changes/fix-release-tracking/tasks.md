# Tasks: Fix Release Tracking CI Gate

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~5 (1 YAML line + backfill script) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

## Task Dependency Order

```
T1 (CI gate) ──→ T3 (GGA review)
T2 (Backfill) ──→ T4 (Verify idempotency)
```

## T1: Fix build-apk.yml gate

**File**: `.github/workflows/build-apk.yml` (line ~97)

Change the track-release step condition:
- **Current**: `if: github.event_name != 'pull_request' && steps.check_release.outputs.exists == 'false'`
- **New**: `if: github.event_name != 'pull_request'`

This is a **1-line change**. The `exists == 'false'` guard is removed because the Edge Function is idempotent.

**Verification**: Push to main triggers track-release call regardless of existing tag.

## T2: Backfill 5 releases to app_releases

**Tool**: curl to track-release Edge Function

Run the backfill script for missing versions (exact versions to be confirmed against GitHub Releases):
- 1.10.0
- 1.10.1 (or equivalent next incremental)
- 1.11.0
- 1.14.0
- 1.15.8

**Verification**: Query `app_releases` to confirm 5 new rows with correct download URLs.

## T3: Run GGA on workflow change

Submit the modified `build-apk.yml` for GGA dual-blind review. Resolve all observations before merge.

**Verification**: GGA report shows zero unresolved observations.

## T4: Verify Edge Function idempotency

Call track-release Edge Function twice with the same version and confirm no duplicate rows in `app_releases`.

**Verification**: `SELECT version, COUNT(*) FROM app_releases GROUP BY version HAVING COUNT(*) > 1` returns zero rows.
