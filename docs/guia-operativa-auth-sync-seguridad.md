# Guia operativa: Auth, Sync y Seguridad

Documento vivo para registrar cambios clave de autenticacion, sincronizacion y protecciones operativas del proyecto.

## Objetivo

- Dejar trazabilidad tecnica y operativa de decisiones de seguridad.
- Estandarizar configuracion de Google OAuth + Supabase.
- Definir controles para evitar fuga/borrado masivo de datos.

## Resumen de cambios implementados

### 1) Login con Google (Supabase Auth + Android)

- Se habilito login OAuth con Google en la app Android.
- Se configuro callback por deeplink (`scheme://host`) para retorno seguro a la app.
- Se integro manejo de deeplink en `MainActivity` y cierre de flujo en `AuthViewModel`.
- Se unifico el flujo post-login para Email y Google (membresias, seleccion de optica, onboarding).

Archivos clave:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/optoapp/MainActivity.kt`
- `app/src/main/java/com/example/optoapp/di/SupabaseModule.kt`
- `app/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt`
- `app/src/main/java/com/example/optoapp/ui/screens/LoginScreen.kt`
- `app/build.gradle.kts`

### 2) Robustez de sincronizacion clinica

- Se evito que conflictos de `historia_optometrica` tumben batch completo de pacientes.
- Se remapea/filtra `paciente_id` en evaluaciones para evitar errores FK en lote.
- Resultado: sync resiliente (se omiten conflictos puntuales y se recupera la mayor parte de datos).

Archivos clave:

- `app/src/main/java/com/example/optoapp/domain/SyncPacientesUseCase.kt`
- `app/src/main/java/com/example/optoapp/domain/SyncHistorialUseCase.kt`

### 3) Eliminacion de pacientes con guardrails

- Se agrego eliminacion desde UI (`DetallePacienteScreen`) con confirmacion explicita.
- Restriccion por rol en app: solo `admin` o `gerente`.
- Limite diario en app: 10 eliminaciones por usuario/optica.
- Guardrail server-side en Supabase:
  - trigger de control antes de borrar paciente
  - auditoria de borrados
  - limite diario real en backend
  - policy de delete restringida por rol

Archivos/migraciones clave:

- `app/src/main/java/com/example/optoapp/ui/screens/DetallePacienteScreen.kt`
- `app/src/main/java/com/example/optoapp/viewmodel/PacienteViewModel.kt`
- `app/src/main/java/com/example/optoapp/data/SessionManager.kt`
- `supabase/migrations/20260425013000_pacientes_delete_guardrails.sql`

### 4) Backup/Restore endurecido (anti-exfiltracion)

- Fase 1 (app):
  - solo `admin` puede descargar/restaurar respaldo total.
- Fase 2 (server-side):
  - se agrega metadata `source_optica_id` al backup.
  - restore solo permitido si `source_optica_id == optica activa`.
  - validacion RPC en Supabase para export/restore.

Archivos/migraciones clave:

- `app/src/main/java/com/example/optoapp/ui/screens/ConfiguracionScreen.kt`
- `app/src/main/java/com/example/optoapp/viewmodel/AuthViewModel.kt`
- `app/src/main/java/com/example/optoapp/data/OptoRepository.kt`
- `supabase/migrations/20260425014500_backup_restore_admin_guardrails.sql`

## Configuracion de Google OAuth (checklist rapido)

1. Google Cloud Console:
   - OAuth Consent Screen en modo `Usuarios externos` (test users configurados).
   - Credencial OAuth tipo `Web application`.
   - Redirect autorizado:
     - `https://<proyecto>.supabase.co/auth/v1/callback`
2. Supabase Auth > Provider Google:
   - `Client ID` y `Client Secret` de Google Cloud.
   - `Skip nonce checks` = OFF.
   - `Allow users without an email` = OFF.
3. Supabase Auth > URL Configuration:
   - Redirect URL movil: `optoapp://auth`
4. `local.properties`:
   - `supabase.redirect.scheme=optoapp`
   - `supabase.redirect.host=auth`

## Auditoria operativa recomendada

### A) Borrados de pacientes

```sql
select
  a.deleted_at,
  a.optica_id,
  a.paciente_id,
  coalesce(up.email, '(sin email)') as deleted_by_email
from public.pacientes_delete_audit a
left join public.user_profiles up on up.user_id = a.deleted_by
order by a.deleted_at desc
limit 50;
```

### B) Conteo diario de borrados por usuario/optica

```sql
select
  date_trunc('day', deleted_at) as dia_utc,
  optica_id,
  deleted_by,
  count(*) as total
from public.pacientes_delete_audit
group by 1,2,3
order by dia_utc desc, total desc;
```

## Politica operativa sugerida

- Backup/restore total: solo `admin`.
- Asignacion de `admin`: solo `admin` actual.
- Borrado de pacientes: `admin/gerente` con limite diario y auditoria.
- Cualquier incidente de sync: priorizar recuperacion de datos, no detener lotes completos por conflictos puntuales.

## Notas de mantenimiento

- Si se cambia el esquema de backup, actualizar:
  - `BackupData`
  - `BackupImportValidator`
  - `restoreBackup(...)` en `AuthViewModel` / `OptoRepository`
- Cualquier nueva operacion sensible debe tener doble defensa:
  - validacion en app (UX/permisos)
  - validacion en backend (RLS/trigger/RPC)
