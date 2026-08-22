# Evidence — PR1 Inventario físico

## Change

- `InventarioFisicoDao.getById(id, opticaId)` scoped
- Repo `getById` / `closeSession` / `upsertSession` plumb tenant
- VM `loadSessionDetail` / `closeSession` pass session optica

## Commands

```
./gradlew :optoapp:testDebugUnitTest --tests "*InventarioFisico*"
./gradlew :optoapp:testDebugUnitTest
```

## Notes

- No remote migrations
- Child `getDetalles` remains FK-only after parent scoped
- Issue #84
