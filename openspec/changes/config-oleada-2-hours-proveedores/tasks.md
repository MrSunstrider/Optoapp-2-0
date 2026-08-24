# Tasks: config-oleada-2-hours-proveedores

**Issue**: Closes #115 · **Branch**: `feat/config-oleada-2-hours-proveedores` · **RDD**: disabled/unmanaged  
**Gates**: GGA-eq before push; focused then `./gradlew :optoapp:testDebugUnitTest`

## WUs

| Unit | Goal |
|------|------|
| WU1 | business_hours helper + DataSource upsert + Membership sync + VM + Config UI |
| WU2 | Proveedor.tipo Room 47 + Supabase + sync DTO + form |
| WU3 | OpenSpec artifacts |

- [x] 1.1 `BusinessHoursConfigJson` + unit tests
- [x] 1.2 `upsertOpticaSettingsRemote` + `syncOpticaSettingsFromRemote`
- [x] 1.3 `BusinessHoursConfigViewModel` + Config section + LaunchedEffect sync
- [x] 2.1 Entity + MIGRATION_46_47 + OptoDatabase wire
- [x] 2.2 Supabase migration + sync mapper + dropdown
- [x] 2.3 Migration/entity default tests
- [x] 3.1 OpenSpec exploration/proposal/design/tasks + specs
- [ ] V.1 Focused tests; GGA before push; Closes #115
