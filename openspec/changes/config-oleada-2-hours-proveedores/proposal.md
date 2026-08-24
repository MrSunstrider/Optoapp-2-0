# Proposal: Config Oleada 2 — business_hours + proveedores tipados

**Issue**: [Closes #115](https://github.com/MrSunstrider/Optoapp-2-0/issues/115) (`status:approved`)  
**Change**: `config-oleada-2-hours-proveedores` · **RDD**: `rdd_mode=disabled/unmanaged`  
**Branch**: `feat/config-oleada-2-hours-proveedores` · **Delivery**: feature branch · **Schema**: Room 47 + Supabase proveedores.tipo

## Intent

Let admin/gerente edit atención hours in Config (persisted in `optica_settings.config_json`) and classify proveedores as monturas / laboratorio / tecnico so Lab legado can be replaced over time.

## Scope IN

- Upsert remote `optica_settings`; sync fetch→Room; Config UI horario + Guardar
- Pure JSON merge helper + unit tests
- `Proveedor.tipo` Room+Supabase+sync DTO + form dropdown

## Scope OUT

- Config financiera UI (#112)
- PagoEffect / SyncFinanzas order
- Play Billing / multi-PRO (Oleada 3)

## Invariants

PIN / roles / backup / PagoEffect / SyncFinanzas order unchanged.

## Success

- Hours round-trip Room↔remote; proveedores default `monturas`; suite green; Closes #115
