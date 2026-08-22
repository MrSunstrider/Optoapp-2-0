# Evidence — B3 Parent FK / reassign tenant scope

## Change

- DispensacionItemDao parent reads + getCostosByDispensacionIds require optica_id
- MonturaMovimientoDao.getMovimientosByMontura scoped
- PacienteDao reassign* UPDATEs require opticaId
- Plumb repos/VMs/ObtenerMovimientosFinancierosUseCase

## Commands

```
./gradlew :optoapp:testDebugUnitTest
```

PASS.

## Notes

- No remote migrations
- Issue #93
