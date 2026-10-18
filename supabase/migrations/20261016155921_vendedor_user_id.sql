-- Seller attribution for Reportes Equipo / commissions (membership auth uid).
-- Legacy rows stay NULL (= unattributed). Client defaults new sales to session user.

ALTER TABLE public.dispensaciones
  ADD COLUMN IF NOT EXISTS vendedor_user_id text;

ALTER TABLE public.servicios_extra
  ADD COLUMN IF NOT EXISTS vendedor_user_id text;

COMMENT ON COLUMN public.dispensaciones.vendedor_user_id IS
  'Membership auth uid of the seller; NULL = legacy / unattributed';

COMMENT ON COLUMN public.servicios_extra.vendedor_user_id IS
  'Membership auth uid of the seller; NULL = legacy / unattributed';
