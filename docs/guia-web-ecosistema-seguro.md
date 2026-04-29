# Guia de ruta web: ecosistema seguro, confiable y persistente

Documento operativo para construir la version web de OptoApp sin romper el sistema actual Android/Supabase.

## Objetivo

- Lanzar una web espejo de OptoApp como extension del ecosistema SaaS.
- Mantener una sola fuente de verdad de datos y permisos.
- Reducir riesgo de regresion en seguridad, consistencia y operacion diaria.

## Principios innegociables

1. Multitenancy estricto por `optica_id`.
2. RLS como control real de acceso (no solo ocultar UI).
3. No exponer `service_role` ni secretos en cliente web.
4. Reutilizar reglas de negocio existentes (clinica, finanzas, roles, guardrails).
5. Mantener trazabilidad operativa (errores, auditoria y cambios de estado).

## Alcance de la version web (vision)

- La web complementa Android; no lo reemplaza.
- Web orientada a backoffice/operacion de escritorio:
  - dashboard gerencial
  - administracion de pacientes y operaciones
  - configuracion, roles, sucursales y reportes
- Android mantiene ventaja offline-first y operacion en punto de atencion.

## Arquitectura objetivo para web

- Framework: Next.js (App Router) + TypeScript.
- UI: Tailwind + shadcn/ui.
- Auth/Sesion: Supabase SSR con `@supabase/ssr` y cookies seguras.
- Backend: mismo Supabase del ecosistema actual.
- Permisos: RLS + `usuario_optica` + rol activo.
- Despliegue recomendado: Vercel.

## Contrato de compatibilidad con Android

La web debe respetar:

- mismas tablas y campos (sin bifurcar modelo).
- misma semantica de estados (`Pendiente`/`Entregado`, etc.).
- mismos criterios de rol y visibilidad.
- misma logica de optica activa.
- misma estrategia de resolucion de conflictos basada en `updated_at` cuando aplique.

## Riesgos principales y mitigacion

1) Duplicacion de reglas de negocio entre Android y web
- Mitigacion: mover reglas compartibles a RPC/Edge Functions o modulo comun documentado.

2) Saltos de seguridad por mala implementacion SSR
- Mitigacion: middleware obligatorio + consultas siempre con usuario autenticado + RLS validada.

3) Lecturas/escrituras cruzadas entre opticas
- Mitigacion: validar `optica_id` activa en cliente y en backend (RLS/policies).

4) Sobre-escritura silenciosa de datos por concurrencia web+app
- Mitigacion: conservar `updated_at`, auditar cambios sensibles y mostrar estado de ultima actualizacion.

## Fases de ejecucion (ruta oficial)

### Fase 0 - Baseline tecnico y seguridad (obligatoria)

- Crear app web base con Next.js + TS + Tailwind + shadcn/ui.
- Configurar Supabase SSR (`server.ts`, `client.ts`, middleware).
- Implementar login (email/password y OAuth si aplica).
- Implementar cierre de sesion seguro.
- Implementar seleccion de optica activa (multi-membresia).
- DoD:
  - rutas privadas protegidas
  - sesion persistente
  - optica activa obligatoria antes de operar

### Fase 1 - Navegacion y dashboard operativo

- Layout autenticado (sidebar, header, estado de optica activa).
- Dashboard inicial con KPIs base.
- Placeholders de modulos con control de permisos por rol.
- DoD:
  - navegacion estable
  - permisos de menu por rol
  - indicadores base sin fuga de datos entre opticas

### Fase 2 - Operacion administrativa core

- Pacientes: listado, busqueda, detalle, alta/edicion.
- Reportes financieros base.
- Configuracion fiscal en modo seguro (segun rol).
- DoD:
  - CRUD funcional con RLS vigente
  - guardrails de eliminacion alineados con backend

### Fase 3 - Clinica y produccion

- Evaluaciones web.
- Dispensaciones y servicios extra.
- Agenda operativa.
- Exportaciones controladas.
- DoD:
  - consistencia funcional con app Android
  - validaciones equivalentes en datos criticos

### Fase 4 - Hardening final y observabilidad

- Telemetria web (errores, tiempos de respuesta, eventos clave).
- Pruebas E2E (login -> seleccion optica -> operacion completa).
- Stress de permisos multi-rol.
- Go-live checklist.

## Checklist de seguridad por release web

- [ ] Ninguna clave sensible en cliente (`NEXT_PUBLIC_*` solo publishable).
- [ ] Sin uso de `service_role` en frontend.
- [ ] RLS validada en tablas expuestas.
- [ ] Politicas de `SELECT/INSERT/UPDATE/DELETE` coherentes por rol.
- [ ] Vistas con `security_invoker` o acceso restringido.
- [ ] Sin funciones `SECURITY DEFINER` en esquema expuesto.
- [ ] Logs de auditoria para operaciones sensibles.

## Checklist de confiabilidad y persistencia

- [ ] Manejo uniforme de errores (mensajes accionables para usuario).
- [ ] Reintentos controlados para red inestable.
- [ ] Timeouts y cancelaciones manejadas sin corrupcion de estado.
- [ ] Indicador claro de ultima actualizacion y estado de sincronizacion.
- [ ] Pruebas de concurrencia app Android + web sobre mismos registros.

## Plan de pruebas minimo para arrancar

1. Login y sesion
- login valido/invalido
- persistencia tras refresh
- logout y revocacion de acceso a rutas privadas

2. Multi-optica
- usuario con una y multiples opticas
- cambio de optica activa y aislamiento de datos

3. Permisos
- validar menus y acciones para `admin`, `gerente`, `especialista`, `ventas`, `asesor`, `invitado`

4. Integridad de datos
- crear/editar/consultar paciente sin mezclar opticas
- probar guardrails en acciones sensibles

Checklist de salida (release readiness):
- `docs/web-readiness-checklist.md`

## Regla operativa de cambios

- Cualquier cambio web que afecte seguridad, auth, sync, roles o integridad debe:
  1) registrarse en `docs/changelog-operativo.md`
  2) actualizar esta guia si cambia el flujo objetivo
  3) actualizar `docs/sdd/tasks.md` con estado (TODO/IN_PROGRESS/DONE)

## Arranque inmediato (primer sprint)

Semana 1:
- baseline Next.js + Supabase SSR + middleware + login + seleccion de optica

Semana 2:
- layout autenticado + dashboard base + permisos de menu

Semana 3:
- pacientes (listado/detalle) + validaciones de seguridad

Semana 4:
- endurecimiento + pruebas E2E + reporte de readiness

## Estado de ejecucion actual

- P4-T1: DONE.
- Ya existe carpeta `web/` con baseline tecnico (SSR, auth base, middleware y dashboard inicial).
- Seleccion real de optica activa implementada usando `usuario_optica`.
- Persistencia de contexto activo implementada con cookie httpOnly (`optoapp_active_optica`).
- Validacion tecnica completada con `npm run lint` y `npm run build`.
- Siguiente paso inmediato: iniciar P4-T2 para endurecer matriz de permisos por rol en rutas/modulos.
