# Evidence — B1 Costo lookup tenant scope

## Change

- `CostoProductoDao.lookup` / `lookupLc` require `optica_id`
- `CostoBiseladoDao.lookup` require `optica_id`
- `DispensacionViewModel.calculateCosts` passes session opticaId

## Commands

```
./gradlew :optoapp:testDebugUnitTest --tests ...CostoProductoDaoTest --tests ...CostoBiseladoDaoTest
```

PASS. Full suite before PR push.

## Notes

- No remote migrations
- Issue #91
