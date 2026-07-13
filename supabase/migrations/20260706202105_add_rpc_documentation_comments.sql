COMMENT ON FUNCTION public.rpc_resumen_financiero(TEXT, DATE, DATE) IS 'DEPRECATED: Uses dispensaciones/servicios_extra directly instead of unified ventas table. Use rpc_analisis_mensual for new development.';

COMMENT ON FUNCTION public.rpc_saldo_pendiente(TEXT) IS 'DEPRECATED: Uses dispensaciones/servicios_extra directly instead of unified ventas table. Use rpc_analisis_mensual which includes saldo_pendiente.';

COMMENT ON FUNCTION public.rpc_deudores(TEXT) IS 'Active. Uses ventas + deduped pagos (matching via venta_id, dispensacion_id, servicio_extra_id). Keep in sync with rpc_analisis_mensual proyeccion_caja CTE.';

COMMENT ON FUNCTION public.rpc_analisis_mensual(TEXT, DATE) IS 'Active. Primary analytics function (8 indicators). Proyeccion_caja uses deduped pagos matching. Preferred over rpc_resumen_financiero and rpc_saldo_pendiente.';

COMMENT ON FUNCTION public.rpc_cierre_caja_resumen(TEXT, DATE, DATE) IS 'Active. Uses exact metodo_pago matching (not LIKE). Categorias: Efectivo, Tarjeta, Transferencia/Yape/Plin/Móvil.';;
