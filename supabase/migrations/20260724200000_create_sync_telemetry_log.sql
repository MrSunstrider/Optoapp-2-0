-- Append-only sync telemetry history: each sync cycle writes a row so
-- diagnostics can show the full history of sync attempts per optica.

create table if not exists public.sync_telemetry_log (
    id uuid primary key default gen_random_uuid(),
    optica_id text not null references public.opticas(id) on delete cascade,
    status text not null,
    stage text not null default '',
    error_message text not null default '',
    created_at timestamptz not null default now()
);

create index if not exists idx_sync_telemetry_log_optica_created_at
    on public.sync_telemetry_log (optica_id, created_at desc);

comment on table public.sync_telemetry_log is 'Append-only log of sync cycle outcomes per optica.';
comment on column public.sync_telemetry_log.status is 'ok or error — final outcome of the sync stage.';
comment on column public.sync_telemetry_log.stage is 'Module name ("finanzas", "pacientes", "finalizado", etc.).';
comment on column public.sync_telemetry_log.error_message is 'Sanitized error message (empty on success).';

-- Grant minimal privileges to authenticated (RLS still governs).
grant select, insert on table public.sync_telemetry_log to authenticated;
revoke all on table public.sync_telemetry_log from anon;

-- RLS: members can SELECT only their own optica's rows.
create policy sync_telemetry_log_select_member
    on public.sync_telemetry_log
    for select
    to authenticated
    using (
        optica_id in (
            select uo.optica_id
            from public.usuario_optica uo
            where uo.user_id = auth.uid()
        )
    );

-- RLS: members can INSERT rows for their own optica.
create policy sync_telemetry_log_insert_member
    on public.sync_telemetry_log
    for insert
    to authenticated
    with check (
        optica_id in (
            select uo.optica_id
            from public.usuario_optica uo
            where uo.user_id = auth.uid()
        )
    );
