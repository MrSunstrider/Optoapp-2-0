-- Allow authenticated members of an optica to upsert their own sync telemetry.
-- Keeps anon blocked. Avoids exposing cross-tenant data.

-- Grant minimal table privileges to authenticated (RLS still applies).
grant select, insert, update on table public.sync_telemetry_optica to authenticated;
revoke all on table public.sync_telemetry_optica from anon;

-- Replace "deny all" policy with member-scoped policies.
drop policy if exists sync_telemetry_optica_no_client_access on public.sync_telemetry_optica;

drop policy if exists sync_telemetry_optica_select_member on public.sync_telemetry_optica;
create policy sync_telemetry_optica_select_member
on public.sync_telemetry_optica
for select
to authenticated
using (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
  )
);

drop policy if exists sync_telemetry_optica_insert_member on public.sync_telemetry_optica;
create policy sync_telemetry_optica_insert_member
on public.sync_telemetry_optica
for insert
to authenticated
with check (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
  )
);

drop policy if exists sync_telemetry_optica_update_member on public.sync_telemetry_optica;
create policy sync_telemetry_optica_update_member
on public.sync_telemetry_optica
for update
to authenticated
using (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
  )
)
with check (
  optica_id in (
    select uo.optica_id
    from public.usuario_optica uo
    where uo.user_id = auth.uid()
  )
);

-- Track actor server-side (do not trust client).
create or replace function public.set_sync_telemetry_audit_fields()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := timezone('utc', now());
  begin
    new.last_actor := auth.uid();
  exception
    when others then
      null;
  end;
  return new;
end;
$$;

drop trigger if exists trg_sync_telemetry_optica_audit on public.sync_telemetry_optica;
create trigger trg_sync_telemetry_optica_audit
before insert or update on public.sync_telemetry_optica
for each row
execute function public.set_sync_telemetry_audit_fields();

