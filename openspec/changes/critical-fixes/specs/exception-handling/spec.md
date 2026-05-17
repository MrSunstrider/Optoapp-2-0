# Spec: Exception Handling in Domain Layer

## Requirement
All domain-layer and sync-layer code MUST use differentiated catch blocks for `CancellationException`, `IOException`, and generic `Exception`.

## Rules
- `CancellationException` MUST always be rethrown immediately — never caught and swallowed
- `IOException` MUST log with `"Error en red"` prefix and include the full exception with stack trace
- Generic `Exception` MUST log with `"Error inesperado"` prefix and include the full exception with stack trace
- The existing error handling behavior (Resource.Error, markError, rethrow, etc.) MUST be preserved after the refactoring

## Scenarios

### Scenario 1: CancellationException received
**Given** a coroutine is cancelled during a sync operation
**When** the catch block executes
**Then** `CancellationException` MUST be rethrown immediately

### Scenario 2: IOException received
**Given** a network error occurs during a sync operation
**When** the catch block executes
**Then** the error MUST be logged with `"Error en red"` prefix and full stack trace
**And** the existing error handling logic MUST be preserved

### Scenario 3: Other exception received
**Given** an unexpected error occurs during a sync operation
**When** the catch block executes
**Then** the error MUST be logged with `"Error inesperado"` prefix and full stack trace
**And** the existing error handling logic MUST be preserved
