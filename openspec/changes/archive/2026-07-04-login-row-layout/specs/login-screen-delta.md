# Delta for Login Screen — login-row-layout

## Purpose

Merge "Recordar Cuenta" checkbox and "¿Olvidaste tu contraseña?" link from two separate rows into a single horizontal `Row`. This is a pure layout change — no behavioral or capability modifications.

## MODIFIED Requirements

### REQ-LAYOUT-01: "¿Olvidaste tu contraseña?" TextButton Alignment

The "¿Olvidaste tu contraseña?" TextButton MUST be placed on the same horizontal `Row` as "Recordar Cuenta". The Row MUST use `fillMaxWidth()` and `Alignment.CenterVertically`. The TextButton SHALL be pushed to the right end of the Row via a `Spacer(modifier = Modifier.weight(1f))` placed before it. The Row MUST NOT be centered — the TextButton anchors to the trailing edge. The text inside the TextButton SHALL use `bodySmall` typography.

(Previously: TextButton was centered in a separate `fillMaxWidth` row with `TextAlign.Center`)

#### Scenario: TextButton is right-aligned in merged Row

- GIVEN the login form Column is rendered
- WHEN the "¿Olvidaste tu contraseña?" TextButton is composed
- THEN it MUST appear on the same horizontal `Row` as "Recordar Cuenta"
- AND it MUST be positioned at the trailing (right) edge of the Row
- AND a `Spacer(weight(1f))` MUST separate it from "Recordar Cuenta" on the left

#### Scenario: Both elements share one Row

- GIVEN the login form is rendered
- WHEN the user observes "Recordar Cuenta" and "¿Olvidaste tu contraseña?"
- THEN both elements MUST render at the same vertical level (same row)
- AND the Row MUST have `fillMaxWidth()` applied

---

### REQ-LAYOUT-02: "Recordar Cuenta" Row Alignment with OutlinedTextField Content

The "Recordar Cuenta" content (CheckBox + Text) MUST be the leading child inside the merged Row. The Row SHALL use `fillMaxWidth()` and `Alignment.CenterVertically`. The Recordar Cuenta content MUST NOT use a `padding(start = iconOffset)` — the merged Row replaces the previous standalone row that had offset alignment. The horizontal start of Recordar Cuenta MAY vary from the OutlinedTextField content since it now shares a row with the right-aligned TextButton. Both text elements SHALL use `bodySmall` typography.

(Previously: Recordar Cuenta was in a standalone Row with `padding(start = iconOffset)` aligning to OutlinedTextField content)

#### Scenario: Recordar Cuenta is left-aligned in merged Row

- GIVEN the login form is rendered with email and password OutlinedTextFields
- WHEN the "Recordar Cuenta" Row is rendered below the password field
- THEN the CheckBox + Text content MUST appear at the leading (left) edge of the merged Row
- AND the Row MUST NOT apply `padding(start = iconOffset)`

#### Scenario: Recordar Cuenta uses bodySmall

- GIVEN the login form is rendered
- WHEN the "Recordar Cuenta" Text is composed
- THEN the font size MUST be `bodySmall`

---

## Unchanged Requirements

The following requirements are NOT modified by this change and remain as-is in the main spec:

- REQ-LAYOUT-03: Typography Tokens Replace Raw Font Size Literals
- REQ-LAYOUT-04: Button Height Consistency
- REQ-LAYOUT-05: Responsive Width Constraint
- REQ-LAYOUT-06: Unused Import Cleanup

## Test Selectors

| Tag | File | Purpose |
|-----|------|---------|
| `LOGIN_REMEMBER_ACCOUNT_CHECK` | `TestTags.kt` | Checkbox for "Recordar Cuenta" — now inside merged Row |
| `LOGIN_OLVIDASTE_BTN` | `TestTags.kt` | "¿Olvidaste tu contraseña?" TextButton — now right-aligned in same Row |

Tests SHALL verify:
1. Both elements render on the same vertical level
2. "Recordar Cuenta" is left-aligned, "¿Olvidaste tu contraseña?" is right-aligned
3. No text clipping or overflow on 320dp–480dp widths

## Non-Functional Notes

- **Visual regression risk**: Low — single-file change, one screen to inspect
- **Test impact**: Existing tests must update position expectations; no new test infrastructure
- **Rollback**: Revert `LoginScreen.kt` to HEAD
