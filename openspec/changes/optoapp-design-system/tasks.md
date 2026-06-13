# Tasks: OptoApp Design System

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

## Work Units

| Unit | Branch | Base | Est. Lines |
|------|--------|------|-----------|
| PR 1: Design Foundation | `feat/optoapp-ds-phase-1` | `feat/optoapp-ds` | ~150 |
| PR 2: Core Components | `feat/optoapp-ds-phase-2` | phase-1 | ~400-500 |
| PR 3: Input Components | `feat/optoapp-ds-phase-3` | phase-2 | ~350-400 |
| PR 4: Screen Migration P1 | `feat/optoapp-ds-phase-4` | phase-3 | ~400-500 |
| PR 5: Screen Migration P2 | `feat/optoapp-ds-phase-5` | phase-4 | ~300-400 |

## PR 1: Design Foundation ✅

- [x] 1.1 Create `ui/theme/OptoTokens.kt` — spacing (4,8,12,16,24,32dp), shapes (small=12dp, medium=16dp, large=24dp), elevation (1,2,4,8dp), color constants
- [x] 1.2 Modify `ui/theme/Color.kt` — purple palette (#6D4AFF primary, #3DD9A5 accent), deprecate old aliases
- [x] 1.3 Modify `ui/theme/Theme.kt` — fill all M3 slots (surfaceVariant, onSurfaceVariant, outline, outlineVariant, inverseSurface, scrim, surfaceTint), inject shapes
- [x] 1.4 Write `OptoTokensTest.kt` — verify values, WCAG AA contrast ratios
- [x] 1.5 Write theme integration test — slots non-null, shapes match tokens
- [x] 1.6 Run full test suite — pass

## PR 2: Core Components ✅

- [x] 2.1 Create `ui/components/OptoCard.kt` — elevation, shape, onClick, content slot
- [x] 2.2 Create `ui/components/OptoButton.kt` — Filled/Outlined/Text, loading, icon+label, fullWidth
- [x] 2.3 Create `ui/components/OptoTopAppBar.kt` — surface/onSurface, nav icon, actions, scroll
- [x] 2.4 Create `ui/components/OptoDialog.kt` — title, content, confirm/cancel/optional third
- [x] 2.5 Create `ui/components/OptoFilterChip.kt` — selected/unselected, leading icon
- [x] 2.6-2.10 Write tests for each component — renders, colors, interactions

## PR 3: Input Components

- [ ] 3.1 Create `OptoSegmentedSelector.kt` — N options, single select, animated indicator
- [ ] 3.2 Create `OptoQuickAddChip.kt` — lens power, toggle, row layout
- [ ] 3.3 Create `OptoVisionInput.kt` — acuity entry, error state, formatting
- [ ] 3.4 Upgrade `OptoTextField.kt` — leadingIcon, maxLength, showCharCount, animated error
- [ ] 3.5 Migrate `RefraccionSection.kt` — use OptoQuickAddChip, OptoSegmentedSelector
- [ ] 3.6-3.9 Write tests for each input component

## PR 4: Screen Migration P1 (TopAppBar, Cards, Shapes)

- [ ] 4.1 Replace `TopAppBar(` in 15 screens → `OptoTopAppBar`
- [ ] 4.2 Replace `Card(`/`ElevatedCard(`/`OutlinedCard(` in screens → `OptoCard`
- [ ] 4.3 Replace `Card(` in components (PacienteInfoHeader, EvaluacionListItem, etc.) → `OptoCard`
- [ ] 4.4 Replace `RoundedCornerShape(N.dp)` in screens → `MaterialTheme.shapes.*`
- [ ] 4.5 Replace `RoundedCornerShape` in components → token shapes
- [ ] 4.6 Run full test suite — pass

## PR 5: Screen Migration P2 (Colors, Empty States, Spacing)

- [x] 5.1 Replace 29+ direct color imports → `MaterialTheme.colorScheme.*`
- [x] 5.2 Clean `RefraccionSection.kt` hardcoded colors → M3 slots
- [x] 5.3 Audit `CommonComponents.kt` — token-based defaults
- [x] 5.4 Add empty states (PacientesList, MonturasScreen, ServiciosExtraScreen)
- [x] 5.5 Apply `OptoTokens.spacing.*` — replace hardcoded padding/gap
- [x] 5.6 Grep-verify: zero RoundedCornerShape outside OptoTokens, zero direct colors outside theme, zero TopAppBar in screens
- [x] 5.7 Run tests + `assembleDebug` — all pass

---

**Note**: PR 4 may exceed 400 lines — split shape replacements into a separate commit. Each PR must pass `testDebugUnitTest` + `assembleDebug` before next PR.
