-- Remove Virtual Try-On columns from monturas (feature removed)

alter table public.monturas
  drop column if exists ancho_mm,
  drop column if exists puente_mm,
  drop column if exists altura_mm,
  drop column if exists imagen_uri;
