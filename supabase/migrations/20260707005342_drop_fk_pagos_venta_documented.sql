-- JD R3 suggested adding FK pagos→ventas as hardening.
-- However, the async sync architecture uploads dispensaciones and pagos
-- in separate HTTP transactions. A pago can arrive before the trigger
-- on its parent dispensacion commits the venta, causing FK violation.
--
-- Data integrity is enforced at the application layer:
-- All Android ViewModels (DispensacionViewModel, ServiciosViewModel,
-- InformacionFinancieraViewModel) now correctly set ventaId on every pago.
-- Verified: 325/325 pagos have valid venta_id, 0 orphan records.
--
-- Decision: Drop FK. App-layer integrity is sufficient for async REST sync.
ALTER TABLE public.pagos DROP CONSTRAINT IF EXISTS fk_pagos_venta;

COMMENT ON TABLE public.pagos IS 'venta_id integrity enforced at app layer (Android ViewModels). FK dropped due to async REST sync architecture.';;
