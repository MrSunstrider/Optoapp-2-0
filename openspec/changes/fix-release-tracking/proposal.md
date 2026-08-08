# Proposal: Fix Release Tracking CI Gate

## Intent

The `build-apk.yml` workflow has a CI gate at line 97 that calls `track-release` Edge Function only when `steps.check_release.outputs.exists == 'false'`. This means releases where the Git tag already exists **silently skip** being recorded in `app_releases`. Five critical releases (1.10.0 → 1.15.8) are missing from the release table as a result.

## Scope

### In Scope
- Remove the `exists` guard on track-release call in `build-apk.yml`
- Backfill 5 missing releases (1.10.0, 1.10.0, 1.15.8 — confirmation needed on exact versions) to `app_releases`
- GGA review of the workflow change

### Out of Scope
- Modifying the track-release Edge Function (already idempotent)
- Android app code changes
- Supabase schema migrations

## Capabilities

### New Capabilities
- `release-tracking`: Ensures every push to main records the release in `app_releases` via the track-release Edge Function

### Modified Capabilities
- None

## Approach

1. **CI gate fix**: Change `build-apk.yml` line 97 condition from `if: github.event_name != 'pull_request' && steps.check_release.outputs.exists == 'false'` to `if: github.event_name != 'pull_request'`
2. **Backfill**: Call track-release via curl for each missing version with correct download URLs
3. **No code changes needed** — Edge Function is idempotent, no migration, no Android changes

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `.github/workflows/build-apk.yml` | Modified | Line 97 guard removal |
| `app_releases` (Supabase) | Data | 5 new rows backfilled |
| `track-release` Edge Function | None | Remains unchanged, already idempotent |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Duplicate release entries from re-runs | Low | Edge Function is idempotent (upsert by version) |
| Backfill script sends wrong URLs | Low | Verify each URL against GitHub Releases before curl |
| Workflow breaks if Edge Function is down | Low | track-release is non-blocking; main workflow continues |

## Rollback Plan

Revert the single-line change in `build-apk.yml` to restore the original gate. Backfilled rows do not need rollback — they are correct data that was missing.

## Dependencies

- `track-release` Edge Function must be deployed and reachable
- GitHub Releases must exist for backfilled versions with valid download URLs

## Success Criteria

- [ ] `track-release` Edge Function is called on every push to main (not gated on tag existence)
- [ ] 5 missing releases backfilled to `app_releases` with correct version, URL, and timestamp
- [ ] GGA review passes on the workflow change
