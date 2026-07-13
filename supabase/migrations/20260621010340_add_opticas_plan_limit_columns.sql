ALTER TABLE public.opticas
  ADD COLUMN IF NOT EXISTS max_pacientes_por_optica integer DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS max_usuarios_por_optica integer DEFAULT NULL;;
