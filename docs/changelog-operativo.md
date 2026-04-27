# Changelog operativo (cronologico)

Registro operativo de cambios relevantes en autenticacion, sincronizacion, seguridad y gobernanza de datos.

> Zona horaria de referencia: UTC-5 (hora local del equipo).

## 2026-04-27

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
