# Delta for sync

## ADDED Requirements

### Requirement: SyncFinanzasUseCase static Resource.Error messages

`SyncFinanzasUseCase` MUST return user-facing `Resource.Error` messages as static, user-friendly strings. `e.localizedMessage`, `e.message`, JSON bodies, and other raw exception text MUST NOT appear in the `Resource.Error` message. Full exception detail MUST be logged via `Log.e` (or the existing sync logger) for diagnostics. Partial-upload static messaging already in place MUST remain static.

#### Scenario: IOException yields static error

- GIVEN an `IOException` during finanzas sync upload or download
- WHEN `SyncFinanzasUseCase` catches the failure
- THEN the returned `Resource.Error` message MUST be static text
- AND the message MUST NOT contain the exception's `localizedMessage` or raw `message`
- AND diagnostics MUST still capture the original exception

#### Scenario: Generic Exception yields static error

- GIVEN a non-IO `Exception` during finanzas sync
- WHEN `SyncFinanzasUseCase` catches the failure
- THEN the returned `Resource.Error` message MUST be static text
- AND the message MUST NOT embed raw exception text
