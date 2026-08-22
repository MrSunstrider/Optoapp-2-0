## Summary
- Drawer INVENTARIO OPTICO: Monturas, Conteo fisico, Pedidos a proveedor, Proveedores.
- Conteo fisico muestra marca/modelo/SKU (no UUID).
- Copy/canEdit en Monturas; titulo Pedidos.
- SDD: `openspec/changes/inventario-ux-optica-accesorios` (PR2 chained after PR1).

No remote migrations.

## Test plan
- [ ] `./gradlew :optoapp:testDebugUnitTest`
- [ ] GGA equivalent R1-R4 before push
- [ ] Manual: drawer entries navigate
