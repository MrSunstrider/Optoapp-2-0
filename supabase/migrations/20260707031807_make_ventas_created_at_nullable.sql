-- created_at has DEFAULT now() in the trigger. Make it nullable so sync upserts don't fail.
ALTER TABLE public.ventas ALTER COLUMN created_at DROP NOT NULL;;
