-- Enable Realtime on usuario_optica so Android clients receive membership
-- INSERT events. Required for the pending-membership waiting screen to
-- detect when an admin adds a new employee without manual refresh.
alter publication supabase_realtime add table public.usuario_optica;
