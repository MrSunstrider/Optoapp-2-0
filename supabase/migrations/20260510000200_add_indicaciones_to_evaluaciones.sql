-- Agrega columna indicaciones a evaluaciones para instrucciones/recomendaciones
-- (existe en el código TypeScript pero faltaba en el schema)
alter table public.evaluaciones
  add column if not exists indicaciones text;
