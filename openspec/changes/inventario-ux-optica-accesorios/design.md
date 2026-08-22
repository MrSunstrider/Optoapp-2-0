# Design — inventario UX óptica + accesorios

## Decisions

1. **Persistencia Accesorio** vía `monturas.categoria = "ACCESORIO"` (`InventarioItemKind`), no tabla nueva.
2. **Form UI** chips Montura|Accesorio; campos aro/material/mm solo Montura; sin catálogo extendido / proveedor / foto URI (columnas DB intactas).
3. **Filtro OT** en `DispensacionViewModel.monturasActivas` y `ServiciosViewModel.monturas` con `isArmazon(categoria)`.
4. **Drawer** sección INVENTARIO ÓPTICO en `DrawerSections` (vivo) + alinear `MainDrawerContent`.
5. **Conteo** labels desde snapshot monturas en `InventarioFisicoViewModel`.

## Delivery

| PR | Branch | WUs |
|----|--------|-----|
| PR1 | `feat/inventario-accesorios-alta` | Kind + form + VM + list badge + OT/Servicios filter + tests |
| PR2 | `feat/inventario-menu-conteo` | Drawer + MonturasScreen chrome + InventarioFisico labels + título Pedidos |

## Risks

- Accesorios en conteo físico (incluido) — OK para stock de vitrina.
- `categoria` SOL/GRADUADA vacío en prod; ACCESORIO no colisiona.
- No tocar stock writers de venta.
