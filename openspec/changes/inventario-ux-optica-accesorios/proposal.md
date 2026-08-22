# Proposal — inventario UX óptica + accesorios

## Intent

Menú de inventario con lenguaje óptico; alta Montura|Accesorio (sin aro/material para accesorios); formulario mínimo; conteo con etiquetas humanas; picker OT/Servicios excluye ACCESORIO.

## Evidence

- Formulario exigía tipoAro/material para líquidos (evidencia usuario).
- Catálogo extendido / proveedor / mm / foto URI: 0 uso en prod (34 monturas).
- Sección Proveedor no persistía en `save()`.
- Drawer vivo no exponía Conteos/Pedidos (MainDrawerContent huérfano).
- Conteo físico mostraba `monturaId` UUID.

## Scope

- **IN**: InventarioItemKind; form Montura|Accesorio; form mínimo; drawer INVENTARIO ÓPTICO; badges; labels en conteo; filtros OT/Servicios; tests.
- **OUT**: migraciones remotas; receiveItems OC; qty ±N; variance wire; venta accesorio con stock; barcode.

## Approach

`categoria=ACCESORIO` en tabla `monturas` existente. Misma sync. Dos PRs encadenados (<400 líneas).

## Causal invariant

INV-1: Accesorio se guarda sin tipoAro/material; Montura los exige.
INV-2: Picker de armazón en OT/Servicios nunca lista `categoria=ACCESORIO`.
INV-3: Single-writer SALIDA_VENTA y (referenciaId,tipo,monturaId) no cambian.
