-- Link servicios extra to inventory row for accessory sales (líquidos, cofres, etc.)
ALTER TABLE public.servicios_extra ADD COLUMN IF NOT EXISTS montura_id TEXT;

COMMENT ON COLUMN public.servicios_extra.montura_id IS
  'Optional FK to monturas.id when the service sells a stocked inventory item';
