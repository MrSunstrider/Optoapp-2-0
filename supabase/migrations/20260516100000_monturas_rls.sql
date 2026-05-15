-- Enable RLS and add policies for monturas and montura_movimientos.
-- Uses the same patterns as 20260509000000: is_optica_member for CRUD,
-- has_optica_role for DELETE.

-- MONTURAS
alter table public.monturas enable row level security;

drop policy if exists monturas_select on public.monturas;
create policy monturas_select on public.monturas for select
    using (app_private.is_optica_member(auth.uid(), optica_id));

drop policy if exists monturas_insert on public.monturas;
create policy monturas_insert on public.monturas for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));

drop policy if exists monturas_update on public.monturas;
create policy monturas_update on public.monturas for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));

drop policy if exists monturas_delete on public.monturas;
create policy monturas_delete on public.monturas for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));

-- MONTURA_MOVIMIENTOS
alter table public.montura_movimientos enable row level security;

drop policy if exists montura_movimientos_select on public.montura_movimientos;
create policy montura_movimientos_select on public.montura_movimientos for select
    using (app_private.is_optica_member(auth.uid(), optica_id));

drop policy if exists montura_movimientos_insert on public.montura_movimientos;
create policy montura_movimientos_insert on public.montura_movimientos for insert
    with check (app_private.is_optica_member(auth.uid(), optica_id));

drop policy if exists montura_movimientos_update on public.montura_movimientos;
create policy montura_movimientos_update on public.montura_movimientos for update
    using (app_private.is_optica_member(auth.uid(), optica_id))
    with check (app_private.is_optica_member(auth.uid(), optica_id));

drop policy if exists montura_movimientos_delete on public.montura_movimientos;
create policy montura_movimientos_delete on public.montura_movimientos for delete
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']));
