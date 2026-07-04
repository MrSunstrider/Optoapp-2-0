# Login Screen — Layout Alignment Delta Spec

## Purpose

Delta spec for `fix-login-alignment`. Documents 6 layout consistency fixes in `LoginScreen.kt`. This is a **pure layout change** — no behavioral or capability modifications. The login screen's functional behavior (auth flow, navigation, PIN, error handling) is unchanged.

## Scope

- **Files affected**: `LoginScreen.kt`, `TestTags.kt`, `LoginFlowTest.kt`
- **Capability changes**: None
- **Behavioral changes**: None

## Requirements

### REQ-LAYOUT-01: "¿Olvidaste tu contraseña?" TextButton Alignment

The "¿Olvidaste tu contraseña?" TextButton MUST NOT use `align(Alignment.End)`. The dead modifier SHALL be removed. The text inside the TextButton MUST use `TextAlign.Center`. The parent Column's `Alignment.CenterHorizontally` + `fillMaxWidth()` SHALL handle horizontal centering.

#### Scenario: TextButton has no end-alignment modifier

- GIVEN the `LoginScreen` composable is rendered
- WHEN the "¿Olvidaste tu contraseña?" TextButton is composed
- THEN the TextButton MUST NOT have an `align(Alignment.End)` modifier applied
- AND the text inside the TextButton MUST render with `TextAlign.Center`

#### Scenario: TextButton is centered horizontally in the form

- GIVEN the login form Column is rendered on any screen width
- WHEN the user observes the "¿Olvidaste tu contraseña?" TextButton
- THEN the TextButton MUST appear centered horizontally relative to the form content

---

### REQ-LAYOUT-02: "Recordar Cuenta" Row Alignment with OutlinedTextField Content

The "Recordar Cuenta" Row (containing CheckBox + Text) MUST be horizontally aligned with the text content area of the OutlinedTextField components above it. The Row SHALL use `Modifier.padding(start = iconOffset)` where `iconOffset` matches the OutlinedTextField leading icon area width (~56dp).

#### Scenario: Recordar Cuenta text starts at same X as OutlinedTextField content

- GIVEN the login form is rendered with email and password OutlinedTextFields
- WHEN the "Recordar Cuenta" Row is rendered below the password field
- THEN the start edge of the CheckBox + Text content MUST align with the start edge of the text content inside the OutlinedTextFields
- AND the Row MUST have a `padding(start)` value that offsets past the OutlinedTextField's leading icon area

#### Scenario: Alignment is consistent across screen sizes

- GIVEN the login form is rendered on a phone or tablet screen
- WHEN the "Recordar Cuenta" Row is rendered
- THEN the horizontal alignment with OutlinedTextField content MUST be preserved regardless of screen width

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

The following test tag MUST be available for the "¿Olvidaste tu contraseña?" TextButton:

| Tag | File | Purpose |
|-----|------|---------|
| `LOGIN_OLVIDASTE_BTN` | `TestTags.kt` | Enables UI testing of the forgot-password button |

New TDD tests in `LoginFlowTest.kt` SHALL verify:

1. The "¿Olvidaste tu contraseña?" button is reachable and centered
2. The "Recordar Cuenta" row alignment is correct relative to OutlinedTextField content

## Non-Functional Notes

- **Visual regression risk**: Medium — manual verification recommended against production screenshots
- **Test impact**: 2 new tests added, existing tests must continue passing
- **Rollback**: Single-file revert of `LoginScreen.kt` + test files to HEAD
