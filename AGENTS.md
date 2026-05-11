# OptoApp SaaS — Project Standards

## Tech Stack
- **Android**: Kotlin, Jetpack Compose, Hilt (DI), KSP, Gradle KTS
- **Web**: Next.js 15, React 19, TypeScript, Tailwind CSS, Supabase SSR, Zod
- **Supabase**: PostgreSQL, migrations in `supabase/migrations/`

## General Rules
- No Co-Authored-By or AI attribution in commits
- Conventional commits only
- No comments in code unless explaining *why*, never *what*

## Android (Kotlin)
- Use Hilt for DI, scoped per feature
- Compose for all UI, no XML layouts
- Kotlin Serialization for data classes
- Clean Architecture: data / domain / presentation layers
- Coroutines + Flow for async
- Repository pattern for data access
- ViewModel + StateFlow for screen state

## Web (Next.js / TypeScript)
- App Router (no pages/)
- Server Components by default, client only when needed
- Zod schemas for all API validation
- Supabase SSR client (`@supabase/ssr`)
- Tailwind for styling, no CSS modules
- Path aliases: `@/` maps to `src/`

## Supabase
- All schema changes via migrations in `supabase/migrations/`
- Use RLS policies, never trust the client
- SQL scripts in `supabase/sql/` for ad-hoc queries

## Code Review
- Type safety: no `any`, no unchecked casts
- Error handling: no catch-and-silence, use Result type or proper error boundaries
- Performance: no unnecessary recompositions in Compose, no `useEffect` without deps
- Security: validate all inputs, never expose secrets, RLS everywhere
