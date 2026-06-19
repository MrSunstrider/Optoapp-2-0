# CierreCaja Crash Fix — Specification

## Purpose

This spec defines the required system behaviors for the cash-close (arqueo de caja) vertical slice after fixing six defects that make the feature unusable. All requirements are behavioral contracts — they describe WHAT the system must do, not HOW the fix is implemented.

## Requirements

### REQ-1: LazyColumn Nesting — Crash-Free Rendering

The screen MUST render the payments list without crashing regardless of how many payment entries exist in the dataset.

#### Scenario: Non-empty payments list renders without exception

- GIVEN a user has at least one registered payment for the current day
- WHEN the user navigates to CierreCajaScreen
- THEN the screen displays all payment rows without throwing IllegalStateException
- AND the scrollable content area remains fully functional

#### Scenario: Empty payments list renders without exception

- GIVEN no payments are registered for the current day
- WHEN the user navigates to CierreCajaScreen
- THEN the screen displays an empty state without crashing

**Test type**: instrumented (Compose UI test)

---

### REQ-2: ViewModel Injection — Correct Hilt Integration

The screen's ViewModel MUST be obtained through the Hilt Navigation Compose integration so that the ViewModel lifecycle is correctly tied to the NavBackStackEntry.

#### Scenario: ViewModel provided via correct Hilt integration

- GIVEN the screen is part of a Compose Navigation graph
- WHEN CierreCajaScreen is composed
- THEN the ViewModel instance is scoped to the correct NavBackStackEntry without runtime error

#### Scenario: Wrong import causes runtime failure (negative — regression guard)

- GIVEN the incorrect `hiltViewModel` import source is used (not from `androidx.hilt.navigation.compose`)
- WHEN the screen is composed within a NavHost
- THEN a `CreationExtras` IllegalStateException is thrown at runtime
- AND this scenario MUST NOT occur in production code

**Test type**: integration (navigation graph smoke test)

---

### REQ-3: Hilt DI Initialization — No Main-Thread Blocking

The Hilt module providing `currentUserId` MUST NOT perform blocking I/O on the main thread. User identity resolution MUST be deferred to a background coroutine.

#### Scenario: App cold start completes without ANR

- GIVEN the app is launched for the first time (cold start)
- WHEN Hilt initializes the database module
- THEN no `runBlocking` call executes on the main thread
- AND the app reaches the home screen within the ANR threshold (5 seconds)

#### Scenario: User ID available before arqueo operations require it

- GIVEN the ViewModel begins initialization
- WHEN the async user-ID resolution completes
- THEN all subsequent arqueo operations use the resolved user ID
- AND the UI reflects a loading or pending state until the ID is available

**Test type**: unit (ViewModel init with a fake SessionManager)

---

### REQ-4: Payment Method Totals — Correct Key Matching

The arqueo totals per payment method MUST be non-zero when payments with that method exist. Key lookup MUST use title-case values matching the `Pago.metodoPago` field exactly (`Efectivo`, `Tarjeta`, `Transferencia`, `Movil`).

#### Scenario: Title-case metodoPago values produce correct totals

- GIVEN payments exist with `metodoPago = "Efectivo"` totaling 500.0
- WHEN the arqueo totals are computed
- THEN `totales["Efectivo"]` equals 500.0
- AND no method total is 0.0 when at least one payment with that method exists

#### Scenario: Lowercase key lookup misses title-case data (regression guard)

- GIVEN the totals map is keyed with `"efectivo"` (all-lowercase)
- WHEN payments exist with `metodoPago = "Efectivo"`
- THEN `totales["efectivo"]` is null or 0.0 (lookup miss confirmed)
- AND this scenario MUST NOT occur in production code

**Test type**: unit (ArqueoCajaViewModel totals computation)

---

### REQ-5: Arqueo Observation — Single Active Collector

The arqueo collector for a given date MUST be cancelled before a new one is started. At most one active collector MUST exist at any time for `observeArqueoForDate`.

#### Scenario: Date change cancels previous collector

- GIVEN an active collector observing arqueo for date D1
- WHEN `observeArqueoForDate` is called with a different date D2
- THEN the D1 collector is cancelled before the D2 collector starts
- AND only emissions from the D2 flow are received after the call

#### Scenario: Repeated same-date call does not create duplicate collectors

- GIVEN `observeArqueoForDate` is called twice with the same date D1
- WHEN the second call completes
- THEN only one active collector remains
- AND no duplicate emissions arrive from the flow

**Test type**: unit (ViewModel with TestCoroutineDispatcher and fake Flow)

---

### REQ-6: Arqueo Insert — Idempotent Same-Day Close

Saving an arqueo for a date and opticaId combination that already exists MUST update the existing record. The operation MUST NOT fail silently or propagate an unhandled exception.

#### Scenario: Second save for same date updates the existing record

- GIVEN an arqueo already exists for date D and opticaId X
- WHEN a new arqueo is saved for the same date D and opticaId X
- THEN the existing record is updated with the new values
- AND no exception is thrown or swallowed
- AND the updated values are immediately visible via the arqueo query

#### Scenario: First save for a new date inserts successfully

- GIVEN no arqueo exists for date D and opticaId X
- WHEN an arqueo is saved for date D and opticaId X
- THEN a new record is inserted
- AND the record is observable via the arqueo query with the correct values

**Test type**: integration (Room in-memory database test)
