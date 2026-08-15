# Pre-purge snapshot — production phantom `venta` movements

Captured 2026-08-14 before the irreversible DELETE in
`20260815*_inventory_single_writer_purge_and_guard.sql`.

Project `sflhtihqdhrlryeyrzdo`, `optica_id = 25af5a92-4a2d-4e7a-957f-61bec87a07d8`.

## Counts at capture time

| Probe | Value |
|-------|-------|
| `montura_movimientos` where `tipo='venta'` | 24 |
| …matching the purge predicate | **24** |
| …**not** matching (would survive) | **0** |
| `montura_movimientos` where `tipo='SALIDA_VENTA'` | 25 |

Every `venta` row is a phantom; no legitimate `venta` movement exists in this tenant.

## Constant columns across all 24 rows

`tipo='venta'`, `nota='venta_dispensacion'`, `cantidad=1`,
`optica_id='25af5a92-4a2d-4e7a-957f-61bec87a07d8'`,
`updated_by='25af5a92-4a2d-4e7a-957f-61bec87a07d8'`, `user_id=''`,
`costo_unitario=0`, `tipo_documento=''`.

## Varying columns

| id | montura_id | referencia_id | fecha | stock_previo | stock_nuevo | updated_at |
|----|-----------|---------------|-------|--------------|-------------|------------|
| 0848b9fa-9e4c-4382-b450-996bd7fd13b0 | bcb655cd-9cf9-438a-b50e-a66cd3aa46b7 | 923b4e8d-dfda-4f98-b83e-8bffd08087f5 | 2026-07-11 | 2 | 1 | 2026-08-08T07:02:45.461118+00 |
| 14047c7e-7abd-403d-abac-13ecf98f716f | 8dd2dd4a-f1dd-4ea7-aa3f-b880b1c0e6d9 | c460129f-22dd-4a98-bec5-aa4fd61b87ec | 2026-07-15 | 1 | 0 | 2026-08-08T07:02:47.170633+00 |
| 2c7c6ffd-0079-4796-8ac3-e4ef30f4500b | e8e3e9b0-a10e-4d44-a919-b1ce253dff6b | 43f20a41-4898-45ff-9e95-e2f23077172e | 2026-08-06 | 15 | 14 | 2026-08-08T07:02:51.355533+00 |
| 3453c82e-1462-43ee-a10a-3719236d2d85 | 8dd2dd4a-f1dd-4ea7-aa3f-b880b1c0e6d9 | d4227991-74d6-4856-ae6e-ab0124e6f562 | 2026-07-26 | 2 | 1 | 2026-08-08T07:02:46.702109+00 |
| 3b95f1ff-fa2e-435b-96e1-eed5e7a00df6 | 16f5f6f4-71a8-441f-a720-32d2e1ee73ca | 95a02902-fd1b-4730-8ab1-9e17088069cb | 2026-07-23 | 19 | 18 | 2026-08-08T07:02:46.012316+00 |
| 5268e744-976d-417f-b568-8e32fce55bbc | 44260f2c-eecd-4605-8421-be3101ab7b01 | 4d3a5451-e7e7-495d-9731-4f33c222b065 | 2026-08-06 | 2 | 1 | 2026-08-08T07:02:50.626258+00 |
| 5a44aae0-3b5b-407f-b269-92c9f25365d7 | e8e3e9b0-a10e-4d44-a919-b1ce253dff6b | a2ac7294-dc89-41b5-a1f6-5eb1ad1b786e | 2026-07-22 | 16 | 15 | 2026-08-08T07:02:46.939814+00 |
| 5be9cba6-cc77-4a44-b4fb-c9710d5a3a05 | 5fe7082d-f71d-40a5-b860-c607e029e1c9 | 79f5ec15-cc6b-4c75-894e-eb23d25d0188 | 2026-08-02 | 7 | 6 | 2026-08-08T07:02:44.754236+00 |
| 6372759e-12bc-4ce5-8316-480aaab0a5bf | dfc60250-edb3-4824-9636-5bf24b3dd785 | a355d8af-85f9-4692-ba5f-199bb2578f67 | 2026-08-05 | 3 | 2 | 2026-08-08T07:02:50.135546+00 |
| 6cb6567f-2705-488d-9842-a2f47bee1dc4 | f89d43b8-224a-40a3-a4f2-cadb11079187 | 8ff28658-98c6-472e-9fdc-349a57b5e125 | 2026-06-02 | 9 | 8 | 2026-08-08T07:02:49.88091+00 |
| 700df704-5416-4bd6-a51c-8e5b5a24fc20 | d29f93a3-8223-4842-81c7-074f07709ec8 | a9a99066-2025-4439-a854-06494d1912d4 | 2026-07-16 | 4 | 3 | 2026-08-08T07:02:48.000674+00 |
| 859ffaca-06f3-4cd3-98bc-c267bbaf8d28 | 16f5f6f4-71a8-441f-a720-32d2e1ee73ca | 4d61eda9-0fd8-41ad-826e-1e9b851a3d73 | 2026-08-11 | 19 | 18 | 2026-08-11T16:29:40.2765+00 |
| 8eede80f-33d4-4a9b-ab1e-42b118e08d5f | 129e3487-171d-4074-8a70-8b8a07bb9ea1 | 17a81b77-7565-4360-bc31-497da764ce89 | 2026-08-07 | 10 | 9 | 2026-08-11T15:05:05.744331+00 |
| 9207950e-a79a-47d6-915c-50633cab2936 | 44260f2c-eecd-4605-8421-be3101ab7b01 | 6d153bea-0936-4dd6-bca9-6d00a2857ed6 | 2026-07-17 | 4 | 3 | 2026-08-08T07:02:48.227606+00 |
| c7f26b22-425e-42d2-9ecd-6acc9946fa6d | 44260f2c-eecd-4605-8421-be3101ab7b01 | 41483db2-a031-42df-8cc0-a6014b972f46 | 2026-07-08 | 5 | 4 | 2026-08-08T07:02:45.239784+00 |
| ca396d11-bc72-4538-ac76-ae839f77c44d | d645f642-5d5a-429d-9291-4b2f4dd9dbed | 6ae7f732-1e2c-4b54-88f6-6d3cab5279f3 | 2026-07-11 | 1 | 0 | 2026-08-08T07:02:49.319236+00 |
| d96e83a2-722a-404c-9669-4a8f4bf9518d | f89d43b8-224a-40a3-a4f2-cadb11079187 | dccef3a8-65b2-4d45-be4b-0894658e5d25 | 2026-07-25 | 10 | 9 | 2026-08-08T07:02:47.772578+00 |
| f0f4e6fd-efd0-4fc6-8c08-9232c367d7cf | 16f5f6f4-71a8-441f-a720-32d2e1ee73ca | 067aa473-dc50-46fa-8ace-32c6498aae68 | 2026-08-06 | 18 | 17 | 2026-08-08T07:02:50.862382+00 |
| f4a1ae9c-4e83-469c-9bb4-4ce7d087ffab | f9f7518f-ac1b-48af-8339-928a77829149 | 2bb4317a-9e5d-4990-921b-361a55498569 | 2026-07-18 | 2 | 1 | 2026-08-08T07:02:46.248692+00 |
| f792aef6-ed85-48b2-bd8c-2173a27e143a | 8dd2dd4a-f1dd-4ea7-aa3f-b880b1c0e6d9 | 552a84fa-ab5a-499d-9228-86a9e6b86b74 | 2026-08-05 | 2 | 1 | 2026-08-08T07:03:14.907528+00 |
| fae411b6-a978-48ce-ae8a-5bee4912d45c | afb922f8-4381-4db9-a262-b1218ff2130e | a1bed996-7851-4d05-9697-cb7757e63f47 | 2026-07-12 | 4 | 3 | 2026-08-08T07:02:46.471424+00 |
| fc0069f4-eb5a-4bf0-af1d-5f9a92c532d8 | 16f5f6f4-71a8-441f-a720-32d2e1ee73ca | 969ceb93-3035-4630-85b6-f4851091cc9b | 2026-08-06 | 17 | 16 | 2026-08-08T07:02:51.106185+00 |
| fdd8c26b-ff04-429f-bb97-37ca4f001546 | 44260f2c-eecd-4605-8421-be3101ab7b01 | f1a1e51f-d520-4cee-84e1-86c9617acc5f | 2026-07-18 | 3 | 2 | 2026-08-08T07:02:49.066259+00 |
| fed6fe9e-e374-4148-b118-4efe7bcdc185 | 129e3487-171d-4074-8a70-8b8a07bb9ea1 | 62b1e900-ce33-4e2c-a2c9-113e6975d836 | 2026-07-22 | 11 | 10 | 2026-08-08T07:02:45.018166+00 |

## Why the `stock_previo`/`stock_nuevo` values here are not authoritative

These pairs record the transient double decrement the RPC applied before the inventario
monturas upsert overwrote `stock_actual` with the locally computed value. Montura
`16f5f6f4` shows three phantoms claiming 19→18, 18→17 and 17→16 while the current
`stock_actual` is 18, matching the `SALIDA_VENTA` ledger. Restoring these rows would
re-introduce a ledger that contradicts stock — they are kept only for audit, not rollback.
