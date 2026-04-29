-- PostgREST requests without a valid JWT use role `anon`. This table had no SELECT
-- grant for `anon`, so clients got "permission denied for table sync_telemetry_optica"
-- while other tables like `opticas` return 0 rows under RLS. Granting SELECT allows
-- the query to reach RLS; policies are only for `authenticated`, so `anon` sees no rows.
grant select on table public.sync_telemetry_optica to anon;
