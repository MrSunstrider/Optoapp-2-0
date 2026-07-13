-- Alinear RLS policies con la matriz de roles de la app.
--
-- Matriz:
--   SELECT: todos los miembros (is_optica_member) — todos leen
--   INSERT/UPDATE: según tabla (has_optica_role con roles específicos)
--   DELETE: solo admin/gerente (has_optica_role con admin,gerente)
--
-- Roles permitidos para INSERT/UPDATE:
--   pacientes, dispensaciones, monturas, montura_movimientos, pagos, servicios_extra
--     → admin, gerente, especialista, asesor, asesora, ventas (todo excepto invitado)
--   evaluaciones
--     → admin, gerente, especialista (asesor/ventas no escriben evaluaciones)

-- ── Helper: rol pertenece a un conjunto ──────────────────────────────────────
-- Reutiliza has_optica_role que ya existe en app_private.

-- ── PACIENTES ────────────────────────────────────────────────────────────────
drop policy if exists pacientes_insert on public.pacientes;
create policy pacientes_insert on public.pacientes for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists pacientes_update on public.pacientes;
create policy pacientes_update on public.pacientes for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
-- SELECT sigue siendo para todos los miembros
drop policy if exists pacientes_select on public.pacientes;
create policy pacientes_select on public.pacientes for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- DELETE ya está correcto (admin/gerente)
-- (se mantiene la policy existente)

-- ── EVALUACIONES ─────────────────────────────────────────────────────────────
drop policy if exists evaluaciones_insert on public.evaluaciones;
create policy evaluaciones_insert on public.evaluaciones for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista']));
drop policy if exists evaluaciones_update on public.evaluaciones;
create policy evaluaciones_update on public.evaluaciones for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista']));
drop policy if exists evaluaciones_select on public.evaluaciones;
create policy evaluaciones_select on public.evaluaciones for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- ── DISPENSACIONES ───────────────────────────────────────────────────────────
drop policy if exists dispensaciones_insert on public.dispensaciones;
create policy dispensaciones_insert on public.dispensaciones for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists dispensaciones_update on public.dispensaciones;
create policy dispensaciones_update on public.dispensaciones for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists dispensaciones_select on public.dispensaciones;
create policy dispensaciones_select on public.dispensaciones for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- ── MONTURAS ─────────────────────────────────────────────────────────────────
drop policy if exists monturas_insert on public.monturas;
create policy monturas_insert on public.monturas for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists monturas_update on public.monturas;
create policy monturas_update on public.monturas for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists monturas_select on public.monturas;
create policy monturas_select on public.monturas for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- ── MONTURA_MOVIMIENTOS ──────────────────────────────────────────────────────
drop policy if exists montura_movimientos_insert on public.montura_movimientos;
create policy montura_movimientos_insert on public.montura_movimientos for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists montura_movimientos_update on public.montura_movimientos;
create policy montura_movimientos_update on public.montura_movimientos for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists montura_movimientos_select on public.montura_movimientos;
create policy montura_movimientos_select on public.montura_movimientos for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- ── PAGOS ────────────────────────────────────────────────────────────────────
drop policy if exists pagos_insert on public.pagos;
create policy pagos_insert on public.pagos for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists pagos_update on public.pagos;
create policy pagos_update on public.pagos for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists pagos_select on public.pagos;
create policy pagos_select on public.pagos for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- ── SERVICIOS_EXTRA ──────────────────────────────────────────────────────────
drop policy if exists servicios_extra_insert on public.servicios_extra;
create policy servicios_extra_insert on public.servicios_extra for insert
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists servicios_extra_update on public.servicios_extra;
create policy servicios_extra_update on public.servicios_extra for update
    using (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']))
    with check (app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas']));
drop policy if exists servicios_extra_select on public.servicios_extra;
create policy servicios_extra_select on public.servicios_extra for select
    using (app_private.is_optica_member(auth.uid(), optica_id));
-- ── DELETE policies (se mantienen: solo admin/gerente) ───────────────────────
-- Se dejan explícitas para claridad, pero no cambian respecto a las existentes.
-- pacientes_delete, evaluaciones_delete, dispensaciones_delete,
-- monturas_delete, montura_movimientos_delete, pagos_delete, servicios_extra_delete
-- ya usan: app_private.has_optica_role(auth.uid(), optica_id, array['admin', 'gerente']);
