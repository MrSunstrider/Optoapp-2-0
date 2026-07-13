-- FIX #13 (retry): Skip optica_members (it's a view). Only fix invitaciones.
UPDATE public.invitaciones SET optica_id = '' WHERE optica_id IS NULL;
ALTER TABLE public.invitaciones ALTER COLUMN optica_id SET NOT NULL;;
