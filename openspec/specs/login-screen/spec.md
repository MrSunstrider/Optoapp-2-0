# Login Screen — Layout Alignment Delta Spec

## Purpose

Delta spec for `fix-login-alignment`. Documents 6 layout consistency fixes in `LoginScreen.kt`. This is a **pure layout change** — no behavioral or capability modifications. The login screen's functional behavior (auth flow, navigation, PIN, error handling) is unchanged.

## Scope

- **Files affected**: `LoginScreen.kt`, `TestTags.kt`, `LoginFlowTest.kt`
- **Capability changes**: None
- **Behavioral changes**: None

## Requirements

### REQ-LAYOUT-01: "¿Olvidaste tu contraseña?" TextButton Alignment

The "¿Olvidaste tu contraseña?" TextButton MUST be placed on the same horizontal `Row` as "Recordar Cuenta". The Row MUST use `fillMaxWidth()` and `Alignment.CenterVertically`. The TextButton SHALL be pushed to the right end of the Row via a `Spacer(modifier = Modifier.weight(1f))` placed before it. The Row MUST NOT be centered — the TextButton anchors to the trailing edge. The text inside the TextButton SHALL use `bodySmall` typography.

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

### REQ-LAYOUT-03: Typography Tokens Replace Raw Font Size Literals

Raw `fontSize = N.sp` literals in `LoginScreen.kt` MUST be replaced with MaterialTheme.typography tokens where the font sizes are visually equivalent. The mapping SHALL be: `13.sp` → `bodySmall`, `14.sp` → `bodyMedium`, `16.sp` → `bodyLarge`, and small labels → `labelSmall`. If a token's size does not visually match the original, the raw size MAY be kept with a comment explaining the deviation.

#### Scenario: All text elements use typography tokens

- GIVEN the login screen is rendered
- WHEN any Text composable is composed
- THEN the font size MUST be derived from a MaterialTheme.typography token
- AND no raw `fontSize = N.sp` literal SHALL appear in the code unless documented with a justification comment

#### Scenario: Visual equivalence is preserved

- GIVEN the login screen was rendered before the change with specific font sizes
- WHEN the typography tokens are applied
- THEN the rendered text sizes MUST be visually indistinguishable from the original

---

### REQ-LAYOUT-04: Button Height Consistency

All buttons in the login screen (ENTRAR, Google sign-in, Crear cuenta) MUST have the same height. The unified height SHALL be `48.dp`. The previous `52.dp` height on the ENTRAR button SHALL be changed to `48.dp`.

#### Scenario: ENTRAR button matches other button heights

- GIVEN the login form is rendered with all three buttons
- WHEN the ENTRAR, Google, and Crear cuenta buttons are composed
- THEN all three buttons MUST have identical height of `48.dp`

#### Scenario: ENTRAR button was previously 52dp

- GIVEN the login screen before this change had an ENTRAR button with `height(52.dp)`
- WHEN the layout fix is applied
- THEN the ENTRAR button height MUST be `48.dp`
- AND the visual difference (~4px) SHALL be considered negligible

---

### REQ-LAYOUT-05: Responsive Width Constraint

The login form Column MUST include a responsive width constraint. The Column SHALL use `widthIn(max = 420.dp)` to prevent the form from stretching excessively on wide screens (tablets, landscape). The form MUST remain centered in its parent.

#### Scenario: Form is constrained on wide screens

- GIVEN the login screen is rendered on a tablet or landscape orientation
- WHEN the form Column is composed
- THEN the Column width MUST NOT exceed `420.dp`
- AND the form MUST be centered horizontally in the parent

#### Scenario: Form is unconstrained on narrow screens

- GIVEN the login screen is rendered on a phone with width less than 420dp
- WHEN the form Column is composed
- THEN the Column SHALL use available width up to the screen edges
- AND no horizontal clipping or overflow SHALL occur

---

### REQ-LAYOUT-06: Unused Import Cleanup

The unused `import sp` statement in `LoginScreen.kt` MUST be removed. No unused imports SHALL remain after the typography token migration.

#### Scenario: No unused imports after refactoring

- GIVEN the login screen layout changes are applied
- WHEN `LoginScreen.kt` is compiled
- THEN no unused import warnings SHALL be present for `sp` or any other removed literal import

---

## Test Selectors

The following test tags MUST be available for login screen elements:

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
