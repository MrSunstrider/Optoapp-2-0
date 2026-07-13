ALTER TABLE arqueo_caja ADD COLUMN IF NOT EXISTS diferencia_tarjeta double precision NOT NULL DEFAULT 0;
ALTER TABLE arqueo_caja ADD COLUMN IF NOT EXISTS diferencia_transferencia double precision NOT NULL DEFAULT 0;
ALTER TABLE arqueo_caja ADD COLUMN IF NOT EXISTS diferencia_movil double precision NOT NULL DEFAULT 0;;
