-- Migration: dispensacion multi-item backfill (PR1)
-- Purpose: Back-fill dispensacion_items for existing optoweb rows that were
--          created before the multi-item migration. The dispensacion_items
--          table already exists in the remote DB (created by optoapp/Android).
--          Legacy columns on dispensaciones remain NOT NULL with defaults.
--          Going forward, optoweb writes to dispensacion_items only.
--
-- Only runs for rows where tipo_lente != '' (i.e. rows that have data
-- in the legacy columns that hasn't been migrated to dispensacion_items yet).
--
-- Rollback (emergency):
--   DELETE FROM dispensacion_items
--   WHERE id IN (SELECT id FROM dispensacion_items WHERE dispensacion_id IN
--     (SELECT id FROM dispensaciones));

INSERT INTO dispensacion_items (
  id,
  dispensacion_id,
  optica_id,
  tipo_lente,
  sub_tipo_bifocal,
  distancia_lente,
  altura,
  material_lente,
  tratamientos,
  color_lente,
  notas_diseno,
  filtro_discromatopsia_tipo,
  montura_id,
  origen_montura,
  tipo_aro,
  material_montura,
  descripcion_montura
)
SELECT
  gen_random_uuid() AS id,
  d.id AS dispensacion_id,
  d.optica_id,
  d.tipo_lente,
  d.sub_tipo_bifocal,
  d.distancia_lente,
  d.altura,
  d.material_lente,
  d.tratamientos,
  d.color_lente,
  d.notas_diseno,
  d.filtro_discromatopsia_tipo,
  d.montura_id,
  d.origen_montura,
  d.tipo_aro,
  d.material_montura,
  d.descripcion_montura
FROM dispensaciones d
WHERE d.tipo_lente <> ''
  AND NOT EXISTS (
    SELECT 1 FROM dispensacion_items di
    WHERE di.dispensacion_id = d.id
  );;
