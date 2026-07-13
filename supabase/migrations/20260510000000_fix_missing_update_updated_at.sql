-- Crea la funcion update_updated_at() que faltaba.
-- Los triggers trg_cierres_caja_updated_at y trg_optica_settings_updated_at
-- la referencian desde 20260429214000 pero nunca fue definida.

create or replace function public.update_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := timezone('utc', now());
  return new;
end;
$$;
comment on function public.update_updated_at is
'Actualiza updated_at en tablas sin columna updated_by (cierres_caja, optica_settings).';
