-- Blank periodo rows are pre-stableId leftovers that would shadow real seguimiento keys.
DELETE FROM public.feedback_recomendaciones
WHERE periodo = '';
