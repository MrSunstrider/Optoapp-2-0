-- =============================================================================
-- Membresía usuario ↔ óptica (requerido para RLS). NO uses texto entre < > en SQL:
-- Postgres espera un UUID real, p. ej. f47ac10b-58cc-4372-a567-0e02b2c3d479
-- =============================================================================

-- PASO 1 — Ejecuta esto tal cual y copia de los resultados:
--   - user id (columna id) del usuario en Auth
--   - optica_id (TEXT) que usa la app al iniciar sesión (logs / pantalla de óptica)

SELECT id, email, created_at
FROM auth.users
ORDER BY created_at DESC
LIMIT 50;

SELECT id, nombre
FROM public.opticas
ORDER BY id;

SELECT user_id, optica_id, rol
FROM public.usuario_optica
ORDER BY user_id, optica_id;

-- PASO 2 — Edita el bloque de abajo: sustituye SOLO los tres valores entre comillas.
-- user_id: debe ser un UUID válido (36 caracteres con guiones, solo hex en cada tramo).
-- optica_id: el mismo identificador de tenant que guarda la app (TEXT).

/*
INSERT INTO public.opticas (id, nombre)
VALUES ('25af5a92-4a2d-4e7a-957f-61bec87a07d8', 'Sersa Visual y Preventiva')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES (
  '25af5a92-4a2d-4e7a-957f-61bec87a07d8'::uuid,
  '25af5a92-4a2d-4e7a-957f-61bec87a07d8',
  'admin'
)
ON CONFLICT (user_id, optica_id) DO NOTHING;
*/

-- Ejemplo de formato (NO uses estos datos; son ilustrativos). Descomenta y reemplaza:
/*
INSERT INTO public.opticas (id, nombre)
VALUES ('abc123-tu-optica', 'Mi óptica')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.usuario_optica (user_id, optica_id, rol)
VALUES (
  'f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid,
  'abc123-tu-optica',
  'admin'
)
ON CONFLICT (user_id, optica_id) DO NOTHING;
*/

-- PASO 3 — Plan PRO para la óptica (ver también scripts/set_optica_plan_pro.sql).
-- Ejecutar en SQL Editor; luego en la app: Configuración → Sincronizar plan desde servidor.
/*
UPDATE public.opticas
SET plan = 'pro'
WHERE id = 'TU_OPTICA_ID';
*/
