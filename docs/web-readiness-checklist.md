# Web Readiness Checklist (P4-T5)

Checklist operativo para salida controlada de la version web de OptoApp.

## 1) Seguridad (bloqueante)

- [ ] Variables publicas limitadas a:
  - `NEXT_PUBLIC_SUPABASE_URL`
  - `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`
- [ ] No existe uso de `service_role` en frontend ni en `NEXT_PUBLIC_*`.
- [ ] Middleware exige sesion valida para rutas privadas.
- [ ] Middleware exige `optica_id` activo y valida pertenencia en `usuario_optica`.
- [ ] Modulos sensibles con guardia por rol server-side (`reportes`, `configuracion` editable).
- [ ] Acciones de escritura verifican persistencia real (`0 rows` por RLS se trata como error).

## 2) Integridad multi-tenant (bloqueante)

- [ ] Todas las consultas de datos operativos usan `optica_id` activo.
- [ ] Todas las mutaciones usan `eq("optica_id", activeOptica.opticaId)`.
- [ ] No hay mezclas de datos al cambiar de optica.
- [ ] Casos de prueba con usuario multi-optica validados.

## 3) Pruebas funcionales minimas (bloqueante)

### Auth y sesion
- [ ] Login valido redirige correctamente.
- [ ] Login sin optica activa redirige a seleccion.
- [ ] Logout limpia sesion y contexto.

### Seleccion de optica
- [ ] Usuario con una membresia entra directo a dashboard.
- [ ] Usuario con varias membresias debe seleccionar optica.
- [ ] Cookie de optica invalida se corrige con redireccion segura.

### Dashboard
- [ ] KPIs visibles y coherentes para `optica_id` activo.
- [ ] Estado operativo muestra salud de fuentes y timestamp.

### Pacientes (MVP)
- [ ] Listado con busqueda + filtros por edad + paginacion.
- [ ] Crear paciente funciona y confirma resultado.
- [ ] Editar paciente funciona y confirma resultado.
- [ ] Eliminar paciente requiere confirmacion `ELIMINAR`.
- [ ] Errores de permisos/RLS no muestran falso exito.

### Configuracion fiscal
- [ ] Admin/Gerente pueden editar y guardar.
- [ ] Roles sin permiso ven solo lectura.
- [ ] Validaciones obligatorias activas.

### Reportes
- [ ] Reportes visibles solo para roles con BI.
- [ ] Filtro por periodo funciona (`dia/semana/mes/anio`).
- [ ] KPI financieros no mezclan opticas.

## 4) Calidad tecnica (bloqueante)

- [ ] `npm run lint` en verde.
- [ ] `npm run build` en verde.
- [ ] Sin errores criticos en consola del navegador durante smoke test.

## 5) Go/No-Go

Condiciones de GO:
- todos los checks bloqueantes completados,
- sin bug critico de seguridad/tenanting,
- sin bug critico de datos financieros.

Condiciones de NO-GO:
- cualquier fuga entre opticas,
- cualquier bypass de permiso sensible,
- cualquier falso exito en escritura por RLS.

## 6) Rollback operativo

Si hay incidente post-release:
1. Detener despliegue actual y volver al build anterior.
2. Verificar login + seleccion de optica + dashboard.
3. Revisar cambios de middleware, roles y acciones server.
4. Registrar incidente en changelog operativo y abrir tarea de remediacion.

## 7) Evidencia de cierre P4-T5

Al cerrar P4-T5 registrar:
- fecha/hora local,
- commit/hash,
- resultado de lint/build,
- resumen de smoke tests por rol,
- decision GO/NO-GO con motivo.

---

## Ejecucion runbook (2026-04-27)

Estado de verificacion automatica en esta sesion:

- PASS · `npm run lint`
- PASS · `npm run build`
- PASS · sin referencias a `service_role` en `web/`
- PASS · uso de variables `NEXT_PUBLIC_*` limitado a URL + publishable key
- PASS · middleware exige sesion y valida pertenencia de `optica_id` en `usuario_optica`
- PASS · consultas/mutaciones operativas web con filtro por `optica_id` activo
- PASS · guardias por rol en modulos sensibles (`reportes`, `configuracion` editable)

Pendiente manual (requiere smoke test por operador/QA):

- PENDING · prueba de login/logout en entorno real
- PENDING · prueba multi-optica (una vs multiples membresias)
- PENDING · prueba por rol (`admin`, `gerente`, `especialista`, `ventas/asesor`, `invitado`)
- PENDING · validacion funcional de errores de red/permisos en UX final

Decision actual:
- NO-GO temporal hasta cerrar pendientes manuales de smoke test por rol.
