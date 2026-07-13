-- ============================================================================
-- Migration: Add RLS policies for invitaciones (#18)
-- Date: 2026-07-14
--
-- invitaciones had only service_role access. This adds member-level RLS
-- consistent with all other business tables in the schema.
--
-- Policies:
--   SELECT  → any optica member can view invitations for their optica
--   INSERT  → admin/gerente only
--   UPDATE  → admin/gerente only
--   DELETE  → admin/gerente only
--
-- Keeps existing: invitaciones_service_role_all (FOR ALL TO service_role)
-- ============================================================================

-- SELECT: members see their optica's invitations
CREATE POLICY invitaciones_select_member ON public.invitaciones
  FOR SELECT TO public
  USING (app_private.is_optica_member(auth.uid(), optica_id));

-- INSERT: admin/gerente create invitations
CREATE POLICY invitaciones_insert_admin ON public.invitaciones
  FOR INSERT TO public
  WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin','gerente']));

-- UPDATE: admin/gerente modify invitations
CREATE POLICY invitaciones_update_admin ON public.invitaciones
  FOR UPDATE TO public
  USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin','gerente']))
  WITH CHECK (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin','gerente']));

-- DELETE: admin/gerente remove invitations
CREATE POLICY invitaciones_delete_admin ON public.invitaciones
  FOR DELETE TO public
  USING (app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin','gerente']));

-- Verify the fix
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_policy pol
        JOIN pg_class c ON c.oid = pol.polrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relname = 'invitaciones'
          AND pg_get_expr(pol.polqual, pol.polrelid)::text ILIKE '%optica_id%'
    ) THEN
        RAISE NOTICE 'invitaciones now has optica_id-based RLS policies.';
    ELSE
        RAISE WARNING 'invitaciones still missing optica_id RLS policy!';
    END IF;
END;
$$;
