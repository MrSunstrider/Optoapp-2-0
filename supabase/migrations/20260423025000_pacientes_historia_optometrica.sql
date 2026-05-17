-- P3-T4: número de historia optométrica en pacientes (opcional).
-- Compatible con datos existentes: columna nullable y normalización básica de espacios.

begin;

alter table public.pacientes
  add column if not exists historia_optometrica text;

update public.pacientes
set historia_optometrica = nullif(btrim(coalesce(historia_optometrica, '')), '')
where historia_optometrica is distinct from nullif(btrim(coalesce(historia_optometrica, '')), '');

comment on column public.pacientes.historia_optometrica
  is 'Número de historia optométrica del paciente (opcional).';

commit;
