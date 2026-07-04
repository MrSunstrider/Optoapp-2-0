# Proposal: Login Row Layout

## Intent

Consolidate the "Recordar Cuenta" checkbox row and the "¿Olvidaste tu contraseña?" button into a single row. Currently they occupy two separate horizontal lines inside the login form card, wasting vertical space and breaking the visual density expected in a compact login form.

## Scope

### In Scope
- Merge two separate rows into one `Row` composable inside `OptoCard`
- Place "Recordar Cuenta" (Checkbox + Text) on the left
- Place "¿Olvidaste tu contraseña?" (TextButton) on the right
- Use `Spacer(modifier.weight(1f))` to push them apart
- Reduce font size to `bodySmall` on both elements so they fit without overflow
- Adjust button section below to maintain consistent vertical rhythm

### Out of Scope
- Behavioral changes to auth flow, navigation, PIN, or error handling
- Supabase schema, RLS, or any backend changes
- Changes to other screens or composables
- Test infrastructure changes beyond adjusting existing expectations

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `login-screen`: The layout requirements REQ-LAYOUT-01 (Olvidaste TextButton alignment) and REQ-LAYOUT-02 (Recordar Cuenta row alignment) need delta updates to reflect the merged single-row structure

## Approach

Replace the two top-level children (a `TextButton` with `fillMaxWidth()` and a `Row` with `padding(start = iconOffset)`) inside the form `Column` with a single `Row` that has two children separated by `Spacer(weight(1f))`:

```
Row(fillMaxWidth, CenterVertically) {
    Row(CenterVertically, testTag="remember") { Checkbox + "Recordar Cuenta" }
    Spacer(weight(1f))
    TextButton(tag="olvidaste") { "¿Olvidaste tu contraseña?" }
}
```

Both text elements use `bodySmall`. The outer Row sits in the same 12dp spaced Column as the rest of the form.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `LoginScreen.kt` | Modified | Replace two separate rows with one merged row |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Text overflow on small screens | Low | `bodySmall` provides sufficient reduction; weight spacer absorbs remaining space |
| Visual regression vs previous layout | Low | Single-file change, visually inspectable in one screen |

## Rollback Plan

Revert `LoginScreen.kt` to HEAD. No other files affected. Zero damage surface.

## Dependencies

- None

## Success Criteria

- [ ] "Recordar Cuenta" and "¿Olvidaste tu contraseña?" render on the same vertical level
- [ ] "Recordar Cuenta" is left-aligned, "¿Olvidaste tu contraseña?" is right-aligned
- [ ] Both use `bodySmall` typography
- [ ] No text clipping or overflow on 320dp–480dp widths
- [ ] All existing login tests pass
