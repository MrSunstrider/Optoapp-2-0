-- 1) Elimina el exception handler silencioso en set_updated_audit_fields.
--    auth.uid() retorna null cuando no hay contexto de autenticación
--    (service_role, triggers internos) y NO lanza excepción.
--    El when others then null tragaba errores reales sin reportarlos.

create or replace function public.set_updated_audit_fields()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at := timezone('utc', now());
  new.updated_by := auth.uid();
  return new;
end;
$$;

-- 2) Marca la columna legacy opticas.plan como deprecada.
--    plan_code (creada en 20260423054500) es el reemplazo oficial.

comment on column public.opticas.plan is
'DEPRECATED — usar plan_code. Columna legacy mantenida por compatibilidad con clientes viejos.';
