# Judgment Day immutable target

- identity_sha256: E9C97B1C164E6F3B592702A0432291050B7F9441E66084624C4366AA233A417A
- base: HEAD (121003c0363266d8935651c93a3377c67710e6e0) working tree (uncommitted)
- head: working tree OC receive UI option 3
- scope: OrdenesCompra receive dialog — per-item absolute quantities + fill-pending + confirm → receiveItems
- files:
  - optoapp/src/main/java/com/example/optoapp/ui/screens/ordenescompra/OrdenesCompraScreen.kt
  - optoapp/src/main/java/com/example/optoapp/ui/screens/ordenescompra/ReceiveQuantities.kt
  - optoapp/src/test/java/com/example/optoapp/ui/screens/ordenescompra/ReceiveQuantitiesTest.kt
- frozen_diff: openspec/changes/_jd-oc-recibir-items-ui/immutable-target.diff
- domain_contract_note: ViewModel.receiveItems / Repository.receiveItems take absolute cumulative recibido (not deltas); stock/cost move only when fully received — out of edit scope but judges may reason about UI/domain mismatch
