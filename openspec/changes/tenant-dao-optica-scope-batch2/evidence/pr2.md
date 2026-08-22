# Evidence — B2 Pago helpers tenant scope

## Change

- Scoped: getPagosByDispensacion/Servicio, getCreditPagosByParent, getReversoByOriginalId, sumMonto*
- Removed unscoped getPagosByDateRange, getAllPagos, reassignDispensacionId overloads
- Plumb CalcularMontoPagadoUseCase, CancelLedger, repos, Disp/Servicios/IF VMs

## Commands

```
./gradlew :optoapp:testDebugUnitTest
```

## Notes

- No remote migrations
- Issue #92
- Base: B1 `fix/tenant-scope-costo-lookup`
