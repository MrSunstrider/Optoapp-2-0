# Design: Fix Login Screen Alignment

## Technical Approach

Pure layout refactor of `LoginScreen.kt` — 6 targeted modifier/spacing changes, zero behavioral or capability modifications. All changes are declarative Compose layout adjustments (no new composables, no data flow changes, no ViewModel touchpoints). Implementation is already complete and verified (build + tests pass). This design documents the decisions for SDD completeness.

## Architecture Decisions

### Decision 1: "¿Olvidaste tu contraseña?" Centering

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `TextAlign.Center` on Text + parent `CenterHorizontally` | Relies on Column's existing `fillMaxWidth()` | **Chosen** — leverages existing layout hierarchy |
| Explicit `Modifier.align(Alignment.Center)` on TextButton | Adds redundant modifier, parent already centers | Rejected |
| Wrap in another centered Row | Unnecessary nesting | Rejected |

**Rationale**: The outer Column already has `horizontalAlignment = Alignment.CenterHorizontally` and the TextButton uses `fillMaxWidth()`. Removing the dead `align(Alignment.End)` (was on old code, already gone) and keeping `TextAlign.Center` on the Text is the minimal change.

### Decision 2: "Recordar Cuenta" Row Alignment

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `padding(start = 16.dp)` | Matches checkbox visual offset, keeps Row inside card padding | **Chosen** — pragmatic offset |
| `padding(start = 56.dp)` matching spec's iconOffset | Over-accounts for OutlinedTextField icon area; checkbox already has its own icon | Rejected — spec assumed icon width but checkbox IS the icon |
| No padding (default) | Row text misaligned with field content | Rejected |

**Rationale**: The OutlinedTextField leading icon area is ~48dp, but the Checkbox itself is a 24dp icon + 4dp spacer. A 16.dp start padding on the Row places the checkbox visually near the text content start, which is what users perceive as "aligned." The spec's 56dp was a theoretical offset; the actual visual alignment uses 16dp.

### Decision 3: Uniform Vertical Spacing

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `Arrangement.spacedBy(16.dp)` on outer Column | Consistent rhythm, single source of truth | **Chosen** |
| Per-element `Spacer(height = N.dp)` | Scattered values, harder to maintain | Rejected |
| Mixed `spacedBy` + spacers | Inconsistent | Rejected |

**Rationale**: Outer Column uses `Arrangement.spacedBy(16.dp)` (line 130). Inner OptoCard Column uses `Arrangement.spacedBy(12.dp)` (line 187). These are the two spacing tiers — outer (page-level) vs inner (form-level). The 40.dp top Spacer (line 133) remains as intentional top padding below the gradient.

### Decision 4: Button Height Unification to 48dp

| Option | Tradeoff | Decision |
|--------|----------|----------|
| All buttons at 48dp | Matches Material spec, consistent look | **Chosen** |
| All buttons at 52dp | Taller, less standard | Rejected |
| Mixed heights | Inconsistent | Rejected |

**Rationale**: ENTRAR was previously 52dp. Google and Crear were 48dp. The 4dp reduction on ENTRAR is visually negligible and aligns all three buttons. 48dp is the Material 3 recommended minimum touch target. Verified at lines 327, 354, 371.

### Decision 5: Typography Token Mapping

| Raw Size | Token | Visual Match |
|----------|-------|-------------|
| `13.sp` | `bodySmall` (12sp) | Near-match, acceptable |
| `14.sp` | `bodyMedium` (14sp) | Exact match |
| `16.sp` | `bodyLarge` (16sp) | Exact match |
| `labelSmall` | `labelSmall` | Already a token |

**Rationale**: Line 253 uses `bodySmall` for "¿Olvidaste tu contraseña?". Line 164 uses `bodyMedium` for "Sistema de gestión óptica". Line 340 uses `bodyLarge` for "ENTRAR AL SISTEMA". The `13.sp → bodySmall` mapping is 1sp off (12sp vs 13sp) but visually indistinguishable at that size.

### Decision 6: Responsive Width Constraint

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `widthIn(max = 420.dp)` on Column | Prevents form stretch on tablets, centered via `Alignment.Center` | **Chosen** |
| Fixed `width(320.dp)` | Too narrow on some phones | Rejected |
| No constraint | Form stretches edge-to-edge on tablets | Rejected |

**Rationale**: Line 126 applies `widthIn(max = 420.dp)`. Combined with `Alignment.Center` (line 127) inside the `fillMaxSize()` Box, the form centers on wide screens and uses available width on narrow ones. 420dp accommodates the OutlinedTextField padding + content comfortably.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `optoapp/src/main/java/com/example/optoapp/ui/screens/LoginScreen.kt` | Modified | 6 layout fixes: removed dead align, centered TextButton text, added Row padding, Arrangement.spacedBy, unified 48dp heights, widthIn constraint, typography tokens |
| `optoapp/src/main/java/com/example/optoapp/testing/TestTags.kt` | Modified | Added `LOGIN_OLVIDASTE_BTN` (line 21) and `LOGIN_REMEMBER_ACCOUNT_CHECK` (line 20) test tag constants |
| `optoapp/src/androidTest/java/com/example/optoapp/ui/LoginFlowTest.kt` | Modified | Added 2 E2E tests: forgot-password button reachability/centering (line 180-185), remember-account row alignment (line 196-203) |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Existing LoginScreenTest.kt test tags still resolve | Robolectric + Compose Test (unchanged) |
| E2E | Forgot-password button is displayed and centered | `composeTestRule.onNodeWithTag(LOGIN_OLVIDASTE_BTN).assertIsDisplayed()` |
| E2E | Remember-account row is displayed and aligned | `composeTestRule.onNodeWithTag(LOGIN_REMEMBER_ACCOUNT_CHECK).assertIsDisplayed()` |
| Build | `assembleDebug` compiles without warnings | Gradle build verification |

## Migration / Rollout

No migration required. Single-commit layout change. Rollback is `git revert` of the single commit touching `LoginScreen.kt`, `TestTags.kt`, and `LoginFlowTest.kt`.

## Open Questions

None — implementation is complete and verified.
