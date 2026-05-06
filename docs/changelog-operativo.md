# Changelog operativo (cronologico)

Registro operativo de cambios relevantes en autenticacion, sincronizacion, seguridad y gobernanza de datos.

> Zona horaria de referencia: UTC-5 (hora local del equipo).

## 2026-05-05

- `23:15` · `(sin commit)`  
  Modernización Arquitectónica OptoApp (Android & Web): Implementación de patrones de diseño Clean Architecture para escalabilidad y paridad de plataformas.
  - **Repository & Mappers**: Desacoplamiento de la base de datos (Room/Supabase) mediante repositorios modulares y mappers bidireccionales para todas las entidades financieras y clínicas.
  - **Strategy (Sync)**: Nuevo `SyncManager` en Android que delega la lógica de sincronización a estrategias intercambiables (`FullSyncStrategy`, `ReadOnlySyncStrategy`), preparando el camino para sync incremental.
  - **Builder (Clínico)**: Implementación de `EvaluacionBuilder` con automatización de transposición de cilindros positivos a negativos y normalización de valores "plano/neutro".
  - **Observer (Realtime)**: Infraestructura de actualización en tiempo real con `SupabaseObserver` (Android Flow) y `useSupabaseRealtime` (React hook) para reactividad instantánea.
  - **Factory**: `DiagnosticoFactory` para suministrar diferentes motores de diagnóstico según el rol del usuario (Admin/Especialista vs Asesor).
  - **Command**: Encapsulamiento de operaciones críticas como `Backup` y `Export` con soporte para colas e historial.
  - **Proxy**: Capa de seguridad RBAC en los repositorios para validar permisos de borrado/edición según el rol del usuario.
  - **Decorator**: Implementación de `LoggingPacienteRepository` para observabilidad transparente sin alterar la lógica de negocio.
  - **Estabilidad Web**: Corrección de errores de linting (unescaped entities) en el proyecto Next.js para asegurar build limpio.

## 2026-04-29

- `21:38` · `(sin commit)`  
  Producción Supabase actualizada en runtime: aplicadas migraciones `cierres_caja_and_optica_settings` y `cierres_optica_settings_rls_perf_fix`; se corrigen tipos (`optica_id` como `text`) y trigger de `updated_at` (`public.update_updated_at()`), se añaden índices FK (`closed_by`, `reopened_by`) y se optimizan políticas RLS (`auth.uid()` con `SELECT` + separación SELECT/INSERT/UPDATE/DELETE).

- `21:38` · `(sin commit)`  
  Validación runtime de permisos efectiva: en contexto `authenticated` con JWT simulado, `admin` logra `INSERT/UPSERT` en `public.optica_settings` y `public.cierres_caja`, mientras usuario no miembro recibe bloqueo RLS (`new row violates row-level security policy`) en ambas tablas.

- `21:38` · `(sin commit)`  
  Verificación post-migración en proyecto `OptoApp`: `list_migrations` confirma versiones nuevas registradas en base; `get_advisors` sin alertas nuevas de seguridad por estas tablas (queda WARN global de Auth por leaked password protection deshabilitado).

- `01:12` · `(sin commit)`  
  Cierre funcional web del módulo Pacientes con paridad operativa frente a app móvil: listado con búsqueda/chips/paginación, CRUD de paciente con HO sugerida + validación de duplicado por óptica, ficha con tabs y acciones críticas (WhatsApp plantillas, PDF receta de última evaluación, eliminación protegida con límite diario), flujo completo de Evaluaciones (5 tabs) con automatismos clínicos y OSDI, Dispensaciones (OT sugerida, OT única por óptica, stock de montura en edición, ciclo de abonos/anulaciones), y Servicios Extra (asociación opcional a paciente + reglas financieras completas con `a_cuenta` y anulaciones contables).

- `01:12` · `(sin commit)`  
  Hardening transversal App/Web en Pacientes: controles server-side por rol/óptica activa, guardrails de coherencia financiera en eliminación de abonos (anulación negativa y validación de pertenencia por documento), ajuste de permisos UI para evitar acciones de edición sin gestión, y actualización de `docs/sdd/tasks.md` con cierre por prioridad P1/P2 y remanente P3 visual.

- `01:12` · `(sin commit)`  
  Verificación técnica de estabilidad posterior a cambios: `npm run lint` y `npm run build` en `web/` ejecutados en verde tras cada bloque crítico de correcciones.

## 2026-04-27

- `17:25` · `(sin commit)`  
  Errores de datos sin ocultar: `assertNoDbError` (`lib/supabase/db-error.ts`) en pacientes, clínico por paciente, membresías y óptica fiscal; reportes financieros validan todas las queries; KPIs del dashboard conservan KPI parcial pero muestran **mensaje Supabase por fuente** en “Estado operativo”; configuración fiscal propaga mensaje real de Postgres/RLS en `detalle` al fallar guardado.

- `17:10` · `(sin commit)`  
  Auditoría web: módulo `lib/supabase/env-public.ts` con validación explícita de variables públicas Supabase; cliente servidor/browser usa `requirePublicSupabaseEnv`; `server.ts` deja de ignorar errores desconocidos en `setAll` (solo omite la restricción conocida de cookies en RSC con log en dev); middleware usa `getPublicSupabaseEnv`, redirección visible a `/login?error=configuracion` si falta env, `console.error` en fallos de `usuario_optica`, cookie inválida advertida en dev; página login muestra banner de configuración; `error.tsx` muestra digest y stack en desarrollo; logout registra errores sin ocultarlos; navegación shell con espacio entre etiqueta y estado.

- `16:48` · `(sin commit)`  
  Estabilidad web dev UX: `npm run dev` usa Turbopack por defecto (`next dev --turbo`) para evitar errores recurrentes de Webpack/HMR; matcher del middleware incluye `/` explícito; middleware valida env Supabase y limita consulta `usuario_optica` con `abortSignal` 6s para no colgar navegación si la red va lenta.

- `16:40` · `(sin commit)`  
  Fix crítico “no carga nada” / HTTP 500 en rutas web: el middleware importaba `ACTIVE_OPTICA_COOKIE` desde `optica-context.ts`, archivo que depende de `next/headers` (no válido en Edge Middleware); se extrae la constante a `web/src/lib/optica-cookie.ts`, el middleware solo importa ese módulo y se añade try/catch defensivo en sesión.

- `16:25` · `(sin commit)`  
  Mitigación de UI sin estilos en web: middleware deja de aplicarse a todo `/_next/*` (no solo `static`/`image`) y `updateSession` hace bypass explícito de `/_next/`, `/api/` y favicon, evitando interferir con chunks/CSS y dejando de servir HTML accidental en lugar de hojas de estilo.

- `16:12` · `(sin commit)`  
  Mitigación de error runtime Webpack en dev (`__webpack_modules__[moduleId] is not a function`): en `web/next.config.mjs` se desactiva caché de Webpack solo en modo desarrollo, se añaden scripts `npm run clean` / `npm run dev:clean` y alternativa `npm run dev:turbo` (Turbopack) para evitar HMR/caché corrupta; `build` de producción verificado.

- `16:06` · `(sin commit)`  
  Navegación web alineada a la app móvil: Evaluaciones, Dispensaciones y Servicios extra salen del menú lateral global y pasan al expediente del paciente (`/pacientes/[id]/…`) con pestañas; rutas huérfanas `/evaluaciones`, `/dispensaciones`, `/servicios` redirigen al listado de pacientes.

- `15:55` · `(sin commit)`  
  Estabilización de runtime web ante error `__webpack_modules__[moduleId] is not a function`: se limpia estado dev dejando un único `next dev` activo y se fija persistencia de cookie de óptica activa en `/auth/select-optica` escribiéndola directamente sobre `NextResponse.redirect`, evitando rebotes de sesión/contexto que forzaban recargas completas.

- `15:53` · `(sin commit)`  
  Fix de bucle de redirección web (`ERR_TOO_MANY_REDIRECTS`): middleware ahora permite explícitamente la ruta `/auth/select-optica` sin exigir cookie de óptica activa previa, evitando loop entre selección de óptica y auto-selección tras login.

- `17:58` · `(sin commit)`  
  Fix runtime en selección de óptica web: auto-selección (usuario con 1 membresía) deja de escribir cookie desde Server Component y ahora delega a Route Handler (`/auth/select-optica`), eliminando el error `Cookies can only be modified...` y la cascada de fallos de bundler en dev.

- `17:50` · `(sin commit)`  
  Corrección de login web post-feedback: se evita excepción de Next.js por escritura de cookies en Server Components (`Cookies can only be modified...`) envolviendo `setAll` de Supabase SSR en `try/catch` y delegando escrituras efectivas a middleware/handlers; validado con lint/build.

- `17:37` · `(sin commit)`  
  Ejecución de runbook P4-T5: se validan checks automáticos de readiness (lint/build, ausencia de `service_role`, uso controlado de `NEXT_PUBLIC_*`, guardias de sesión/optica/rol y filtros por `optica_id`); se mantiene NO-GO temporal hasta completar smoke tests manuales por rol.

- `17:27` · `(sin commit)`  
  Inicio P4-T5: se define checklist operativo de readiness para release web (`docs/web-readiness-checklist.md`) con controles bloqueantes de seguridad/multi-tenant, smoke tests por flujo/rol, criterios GO/NO-GO y plan de rollback.

- `17:14` · `(sin commit)`  
  Optimización post-cierre de P4-T4: se endurecen acciones de pacientes y configuración fiscal para detectar `0 rows` por RLS como error real (evita falsos “guardado/eliminado”), y se corrige paginación fuera de rango en listado de pacientes.

- `17:01` · `(sin commit)`  
  Se completa P4-T4 web (operación administrativa MVP): pacientes con CRUD mínimo endurecido, configuración fiscal segura por rol y reportes financieros base por período; verificado con lint/build y cumplimiento explícito de DoD en `docs/sdd/tasks.md`.

- `16:48` · `(sin commit)`  
  P4-T4 web añade Configuración fiscal segura por rol: vista de lectura para todos los roles permitidos y edición únicamente para `admin/gerente` con guardia server-side y feedback de validación/guardado.

- `16:34` · `(sin commit)`  
  P4-T4 web endurece flujo de pacientes: eliminación protegida con confirmación explícita y `delete` acotado por `optica_id`, paginación real + filtros por edad en listado, y feedback visible de éxito/error en crear/editar/eliminar.

- `16:26` · `(sin commit)`  
  Se revisa y cierra P4-T3 como DONE tras verificación explícita de DoD: navegación web estable con sesión activa (guardias + selección de óptica + shell) y KPIs visibles sin mezcla de tenants (filtro por `optica_id`).

- `16:19` · `(sin commit)`  
  Inicio P4-T4 web (operación administrativa): se implementa Pacientes MVP con listado + búsqueda, alta, detalle y edición básica, aplicando guardias por rol y aislamiento por `optica_id` en consultas y mutaciones; validado con lint/build.

- `16:07` · `(sin commit)`  
  Revisión y optimización transversal de P4-T1/T2/T3: middleware ahora valida que la óptica activa del contexto pertenezca realmente al usuario autenticado (mitiga tampering de cookie), `secure` de cookie ajustado por entorno (dev/prod), y KPIs del dashboard corrigen borde de zona horaria usando fechas locales con timeout por consulta.

- `15:56` · `(sin commit)`  
  P4-T3 web avanza en operabilidad: dashboard incorpora tarjeta de estado operativo (salud por fuente + timestamp de última actualización) y sidebar agrega indicadores visuales de estado por módulo (`listo`/`base`), con validación lint/build.

- `15:43` · `(sin commit)`  
  Inicio P4-T3 web: dashboard deja estado placeholder y ahora calcula KPIs reales por `optica_id` (ventas día/mes, pacientes del día y saldos pendientes) consultando `pagos`, `pacientes`, `dispensaciones` y `servicios_extra`; validado con lint/build.

- `15:32` · `(sin commit)`  
  Se cierra P4-T2 al 100% a nivel de gobernanza: matriz de permisos web↔Android agregada en `docs/sdd/plan.md` y criterio obligatorio para futuros módulos sensibles (guardia server-side + filtro de menú + actualización de matriz + validación por rol) registrado en `docs/sdd/tasks.md`.

- `15:20` · `(sin commit)`  
  Inicio P4-T2 web (permisos): menú lateral ahora filtra módulos por rol activo y rutas de módulo aplican guardia server-side; `reportes` queda restringido a roles con acceso BI (admin/especialista/gerente), validado con build/lint.

- `15:12` · `(sin commit)`  
  Se completa P4-T1 web: selección de óptica activa conectada a `usuario_optica`, persistencia de contexto en cookie httpOnly (`optoapp_active_optica`), auto-selección con membresía única, guardia en middleware para exigir contexto antes de operar y logout server-side; verificado con `npm run lint` + `npm run build`.

- `15:01` · `(sin commit)`  
  Entorno web habilitado para ejecucion local: instalacion Node LTS con `npm`, instalacion de dependencias en `web/`, ajuste de tipados Supabase SSR y validacion exitosa de `npm run lint` + `npm run build`.

- `14:52` · `(sin commit)`  
  Inicio de implementacion P4-T1 web: bootstrap manual de `web/` con Next.js App Router + TypeScript + Tailwind, cliente Supabase SSR (`server/client/middleware`), login base, rutas protegidas y dashboard inicial; queda pendiente validacion de build/dev al habilitar `npm` en entorno.

- `14:40` · `(sin commit)`  
  Se define y documenta la ruta oficial para la version web como ecosistema seguro/confiable/persistente: nueva guia `docs/guia-web-ecosistema-seguro.md`, enlace en `README.md` y apertura formal de fase web en backlog SDD.

- `00:59` · `(sin commit)`  
  Hardening integral de base de datos: índices compuestos por `optica_id + fecha/updated_at`, constraints explícitos de estado (`Pendiente/Entregado`), normalización `updated_at` en UTC y limpieza de índices OT redundantes en `dispensaciones`.

- `00:59` · `(sin commit)`  
  Observabilidad operativa de sincronización: nueva tabla `sync_telemetry_optica` con trigger de auditoría, RLS por membresía de óptica y bloqueo de `anon`; app ahora escribe telemetría en sync manual y sync silenciosa.

- `00:59` · `(sin commit)`  
  Dashboard de soporte en Configuración: estado remoto de sync (estado/etapa/fecha/error), refresco manual y automático tras sincronización, más etiqueta relativa “actualizado hace X min”.

- `00:59` · `(sin commit)`  
  Endurecimiento de funciones `SECURITY DEFINER`: revocación de `PUBLIC`/`anon` y recorte de `authenticated` a funciones mínimas requeridas por RPC y helpers de RLS.

- `00:59` · `(sin commit)`  
  Se agrega `supabase/scripts/weekly_health_check.sql` para chequeo semanal de integridad, duplicados operativos, drift de estructura y estado de sync por óptica.

- `01:05` · `(sin commit)`  
  Limpieza adicional de advisors en telemetría: índice faltante para FK `last_actor` y ajuste de policies RLS para usar `(select auth.uid())`, reduciendo reevaluaciones por fila.

- `01:12` · `(sin commit)`  
  Reducción de superficie `SECURITY DEFINER`: helpers RLS movidos a esquema privado `app_private` (no expuesto por RPC), policies/triggers actualizados para usarlo y `assert_backup_operation_allowed` cambiado a `SECURITY INVOKER`.

- `01:15` · `(sin commit)`  
  Ajuste de performance en RLS de `usuario_optica` y `opticas`: reemplazo de `auth.uid()` por `(select auth.uid())` en policies para eliminar reevaluación por fila (`auth_rls_initplan`).

## 2026-04-25

- `22:52` · `(sin commit)`  
  Refactor integral de `Configuración`: extracción en secciones composables (seguridad, plan, integridad clínica, usuarios/roles, sucursales y datos fiscales) para reducir complejidad, mejorar mantenibilidad y minimizar riesgo de regresiones en cambios futuros.

- `22:52` · `(sin commit)`  
  Endurecimiento de UX/arquitectura en Configuración: migración de preferencias de recordatorios a DataStore + ViewModel, draft fiscal en ViewModel para evitar sobrescritura mientras se edita, y externalización completa de textos a `strings.xml` (base lista para internacionalización).

- `21:46` · `(sin commit)`  
  Datos fiscales endurecidos: `razón comercial` y `razón social` pasan a obligatorios (junto a RUC/RUS y dirección), con persistencia completa en store/VM/repositorio y actualización de `opticas.nombre` en Supabase.

- `21:46` · `(sin commit)`  
  Header operativo de óptica activa prioriza identificación rápida de tienda con razón comercial + etiqueta fiscal (RUC/RUS) para reducir confusión entre sucursales.

- `21:33` · `(sin commit)`  
  Endurece sync de finanzas ante red inestable: subida en lotes (`dispensaciones`, `servicios_extra`, `pagos`) + reintentos automáticos con backoff para timeouts/conexión transitoria.

- `21:32` · `(sin commit)`  
  Sanitiza errores de sincronización de red para UX: reemplaza mensajes técnicos extensos de timeout por texto corto y accionable para el usuario.

- `21:26` · `(sin commit)`  
  Corrige feedback silencioso en “Datos fiscales (esta óptica)”: el estado de guardado ahora es reactivo, con manejo robusto de excepciones y confirmación visible por diálogo (éxito/error) en Configuración.

- `21:24` · `(sin commit)`  
  Endurece persistencia fiscal en Supabase: tras `update` se verifica lectura de vuelta para detectar `0 rows`/RLS y evitar falsos “guardado correcto”.

- `21:18` · `(sin commit)`  
  Ajusta insets del dashboard (`statusBarsPadding`) para evitar superposición del bloque “Óptica activa” con la barra de estado del móvil.

- `21:12` · `(sin commit)`  
  Reorganiza menú lateral principal con scroll completo en drawer para garantizar acceso a opciones al final (incluye “Cerrar sesión”) en pantallas pequeñas.

- `20:57` · `(sin commit)`  
  Reubica y visibiliza la acción de “Cerrar sesión” en el dashboard para facilitar cambio de cuenta y recuperación operativa.

- `19:41` · `(sin commit)`  
  Agrega validaciones de contexto previo a sincronización (sesión Supabase activa, óptica válida y membresía vigente) para cortar cascadas de errores RLS/FK tras login OAuth.

- `01:56` · `(sin commit)`  
  La franja de “Óptica activa” en el header principal ahora permite cambiar de sucursal en un toque (abre selector cuando hay múltiples membresías).

- `01:52` · `(sin commit)`  
  Muestra en header principal la óptica activa con contexto fiscal (`Óptica activa: nombre · RUC/RUS + número`) para reducir errores operativos entre sucursales.

- `01:48` · `(sin commit)`  
  Agrega campos opcionales de perfil de óptica (distrito/ciudad/departamento, moneda, país y WhatsApp/teléfono) en Configuración, con persistencia en Supabase y restricción de edición a admin/gerente.

- `01:41` · `(sin commit)`  
  Onboarding y Configuración ahora capturan/gestionan perfil fiscal de la óptica (`RUC/RUS`, razón social, dirección) con restricción de edición para admin/gerente y guardrails en Supabase.

- `01:28` · `(sin commit)`  
  Agrega en Configuracion el flujo para crear sucursales (admin/gerente), con validacion de nombre y mensajes de limite/permisos desde backend.

- `01:12` · `f7fce80`  
  Restringe respaldo total a `admin`, agrega validaciones de restore por optica origen/destino y RPC server-side para autorizacion de backup/restore.

- `01:02` · `41de343`  
  Protege eliminacion de pacientes con guardrails: UI de borrado, control por rol, limite diario y trigger/policy/auditoria en Supabase.

- `00:54` · `b6cf1cf`  
  Robustez de cierre OAuth y tolerancia de sync clinica frente a conflictos de HO/FK para no bloquear lotes completos.

## 2026-04-24

- `23:39` · `965ca3c`  
  Habilita login OAuth con Google en Android (deeplink, callback, flujo post-login integrado con membresias).

- `23:34` · `0c28054`  
  Normaliza migracion legacy `20260414` para alinear historial de migraciones local/remoto.

- `01:19` · `95454ca`  
  Ajustes de UX clinica y endurecimiento de migraciones de rendimiento.

## 2026-04-23

- `01:43` · `f1e7eee`  
  Correcciones de sync de servicios y hardening de seguridad RLS.

- `01:19` · `0c222e9`  
  Blindaje de `dev_owner` privado y fortalecimiento de recordatorios.

- `00:18` · `ba0cc59`  
  Mejora UX de limites y agrega panel interno de planes.

- `00:15` · `229ba22`  
  Guard de limite de opticas por plan.

- `00:12` · `c38a7e5`  
  Saneamiento de base de planes con soporte Pro Multi-sede 15.

- `23:59` · `b9c44d8` (2026-04-22)  
  Onboarding de optica y plan free de 20 pacientes.

## Historico previo importante (backfill)

## 2026-04-29

- `21:34` · (working tree)  
  Integracion final web: nueva ruta `estadisticas`, migracion SQL para `cierres_caja` con RLS, y configuracion operativa por `optica_settings` compartida (con fallback temporal a metadata).

## 2026-04-22

- `23:45` · `483e2f9`  
  Gestion segura de usuarios y roles por optica (base de gobierno de acceso).

- `23:29` · `2bc1240`  
  Restriccion de modulo "Operacion de Hoy" por rol.

- `23:28` · `8f1ae6a`  
  Refuerzo de seguridad local, operacion diaria y reglas de gobernanza de datos.

- `23:01` · `e74d7f0`  
  Fortalecimiento de finanzas/pacientes con HO, BI y ticket de laboratorio.

## 2026-04-20 a 2026-04-14

- `1dda096` (2026-04-20)  
  Sync post-guardado, ticket laboratorio, formula UI y ajustes de respaldo.

- `bd42664` (2026-04-18)  
  Hito SaaS: sync + suscripcion + agenda + recordatorios + pulido UX.

- `f226ebe` (2026-04-15)  
  Sesion/sync por optica, login y pacientes en flujo SaaS.

- `4bd501d` (2026-04-14)  
  Migracion de fechas clinicas a `LocalDate` para eliminar desfases por zona horaria.

- `cb8bf5d` (2026-04-13)  
  Base multi-optica con RLS en Supabase + Room por `optica_id`.

## 2026-04-10 a 2026-03-27 (fundacion y estabilizacion)

- `08685cd` (2026-04-10)  
  Migracion completa a modelo SaaS con Supabase, sync automatica y seguridad cifrada.

- `53a131c` y `47908f1` (2026-04-09)  
  Retrocompatibilidad de respaldos + sesion 24h + PIN lock dinamico.

- `2c28bba` (2026-04-06)  
  Refactor de examen visual, OSDI y migracion segura de base de datos.

- `69c01ea` a `b4658e1` (2026-04-03)  
  Normalizacion UI (incluye modo oscuro), BI y mejoras mayores de reporting.

- `12c544c` y `88aa5ea` (2026-03-28 a 2026-03-29)  
  Integridad referencial con `@Upsert` y mejoras de dispensacion/UI clinica.

- `9d50002` (2026-03-08)  
  Primer commit de la aplicacion.

## Uso recomendado

- Para auditoria tecnica completa usa GitHub (`Commits`, `PRs`, `Blame`, `Diff`).
- Usa este archivo como bitacora operativa de cambios de alto impacto en negocio/seguridad.
- Al cerrar una sesion importante, agregar 1 entrada con:
  - fecha/hora local
  - hash corto
  - efecto en negocio o riesgo mitigado
