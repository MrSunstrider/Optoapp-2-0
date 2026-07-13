-- Restore the migration's intended function that:
-- 1. Preserves client-provided updated_at
-- 2. Uses auth.uid() which returns null for anon key calls (no crash)
-- 3. Falls back to null on any exception
-- 4. Does NOT use current_setting('request.jwt.claim.sub', true) which falls back to 'system'

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
;
