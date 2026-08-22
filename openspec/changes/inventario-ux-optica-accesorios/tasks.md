# Tasks — inventario-ux-optica-accesorios

## WU1 — Accesorios + form mínimo + filtros OT (PR1)

- [x] `InventarioItemKind` + unit tests
- [x] `MonturaForm` chips + form mínimo
- [x] `MonturasViewModel` validación condicional + `categoriaForSave`
- [x] `MonturaList` badge Accesorio
- [x] `DispensacionViewModel` / `ServiciosViewModel` filter armazón
- [ ] Tests VM: Accesorio save sin aro; Montura save sin aro falla
- [ ] GGA-eq R1–R4
- [ ] PR `feat/inventario-accesorios-alta`

## WU2 — Menú + conteo labels (PR2)

- [x] `DrawerSections` INVENTARIO ÓPTICO
- [x] `MainDrawerContent` alineado
- [x] `MonturasScreen` copy / canEdit / títulos
- [x] `InventarioFisicoViewModel` + `MonturaScanScreen` labels
- [x] Título Pedidos en `OrdenesCompraScreen`
- [ ] GGA-eq R1–R4
- [ ] PR `feat/inventario-menu-conteo`

## Verify

- [ ] `./gradlew :optoapp:testDebugUnitTest`
- [ ] `verify-report.md`
