# Tasks

## 1. RED
- [x] 1.1 SQL `test_parent_balance_single_writer.sql`: parent cache 100 + Abono 100 ⇒ 100 (disp + serv)
- [x] 1.2 `cierreVentaPagado` unit tests (ledger wins / cache fallback)
- [x] 1.3 `CierreCajaViewModelTest`: doubled `montoPagado` + Abono 100 ⇒ saldoPendiente 70

## 2. GREEN
- [x] 2.1 Migration: trigger SET from SUM(pago_effect) for both parents
- [x] 2.2 Helper + ViewModel maps + Cierre cards (disp + serv)
- [x] 2.3 `saldoPendiente` uses helper

## 3. VERIFY
- [x] 3.1 Unit tests green (`CierreCajaVentaDisplayTest` + `CierreCajaViewModelTest`)
- [x] 3.2 Remote apply after GGA equivalent (user: hazlo todo)
