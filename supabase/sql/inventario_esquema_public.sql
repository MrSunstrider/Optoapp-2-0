-- Ejecutar en Supabase → SQL Editor → Run (solo lectura).
-- Copia el resultado (o exporta CSV) y guárdalo en el repo si quieres historial del esquema.

-- 1) Tablas en public
SELECT table_name AS tabla
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- 2) Columnas por tabla (tipos y nullability)
SELECT
  c.table_name AS tabla,
  c.column_name AS columna,
  c.data_type AS tipo,
  c.is_nullable AS admite_null,
  c.column_default AS valor_defecto
FROM information_schema.columns c
WHERE c.table_schema = 'public'
ORDER BY c.table_name, c.ordinal_position;

-- 3) Claves foráneas
SELECT
  tc.table_name AS tabla_hija,
  kcu.column_name AS columna_hija,
  ccu.table_name AS tabla_padre,
  ccu.column_name AS columna_padre
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage ccu
  ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- 4) Políticas RLS (si existen)
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;

-- 5) Tablas con RLS activado
SELECT c.relname AS tabla
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind = 'r'
  AND c.relrowsecurity = true
ORDER BY c.relname;
