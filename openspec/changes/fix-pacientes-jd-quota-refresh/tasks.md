# Tasks: fix-pacientes-jd-quota-refresh

## 1. Android (TDD)
- [x] 1.1 RED/GREEN: remote IOException still increments daily delete counter
- [x] 1.2 RED/GREEN: 10th local delete blocks further deletes
- [x] 1.3 RED/GREEN: refresh clears isLoading after timeout on empty flow
- [x] 1.4 Apply PacienteViewModel fixes (quota after local delete; refresh timeout)

## 2. Supabase
- [x] 2.1 Author forward migration restoring RPC + membership guard
- [x] 2.2 GGA-eq R1+R3 CLEAN on migration/Android delta
- [x] 2.3 Apply migration to production project `sflhtihqdhrlryeyrzdo` (MCP apply_migration success)
- [x] 2.4 Verify function attributes via SQL (security_definer + membership body)

## 3. Verify
- [x] 3.1 `./gradlew :optoapp:testDebugUnitTest --tests "*PacienteViewModel*"` (32/32)
- [x] 3.2 JD scoped re-judgment Round 1: confirmed severes closed (APPROVED)
- [x] 3.3 Fresh JD on fix delta — both judges findings=[] → APPROVED
