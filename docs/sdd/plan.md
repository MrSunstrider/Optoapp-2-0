# Plan tecnico de OptoApp

## Objetivo tecnico
Implementar y operar OptoApp con consistencia entre capa local (Room), capa remota (Supabase) y reglas de negocio clinicas, priorizando estabilidad de sincronizacion y aislamiento multi-tenant.

## Arquitectura objetivo (estado vigente)
- Cliente Android: Kotlin + Compose + Material 3.
- Patron: MVVM con ViewModel y StateFlow.
- Datos locales: Room como fuente de trabajo offline.
- Datos remotos: Supabase Postgres con RLS.
- Integracion remota: supabase-kt + Ktor + kotlinx.serialization.
- Inyeccion: Hilt.

## Modelo de datos y contratos
- Todas las entidades de negocio compartidas incorporan `optica_id`.
- Entidades principales:
  - Paciente
  - Evaluacion
  - Dispensacion
  - ServicioExtra
  - Pago
  - UsuarioOptica (puente de pertenencia y rol)
- Recomendacion de contrato:
  - `created_at`, `updated_at`, `sync_status` donde aplique
  - Mapeos DTO con `@SerialName` en `snake_case`
  - Compatibilidad estricta de nullabilidad entre Room y Supabase

## Estrategia de sincronizacion
- Orden fijo por dependencias:
  1. Pacientes
  2. Evaluaciones
  3. Dispensaciones
  4. ServiciosExtra
  5. Pagos
- Politica de conflicto: Last Write Wins con `updated_at`.
- Reintentos: backoff exponencial para fallos de red o dependencia temporal.
- Estado local: `pending/synced/error` para diagnostico y reintento selectivo.
- Observabilidad minima:
  - logs por entidad y lote
  - causa raiz por error (RLS, nullabilidad, FK, token)

### Principio: los errores se solucionan, no se esconden

- Un fallo de sync debe **corregirse en origen** (causa raiz, orden de upserts, cancelacion de corrutinas, red, RLS, etc.), no **ocultarse** en la UI ni borrarse del historial sin haber abordado el problema.
- Quitar el mensaje o limpiar la lista de diagnostico es una **accion de usuario** o consecuencia de una **sync exitosa**, no un sustituto de arreglar datos o contratos.
- Las **cancelaciones de corrutina** (`CancellationException`) no son fallos de negocio: no deben registrarse como error de sync salvo diagnostico residual; la sync pesada debe ejecutarse en **scope de aplicacion** para no cancelarse al salir de pantalla.

## Seguridad y acceso
- Login con Supabase Auth (email/contrasena).
- Bloqueo por PIN de 6 digitos en ciclo de sesion.
- Secrets solo en `local.properties` -> `BuildConfig`.
- RLS obligatoria por `optica_id` y rol activo.

## Roles y permisos
- Tabla `usuario_optica` con `usuario_id`, `optica_id`, `rol`.
- Al iniciar sesion:
  - Si hay multiples opticas, seleccionar contexto activo.
  - Persistir seleccion y usarla en queries locales/remotas.
- Capa de permisos en cliente:
  - controla visibilidad de pantallas y acciones sensibles.

## Alcance futuro (no bloqueante de P0)
- Suscripciones y paywall.
- Integracion Google Play Billing.
- Version web en Next.js conectada al mismo backend.

## Trazabilidad hacia tareas
- Requisitos funcionales y clinicos: `spec.md`.
- Ambiguedades resueltas y criterios de negocio: `clarification.md`.
- Ejecucion por etapas y Definition of Done: `tasks.md`.
