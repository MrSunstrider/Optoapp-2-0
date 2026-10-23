-- Legacy UUID rows kept periodo='' after 20261023090000; clients never write blank periodo
-- (stableId always includes periodo; upload/download skip blank). Safe to drop orphans.
DELETE FROM public.feedback_recomendaciones
WHERE periodo = '';
