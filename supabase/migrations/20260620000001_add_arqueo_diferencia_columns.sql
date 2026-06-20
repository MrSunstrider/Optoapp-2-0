-- Bug 1 fix: Add missing diferencia_tarjeta, diferencia_transferencia, diferencia_movil columns
-- to arqueo_caja. These columns are required by the Kotlin sync DTO (SyncFinanzasDto.kt:269-271)
-- and the Room entity (ArqueoCajaEntity.kt:26-28) but were missing from the remote DB table.
-- The CREATE TABLE migration (20260617100000) includes them, but if the table was created
-- before that migration ran, these columns might not exist. This safety-net ALTER fixes that.

ALTER TABLE public.arqueo_caja ADD COLUMN IF NOT EXISTS diferencia_tarjeta double precision NOT NULL DEFAULT 0;
ALTER TABLE public.arqueo_caja ADD COLUMN IF NOT EXISTS diferencia_transferencia double precision NOT NULL DEFAULT 0;
ALTER TABLE public.arqueo_caja ADD COLUMN IF NOT EXISTS diferencia_movil double precision NOT NULL DEFAULT 0;
