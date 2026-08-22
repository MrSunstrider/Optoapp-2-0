## Summary
- Alta inventario Montura | Accesorio (`categoria=ACCESORIO`); accesorio sin tipo de aro/material.
- Formulario minimo (sin catalogo extendido, proveedor, mm, foto URI).
- Picker OT/Servicios excluye accesorios.
- SDD: `openspec/changes/inventario-ux-optica-accesorios` (PR1).

No remote migrations.

## Test plan
- [ ] Unit tests InventarioItemKind + MonturasViewModel save rules
- [ ] `./gradlew :optoapp:testDebugUnitTest`
- [ ] GGA equivalent R1-R4 before push
