# Delta for indicadores-negocio

## No Changes

This change affects only the RPC computation logic within `rpc_analisis_mensual`. The `AnalisisMensual` domain model, `AnalisisMensual.fromJson()` parsing, `ObtenerAnalisisMensualUseCase`, and all indicator consumers are unaffected — they receive the same JSON structure with corrected values. No indicator requirements are modified.
