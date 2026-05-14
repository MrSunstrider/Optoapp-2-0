# OptoApp SaaS — Documentación del Proyecto

> **Última actualización:** 2026-05-12
> **Repositorio:** OptoApp-2-0-saas
> **Versión:** SaaS (Android + Web)

---

## Índice

1. [Visión General](#1-visión-general)
2. [Principios Arquitectónicos](#2-principios-arquitectónicos)
3. [Stack Tecnológico](#3-stack-tecnológico)
4. [Modelo de Datos y Arquitectura](#4-modelo-de-datos-y-arquitectura)
5. [Seguridad y Acceso](#5-seguridad-y-acceso)
6. [Roles y Permisos](#6-roles-y-permisos)
7. [Versión Web — Ecosistema Seguro](#7-versión-web--ecosistema-seguro)
8. [Checklist de Release Web](#8-checklist-de-release-web)
9. [Reglas Clínicas](#9-reglas-clínicas)
10. [Changelog Operativo](#10-changelog-operativo)

---

## 1. Visión General

OptoApp es un SaaS para ópticas y optometristas que gestiona pacientes, evaluaciones clínicas, dispensaciones y servicios extra. Opera **offline-first** en Android con sincronización a Supabase, y cuenta con una **versión web** (OptoWeb) como extensión de backoffice/escritorio.

### Plataformas

| Plataforma | Stack | Enfoque |
|------------|-------|---------|
| **Android** | Kotlin + Jetpack Compose | Offline-first, operación en punto de atención |
| **Web** | Next.js + TypeScript + Tailwind | Backoffice, dashboard gerencial, reporting |
| **Backend** | Supabase (PostgreSQL, Auth, RLS) | Fuente única de verdad |

### Principios rectores

- **Offline-first**: la app funciona sin internet, persistiendo en Room y sincronizando al recuperar conectividad.
- **Multi-tenant por diseño**: toda entidad de negocio está aislada por `optica_id`.
- **Seguridad por defecto**: RLS obligatorio, autenticación robusta, sin secrets en cliente.
- **Integridad clínica**: reglas de diagnóstico reproducibles y documentadas.
- **Evolución controlada**: todo cambio significativo pasa por SDD (spec → plan → tasks).

---

## 2. Principios Arquitectónicos

### 2.1 Reglas no negociables

1. **Offline-first**: la app debe funcionar sin internet. Room es la fuente de trabajo local; Supabase es el espejo remoto.
2. **Multi-tenant por `optica_id`**: ninguna entidad de negocio compartida puede carecer de aislamiento por óptica.
3. **Seguridad en profundidad**: autenticación, RLS, PIN de 6 dígitos, cifrado de preferencias sensibles.
4. **Integridad clínica**: reglas de diagnóstico implementadas exactamente según especificación.
5. **Trazabilidad**: errores de sync se corrigen en origen, no se esconden en UI.
6. **Una sola fuente de verdad**: Supabase es la fuente remota; Android y Web comparten el mismo backend.

### 2.2 Decisiones arquitectónicas cerradas

| Decisión | Resolución |
|----------|-----------|
| UI Android | Jetpack Compose + Material 3. No usar Fragments/ViewBinding como ruta nueva. |
| Arquitectura Android | MVVM con ViewModel + StateFlow. |
| DI | Hilt. |
| Red | Ktor + supabase-kt + kotlinx.serialization. |
| PIN de producto | 6 dígitos. `123456` es solo desarrollo transitorio. |
| Modelo multi-optica | Un usuario puede pertenecer a varias ópticas. La activa se persiste por sesión. |
| Compartición de pacientes | No hay paciente global. Una misma persona puede existir como registros separados por `optica_id`. |
| Edición de pacientes | UPDATE, no DELETE+INSERT. Se preserva historial clínico y FK. |
| Sincronización | Orden obligatorio: Pacientes → Evaluaciones → Dispensaciones → ServiciosExtra → Pagos. |
| Conflicto de sync | Last Write Wins con `updated_at`. Reintentos con backoff exponencial. |
| Preferencias sensibles | `EncryptedSharedPreferences` via `SecurityManager`/`SessionManager`. |
| NOT NULL en Supabase | No crear sin estrategia de compatibilidad con Room, DTOs y sync. |

---

## 3. Stack Tecnológico

### 3.1 Android

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Persistencia local | Room |
| Inyección | Hilt |
| Red | Ktor + supabase-kt + kotlinx.serialization |
| Sesión | SessionManager + SecurityManager |

### 3.2 Web (OptoWeb)

| Capa | Tecnología |
|------|-----------|
| Framework | Next.js 15 (App Router) |
| Lenguaje | TypeScript |
| UI | Tailwind CSS + shadcn/ui |
| Auth/Sesión | Supabase SSR (`@supabase/ssr`) con cookies seguras |
| Backend | Mismo Supabase que Android |
| Despliegue | Vercel (recomendado) |
| PATH alias | `@/` → `src/` |

### 3.3 Supabase (Backend común)

- PostgreSQL con RLS obligatorio en tablas multi-tenant
- Migraciones en `supabase/migrations/`
- Auth: email/password + Google OAuth
- Edge Functions para lógica compartida (futuro)

---

## 4. Modelo de Datos y Arquitectura

### 4.1 Entidades principales

| Entidad | Descripción | Dependencia de sync |
|---------|------------|-------------------|
| Paciente | Datos demográficos y contacto | Ninguna (raíz) |
| Evaluación | Examen visual, refracción, diagnóstico | Paciente |
| Dispensación | Venta de lentes/montura, OT | Paciente |
| ServicioExtra | Venta/servicio no ligado a dispensación | Paciente (opcional) |
| Pago | Abonos a dispensación o servicio | Dispensación / ServicioExtra |
| Montura | Catálogo de inventario | Ninguna |
| MovimientoMontura | Trazabilidad de inventario | Montura |
| UsuarioOptica | Puente N:N usuario ↔ óptica con rol | Ninguna |

### 4.2 Estrategia de sincronización

```
Orden obligatorio:
  1. Pacientes
  2. Evaluaciones (requiere paciente existente en remoto)
  3. Dispensaciones (requiere paciente existente)
  4. ServiciosExtra
  5. Pagos (requiere dispensación/servicio existente)
```

- **Política de conflicto**: Last Write Wins con `updated_at`.
- **Reintentos**: backoff exponencial para fallos de red o dependencia temporal.
- **Estado local**: `sync_entity_state` en Room con `pending/synced/error` por fila.
- **Observabilidad**: telemetría en `sync_telemetry_optica` + UI de diagnóstico en Configuración.

### 4.3 Principio: los errores se solucionan, no se esconden

- Un fallo de sync debe **corregirse en origen** (causa raíz, orden de upserts, cancelación de corrutinas, red, RLS).
- Quitar un mensaje de error o limpiar el diagnóstico es **acción de usuario** o consecuencia de una **sync exitosa**.
- Las **cancelaciones de corrutina** (`CancellationException`) no son fallos de negocio. La sync pesada debe ejecutarse en **scope de aplicación** para no cancelarse al salir de pantalla.

---

## 5. Seguridad y Acceso

### 5.1 Autenticación

| Método | Estado | Detalle |
|--------|--------|---------|
| Email + contraseña | ✅ Implementado | Supabase Auth, flujo unificado post-login |
| Google OAuth (Android) | ✅ Implementado | Deeplink `optoapp://auth`, callback configurado |
| Google OAuth (Web) | 🔲 Pendiente | Misma configuración de Google Cloud |

**Configuración OAuth Google (checklist):**

1. **Google Cloud Console:**
   - OAuth Consent Screen en modo `Usuarios externos`
   - Credencial tipo `Web application`
   - Redirect: `https://<proyecto>.supabase.co/auth/v1/callback`

2. **Supabase Auth > Provider Google:**
   - Client ID y Client Secret de Google Cloud
   - `Skip nonce checks` = OFF
   - `Allow users without an email` = OFF

3. **Supabase Auth > URL Configuration:**
   - Redirect URL móvil: `optoapp://auth`

4. **`local.properties`**: `supabase.redirect.scheme=optoapp`, `supabase.redirect.host=auth`

### 5.2 RLS y Multi-tenancy

- Toda tabla multi-tenant tiene políticas RLS obligatorias.
- Las policies usan `(select auth.uid())` en lugar de `auth.uid()` para evitar reevaluación por fila (`auth_rls_initplan`).
- Las funciones `SECURITY DEFINER` están en esquema privado `app_private`.
- Ninguna clave secreta (`service_role`) se expone en cliente.

### 5.3 PIN y sesión

- PIN obligatorio de 6 dígitos en el ciclo de sesión.
- El valor `123456` es solo desarrollo; debe migrar a PIN definido por usuario.
- Preferencias sensibles cifradas con `EncryptedSharedPreferences`.

### 5.4 Backup/Restore endurecido

| Protección | Implementación |
|-----------|---------------|
| Solo admin | Validación en app + RPC server-side |
| Restricción por óptica | `source_optica_id` en metadata; restore solo si coincide con óptica activa |
| RPC de autorización | `assert_backup_operation_allowed` (SECURITY INVOKER) |

### 5.5 Eliminación de pacientes con guardrails

| Guardrail | App | Supabase |
|-----------|-----|----------|
| Rol permitido | admin o gerente | Policy RLS |
| Límite diario | 10 por usuario/óptica | Trigger + auditoría |
| Confirmación | Diálogo explícito | — |
| Auditoría | — | Tabla `pacientes_delete_audit` |

#### Consultas de auditoría

```sql
-- Últimos 50 borrados
select a.deleted_at, a.optica_id, a.paciente_id,
       coalesce(up.email, '(sin email)') as deleted_by_email
from public.pacientes_delete_audit a
left join public.user_profiles up on up.user_id = a.deleted_by
order by a.deleted_at desc limit 50;

-- Conteo diario por usuario/óptica
select date_trunc('day', deleted_at) as dia_utc, optica_id,
       deleted_by, count(*) as total
from public.pacientes_delete_audit
group by 1,2,3 order by dia_utc desc, total desc;
```

### 5.6 Reglas operativas de seguridad

| Acción | Rol permitido |
|--------|--------------|
| Backup/restore total | Solo admin |
| Asignación de admin | Solo admin actual |
| Borrado de pacientes | admin/gerente (con límite diario) |
| Edición de perfil fiscal de óptica | admin/gerente |
| Alta de sucursales | admin/gerente |

> Cualquier nueva operación sensible debe tener doble defensa: validación en app (UX/permisos) + validación en backend (RLS/trigger/RPC).

---

## 6. Roles y Permisos

### 6.1 Matriz de permisos Android

| Rol | BI/Reportes | Cierre de caja | Operación de hoy | Exportar pendientes | Exportar cierre | Exportar inventario |
|-----|:-----------:|:--------------:|:----------------:|:-------------------:|:--------------:|:------------------:|
| admin | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| especialista | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| gerente | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| asesor / asesora | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| ventas | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ |
| invitado | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

> La referencia central en código vive en `AppRoles`. Si se incorpora un rol nuevo, debe agregarse a la matriz y a `AppRoles` en el mismo cambio.

### 6.2 Matriz de permisos Web ↔ Android (alineación P4)

| Dominio | Android | Web | Regla de alineación |
|---------|---------|-----|-------------------|
| Contexto activo de óptica | Selección y persistencia por sesión | Selección y persistencia por cookie httpOnly | Debe existir contexto antes de operar |
| Dashboard | ✅ Disponible | ✅ Disponible | Solo datos por `optica_id` |
| Reportes/BI | Restringido por rol | Restringido por rol (menú + guardia de ruta) | Cualquier vista financiera hereda esta regla |
| Cierre de caja | Restringido por rol | 🔲 No implementado | Al implementarse, copiar política de Android |
| Operación de hoy | Restringido por rol | 🔲 No implementado | Idem |
| Exportaciones | Permisos finos por rol | 🔲 No implementado | No habilitar sin matriz explícita |

**Regla para nuevos módulos sensibles en web:** el PR debe incluir en el mismo cambio:
1. Guardia server-side por rol
2. Filtro de navegación por rol
3. Actualización de esta matriz
4. Validación técnica (lint/build + prueba funcional por rol)

---

## 7. Versión Web — Ecosistema Seguro

### 7.1 Principios innegociables

1. **Multi-tenancy estricto** por `optica_id`.
2. **RLS como control real** de acceso (no solo ocultar UI).
3. **No exponer `service_role`** ni secretos en cliente web.
4. **Reutilizar reglas de negocio** existentes (clínica, finanzas, roles, guardrails).
5. **Trazabilidad operativa** (errores, auditoría, cambios de estado).

### 7.2 Contrato de compatibilidad con Android

La web debe respetar:
- Mismas tablas y campos (sin bifurcar modelo de datos)
- Misma semántica de estados (`Pendiente`/`Entregado`, etc.)
- Mismos criterios de rol y visibilidad
- Misma lógica de óptica activa
- Misma estrategia de resolución de conflictos basada en `updated_at`

### 7.3 Riesgos y mitigación

| Riesgo | Mitigación |
|--------|-----------|
| Duplicación de reglas de negocio | Mover reglas compartibles a RPC/Edge Functions o módulo común documentado |
| Saltos de seguridad por SSR mal implementado | Middleware obligatorio + consultas con usuario autenticado + RLS |
| Lecturas/escrituras cruzadas entre ópticas | Validar `optica_id` activa en cliente y backend (RLS/policies) |
| Sobre-escritura silenciosa por concurrencia web+app | Conservar `updated_at`, auditar cambios sensibles |

### 7.4 Estado de ejecución (P4)

| Fase | Descripción | Estado |
|------|------------|--------|
| **P4-T1** | Baseline web: Next.js + Supabase SSR + login + middleware + selección de óptica | ✅ DONE |
| **P4-T2** | Permisos multi-rol: menú por rol + guardias server-side + matriz web↔Android | ✅ DONE |
| **P4-T3** | Dashboard y navegación: KPIs reales + sidebar + estado operativo | ✅ DONE |
| **P4-T4** | Operación administrativa: pacientes CRUD + config fiscal + reportes financieros | ✅ DONE |
| **P4-T5** | Hardening, pruebas E2E, readiness y release | 🔲 IN PROGRESS |

### 7.5 Checklist de seguridad por release web

- [ ] Ninguna clave sensible en cliente (`NEXT_PUBLIC_*` solo publishable)
- [ ] Sin uso de `service_role` en frontend
- [ ] RLS validada en tablas expuestas
- [ ] Políticas SELECT/INSERT/UPDATE/DELETE coherentes por rol
- [ ] Vistas con `security_invoker` o acceso restringido
- [ ] Sin funciones SECURITY DEFINER en esquema expuesto
- [ ] Logs de auditoría para operaciones sensibles

### 7.6 Checklist de confiabilidad y persistencia

- [ ] Manejo uniforme de errores (mensajes accionables)
- [ ] Reintentos controlados para red inestable
- [ ] Timeouts y cancelaciones sin corrupción de estado
- [ ] Indicador de última actualización y estado de sincronización
- [ ] Pruebas de concurrencia app Android + web sobre mismos registros

---

## 8. Checklist de Release Web

### 8.1 Seguridad (bloqueante)

- [ ] Variables públicas limitadas a `NEXT_PUBLIC_SUPABASE_URL` y `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`
- [ ] No existe uso de `service_role` en frontend ni `NEXT_PUBLIC_*`
- [ ] Middleware exige sesión válida para rutas privadas
- [ ] Middleware exige `optica_id` activo y valida pertenencia en `usuario_optica`
- [ ] Módulos sensibles con guardia por rol server-side
- [ ] Acciones de escritura verifican persistencia real (0 rows por RLS = error)

### 8.2 Integridad multi-tenant (bloqueante)

- [ ] Todas las consultas usan `optica_id` activo
- [ ] Todas las mutaciones usan `eq("optica_id", activeOptica.opticaId)`
- [ ] No hay mezcla de datos al cambiar de óptica
- [ ] Casos de prueba con usuario multi-óptica validados

### 8.3 Pruebas funcionales mínimas (bloqueante)

**Auth y sesión:**
- [ ] Login válido redirige correctamente
- [ ] Login sin óptica activa redirige a selección
- [ ] Logout limpia sesión y contexto

**Selección de óptica:**
- [ ] Usuario con una membresía entra directo a dashboard
- [ ] Usuario con varias membresías debe seleccionar
- [ ] Cookie de óptica inválida se corrige con redirección segura

**Dashboard:**
- [ ] KPIs visibles y coherentes para `optica_id` activo
- [ ] Estado operativo muestra salud de fuentes y timestamp

**Pacientes:**
- [ ] Listado con búsqueda + filtros + paginación
- [ ] Crear/editar paciente funciona y confirma resultado
- [ ] Eliminar paciente requiere confirmación
- [ ] Errores de permisos/RLS no muestran falso éxito

**Configuración fiscal:**
- [ ] Admin/gerente pueden editar y guardar
- [ ] Roles sin permiso ven solo lectura
- [ ] Validaciones obligatorias activas

**Reportes:**
- [ ] Visibles solo para roles con BI
- [ ] Filtro por período funciona
- [ ] KPI financieros no mezclan ópticas

### 8.4 Calidad técnica (bloqueante)

- [ ] `npm run lint` en verde
- [ ] `npm run build` en verde
- [ ] Sin errores críticos en consola del navegador durante smoke test

### 8.5 GO / NO-GO

**GO:** Todos los checks bloqueantes completados, sin bug crítico de seguridad/tenanting/datos financieros.

**NO-GO:** Cualquier fuga entre ópticas, bypass de permiso sensible, o falso éxito en escritura por RLS.

### 8.6 Rollback operativo

1. Detener despliegue actual y volver al build anterior
2. Verificar login + selección de óptica + dashboard
3. Revisar cambios de middleware, roles y acciones server
4. Registrar incidente en changelog operativo y abrir tarea de remediación

---

## 9. Reglas Clínicas

### 9.1 Diagnóstico por refracción (esfera E, cilindro C, notación negativa)

| Condición | Diagnóstico |
|-----------|------------|
| E = 0 y C = 0 | Emetropía |
| E < 0 y C = 0 | Miopía |
| E > 0 y C = 0 | Hipermetropía |
| E = 0 y C < 0 | Astigmatismo miópico simple |
| E < 0 y C < 0 | Astigmatismo miópico compuesto |
| E > 0 y C < 0 y (E+C) > 0 | Astigmatismo hipermetrópico compuesto |
| E > 0 y C < 0 y (E+C) <= 0 | Astigmatismo mixto |
| E < 0 y C < 0 y (E+C) >= 0 | Astigmatismo mixto |

### 9.2 Reglas especiales

- Si esfera contiene `plano` o `neutro` → interpretar como 0.00 D.
- Si esfera contiene `balance` → diagnóstico de ese ojo es **Balance**.
- **Presbicia**: activa si ADD > 0.
- **Anisometropía**: diferencia de equivalente esférico >= 2.00 D, excluyendo ojos Balance.
- **Ambliopía**: diferencia de AV con corrección >= 2 líneas (0.2 logMAR).

### 9.3 Sugerencia de lente de contacto por astigmatismo corneal

| Diferencia K1-K2 | Sugerencia |
|------------------|------------|
| < 2.50 D | Lente blando |
| 2.50 a 3.99 D | Valorar RGP / Tórico |
| >= 4.00 D | Lente RGP |

---

## 10. Changelog Operativo

> Bitácora de cambios de alto impacto en negocio/seguridad. Para auditoría técnica completa, usar GitHub (commits, blame, diff).

### 2026-05-10

- **Sync**: reparada race condition en post-save que causaba errores "Paciente remoto inexistente" en evaluaciones.
  - `PostSaveSyncScheduler`: `scheduleHistorialSync` y `scheduleFinanzasSync` ejecutan `syncPacientesUseCase` primero dentro del mismo mutex.
  - `SyncHistorialUseCase`: agregado `onFailure` con `Log.w` para visibilidad en diagnóstico.
  - UI: corregido padding duplicado en `MonturasScreen`, `ConfiguracionScreen`.
  - Supabase: migración `20260510000400_add_tipo_aro_material_to_monturas.sql`.
  - Tests: `connectedDebugAndroidTest` 12/12 pasando.

### 2026-05-05

- **Modernización Arquitectónica**: Clean Architecture con Repository + Mappers + Strategy (Sync) + Builder (clínico) + Observer (Realtime) + Factory + Command + Proxy (RBAC) + Decorator (logging).
- **Estabilización Web**: lint + build en verde. Corrección de errores de compilación KSP/SDK en Android.
- **Corrección de fechas (SaaS Ready)**: selector manual de zona horaria en Configuración.
- **Sync post-guardado endurecido**: debounce + gate + `SyncInventarioUseCase` + eliminaciones encolan sync.
- **Hardening RLS inventario**: policies CRUD completas en `montura_movimientos`, escritura restringida por rol.

### 2026-04-29

- Producción Supabase actualizada con migraciones de `cierres_caja` y `optica_settings`.
- Validación runtime de permisos efectiva (admin vs no-miembro bloqueado por RLS).
- **Cierre funcional web P4-T4**: pacientes (CRUD con búsqueda/paginación/guardrails), evaluaciones (5 tabs + automatismos + OSDI), dispensaciones (OT, stock, abonos/anulaciones), servicios extra.

### 2026-04-27

- **Hardening web**: errores de datos visibles (`assertNoDbError`), auditoría de env público Supabase, estabilidad dev UX (Turbopack), fix "no carga nada" (middleware Edge), mitigación de UI sin estilos, fix bucle de redirección.
- **Runbook P4-T5**: checks automáticos PASS, NO-GO temporal hasta smoke tests manuales.

### 2026-04-25

- Refactor integral de Configuración en secciones composables.
- Endurecimiento de UX fiscal, sync financiera con reintentos/batch, validación post-persistencia.
- Contexto permanente de óptica activa en header.
- Onboarding con perfil fiscal, alta de sucursales, backup/restore admin-only.
- Protección de eliminación de pacientes con guardrails completos.

### Hitos anteriores (resumen)

| Fecha | Hito |
|-------|------|
| 2026-04-24 | Google OAuth Android habilitado |
| 2026-04-23 | Hardening RLS, sync servicios, panel de planes |
| 2026-04-22 | Onboarding de óptica, plan free 20 pacientes |
| 2026-04-20 | Sync post-guardado, ticket laboratorio |
| 2026-04-18 | Hito SaaS: sync + suscripción + agenda |
| 2026-04-15 | Sesión/sync por óptica |
| 2026-04-14 | Migración fechas clínicas a LocalDate |
| 2026-04-13 | Base multi-óptica con RLS |
| 2026-04-10 | Migración completa a modelo SaaS |
| 2026-03-27/04-09 | Fundación: respaldos, PIN, UI, BI |
| 2026-03-08 | Primer commit |

---

*Este documento unifica y reemplaza los archivos anteriores: `changelog-operativo.md`, `guia-operativa-auth-sync-seguridad.md`, `guia-web-ecosistema-seguro.md` y `web-readiness-checklist.md`.*
