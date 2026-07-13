-- The set_updated_audit_fields trigger was unconditionally overwriting updated_at = NOW()
-- on every UPDATE (including sync upserts from Android).
--
-- This caused a permanent false-conflict cycle:
--   1. Android upserts row with updated_at = T_client
--   2. Trigger fires → updated_at = NOW() = T_server > T_client
--   3. Room still holds T_client (upload path does not re-fetch server timestamps)
--   4. Next filterConflicts: T_client < T_server → conflict for every synced row
--
-- Fix: updated_at is owned by the client. The trigger preserves the client-provided
-- value and only falls back to NOW() when no value is supplied (defensive; NOT NULL
-- columns always carry a value on UPDATE). updated_by remains server-authoritative.

create or replace function public.set_updated_audit_fields()
returns trigger
language plpgsql
as $$
begin
  if new.updated_at is null then
    new.updated_at := timezone('utc', now());
  end if;
  begin
    new.updated_by := auth.uid();
  exception
    when others then
      null;
  end;
  return new;
end;
$$;
comment on function public.set_updated_audit_fields is
'Audits updated_by server-side. updated_at is preserved from the client; '
'server-side fallback only applies when the value is null (should not occur '
'on NOT NULL columns, but kept for defensive correctness).';
