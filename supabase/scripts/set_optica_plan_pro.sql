-- Marcar una óptica como PRO en Supabase (límite ilimitado en la app tras sincronizar el plan).
-- Ejecutar en el SQL Editor del proyecto (rol postgres / sin JWT de app; el trigger no bloquea esto).
--
-- 1) Sustituye el id por el de tu fila en public.opticas (el mismo tenant que usa la app).
-- 2) En la app: Configuración → "Sincronizar plan desde servidor".

UPDATE public.opticas
SET plan = 'pro'
WHERE id = 'PON_AQUI_TU_OPTICA_ID';

-- Verificación:
-- SELECT id, nombre, plan FROM public.opticas;
