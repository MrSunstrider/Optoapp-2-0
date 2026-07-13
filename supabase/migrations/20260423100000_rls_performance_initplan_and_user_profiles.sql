-- Performance RLS:
-- 1) Reemplaza auth.uid() por (select auth.uid()) en policies de public.*
--    para evitar re-evaluación por fila (auth_rls_initplan).
-- 2) Consolida policies SELECT de user_profiles en una sola policy permisiva.

do $$
declare
  r record;
  v_qual text;
  v_check text;
begin
  for r in
    select schemaname, tablename, policyname, cmd, qual, with_check
    from pg_policies
    where schemaname = 'public'
      and (
        coalesce(qual, '') like '%auth.uid()%' or
        coalesce(with_check, '') like '%auth.uid()%'
      )
  loop
    v_qual := case when r.qual is null then null else replace(r.qual, 'auth.uid()', '(select auth.uid())') end;
    v_check := case when r.with_check is null then null else replace(r.with_check, 'auth.uid()', '(select auth.uid())') end;

    if r.cmd = 'SELECT' then
      execute format(
        'alter policy %I on %I.%I using (%s)',
        r.policyname, r.schemaname, r.tablename, v_qual
      );
    elsif r.cmd = 'INSERT' then
      execute format(
        'alter policy %I on %I.%I with check (%s)',
        r.policyname, r.schemaname, r.tablename, v_check
      );
    elsif r.cmd = 'UPDATE' then
      execute format(
        'alter policy %I on %I.%I using (%s) with check (%s)',
        r.policyname, r.schemaname, r.tablename, v_qual, v_check
      );
    elsif r.cmd = 'DELETE' then
      execute format(
        'alter policy %I on %I.%I using (%s)',
        r.policyname, r.schemaname, r.tablename, v_qual
      );
    elsif r.cmd = 'ALL' then
      if v_qual is not null and v_check is not null then
        execute format(
          'alter policy %I on %I.%I using (%s) with check (%s)',
          r.policyname, r.schemaname, r.tablename, v_qual, v_check
        );
      elsif v_qual is not null then
        execute format(
          'alter policy %I on %I.%I using (%s)',
          r.policyname, r.schemaname, r.tablename, v_qual
        );
      elsif v_check is not null then
        execute format(
          'alter policy %I on %I.%I with check (%s)',
          r.policyname, r.schemaname, r.tablename, v_check
        );
      end if;
    end if;
  end loop;
end $$;
drop policy if exists user_profiles_select_own on public.user_profiles;
drop policy if exists user_profiles_select_admin on public.user_profiles;
drop policy if exists user_profiles_select_access on public.user_profiles;
create policy user_profiles_select_access on public.user_profiles
for select
using (
  user_id = (select auth.uid())
  or exists (
    select 1
    from public.usuario_optica self
    where self.user_id = (select auth.uid())
      and lower(trim(self.rol)) in ('admin', 'gerente')
  )
);
