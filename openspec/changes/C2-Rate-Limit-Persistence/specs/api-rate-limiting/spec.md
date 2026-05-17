# Delta for API Rate Limiting

## Status: No Changes Required

The existing `openspec/specs/api-rate-limiting/spec.md` already describes the target DB-backed state with complete coverage:

| Requirement | Coverage |
|-------------|----------|
| Persistent Attempt Tracking | Complete (5 scenarios including cold start, window reset, blocking) |
| Configurable Window and Max Attempts | Complete (default values scenario) |
| Database Schema and RLS | Complete (idempotent migration, RLS blocking) |
| Cleanup Strategy | Complete (expired rows ignored, pg_cron cleanup) |
| Performance Budget | Complete (single query, <50ms) |
| API Contract Stability | Complete (signature unchanged, callers need only `await`) |

The proposal `C2-Rate-Limit-Persistence` is an **implementation-only change** — the spec was written in anticipation of this work. No ADDED, MODIFIED, or REMOVED requirements are needed.

### Verification Note

During `sdd-verify`, the implementation MUST be validated against every scenario in the main spec. Key scenarios to prioritize:
- Cold start preserves state (cross-invocation persistence)
- Stale rows older than WINDOW_MS are not counted
- Concurrent requests produce correct counting (no race conditions via atomic RPC)
- RPC handles invalid/missing key gracefully
