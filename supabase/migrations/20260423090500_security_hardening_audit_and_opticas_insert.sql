-- Seguridad:
-- 1) Fijar search_path en función audit trigger.
-- 2) Evitar policy INSERT permisiva (WITH CHECK true) en opticas.

create or replace function public.set_updated_audit_fields()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at := timezone('utc', now());
  begin
    new.updated_by := auth.uid();
  exception
    when others then
      null;
  end;
  return new;
end;
$$;

drop policy if exists opticas_insert_authenticated on public.opticas;
create policy opticas_insert_authenticated on public.opticas
for insert to authenticated
with check (
  auth.uid() is not null
  and nullif(btrim(id), '') is not null
  and nullif(btrim(nombre), '') is not null
  and (
    (
      lower(btrim(plan_code)) = 'free'
      and lower(btrim(plan_source)) = 'manual'
      and lower(btrim(plan_status)) = 'active'
    )
    or (
      lower(btrim(plan_code)) = 'dev_owner'
      and public.is_internal_owner()
    )
  )
);
