# OptoApp Web (P4 baseline)

Base inicial de la version web para el ecosistema OptoApp.

## Estado actual

- Estructura Next.js App Router creada manualmente.
- Supabase SSR configurado (`server`, `client`, `middleware`).
- Login base implementado.
- Seleccion de optica activa implementada (`usuario_optica` + cookie httpOnly).
- Dashboard y modulos en placeholder.
- Rutas protegidas con middleware.

## Variables de entorno

Copiar `.env.example` a `.env.local` y completar:

- `NEXT_PUBLIC_SUPABASE_URL`
- `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`

## Arranque local

1. En `web/`, ejecutar:
   - `npm install`
   - `npm run dev`
