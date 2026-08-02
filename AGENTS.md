# OptoApp SaaS — Project Standards

## Layout

```
├── optoapp/          # Android app (Kotlin/Compose/Hilt)
├── supabase/         # PostgreSQL 17, migrations, edge functions (Deno 2), RLS
├── openspec/         # SDD artifacts
├── docs/             # Operational guides
└── gradle/           # Gradle wrapper + libs.versions.toml
```

- Android module: `:optoapp`, root project name: `OptoApp`
- Production Supabase: `https://sflhtihqdhrlryeyrzdo.supabase.co`
- JDK 17, Gradle 8.x, Android SDK 36 (compile), min SDK 24

## Stack

| Layer | Tech |
|-------|------|
| Android | Kotlin 2.2.10, Jetpack Compose, Hilt (DI/KSP), Room, Coroutines+Flow |
| Supabase | PostgreSQL 17, RLS by `optica_id`, Auth (email+Google), Edge Functions (Deno 2) |
| Client | supabase-kt 3.6.0 (Ktor CIO), PostgREST, Realtime |
| Architecture | Clean Architecture (data/domain/presentation), offline-first, ViewModel+StateFlow |

## Commands

| Command | Purpose |
|---------|---------|
| `./gradlew :optoapp:testDebugUnitTest --stacktrace` | Unit tests |
| `./gradlew :optoapp:jacocoTestReport` | Coverage (5% min) |
| `./gradlew :optoapp:assembleDebug` | Debug APK |
| `./gradlew :optoapp:assembleRelease` | Release APK (signed, R8) |

## Environment

`local.properties` (gitignored, root):
```properties
supabase.url=https://sflhtihqdhrlryeyrzdo.supabase.co
supabase.anon.key=<production-anon-key>
```

Never commit credentials. `sdk.dir` is auto-added by Android Studio. Use `JAVA_HOME`, not `org.gradle.java.home`.

## Testing

- **Pure JUnit + MockK** — no Robolectric for new tests
- `kotlinx-coroutines-test` (`runTest`) for coroutines
- Room DAOs tested with `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`
- Tests mirror main source in `optoapp/src/test/java/com/example/optoapp/`
- JaCoCo minimum: 5%

## Sync Architecture

- **Offline-first**: Room local → Supabase remote
- **Order**: pacientes → historial → finanzas (upload 8 tables, download 10 tables) → proveedores → ordenes_compra → inventory_kpis → inventario → inventario_fisico
- **JWT**: margin check at 300s before sync (`SyncSessionHelper`); auto-refresh + retry on 401 (`NetworkRetryHelper`)
- **resumen_diario**: recalculado via RPC `recalcular_resumen_diario` en cada sync finanzas
- **Conflict resolution**: local vs remote timestamps; conflicted entities skipped on upload

## Auth

- email/password + Google OAuth
- PIN 6-digit (EncryptedSharedPreferences) as 2nd factor
- `site_url = "optoapp://auth"` (deep link); `enable_confirmations = false`
- `enableLifecycleCallbacks = false` in Auth plugin (P0-T4 — prevents session clearing in background)
- DEV_FALLBACK_PIN `999999` only for dev/test; blocked at creation

## CI (`.github/workflows/`)

| Workflow | Trigger | Actions |
|----------|---------|---------|
| `android-ci.yml` | PR/main → `optoapp/` | `testDebugUnitTest` → `assembleDebug` |
| `supabase-ci.yml` | PR/main → `supabase/migrations/` | `db lint` → `db diff --linked` |
| `build-apk.yml` | push main → `optoapp/` | release APK → GitHub Release `v{versionName}` |

## SDD Workflow

- **Spec-Driven Development** via Gentle AI / OpenCode
- Artifacts: Engram (primary) + `openspec/`
- **Strict TDD**: red → green, pure JUnit + MockK
- Phases: `explore → propose → spec → design → tasks → apply → verify → archive`

## Mandatory Gates

| Gate | When | Rule |
|------|------|------|
| **GGA** | Before `git push` or remote DB migration | Run GGA, resolve ALL observations, then proceed. No exceptions. |
| **Full test suite** | Before push | `testDebugUnitTest` must pass |
| **Judgment Day** | Explicit request | Dual blind review on target code |

## Conventions

- **No AI attribution** in commits. Conventional commits only (`feat:`, `fix:`, `chore:`).
- **No WHAT comments** — code is self-documenting. Only WHY.
- **Naming**: Kotlin `camelCase/PascalCase`, PostgreSQL `snake_case`, Android resources `snake_case`.
- **Room columns** are camelCase (mirror Kotlin fields). snake_case in queries silently matches nothing.
- **Gradle**: config cache enabled; clean with `--no-configuration-cache` if stale.
- **Web companion**: separate repo `optoapp-web`, shares same Supabase DB.
- **`IMPROVEMENT-PLAN.md`**: known issues C1-C4 (critical), H1-H11 (high).

## Key Files

| File | Purpose |
|------|---------|
| `domain/SyncSessionHelper.kt` | JWT refresh + 300s margin check |
| `domain/NetworkRetryHelper.kt` | Retry (3x backoff) + 401 JWT intercept |
| `domain/DownloadSyncCoordinator.kt` | Download all finanzas tables + resumen RPC |
| `domain/SyncFinanzasUseCase.kt` | Orchestrates upload → download order |
| `domain/sync/SyncOrchestrator.kt` | Runs all 8 sync modules sequentially |
| `di/SupabaseModule.kt` | Supabase client config (Auth, PostgREST, Realtime) |
