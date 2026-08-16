-- Homogeneous timestamp policy for Room↔Supabase sync tables.
--
-- WHY: Five sync tables still had a second BEFORE UPDATE trigger calling
-- update_updated_at(), which unconditionally sets updated_at = now(). That
-- ran alphabetically before set_updated_audit_fields and nullified the
-- preserve-client fix, regenerating false conflict_records on every sync.
--
-- INVARIANT: Every Room sync table uses ONLY set_updated_audit_fields for
-- updated_at mutation. update_updated_at remains solely on cierres_caja and
-- optica_settings (settings tables outside the conflict pipeline).

drop trigger if exists pacientes_updated_at on public.pacientes;
drop trigger if exists evaluaciones_updated_at on public.evaluaciones;
drop trigger if exists dispensaciones_updated_at on public.dispensaciones;
drop trigger if exists pagos_updated_at on public.pagos;
drop trigger if exists servicios_extra_updated_at on public.servicios_extra;
