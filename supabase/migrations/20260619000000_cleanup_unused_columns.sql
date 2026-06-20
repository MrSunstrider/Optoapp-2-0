-- Cleanup: remove columns no longer used by Android app after quality baseline
-- These were removed from the Android Config screen but still exist in the DB.
-- Migration: 2026-06-19
-- Note: CASCADE needed because trigger trg_guard_opticas_business_profile_optional_update
--       references moneda column. That trigger validated fields being removed.

-- 1. Fiscal settings fields removed from Config (moneda, pais, distrito, contacto)
ALTER TABLE opticas DROP COLUMN IF EXISTS moneda CASCADE;
ALTER TABLE opticas DROP COLUMN IF EXISTS pais CASCADE;
ALTER TABLE opticas DROP COLUMN IF EXISTS distrito_ciudad_departamento CASCADE;
ALTER TABLE opticas DROP COLUMN IF EXISTS contacto_whatsapp_telefono CASCADE;

-- 2. Plan management columns no longer edited from Android
-- Note: plan_code and plan columns are kept (read by SubscriptionManager from server)
-- Only remove editable fields that were in PlanManagementSection
ALTER TABLE opticas DROP COLUMN IF EXISTS max_opticas CASCADE;
ALTER TABLE opticas DROP COLUMN IF EXISTS max_pacientes_por_optica CASCADE;
ALTER TABLE opticas DROP COLUMN IF EXISTS max_usuarios_por_optica CASCADE;
