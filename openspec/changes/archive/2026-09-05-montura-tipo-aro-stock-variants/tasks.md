# Tasks — montura-tipo-aro-stock-variants

## 1. Schema (TDD)

- [x] 1.1 RED: Migration51To52Test expects drop old unique + create `(sku, opticaId, tipoAro)`
- [x] 1.2 GREEN: `MIGRATION_51_52`, OptoDatabase v52, entity Index update
- [x] 1.3 GREEN: Supabase migration drop `idx_monturas_sku_optica`, create `idx_monturas_sku_optica_tipo_aro`
- [x] 1.4 DAO helper optional: query by sku+tipoAro for conflict messaging (or rely on UNIQUE)

## 2. Catalog + form + ViewModel (TDD)

- [x] 2.1 RED/GREEN: OpticalCatalog.MATERIALES_MONTURA includes Aluminio + tests
- [x] 2.2 RED: MonturasViewModelTest multi-insert two tipos with stocks
- [x] 2.3 GREEN: FormState selectedTipos + stockPorTipo; save loop; edit path single
- [x] 2.4 GREEN: MonturaEditForm chips + per-tipo stock; material from catalog
- [x] 2.5 GREEN: Dispensación MonturaForm/LenteForm material options from catalog

## 3. Search labels (TDD)

- [x] 3.1 RED/GREEN: monturaLabel includes tipoAro when non-blank
- [x] 3.2 GREEN: MonturaSearchField shows tipoAro; filter by tipoAro

## 4. Verify

- [x] 4.1 `./gradlew :optoapp:testDebugUnitTest --stacktrace`
- [x] 4.2 `./gradlew :optoapp:assembleDebug`
- [x] 4.3 Archive change + Engram summary
