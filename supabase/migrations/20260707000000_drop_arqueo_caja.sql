-- Drop arqueo_caja table and all dependencies (policies, indexes)
-- Feature removed: arqueo de caja was causing more problems than benefits
DROP TABLE IF EXISTS public.arqueo_caja CASCADE;
