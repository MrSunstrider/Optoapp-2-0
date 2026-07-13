-- Tabla para que la app Android consulte la última versión disponible
-- El CI (build-apk.yml) llama a la Edge Function track-release que inserta aquí
-- La app consulta mediante Supabase client autenticado

create table if not exists app_releases (
  id bigint generated always as identity primary key,
  version text not null,
  apk_download_url text not null,
  release_notes text not null default '',
  created_at timestamptz not null default now(),
  unique (version)
);
create index if not exists idx_app_releases_version
  on app_releases (version desc);
-- RLS
alter table app_releases enable row level security;
-- Cualquier usuario autenticado puede leer versiones
create policy "Authenticated users can read releases"
  on app_releases for select
  to authenticated
  using (true);
-- Solo service_role (desde Edge Function) puede insertar/actualizar
create policy "Service role can insert releases"
  on app_releases for insert
  to service_role
  with check (true);
create policy "Service role can update releases"
  on app_releases for update
  to service_role
  using (true);
