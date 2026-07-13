ALTER TABLE public.opticas
  ADD COLUMN IF NOT EXISTS distrito_ciudad_departamento text,
  ADD COLUMN IF NOT EXISTS moneda text,
  ADD COLUMN IF NOT EXISTS pais text,
  ADD COLUMN IF NOT EXISTS contacto_whatsapp_telefono text;;
