# Proposal: Config Oleada 1 — limpieza + FREE 1 sucursal

**Issue**: [Closes #113](https://github.com/MrSunstrider/Optoapp-2-0/issues/113) (`status:approved`)  
**Change**: `config-oleada-1-limpieza-free` · **RDD**: `rdd_mode=disabled/unmanaged`  
**Branch**: `feat/config-oleada-1-limpieza-free` · **Delivery**: auto-chain WUs ≤400 · **Schema**: migration trigger only

## Intent

Align FREE with **1 sucursal (1 óptica)**; honest Config subscription copy; remove dead About; fail-closed Config route; soft-deprecate Lab; collapse TZ/reminders + sync diag under Avanzado.

## Scope IN

- `SubscriptionManager`: `maxOpticas(FREE)=1`; unlimited pacientes; `canAddOptica` helper
- Gate `createAdditionalOptica` when memberships ≥ 1; friendly error “1 óptica”
- Migration `trg_opticas_limit_guard` → `v_count >= 1`
- Config UI: FREE copy, delete About, NavHost+screen gate, lab banner, Avanzado

## Scope OUT

- business_hours / proveedores tipados / config financiera (#112) — Oleada 2
- Play Billing / multi-PRO UI — Oleada 3
- moneda/país forms

## Invariants

PIN / roles / backup / PagoEffect / SyncFinanzas order unchanged.

## Success

- FREE = 1 óptica app+DB+copy; suite green; GGA before push; Closes #113
