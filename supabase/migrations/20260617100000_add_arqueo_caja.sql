-- arqueo_caja: daily cash count reconciliation per optica.
-- Mirrors the Android Room entity com.example.optoapp.data.arqueo.ArqueoCaja.

create table if not exists public.arqueo_caja (
  id text primary key,
  fecha date not null,
  optica_id text not null references public.opticas(id) on delete cascade,
  fondoCaja double precision not null default 0,
  efectivo_contado double precision not null default 0,
  tarjeta_contado double precision not null default 0,
  transferencia_contado double precision not null default 0,
  movil_contado double precision not null default 0,
  efectivo_cobrado double precision not null default 0,
  tarjeta_cobrado double precision not null default 0,
  transferencia_cobrado double precision not null default 0,
  movil_cobrado double precision not null default 0,
  diferencia_efectivo double precision not null default 0,
  diferencia_tarjeta double precision not null default 0,
  diferencia_transferencia double precision not null default 0,
  diferencia_movil double precision not null default 0,
  diferencia_total double precision not null default 0,
  cerrado_por text not null default '',
  sellado boolean not null default false,
  created_at text not null default '',
  updated_at text not null default '',
  updated_by text not null default '',
  unique (fecha, optica_id)
);

create index if not exists idx_arqueo_caja_optica_fecha
  on public.arqueo_caja (optica_id, fecha desc);

alter table public.arqueo_caja enable row level security;

-- SELECT: any member of the optica can view arqueos
drop policy if exists arqueo_caja_select_member on public.arqueo_caja;
create policy arqueo_caja_select_member
on public.arqueo_caja
for select
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = arqueo_caja.optica_id
      and uo.user_id = auth.uid()
  )
);

-- INSERT/UPDATE/DELETE: only admin or gerente of the optica
drop policy if exists arqueo_caja_upsert_admin_manager on public.arqueo_caja;
create policy arqueo_caja_upsert_admin_manager
on public.arqueo_caja
for all
to authenticated
using (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = arqueo_caja.optica_id
      and uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente')
  )
)
with check (
  exists (
    select 1
    from public.usuario_optica uo
    where uo.optica_id = arqueo_caja.optica_id
      and uo.user_id = auth.uid()
      and lower(uo.rol) in ('admin', 'gerente')
  )
);
