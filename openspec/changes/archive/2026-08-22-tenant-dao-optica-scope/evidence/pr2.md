# Evidence — PR2 Disp / Servicio / Item / Regalo / Pago

## Change

- Scoped DAO: Dispensacion, ServicioExtra, DispensacionItem, Regalo get/delete
- Removed legacy `PagoDao.getPagoById`
- Plumb OptoRepository, CancelLedger, BumpEntityStrategy, VMs, Financierarepo

## Commands

```
./gradlew :optoapp:testDebugUnitTest
```

## Notes

- No remote migrations
- Issue #85
- Base: PR1 `fix/tenant-scope-inventario-fisico`
