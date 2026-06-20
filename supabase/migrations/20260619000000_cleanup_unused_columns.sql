-- Cleanup: remove columns no longer used by Android app after quality baseline
-- These were removed from the Android Config screen but still exist in the DB.
-- Migration: 2026-06-19

-- 1. Fiscal settings fields removed from Config (moneda, pais, distrito, contacto)
ALTER TABLE opticas DROP COLUMN IF EXISTS moneda;
ALTER TABLE opticas DROP COLUMN IF EXISTS pais;
ALTER TABLE opticas DROP COLUMN IF EXISTS distrito_ciudad_departamento;
ALTER TABLE opticas DROP COLUMN IF EXISTS contacto_whatsapp_telefono;

-- 2. Plan management columns no longer edited from Android
-- Note: plan_code and plan columns are kept (read by SubscriptionManager from server)
-- Only remove editable fields that were in PlanManagementSection
ALTER TABLE opticas DROP COLUMN IF EXISTS max_opticas;
ALTER TABLE opticas DROP COLUMN IF EXISTS max_pacientes_por_optica;
ALTER TABLE opticas DROP COLUMN IF EXISTS max_usuarios_por_optica;
