# OC Recibir ítems UI

## Objective
Wire the purchase-order receive dialog so staff can register per-item absolute received quantities and optionally fill all pending lines in one tap.

## Problem
`OrdenesCompraScreen` detail dialog shows "Recibir ítems" but only calls `onDismiss()`. Items are loaded and never shown. Domain (`OrdenCompraRepository.receiveItems`) already supports partial → complete with absolute `recibido` values.

## Why
User chose option 3: per-item editing + "Recibir todo lo pendiente". Stock and weighted average still move only when the order is fully received.

## Scope
- `OrdenesCompraScreen.kt` detail/receive UI
- Small pure helper for clamp / fill-pending / map build (unit-tested)
- Close detail and clear error/success UX after a successful submit from the dialog

## Out of scope
- Repository receipt logic
- Judgment Day
- Unrelated dirty files (anulacion, DatabaseTransactionRunner, JD cluster)

## Constraints
- Strict TDD: RED → GREEN for the helper
- Spanish UI copy, tú-neutral
- Do not touch unrelated dirty files
- `receivedQuantities` values are absolute cumulative `recibido` (0..cantidad)

## TDD
- Mode: on (project Strict TDD)
- Runner: `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.ui.screens.ordenescompra.ReceiveQuantitiesTest --stacktrace`

## Tasks
- [x] T1: Add pure `ReceiveQuantities` helper + failing unit tests (clamp, fillPending, toMap)
- [x] T2: Replace detail dialog with item list, editable totals, fill-pending, confirm → `viewModel.receiveItems`, then dismiss
- [x] T3: Run scoped unit tests and record evidence

## Acceptance
- APROBADA/PARCIAL: dialog lists items with pedido / ya recibido / editable total
- "Recibir todo lo pendiente" sets each line to `cantidad`
- Confirm calls `receiveItems` with absolute map and closes dialog
- Helper tests green

## Checks
- Scoped unit tests for helper
- Manual sanity: button no longer only dismisses

## Progress
- Created: 2026-10-15
- T1 RED: `ReceiveQuantitiesTest` failed compile (Unresolved reference) — observed
- T1 GREEN: helper implemented; `ReceiveQuantitiesTest` PASS
- T2: `OrdenCompraDetailDialog` lists items, editable "Recibido total", fill-pending, confirm → `receiveItems` + dismiss
- T3 evidence (2026-10-15):
  - `./gradlew :optoapp:testDebugUnitTest --tests com.example.optoapp.ui.screens.ordenescompra.ReceiveQuantitiesTest --tests com.example.optoapp.viewmodel.OrdenesCompraViewModelTest --stacktrace` → BUILD SUCCESSFUL
- Next: none (implementation complete; commit not requested)

## Route
- T1–T2: delegated writer (screen + helper + tests)

## Verification evidence
- ReceiveQuantitiesTest: PASS (GREEN after helper)
- OrdenesCompraViewModelTest: PASS
- pending: none
