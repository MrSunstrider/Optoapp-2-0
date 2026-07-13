-- Audit columns for monturas and montura_movimientos
--
-- monturas has updated_at (added in capture_schema_drift) but is missing:
--   - updated_by UUID column
--   - BEFORE UPDATE trigger for audit
--
-- montura_movimientos is missing both updated_at and updated_by entirely.
-- Without these, the Android app sending null values causes:
--   PGRST204 (unknown column) or 23502 (NOT NULL violation)

-- Add audit columns to montura_movimientos
alter table public.montura_movimientos
  add column if not exists updated_at timestamptz not null default timezone('utc', now());
alter table public.montura_movimientos
  add column if not exists updated_by uuid;
-- Add updated_by to monturas (updated_at already exists)
alter table public.monturas
  add column if not exists updated_by uuid;
-- Attach the existing audit trigger to monturas
drop trigger if exists trg_monturas_set_updated_audit on public.monturas;
create trigger trg_monturas_set_updated_audit
  before update on public.monturas
  for each row
  execute function public.set_updated_audit_fields();
-- Attach the existing audit trigger to montura_movimientos
drop trigger if exists trg_montura_movimientos_set_updated_audit on public.montura_movimientos;
create trigger trg_montura_movimientos_set_updated_audit
  before update on public.montura_movimientos
  for each row
  execute function public.set_updated_audit_fields();
