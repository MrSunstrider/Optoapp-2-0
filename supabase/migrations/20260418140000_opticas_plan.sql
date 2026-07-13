-- P2: Plan de suscripción por óptica (lectura desde app vía MembershipRepository.fetchOpticaPlan).
-- Valores esperados en cliente: free, pro, paid, premium (normalizados a minúsculas).

ALTER TABLE public.opticas
    ADD COLUMN IF NOT EXISTS plan TEXT NOT NULL DEFAULT 'free';
COMMENT ON COLUMN public.opticas.plan IS 'Suscripción: free | pro (u otros alias consumidos por la app)';
