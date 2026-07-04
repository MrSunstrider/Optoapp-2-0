# Proposal: Fix Login Screen Alignment

## Intent

Six layout inconsistencies in `LoginScreen.kt` degrade the auth screen's visual polish: dead `align(Alignment.End)` on the TextButton, misaligned "Recordar Cuenta" row vs OutlinedTextField content, hardcoded Spacers instead of uniform spacing, mixed 52dp/48dp button heights, raw `fontSize = N.sp` literals instead of Typography tokens, and unused `OptoTokens.spacing.*`. Pure layout refactor — no behavior or spec-level capability changes.

## Scope

### In Scope
- Center "¿Olvidaste tu contraseña?" horizontally relative to the form
- Align "Recordar Cuenta" (CheckBox + Text) start edge with OutlinedTextField's text content area
- Remove dead `align(Alignment.End)` on TextButton
- Uniform vertical spacing: Password → "¿Olvidaste?" → "Recordar Cuenta" → Login button
- Consistent button heights across all 3 buttons
- Replace raw `fontSize = N.sp` with equivalent MaterialTheme.typography tokens where visual match
- Add responsive width constraint (`widthIn(max=...)`) for larger screens
- Update test selectors if layout changes break them

### Out of Scope
- Behavioral changes (navigation, auth logic, PIN, onboarding flow)
- Colors, font family, brand styling (preserve exact visual appearance per Requirement 9)
- Supabase schema, RLS, or any backend change
- Any file outside `LoginScreen.kt` (unless tests require updates)

## Capabilities

### New Capabilities
None — pure layout fix.

### Modified Capabilities
None — no spec-level behavior changes.

## Approach

1. **¿Olvidaste? centering**: Remove `align(Alignment.End)` (dead code). Apply `TextAlign.Center` on the Text inside TextButton. Parent Column's `Alignment.CenterHorizontally` + `fillMaxWidth()` handles centering.

2. **Recordar Cuenta alignment**: Apply `Modifier.padding(start = iconOffset)` to the Row, where `iconOffset` matches the OutlinedTextField leading icon area width (~56dp). This makes the text start at the same X as email/password content.

3. **Uniform spacing**: Replace hardcoded `Spacer(40.dp)` and `Spacer(8.dp)` with `Arrangement.spacedBy()` on the columns. Map to `OptoTokens.spacing.*` where exact value matches.

4. **Button heights**: Unify to 48dp (match Google/Crear). The 52dp → 48dp change is ~4px — negligible visual difference.

5. **Typography tokens**: Map `13.sp` → `bodySmall`, `14.sp` → `bodyMedium`, `16.sp` → `bodyLarge` only if sizes are visually equivalent. Otherwise keep raw sizes.

6. **Responsive**: Add `widthIn(max = 420.dp)` to the outer Column. Center form in parent Box with `Modifier.align(Alignment.Center)`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/screens/LoginScreen.kt` | Modified | Layout modifiers, spacing, alignment |
| `LoginScreenTest.kt` | Possibly updated | If layout changes break test tag selectors |
| `LoginFlowTest.kt` | Possibly updated | Same reason |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Visual regression (pixel-perfect mismatch) | Medium | Manual verification against Requirement 9 |
| Test tag selectors break | Low | Run `testDebugUnitTest` after changes |
| OptoTokens.spacing value differs from current | Low | Use exact dp if token doesn't match |

## Rollback Plan

Revert `LoginScreen.kt` and test files to HEAD. Single-file change makes rollback trivial.

## Dependencies

None.

## Success Criteria

- [ ] "¿Olvidaste tu contraseña?" text is centered horizontally in the form
- [ ] "Recordar Cuenta" text starts at same X position as email/password text inside OutlinedTextField
- [ ] All vertical spacings between form elements are uniform
- [ ] All 3 buttons (ENTRAR, Google, Crear cuenta) have the same height
- [ ] Raw `fontSize = N.sp` literals are either replaced by Typography tokens or explicitly left unchanged with visual justification
- [ ] `optapp` unit tests pass (`./gradlew :optoapp:testDebugUnitTest --stacktrace`)
- [ ] Visual appearance is preserved (no color, font, or spacing value changes)
