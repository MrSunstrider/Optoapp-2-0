# Spec — inventario-stock UX óptica / accesorios

## Requirements

### R1: Tipo de ítem en alta

El sistema SHALL permitir crear ítems de inventario como **Montura** o **Accesorio**.

- Accesorio: NO requiere `tipoAro` ni `materialMontura`; persiste `categoria = ACCESORIO`.
- Montura: requiere `tipoAro` y `materialMontura` no vacíos.

### R2: Formulario mínimo

El formulario de alta/edición SHALL mostrar solo: Tipo, SKU, Marca, Modelo/Nombre, Color, Talla (montura), Aro/Material (montura), Costo, Precio, Stock, Stock mínimo.

SHALL NOT mostrar en UI: catálogo extendido (colección/temporada/estado/género), sección proveedor, mm (ancho/puente/altura), foto URI.

### R3: Picker de armazón

Dispensación y Servicios Extra SHALL listar solo monturas activas con `InventarioItemKind.isArmazon(categoria)` (excluye ACCESORIO).

### R4: Navegación

El drawer vivo SHALL exponer: Monturas, Conteo físico, Pedidos a proveedor, Proveedores bajo sección inventario óptico.

### R5: Conteo físico

Las filas de conteo SHALL mostrar marca/modelo/SKU (y color/talla si hay), no el UUID crudo de `monturaId`.

### R6: Invariantes de stock (no regresión)

Single-writer de venta (`SALIDA_VENTA` local) y unicidad `(referenciaId, tipo, monturaId)` permanecen sin cambios.
