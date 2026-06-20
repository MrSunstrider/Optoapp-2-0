# Supabase y este repositorio

El **Supabase CLI** está instalado de forma local en `tools/supabase-cli/supabase.exe` (no se sube a Git). La carpeta `supabase/` incluye `config.toml` generado por `supabase init`.

## Enlace (`link`) con tu proyecto en la nube

Necesitas dos cosas que **solo tú** puedes obtener en el panel de Supabase (no las pegues en el chat):

1. **Personal Access Token (PAT)**  
   [Cuenta → Access Tokens](https://supabase.com/dashboard/account/tokens) → crea un token con nombre p. ej. `optoapp-cli`.

2. **Contraseña de la base de datos** del proyecto  
   [Tu proyecto → Settings → Database](https://supabase.com/dashboard/project/_/settings/database) → *Database password* (la usas al enlazar el CLI).

Tu **project ref** es el subdominio del host: si la URL es `https://xxxxxx.supabase.co`, el ref es `xxxxxx`.

### Pasos en PowerShell (raíz del repo)

```powershell
cd "C:\Users\usuario\Desktop\Programacion\OptoServices-SaaS\Optoapp"

$supabase = ".\tools\supabase-cli\supabase.exe"

# 1) Autenticación (sustituye el token por el tuyo; no lo guardes en archivos versionados)
& $supabase login --token "TU_PERSONAL_ACCESS_TOKEN"

# 2) Enlazar el proyecto remoto (sustituye PROJECT_REF y la contraseña de la BD)
& $supabase link --project-ref "PROJECT_REF" --password "TU_DATABASE_PASSWORD"
```

Si `login` abre el navegador en lugar del token:

```powershell
& $supabase login
& $supabase link --project-ref "PROJECT_REF" --password "TU_DATABASE_PASSWORD"
```

### Comprobar que el enlace funcionó

```powershell
& $supabase projects list
& $supabase db remote --help
```

Tras un `link` correcto, el CLI puede usar comandos como `db pull`, `db diff` o `migration new` según [la documentación oficial](https://supabase.com/docs/guides/cli).

## Volcado del esquema (`db pull`)

En Windows, `supabase db pull` y `supabase db dump --linked` suelen **requerir Docker Desktop** en ejecución (el CLI usa una imagen de Postgres para comparar / volcar). Si ves un error como *docker_engine* o *Docker Desktop is a prerequisite*:

1. Instala [Docker Desktop para Windows](https://docs.docker.com/desktop), reinicia y deja Docker **arrancado**.
2. En la raíz del repo ejecuta de nuevo:

```powershell
.\tools\supabase-cli\supabase.exe db pull remote_schema_inicial --yes
```

Se creará una migración bajo `supabase/migrations/` con el esquema remoto.

### Sin Docker: inventario del esquema (SQL Editor)

1. En Supabase: **SQL → New query**.
2. Abre el archivo del repo `supabase/sql/inventario_esquema_public.sql`, copia todo y pégalo en el editor.
3. Ejecuta (**Run**). Revisa tablas, columnas, FKs y políticas RLS.
4. Opcional: exporta el resultado o guárdalo en un `.md` / `.txt` local en el repo (sin datos sensibles de usuarios).

### Sin Docker: volcado SQL con `pg_dump` (opcional)

Si instalas solo las [herramientas de cliente de PostgreSQL](https://www.postgresql.org/download/windows/), puedes volcar el esquema usando la **URI** que muestra Supabase en **Settings → Database** (modo *URI*, sustituyendo `[YOUR-PASSWORD]`). Ejemplo de comando (ajusta host, usuario y contraseña):

```powershell
pg_dump "postgresql://postgres:TU_PASSWORD@db.TU_REF.supabase.co:5432/postgres" --schema=public --schema-only -f supabase/sql/remote_schema_manual.sql
```

## Archivos sensibles

Tras enlazar, no subas a Git tokens ni contraseñas. El propio CLI guarda credenciales en tu perfil de usuario; en el repo ya se ignoran `supabase/.branches`, `supabase/.temp` y similares (ver `supabase/.gitignore`).

## Facturación / suscripciones (fase posterior)

Cuando el multi-óptica y la sincronización RLS estén estables, el modelo sugerido es: tabla `suscripciones` (o columnas en `opticas`) con `plan`, `vigencia_hasta`, `max_pacientes` opcional; comprobación en app antes de altas masivas y, si aplica, una Edge Function o webhook de Google Play Billing para actualizar el estado. No bloquea el núcleo de sync; conviene versionar migraciones SQL aparte de las de RLS.
