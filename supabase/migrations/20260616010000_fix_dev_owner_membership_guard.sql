-- Allow the internal owner to add any member to the dev_owner optica.
-- Previous guard blocked adding others because it required new.user_id = auth.uid(),
-- meaning only a self-insert was allowed. The owner needs to onboard staff.

create or replace function public.enforce_dev_owner_membership_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_is_dev_owner_optica boolean;
begin
  select exists (
    select 1
    from public.opticas o
    where o.id = new.optica_id
      and lower(trim(o.plan_code)) = 'dev_owner'
  ) into v_is_dev_owner_optica;

  if not v_is_dev_owner_optica then
    return new;
  end if;

  if not public.is_internal_owner() then
    raise exception 'Membresía restringida: la óptica dev_owner solo puede ser gestionada por el owner interno';
  end if;

  return new;
end;
$$;
