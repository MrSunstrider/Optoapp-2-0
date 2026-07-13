-- La app Android consulta app_releases para mostrar diálogo de actualización.
-- El check corre antes de que el usuario esté autenticado (en LaunchedEffect de MainActivity).
-- Cambiamos de "authenticated only" a "public read" — la tabla solo tiene versiones y URLs de APK,
-- no hay datos sensibles que proteger.

drop policy if exists "Authenticated users can read releases" on app_releases;
create policy "Anyone can read releases"
  on app_releases for select
  to anon, authenticated
  using (true);
