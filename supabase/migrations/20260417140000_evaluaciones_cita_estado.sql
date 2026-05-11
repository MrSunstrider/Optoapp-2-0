-- Estado de la próxima cita (sync con app / Room citaEstado)
ALTER TABLE evaluaciones
  ADD COLUMN IF NOT EXISTS cita_estado text NOT NULL DEFAULT 'programada';
