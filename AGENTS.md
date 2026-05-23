# OptoApp SaaS — Project Standards

## Monorepo Layout

```
├── optoapp/          # Android app (Kotlin/Compose/Hilt)
├── optoweb/          # Web app (Next.js 15/React 19/TypeScript) — gitignored in main, lives in separate branch
├── supabase/         # PostgreSQL migrations, edge functions (Deno 2), RLS
├── openspec/         # SDD artifacts (change proposals, specs, designs, tasks)
├── docs/             # Operational guides, SDD docs
└── gradle/           # Gradle wrapper + version catalog (libs.versions.toml)
```

- Android module: `:optoapp`, root project name: `OptoApp`
- Web entrypoint: `optoweb/src/app/layout.tsx`, middleware at `optoweb/src/middleware.ts`
- Supabase: 54 migrations, RLS by `optica_id`, local dev via `supabase start`

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, Hilt (DI), KSP, Room, Coroutines+Flow, Clean Architecture (data/domain/presentation), JaCoCo
- **Web**: Next.js 15 App Router, React 19, TypeScript 6, Tailwind CSS 4, Zod 4, Supabase SSR, Vitest 4, ESLint next/core-web-vitals
- **Supabase**: PostgreSQL 17, RLS, Edge Functions (Deno 2), Auth (email/password + Google OAuth)
- **Architecture**: offline-first (Room local → Supabase sync), multi-tenant by `optica_id`, ViewModel+StateFlow (Android), Server Components (Web)

## Commands

### Android (from root)
| Command | What |
|---------|------|
| `./gradlew :optoapp:testDebugUnitTest --stacktrace` | Unit tests (Robolectric) |
| `./gradlew :optoapp:jacocoTestReport` | JaCoCo coverage report (5% min threshold) |
| `./gradlew :optoapp:assembleDebug` | Debug APK |
| `./gradlew :optoapp:assembleRelease` | Release APK (signed) |

### Web (from `optoweb/`)
| Command | What |
|---------|------|
| `npm run dev` | Dev server (no turbo) |
| `npm run dev:turbo` | Dev server with Turbopack |
| `npm run dev:clean` | Clean `.next` then dev |
| `npm run clean` | Remove `.next` only |
| `npm run build` | Production build |
| `npm run test` | Vitest run (all tests) |
| `npm run test:watch` | Vitest watch mode |
| `npm run lint` | ESLint (next/core-web-vitals) |

### Android CI (`.github/workflows/android-ci.yml`)
- Trigger: pushes/PRs to `main` or `version-saas` touching `optoapp/`
- Steps: `testDebugUnitTest` → `assembleDebug`

### Supabase CI (`.github/workflows/supabase-ci.yml`)
- Trigger: pushes/PRs to `main` or `version-saas` touching `supabase/migrations/`
- Steps: `supabase start` → `supabase db lint` → `supabase db diff --linked`

### Release (`.github/workflows/build-apk.yml`)
- Pushes to `main` touching `optoapp/`: builds debug APK, creates GitHub Release with tag `v{versionName}`, posts to Supabase `track-release` edge function

## Environment

### Android
- `local.properties` at root (`local.properties` is gitignored) — must contain:
  ```properties
  supabase.url=https://your-project.supabase.co
  supabase.anon.key=your-anon-key
  ```
- `sdk.dir` is auto-added by Android Studio
- `gradle.properties` has **machine-specific settings** — do NOT add `org.gradle.java.home` there (use `JAVA_HOME`)
- JDK 17, Gradle 8.x, Android SDK 36 (compile), min SDK 24

### Web
- Copy `optoweb/.env.example` → `optoweb/.env.local`:
  - `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` — required
  - `OPTOAPP_WEB_PIN_PEPPER` — 64-char HMAC pepper for PIN hashing (required in production)
  - `OPTOAPP_WEB_PIN_DEV` — `123456` for dev (default PIN until user changes it)
- `@/` path alias maps to `optoweb/src/`

## Testing

### Android
- **Robolectric** for unit tests (Room in-memory DB), `@RunWith(RobolectricTestRunner::class)`
- Room DAOs tested with `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`
- Tests in `optoapp/src/test/java/com/example/optoapp/` (mirrors main source)
- Some files named with suffix `Test.kt`, others `*CharacterizationTest.kt` or `*CatchRefactorTest.kt`
- No ViewModel tests yet — business logic coverage is thin
- JaCoCo minimum: 5% instruction coverage (low bar, was higher but lowered post-refactor)

### Web
- **Vitest** with `globals: true`, `environment: "node"`, `@/` alias via `resolve.alias`
- Test files: `*.test.ts` or `*.test.tsx` co-located in `src/lib/__tests__/`
- Coverage thresholds in `vitest.config.ts`: statements 20%, branches 15%, functions 25%, lines 20%
- Tests exist for: API schemas, roles/permissions, rate-limit, cierre-caja, reportes-financieros, inventario, sanitize, date-utils, dispensacion-types, pacientes-utils, evaluacion-constants
- Missing tests: Server Actions, API routes, Supabase queries, middleware, PIN logic

## Architecture Notes

- **Web `src/lib/` is the real logic layer**, not `src/domain/`. The `domain/` directory is dead code (never imported by any page/action, contains console.log in production, carries SQL injection in dead path). Do NOT use it unless explicitly revived.
- **Android `OptoRepository`** is a god repository (~578 lines) handling all entities. There are entity-specific repos too (`PacienteRepository`, `DispensacionRepository`, `MembershipRepository`).
- **Supabase client setup**: `supabase-kt` on Android (via Ktor CIO), `@supabase/ssr` on Web (server.ts + client.ts + middleware.ts).
- **Sync architecture**: offline-first with Room → Supabase. Sync order: pacientes → evaluaciones → dispensaciones → servicios extra → pagos. Uses command pattern, snapshot coordinator, and upload/download coordinators.
- **Auth**: email/password + Google OAuth on Android. PIN (6-digit, encrypted via `EncryptedSharedPreferences`) as 2nd factor. Web uses Supabase SSR with httpOnly cookie `usuario_optica` for multi-tenant context.
- **Supabase auth config quirk**: `site_url = "optoapp://auth"` (Android deep link scheme), `additional_redirect_urls = ["optoapp://auth"]`. Email confirmation is DISABLED (`enable_confirmations = false`).

## SDD Workflow

- This project uses **Spec-Driven Development** (SDD) via Gentle AI / OpenCode
- Artifacts in `openspec/` (config at `openspec/config.yaml`)
- Strict TDD mode is enabled — test changes before implementation
- Phases: `explore → propose → spec → design → tasks → apply → verify → archive`
- `docs/sdd/` contains constitutional docs, plans, specs, tasks, code review checklist

## Gotchas & Conventions

- **Optoweb is NOT on main branch** — it's in `.gitignore` on main. Web work happens in a separate branch that includes the `optoweb/` directory.
- **No AI attribution** in commits (no Co-Authored-By). Conventional commits only.
- **No comments explaining WHAT** — only WHY. Code should be self-documenting.
- **Kotlin**: `camelCase` / `PascalCase`. **PostgreSQL**: `snake_case`. **Android resources**: `snake_case`.
- **Default PIN `123456`** — hardcoded in Android `SecurityManager`, forced on new installs until user changes it.
- **No pre-commit hooks**, no husky, no lint-staged. CI runs on push to main/version-saas.
- **Configuration cache enabled** in Gradle — clean with `--no-configuration-cache` if stale.
- **`web/src/domain/` is dead code** — don't add to it, don't rely on it. Real logic in `web/src/lib/`.
- **`IMPROVEMENT-PLAN.md`** catalogs known issues (C1-C4 critical, H1-H11 high) — check before starting new work to avoid duplicating effort.
