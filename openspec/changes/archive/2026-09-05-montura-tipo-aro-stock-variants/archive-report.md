# Archive Report — montura-tipo-aro-stock-variants

**Archived**: 2026-09-05  
**Verdict**: PASS  
**Specs synced**: `inventario-stock` (new main capability)  
**Archive path**: `openspec/changes/archive/2026-09-05-montura-tipo-aro-stock-variants/`

## Final state

- Room DB v52: unique `(sku, opticaId, tipoAro)`
- Supabase migration `20260905010000_monturas_unique_sku_tipo_aro.sql` (pending remote apply)
- Create UX: multi-select tipo aro + stock por tipo
- Search/label: tipoAro visible in dispensación + servicios extra
- Material Aluminio in `OpticalCatalog.MATERIALES_MONTURA`
- Unit tests + assembleDebug green
