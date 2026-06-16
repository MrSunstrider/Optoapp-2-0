# OptoApp SaaS — Project Standards

## Layout

```
├── optoapp/          # Android app (Kotlin/Compose/Hilt)
├── supabase/         # PostgreSQL migrations, edge functions (Deno 2), RLS
├── openspec/         # SDD artifacts (change proposals, specs, designs, tasks)
├── docs/             # Operational guides
└── gradle/           # Gradle wrapper + version catalog (libs.versions.toml)
```

- Android module: `:optoapp`, root project name: `OptoApp`
- Supabase: 54+ migrations, RLS by `optica_id`, local dev via `supabase start`

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, Hilt (DI), KSP, Room, Coroutines+Flow, Clean Architecture (data/domain/presentation), JaCoCo
- **Supabase**: PostgreSQL 17, RLS, Edge Functions (Deno 2), Auth (email/password + Google OAuth)
- **Architecture**: offline-first (Room local → Supabase sync), multi-tenant by `optica_id`, ViewModel+StateFlow

## Commands

| Command | Purpose |
|---------|---------|
| `./gradlew :optoapp:testDebugUnitTest --stacktrace` | Unit tests (Robolectric) |
| `./gradlew :optoapp:jacocoTestReport` | JaCoCo coverage report (5% min threshold) |
| `./gradlew :optoapp:assembleDebug` | Debug APK |
| `./gradlew :optoapp:assembleRelease` | Release APK (signed) |

### CI (`.github/workflows/`)

- **`android-ci.yml`**: pushes/PRs to `main` touching `optoapp/` → `testDebugUnitTest` → `assembleDebug`
- **`supabase-ci.yml`**: pushes/PRs to `main` touching `supabase/migrations/` → `supabase db lint` → `supabase db diff --linked`
- **`build-apk.yml`**: pushes to `main` touching `optoapp/` → debug APK → GitHub Release with tag `v{versionName}` → posts to `track-release` edge function

## Environment

`local.properties` at root (gitignored) — must contain:
```properties
supabase.url=https://your-project.supabase.co
supabase.anon.key=your-anon-key
```

- `sdk.dir` is auto-added by Android Studio
- `gradle.properties` has machine-specific settings — do NOT add `org.gradle.java.home` there (use `JAVA_HOME`)
- JDK 17, Gradle 8.x, Android SDK 36 (compile), min SDK 24

## Testing

- **Robolectric** for unit tests (Room in-memory DB), `@RunWith(RobolectricTestRunner::class)`
- Room DAOs tested with `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`
- Tests in `optoapp/src/test/java/com/example/optoapp/` (mirrors main source)
- JaCoCo minimum: 5% instruction coverage

## Architecture Notes

- **`OptoRepository`** is a god repository (~578 lines) handling all entities. Entity-specific repos also exist (`PacienteRepository`, `DispensacionRepository`, `MembershipRepository`).
- **Supabase client**: `supabase-kt` via Ktor CIO.
- **Sync architecture**: offline-first with Room → Supabase. Sync order: pacientes → evaluaciones → dispensaciones → servicios extra → pagos. Uses command pattern, snapshot coordinator, upload/download coordinators.
- **Auth**: email/password + Google OAuth. PIN (6-digit, encrypted via `EncryptedSharedPreferences`) as 2nd factor.
- **Supabase auth config quirk**: `site_url = "optoapp://auth"` (Android deep link scheme); `enable_confirmations = false`.

## SDD Workflow

- This project uses **Spec-Driven Development** (SDD) via Gentle AI / OpenCode
- Artifacts in `openspec/` (config at `openspec/config.yaml`)
- Strict TDD mode is enabled — write failing tests before implementation
- Phases: `explore → propose → spec → design → tasks → apply → verify → archive`

## Gotchas & Conventions

- **No AI attribution** in commits (no Co-Authored-By). Conventional commits only.
- **No comments explaining WHAT** — only WHY. Code should be self-documenting.
- **Kotlin**: `camelCase` / `PascalCase`. **PostgreSQL**: `snake_case`. **Android resources**: `snake_case`.
- **Room column names are camelCase** — they mirror Kotlin field names. Using snake_case in a query silently matches nothing.
- **Default PIN `123456`** — hardcoded in Android `SecurityManager`, forced on new installs until user changes it.
- **Configuration cache enabled** in Gradle — clean with `--no-configuration-cache` if stale.
- **`IMPROVEMENT-PLAN.md`** catalogs known issues (C1-C4 critical, H1-H11 high) — check before starting new work.
- **Web companion** lives in a separate GitHub repo (`optoapp-web`) — shares the same Supabase DB.
