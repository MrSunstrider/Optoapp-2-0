# Changelog operativo (cronologico)

Registro operativo de cambios relevantes en autenticacion, sincronizacion, seguridad y gobernanza de datos.

> Zona horaria de referencia: UTC-5 (hora local del equipo).

## 2026-04-25

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

## Uso recomendado

- Para auditoria tecnica completa usa GitHub (`Commits`, `PRs`, `Blame`, `Diff`).
- Usa este archivo como bitacora operativa de cambios de alto impacto en negocio/seguridad.
- Al cerrar una sesion importante, agregar 1 entrada con:
  - fecha/hora local
  - hash corto
  - efecto en negocio o riesgo mitigado
