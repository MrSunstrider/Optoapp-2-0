# av-ambliopia-ambidirectional Specification

## Purpose

Define bidirectional AV cc lejos (OD vs OI) → logMAR normalization and amblyopia auto-detection so decimal and Snellen fractional inputs both participate in the clinical threshold from `docs/SRS.md` RF-EVA-06 and §8.2. Covers Android parse/wiring and OptoWeb helper parity. No UI redesign, schema, RLS, or explicit 6m product work.

Authority: `docs/SRS.md` RF-EVA-06 + §8.2. Decimal upper bound is intentionally narrowed from SRS `(0, ∞)` to `(0, 2.0]` per locked product decision (clinical safety against leftover Snellen numerators).

## ADDED Requirements

### Requirement: Bidirectional AV to logMAR parse order

The system MUST expose a unified AV→logMAR conversion that, for a raw input string:

1. Trims surrounding whitespace; blank after trim MUST return null (unparseable).
2. MUST attempt Snellen fractional parse first (existing Snellen rules).
3. If Snellen fails, MUST attempt decimal parse per the decimal rules below.
4. If both fail, MUST return null.

Snellen fractional MUST take priority so strings like `20/40` are never interpreted as decimal.

#### Scenario: Blank input is unparseable

- GIVEN a raw AV string that is empty or whitespace-only after trim
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST be null

#### Scenario: Snellen fractional succeeds before decimal

- GIVEN a raw AV string `20/40`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST equal `-log10(20/40)` (within float tolerance)
- AND the decimal path MUST NOT be used for that input

#### Scenario: Spaced Snellen slash is accepted

- GIVEN a raw AV string `20 / 40`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST equal the same logMAR as `20/40` (within float tolerance)

#### Scenario: Decimal with dot separator succeeds after Snellen fails

- GIVEN a raw AV string `0.5`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN Snellen MUST fail
- AND the decimal path MUST succeed
- AND the result MUST equal `-log10(0.5)` (within float tolerance)

#### Scenario: Decimal with comma separator succeeds

- GIVEN a raw AV string `0,8`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST equal `-log10(0.8)` (within float tolerance)

### Requirement: Decimal AV acceptance and rejection rules

After Snellen fails, the decimal path MUST:

1. Require an explicit decimal separator (`.` or `,`) in the trimmed string.
2. Normalize comma to dot, then parse as a finite number.
3. Accept only values in the open-closed interval `(0, 2.0]` (exclusive of 0, inclusive of 2.0).
4. Return logMAR as `-log10(decimal)` on acceptance.
5. Return null for any other case.

Bare integers without `/` and without a decimal separator MUST be null. Values with a separator but outside `(0, 2.0]` MUST be null. Zero, negative, and non-finite values MUST be null.

#### Scenario: Bare integer without separator is rejected

- GIVEN a raw AV string `20` (no slash, no decimal separator)
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST be null

#### Scenario: Zero decimal is rejected

- GIVEN a raw AV string `0.0`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST be null

#### Scenario: Negative decimal is rejected

- GIVEN a raw AV string `-0.5`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST be null

#### Scenario: Decimal above 2.0 is rejected

- GIVEN a raw AV string `20.0`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST be null

#### Scenario: Comma-separated value above 2.0 without slash is rejected

- GIVEN a raw AV string `20,40` (no slash)
- WHEN the bidirectional AV→logMAR conversion runs
- THEN Snellen MUST fail
- AND the decimal path MUST reject the value because it is greater than 2.0
- AND the result MUST be null

#### Scenario: Decimal at upper inclusive bound is accepted

- GIVEN a raw AV string `2.0`
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST equal `-log10(2.0)` (within float tolerance)

#### Scenario: Typical decimal VA values are accepted

- GIVEN raw AV strings `1.0`, `0.8`, and `0.5`
- WHEN each is converted via the bidirectional AV→logMAR path
- THEN each result MUST be non-null
- AND each MUST equal `-log10` of that decimal value (within float tolerance)

### Requirement: Snellen fractional parse stability

The existing Snellen fractional conversion MUST remain behaviorally stable: accept `(\d+)\s*/\s*(\d+)` with positive non-zero denominator, return `-log10(numerator/denominator)`, and return null for blank, invalid, zero denominator, or missing slash parts. Callers that today use Snellen-only conversion MUST keep that contract unchanged. The bidirectional converter MUST compose Snellen first rather than altering Snellen semantics.

#### Scenario: Valid Snellen remains parseable

- GIVEN a raw AV string `20/20`
- WHEN Snellen fractional conversion runs
- THEN the result MUST equal `0.0` (within float tolerance)

#### Scenario: Missing numerator or denominator is rejected

- GIVEN a raw AV string that matches neither a complete `num/den` form nor a valid decimal (e.g. `/40`, `20/`, or `/`)
- WHEN the bidirectional AV→logMAR conversion runs
- THEN the result MUST be null

#### Scenario: Zero denominator is rejected

- GIVEN a raw AV string `20/0`
- WHEN Snellen fractional conversion runs
- THEN the result MUST be null

#### Scenario: Existing Snellen characterization remains green

- GIVEN the existing Snellen characterization / unit tests for Snellen fractional conversion
- WHEN the bidirectional AV→logMAR feature is introduced
- THEN those Snellen tests MUST remain passing without changing the Snellen contract

### Requirement: Snellen and decimal logMAR equivalence

Snellen fractional and decimal forms that represent the same acuity MUST produce equivalent logMAR values (within float tolerance) so mixed OD/OI pairs compare correctly.

#### Scenario: Twenty over forty equals decimal half

- GIVEN raw AV strings `20/40` and `0.5`
- WHEN each is converted via the bidirectional AV→logMAR path
- THEN both results MUST be non-null
- AND the absolute difference between the two logMAR values MUST be within float tolerance of zero

#### Scenario: Twenty over twenty equals decimal one

- GIVEN raw AV strings `20/20` and `1.0`
- WHEN each is converted via the bidirectional AV→logMAR path
- THEN both results MUST be non-null
- AND the absolute difference between the two logMAR values MUST be within float tolerance of zero

### Requirement: Amblyopia auto integration on Android

Android amblyopia auto for distance corrected acuity MUST parse `avCcOdLejos` and `avCcOiLejos` with the bidirectional AV→logMAR conversion. When both eyes parse successfully and auto amblyopia is enabled, the system MUST set `otrosAmbliopia` to true if and only if `|logMAR_OD − logMAR_OI| ≥ 0.19`; otherwise false. The threshold value `0.19` MUST NOT change. When auto amblyopia is disabled, the system MUST preserve the existing manual `otrosAmbliopia` value.

#### Scenario: Mixed Snellen and decimal eyes evaluate threshold

- GIVEN `avCcOdLejos` is `20/40` and `avCcOiLejos` is `1.0`
- AND auto amblyopia is enabled
- WHEN amblyopia auto computation runs
- THEN both eyes MUST parse to logMAR
- AND `|ΔlogMAR|` MUST be evaluated against `0.19`
- AND `otrosAmbliopia` MUST be true because `|−log10(0.5) − (−log10(1.0))| = 0.3010… ≥ 0.19`

#### Scenario: Decimal-only eyes trigger auto when above threshold

- GIVEN `avCcOdLejos` is `0.5` and `avCcOiLejos` is `1.0`
- AND auto amblyopia is enabled
- WHEN amblyopia auto computation runs
- THEN `otrosAmbliopia` MUST be true

#### Scenario: Difference below threshold clears auto amblyopia

- GIVEN both eyes parse successfully to logMAR values whose absolute difference is strictly less than `0.19`
- AND auto amblyopia is enabled
- WHEN amblyopia auto computation runs
- THEN `otrosAmbliopia` MUST be false

#### Scenario: Threshold boundary at exactly 0.19 is inclusive

- GIVEN both eyes parse successfully to logMAR values whose absolute difference equals `0.19`
- AND auto amblyopia is enabled
- WHEN amblyopia auto computation runs
- THEN `otrosAmbliopia` MUST be true

#### Scenario: Auto amblyopia disabled preserves manual flag

- GIVEN auto amblyopia is disabled
- AND `otrosAmbliopia` is already true (or false)
- WHEN amblyopia auto computation runs
- THEN `otrosAmbliopia` MUST remain unchanged regardless of AV parse results

### Requirement: Unparseable eye preserves manual amblyopia

If either eye fails bidirectional parse (null logMAR), the system MUST NOT force amblyopia auto on or off. Android MUST keep the existing `otrosAmbliopia` boolean. OptoWeb helper that returns an optional auto value MUST return null so the caller retains prior/manual state.

#### Scenario: Android keeps manual when OD is unparseable

- GIVEN `avCcOdLejos` is `20` (bare integer, unparseable)
- AND `avCcOiLejos` is `1.0`
- AND `otrosAmbliopia` is currently true
- AND auto amblyopia is enabled
- WHEN amblyopia auto computation runs
- THEN `otrosAmbliopia` MUST remain true

#### Scenario: Android keeps manual when OI is blank

- GIVEN `avCcOdLejos` is `20/20`
- AND `avCcOiLejos` is blank
- AND `otrosAmbliopia` is currently false
- AND auto amblyopia is enabled
- WHEN amblyopia auto computation runs
- THEN `otrosAmbliopia` MUST remain false

#### Scenario: OptoWeb helper returns null when either eye is unparseable

- GIVEN OD or OI AV input fails bidirectional parse
- WHEN the OptoWeb amblyopia auto helper runs
- THEN the helper MUST return null
- AND MUST NOT return true or false for that invocation

### Requirement: OptoWeb helper parity

> **Status: DEFERRED.** OptoWeb work was reverted and postponed until the OptoApp MVP is complete (see `openspec/changes/archive/2026-09-26-av-ambliopia-ambidirectional/scope-correction-optoweb-deferred.md`). The OptoWeb requirements and scenarios in this spec are not yet implemented.

OptoWeb MUST provide the same bidirectional parse order (Snellen first, then decimal with explicit separator and `(0, 2.0]` acceptance) and MUST fix Snellen fractional conversion to use captured `numerator/denominator` (not a hardcoded numerator of 20). Amblyopia auto on the web MUST route through the bidirectional conversion. UI redesign is out of scope; helper and tests only. Schema and RLS MUST NOT change.

#### Scenario: Web Snellen uses captured numerator and denominator

- GIVEN a raw AV string `6/6`
- WHEN OptoWeb Snellen fractional conversion runs
- THEN the result MUST equal `-log10(6/6)` which is `0.0` (within float tolerance)
- AND MUST NOT use a hardcoded numerator of 20

#### Scenario: Web twenty over twenty remains zero logMAR

- GIVEN a raw AV string `20/20`
- WHEN OptoWeb Snellen fractional conversion runs
- THEN the result MUST equal `0.0` (within float tolerance)

#### Scenario: Web decimal path matches Android acceptance

- GIVEN raw AV strings `0.5` and `0,8`
- WHEN OptoWeb bidirectional AV→logMAR conversion runs for each
- THEN both MUST succeed with `-log10` of the normalized decimal (within float tolerance)

#### Scenario: Web rejects bare integer and over-ceiling decimal

- GIVEN raw AV strings `20` and `20.0`
- WHEN OptoWeb bidirectional AV→logMAR conversion runs for each
- THEN both results MUST be null

#### Scenario: No schema or RLS changes

- GIVEN this change is implemented
- WHEN database migrations, RLS policies, and Room entities are inspected
- THEN none MUST be added or modified for this capability
