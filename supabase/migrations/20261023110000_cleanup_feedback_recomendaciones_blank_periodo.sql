-- Do NOT delete blank-periodo rows: they are pre-stableId historical feedback.
-- SyncSeguimiento skips periodo='' on upload/download; the unique index excludes them,
-- so they cannot collide with optica|periodo|recomendacionId keys.
COMMENT ON TABLE public.feedback_recomendaciones IS
  'Plan-de-acción seguimiento; id is client stableId; LWW via updated_at; blank periodo = legacy retained.';
