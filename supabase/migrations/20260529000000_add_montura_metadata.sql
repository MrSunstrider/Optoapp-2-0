-- Virtual Try-On: frame metadata for 2D overlay scaling and rendering
-- Adds physical frame dimensions and transparent PNG image reference

ALTER TABLE monturas ADD COLUMN ancho_mm REAL;
ALTER TABLE monturas ADD COLUMN puente_mm REAL;
ALTER TABLE monturas ADD COLUMN altura_mm REAL;
ALTER TABLE monturas ADD COLUMN imagen_uri TEXT;

COMMENT ON COLUMN monturas.ancho_mm IS 'Frame total width in mm (for try-on scale)';
COMMENT ON COLUMN monturas.puente_mm IS 'Bridge width in mm (for DIP alignment)';
COMMENT ON COLUMN monturas.altura_mm IS 'Lens height in mm (for segment height reference)';
COMMENT ON COLUMN monturas.imagen_uri IS 'Local PNG path to transparent frame image for overlay';
