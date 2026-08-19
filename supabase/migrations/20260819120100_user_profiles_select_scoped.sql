-- Scope user_profiles SELECT to own row or peers who share an óptica
-- where the caller is admin/gerente. Global SELECT via any admin role is removed.

DROP POLICY IF EXISTS user_profiles_select_access ON public.user_profiles;

CREATE POLICY user_profiles_select_access ON public.user_profiles
FOR SELECT
USING (
    user_id = (SELECT auth.uid())
    OR EXISTS (
        SELECT 1
        FROM public.usuario_optica AS self
        JOIN public.usuario_optica AS peer
          ON peer.optica_id = self.optica_id
        WHERE self.user_id = (SELECT auth.uid())
          AND peer.user_id = user_profiles.user_id
          AND lower(btrim(self.rol)) = ANY (ARRAY['admin'::text, 'gerente'::text])
    )
);
