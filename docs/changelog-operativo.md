# Changelog operativo (cronologico)

Registro operativo de cambios relevantes en autenticacion, sincronizacion, seguridad y gobernanza de datos.

> Zona horaria de referencia: UTC-5 (hora local del equipo).

## 2026-04-25

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
