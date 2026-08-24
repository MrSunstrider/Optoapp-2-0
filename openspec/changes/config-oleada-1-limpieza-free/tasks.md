# Tasks: config-oleada-1-limpieza-free

**Issue**: Closes #113 · **Branch**: `feat/config-oleada-1-limpieza-free` · **RDD**: disabled/unmanaged  
**Gates**: GGA-eq before push; `./gradlew :optoapp:testDebugUnitTest`

## WUs

| Unit | Goal |
|------|------|
| WU1 | FREE domain + tests (`SubscriptionManager`, VM tests, AuthDelegate gate, friendly error) |
| WU2 | Migration trigger max 1 |
| WU3 | Config UI cleanup (card, About delete, gate, lab banner, Avanzado) |

- [ ] 1.1 RED/GREEN SubscriptionManager FREE=1 + unlimited pacientes + canAddOptica
- [ ] 1.2 GREEN AuthDelegate createAdditionalOptica gate + friendlyOpticaError 1
- [ ] 2.1 Migration `20260824143000_fix_opticas_limit_guard_free_max_1.sql`
- [ ] 3.1 Config UI + delete About + tests
- [ ] V.1 Full suite + GGA + push PR Closes #113
