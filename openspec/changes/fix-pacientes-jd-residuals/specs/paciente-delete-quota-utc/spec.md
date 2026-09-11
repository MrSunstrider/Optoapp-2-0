# Paciente Delete Quota UTC Specification

## Purpose

Align the offline daily patient-delete counter key with server-side UTC calendar-day enforcement (`guard_pacientes_delete`, `paciente_eliminaciones_restantes_hoy`) so local quota windows match server audit boundaries.

## ADDED Requirements

### Requirement: dailyPacienteDeleteKey MUST use UTC calendar day

`SessionManager.dailyPacienteDeleteKey(opticaId)` MUST derive the date component from `LocalDate.now(ZoneOffset.UTC)` formatted as `BASIC_ISO_DATE` (`yyyyMMdd`). The key MUST NOT use the device default timezone. Increment and read paths for `getPacienteDeleteCountToday` / `incrementPacienteDeleteCountToday` MUST use the same UTC-based key.

#### Scenario: Key uses UTC date when device local date differs

- GIVEN device local calendar date is 2026-09-10 (UTC−5 evening)
- AND UTC calendar date is 2026-09-11
- WHEN `dailyPacienteDeleteKey(opticaId)` is computed
- THEN the key contains `20260911`
- AND NOT `20260910`

#### Scenario: Same UTC day — counter accumulates

- GIVEN two delete operations within the same UTC calendar day
- WHEN `incrementPacienteDeleteCountToday` runs twice for the same optica
- THEN both increments apply to the same preference key
- AND `getPacienteDeleteCountToday` reflects the cumulative count

#### Scenario: UTC midnight rolls counter to new key

- GIVEN delete count = 3 for optica A on UTC day D
- WHEN UTC calendar day advances to D+1
- AND `getPacienteDeleteCountToday(A)` is read
- THEN the count is 0 (new key, no prior increments on D+1)
- AND prior D key data is not counted toward D+1 limit

#### Scenario: Offline delete gate remains authoritative locally

- GIVEN no network connectivity
- WHEN an authorized user deletes a patient locally
- THEN quota enforcement uses the UTC-keyed counter
- AND behavior of consume-on-local-delete from `paciente-delete-quota` MUST NOT regress
