# Proposal: InformacionFinancieraScreen

## Intent

Los pagos de dispensaciones se gestionan inline en `NuevaDispensacionScreen` scrolleando hasta el final del formulario. Extraer la gestion financiera a una pantalla dedicada permite editar monto total, historial de pagos y estado sin mezclar con los datos de produccion (lente/montura). Mejora UX y desacopla responsabilidades.

## Scope

### In Scope
- `InformacionFinancieraScreen` con sticky header (OT, paciente, fecha, descripcion breve), monto total editable, historial de pagos (CRUD), saldo restante reactivo, estado Pendiente/Entregado, boton Guardar.
- `InformacionFinancieraViewModel` (Hilt, StateFlow, inyecta `OptoRepository` + `VentaDao` + `SessionManager`).
- `DispensacionFinancieraRepository` interface (abstraccion para tests).
- Refactor `NuevaDispensacionScreen`: reemplazar `FinancieraInfoSection` inline por Card resumen + boton "Gestionar Pagos".
- Ruta `informacion_financiera/{dispensacionId}` en `MainDrawerScreen` NavHost.
- `Venta` upsert local al guardar cambios financieros.

### Out of Scope
- ServiciosExtra (su gestion financiera inline se mantiene intacta).
- Fase 4 (corregir navegacion "Ir a Financiero" desde dialogo resumen).
- Refactor de `pagos.dispensacionId`/`servicioExtraId` (conviven durante transicion).
- Cambios a `Pago` entity o `PagoDao`.

## Capabilities

### New Capabilities
- `informacion-financiera-dispensacion`: gestion financiera dedicada para una dispensacion (monto total, pagos CRUD, saldo, estado). Accesible via ruta independiente.

### Modified Capabilities
- None — el spec de `servicio-extra` no cambia. No existe spec para `dispensacion-form` a nivel de requirements.

## Approach

1. **Repository**: crear `DispensacionFinancieraRepository` interface con metodos `obtenerDispensacion()`, `obtenerContexto()`, `actualizarMontoTotal()`, `agregarPago()`, `editarPago()`, `eliminarPago()`, `actualizarEstado()`. Implementacion concreta usa `OptoRepository` + `VentaDao` existentes.
2. **ViewModel**: `InformacionFinancieraViewModel` con `DispensacionFinancieraRepository`, `SessionManager`. StateFlow expone `montoTotal`, `pagos`, `saldoRestante`, `estado`, `contextoVenta`. `syncVenta()` se llama en cada guardado.
3. **Screen**: `InformacionFinancieraScreen` — sticky header via `Column` con fondo de card, scroll vertical para el resto. Monto total con `OptoTextField`. Historial de pagos reuse `AbonoDialog`. Saldo calculado reactivamente. Dropdown estado.
4. **Refactor**: `NuevaDispensacionScreen` — `FinancieraInfoSection` se reemplaza por un Card con monto total, saldo, estado, y boton "Gestionar Pagos" que navega via `navController.navigate("informacion_financiera/{dispId}")`.
5. **Navigation**: agregar `composable("informacion_financiera/{dispensacionId}")` en `MainDrawerScreen` NavHost, con guard por null.
6. **Venta sync**: al guardar cambios, `InformacionFinancieraViewModel` construye Venta actualizada y llama `repository.upsertVenta()` + `postSaveSyncScheduler.scheduleFinanzasSync()`.
7. **Estilo**: Cards 12dp, Material 3, colores `#2C3E50`/`#27AE60`, modo oscuro soportado.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `.../ui/screens/InformacionFinancieraScreen.kt` | New | Nueva pantalla dedicada |
| `.../viewmodel/InformacionFinancieraViewModel.kt` | New | ViewModel dedicado |
| `.../data/DispensacionFinancieraRepository.kt` | New | Interface + impl |
| `.../ui/screens/NuevaDispensacionScreen.kt` | Modified | FinancieraInfoSection → Card resumen + boton |
| `.../ui/screens/DispensacionFormSections.kt` | Modified | FinancieraInfoSection se mantiene (no se borra aun, se deja para rollback) pero deja de usarse desde NuevaDispensacionScreen |
| `.../ui/screens/MainDrawerScreen.kt` | Modified | Nueva ruta en NavHost |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `FinancieraInfoSection` legacy sigue existiendo pero sin uso | Low | Marcar @Deprecated con mensaje; eliminar en Fase 4 cleanup |
| Saldo inconsistente si pagos se modifican concurrentemente desde otro lugar | Low | Offline-first: unico punto de escritura es este ViewModel |
| Regresion en guardado de dispensacion (falta sync de venta) | Medium | El boton Guardar del refactor solo persiste disp + items + pagos; venta se sincroniza desde el nuevo screen |

## Rollback Plan

- **Revertir archivos**: `git checkout HEAD -- openspec/changes/informacion-financiera-screen/` + revertir `NuevaDispensacionScreen.kt`, `MainDrawerScreen.kt`, `DispensacionFormSections.kt`.
- `FinancieraInfoSection` se conserva en `DispensacionFormSections.kt` — solo se elimina su invocacion desde `NuevaDispensacionScreen`.
- Los datos financieros no se pierden: toda la informacion persiste en `dispensaciones`, `pagos` y `ventas`.

## Dependencies

- Fase 1 completa (Venta entity, VentaDao, OptoRepository.upsertVenta). Verificado.
- `OptoRepository` expone `getPagosByDispensacion()`, `insertPago()`, `updatePago()`, `deletePagoRegistrandoAnulacionEnCaja()`.

## Success Criteria

- [ ] InformacionFinancieraScreen muestra sticky header con OT, paciente, fecha, descripcion
- [ ] Monto total editable persiste al guardar
- [ ] Agregar/editar/eliminar pago actualiza saldo y persiste
- [ ] Cambio de estado Pendiente/Entregado persiste
- [ ] Al guardar, Venta upsert local via repository + scheduleFinanzasSync
- [ ] Boton "Gestionar Pagos" en NuevaDispensacionScreen navega a la nueva ruta
- [ ] Tests unitarios pasan (`./gradlew :optoapp:testDebugUnitTest`)
