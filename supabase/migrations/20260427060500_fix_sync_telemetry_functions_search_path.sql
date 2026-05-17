-- Security lint hardening:
-- Ensure stable search_path for sync telemetry helper functions.

create or replace function public.set_sync_telemetry_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at := timezone('utc', now());
  return new;
end;
$$;

create or replace function public.set_sync_telemetry_audit_fields()
returns trigger
language plpgsql
set search_path = public
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

