# API Rate Limiting Specification

## Purpose

DB-backed rate limiting for PIN brute-force protection that survives serverless cold starts. Replaces the in-memory `Map` in `web/src/lib/rate-limit.ts` with Supabase PostgreSQL persistence.

## Requirements

### Requirement: Persistent Attempt Tracking

The system MUST store rate-limit attempts in the `pin_attempts` table so that state survives across serverless invocations and cold starts. Each call to `checkRateLimit(key)` MUST read the current attempt count and window start from the database, increment or reset as appropriate, and return `{ allowed: boolean; remaining: number }`.

#### Scenario: First request for a new key creates a row

- GIVEN no row exists in `pin_attempts` for `limit_key = "user:abc"`
- WHEN `checkRateLimit("user:abc")` is called
- THEN a new row is inserted with `attempts = 1` and `window_start = now()`
- AND the response is `{ allowed: true, remaining: 4 }`

#### Scenario: Subsequent requests increment the counter within the window

- GIVEN a row exists with `attempts = 2` and `window_start` within the last 60 seconds
- WHEN `checkRateLimit("user:abc")` is called
- THEN `attempts` is incremented to 3
- AND the response is `{ allowed: true, remaining: 2 }`

#### Scenario: Request is blocked after max attempts within the window

- GIVEN a row exists with `attempts = 5` and `window_start` within the last 60 seconds
- WHEN `checkRateLimit("user:abc")` is called
- THEN the response is `{ allowed: false, remaining: 0 }`
- AND the row is NOT incremented further

#### Scenario: Window expiry resets the counter

- GIVEN a row exists with `attempts = 5` and `window_start` older than 60 seconds
- WHEN `checkRateLimit("user:abc")` is called
- THEN the row is updated: `attempts = 1`, `window_start = now()`
- AND the response is `{ allowed: true, remaining: 4 }`

#### Scenario: Cold start preserves state

- GIVEN two separate Supabase client instances (simulating cold starts)
- AND instance A calls `checkRateLimit("user:abc")` 5 times
- WHEN instance B calls `checkRateLimit("user:abc")`
- THEN the response is `{ allowed: false, remaining: 0 }`

### Requirement: Configurable Window and Max Attempts

The system MUST expose `WINDOW_MS` (default 60 000) and `MAX_ATTEMPTS` (default 5) as configurable constants. These values MUST be used consistently in both the query logic and the response calculation.

#### Scenario: Default values match current behavior

- GIVEN no configuration override
- WHEN `checkRateLimit` is called 5 times within 60 seconds
- THEN the 6th call is blocked (matching existing test expectations)

### Requirement: Database Schema and RLS

The `pin_attempts` table MUST include columns: `id` (uuid, PK), `limit_key` (text, NOT NULL), `attempts` (int, NOT NULL, default 1), `window_start` (timestamptz, NOT NULL), `created_at` (timestamptz, default now()). An index on `(limit_key, window_start)` MUST exist. RLS policies MUST restrict access to authenticated server-side service role only.

#### Scenario: Migration is idempotent

- GIVEN the migration is applied twice
- WHEN the second application completes
- THEN no error is raised and the table structure is unchanged

#### Scenario: RLS blocks anonymous access

- GIVEN an anonymous (anon) Supabase client
- WHEN a SELECT is attempted on `pin_attempts`
- THEN the query is rejected by RLS

### Requirement: Cleanup Strategy

The system MUST NOT accumulate stale rows indefinitely. Rows where `window_start + interval '60 seconds' < now()` MUST be excluded from rate-limit checks via a `WHERE` clause. A periodic cleanup mechanism (pg_cron or equivalent) SHOULD delete expired rows to keep the table lean.

#### Scenario: Expired rows are ignored in queries

- GIVEN a row with `window_start` 120 seconds ago
- WHEN `checkRateLimit` is called for that key
- THEN the row is treated as expired and a fresh window starts

#### Scenario: Cleanup removes rows older than 24 hours

- GIVEN a pg_cron job runs hourly
- WHEN the job executes `DELETE FROM pin_attempts WHERE window_start < now() - interval '24 hours'`
- THEN only rows older than 24 hours are removed

### Requirement: Performance Budget

The total overhead of a `checkRateLimit` call (DB round-trip + logic) MUST NOT exceed 50ms under normal load. No new npm dependencies MAY be introduced.

#### Scenario: Single query per check

- GIVEN a warm Supabase connection
- WHEN `checkRateLimit("user:abc")` is called
- THEN exactly one SQL statement is executed (UPSERT or SELECT + conditional INSERT)
- AND the total latency is under 50ms

### Requirement: API Contract Stability

The function `checkRateLimit(key: string)` MUST retain its exact signature and return type. Callers MUST NOT need to change. The function becomes async internally (`async function checkRateLimit`) but the return shape `{ allowed: boolean; remaining: number }` is unchanged.

#### Scenario: Existing callers compile without changes

- GIVEN a file that imports and calls `checkRateLimit("some-key")`
- WHEN the implementation switches to Supabase-backed persistence
- THEN the call site requires only `await` addition (or the caller is already in an async context)
- AND the return value destructuring `{ allowed, remaining }` works identically
