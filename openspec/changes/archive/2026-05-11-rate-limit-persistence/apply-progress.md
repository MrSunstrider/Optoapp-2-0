# Apply Progress: rate-limit-persistence

**What**: Implemented DB-backed rate limiter replacing in-memory Map for PIN brute-force protection. All 6 tasks completed.

**Why**: In-memory Map resets on serverless cold starts (Vercel). Supabase persistence survives across invocations.

**Where**: 
- supabase/migrations/20260511000000_pin_attempts_rate_limit.sql
- web/src/lib/rate-limit.ts
- web/src/lib/rate-limit.test.ts
- web/src/app/api/auth/verify-pin/route.ts
- web/src/app/api/config/change-pin/route.ts

**Learned**: Mock pattern for supabase/ssr: use vi.mock hoisted at module level, then vi.mocked(createClient).mockResolvedValue() per test for fresh RPC mock state. The pg_cron cleanup job requires pg_cron to be enabled in Supabase project — if not available, the cron.schedule call in migration will fail and should be removed.

## Task Status

| Task | Status |
|------|--------|
| 1.1 Create migration (table, index, RLS, RPC, pg_cron) | ✅ Done |
| 2.1 Rewrite rate-limit.ts (async wrapper, supabase.rpc) | ✅ Done |
| 3.1 Update verify-pin/route.ts (add await) | ✅ Done |
| 3.2 Update change-pin/route.ts (add await) | ✅ Done |
| 4.1 Rewrite rate-limit.test.ts (mock supabase, await, fake timers) | ✅ Done |
| 4.2 npm test passes + migration verified | ✅ Done |

**Tasks completed**: 6/6
