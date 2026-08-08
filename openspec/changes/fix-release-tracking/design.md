# Design: Fix Release Tracking CI Gate

## Architecture Decision

This is a **workflow-only change** — no Android code, no Supabase migrations, no Edge Function modifications.

### Decision: Remove `exists` guard from track-release condition

**Rationale**: The `steps.check_release.outputs.exists == 'false'` guard was added to avoid calling the Edge Function when a release already exists. However, the Edge Function is already idempotent (upsert by version). The guard introduces a silent skip that causes missing data — every push to main should record the release.

## Change Details

### 1. build-apk.yml — Line 97

**Current**:
```yaml
if: github.event_name != 'pull_request' && steps.check_release.outputs.exists == 'false'
```

**New**:
```yaml
if: github.event_name != 'pull_request'
```

### 2. Backfill Script

```bash
#!/bin/bash
# Backfill missing releases 1.10.0 → 1.15.8
SUPABASE_URL="https://sflhtihqdhrlryeyrzdo.supabase.co"
ANON_KEY="$SUPABASE_ANON_KEY"
FUNCTION_URL="$SUPABASE_URL/functions/v1/track-release"

VERSIONS=("1.10.0" "1.10.1" "1.11.0" "1.14.0" "1.15.8")  # exact versions TBC

for VERSION in "${VERSIONS[@]}"; do
  DOWNLOAD_URL="https://github.com/OptoServices/Optoapp/releases/tag/v$VERSION"
  curl -X POST "$FUNCTION_URL" \
    -H "Authorization: Bearer $ANON_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"version\": \"$VERSION\", \"download_url\": \"$DOWNLOAD_URL\"}"
done
```

## Affected Components

| Component | Change | Lines |
|-----------|--------|-------|
| `build-apk.yml` | Remove guard condition | 1 line |
| `app_releases` table | 5 rows inserted | 0 lines (data only) |
| `track-release` Edge Function | None | 0 lines |

## Sequence

```
Push to main
  → build-apk.yml triggers
    → check_release step (unchanged)
    → build APK (unchanged)
    → track-release step ← NOW ALWAYS CALLS Edge Function (was gated)
      → Edge Function upserts release (idempotent)
```

## No Changes Needed

- **Edge Function**: Already idempotent — no code change
- **Supabase migrations**: No schema change needed
- **Android app**: No code change needed
- **RLS policies**: No change needed
