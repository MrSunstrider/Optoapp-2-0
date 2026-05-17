-- Captura schema drift detectado entre migrations locales y base remota.
-- Estas tablas/columnas existen en la DB remota pero nunca fueron capturadas
-- en las migrations existentes.

-- Tabla monturas (existe en remoto, referenciada en migrations de índices)
create table if not exists public.monturas (
  id text primary key,
  sku text,
  marca text,
  modelo text,
  color text,
  talla text,
  costo numeric,
  precio numeric,
  stock_actual integer not null default 0,
  stock_minimo integer not null default 0,
  activo boolean not null default true,
  optica_id text not null default 'mi_optica_base',
  updated_at timestamptz not null default now()
);

create index if not exists idx_monturas_optica_id
  on public.monturas (optica_id);

-- Tabla montura_movimientos (existe en remoto)
create table if not exists public.montura_movimientos (
  id text primary key,
  montura_id text not null,
  fecha date not null default current_date,
  tipo text not null,
  cantidad integer not null default 0,
  stock_previo integer not null default 0,
  stock_nuevo integer not null default 0,
  referencia_id text,
  nota text,
  optica_id text not null
);

create index if not exists idx_montura_movimientos_optica_id
  on public.montura_movimientos (optica_id);

create index if not exists idx_montura_movimientos_montura_id
  on public.montura_movimientos (montura_id);

-- Columnas cerrado_at/cerrado_por en cierres_caja (existen en remoto,
-- no estaban en 20260429214000)
alter table public.cierres_caja
  add column if not exists cerrado_at timestamptz;

alter table public.cierres_caja
  add column if not exists cerrado_por uuid references auth.users(id) on delete set null;
