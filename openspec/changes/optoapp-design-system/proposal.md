# Proposal: OptoApp Design System — Material 3 + Component Library + UI Refinement

## Intent

OptoApp's Android UI has grown organically across ~25 screens and 40+ components without a design system foundation. The result: 54 hardcoded `RoundedCornerShape` values (8dp, 10dp, 12dp, 14dp, 16dp, 20dp, 24dp, 30dp), 29+ direct color imports bypassing `MaterialTheme.colorScheme`, 3 missing M3 color slots (`surfaceVariant`, `onSurfaceVariant`, `outline`), inconsistent TopAppBar styling (some `primary`, some `surface`, some `primaryContainer`), and a minimal component library (only `OptoTextField` and `DropdownField` as reusable primitives). This change establishes a proper design system: token layer, component library, and screen-wide consistency — giving OptoApp a professional, cohesive medical UI.

## Scope

### In Scope (Android only)
- **Design tokens**: Color palette (light + dark), spacing scale, shape system, typography audit
- **M3 color scheme fix**: Fill all missing slots (`surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`, `surfaceTint`, `inverseSurface`, etc.)
- **Component library**: OptoCard, OptoButton (filled/outlined/text), OptoTopAppBar, OptoDialog, OptoFilterChip, OptoSegmentedSelector, OptoQuickAddChip, OptoVisionInput
- **Iconography audit**: Leverage `material-icons-extended` (already in deps) consistently
- **Dark mode cleanup**: Remove 29+ direct color imports (`SurfaceDarkMuted`, `PrimaryDark`, etc.), replace with `MaterialTheme.colorScheme` references
- **Hardcoded shape replacement**: Replace all 54 `RoundedCornerShape(N.dp)` with `MaterialTheme.shapes` tokens
- **TopAppBar unification**: Single `OptoTopAppBar` component with consistent color behavior
- **Screen audit**: Apply components to all 25 screens, fix inconsistencies

### Out of Scope
- Web app (optoweb) — separate design system effort
- Custom fonts — using system default, typography scale only
- Animations/motion system — deferred
- Dark mode toggle in-app — respects system setting only
- New screens/features — this is a refinement pass, not new functionality

## Capabilities

### New Capabilities
- `design-tokens`: Color palette, spacing scale, shape system, typography tokens — the foundation layer
- `opto-components`: Reusable component library (OptoCard, OptoButton, OptoTopAppBar, OptoDialog, OptoFilterChip, OptoSegmentedSelector, OptoQuickAddChip, OptoVisionInput)

### Modified Capabilities
- None — no existing specs change at the spec level; this is additive UI infrastructure

## Approach

**Phased delivery** to manage risk across 25+ screens and 40+ components. Each phase is independently shippable and testable.

### Phase 1: Design Foundation
**Goal**: Establish the token layer without breaking any existing screen.

- Define `OptoTokens.kt` (colors, spacing, shapes) as the single source of truth
- Fix M3 color scheme: fill all missing slots (`surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`)
- Apply user's purple palette: Primary #6D4AFF, Accent #3DD9A5, Background #0B1220, Surface #172033
- Define shape tokens: Small (12dp), Medium (16dp), Large (24dp) — replacing 54 hardcoded values
- Define spacing tokens: 4, 8, 12, 16, 24, 32 dp
- Audit typography: ensure all screens use `MaterialTheme.typography` instead of hardcoded `fontSize = 14.sp`
- **Deliverable**: Token files, updated Theme.kt, zero visual regression (same output, new tokens under the hood)

### Phase 2: Core Components
**Goal**: Build the reusable component primitives. Each gets its own file + preview + test.

- `OptoCard` — clinical card with elevation, shape from tokens, content slot
- `OptoButton` — Filled, Outlined, Text variants with proper M3 button colors
- `OptoTopAppBar` — single consistent TopAppBar (replaces 15+ inline definitions)
- `OptoDialog` — consistent alert dialog wrapper
- `OptoFilterChip` — for toggling filters (replaces inline FilterChip usage)
- **Deliverable**: 5 component files, each with `@Preview` and composable tests

### Phase 3: Input Components
**Goal**: Build domain-specific input primitives for clinical data.

- `OptoSegmentedSelector` — segmented controls replacing switches (e.g., VP selection)
- `OptoQuickAddChip` — quick-add lens powers (currently inline in RefraccionSection)
- `OptoVisionInput` — specialized vision acuity input field
- Upgrade `OptoTextField` — add leading icon, character count, proper error states
- **Deliverable**: 4 component files, RefraccionSection migrated to use new components

### Phase 4: Screen Audit & Migration
**Goal**: Replace inline components across all 25 screens with design system components.

- Migrate TopAppBar usages (15+ screens) → `OptoTopAppBar`
- Migrate Card usages → `OptoCard`
- Replace 54 hardcoded `RoundedCornerShape` → shape tokens
- Replace 29+ direct color imports → `MaterialTheme.colorScheme`
- Add empty states for screens missing them
- Ensure consistent spacing using tokens
- **Deliverable**: All screens using design system, zero hardcoded colors/shapes

## Color Palette Decision

**Recommendation: Modified purple palette with indigo lean.**

The user proposes Primary #6D4AFF (vivid purple). For an optometry SaaS:

- **Pro**: Purple signals premium/innovation, differentiates from generic medical blue. The teal accent (#3DD9A5) adds medical warmth.
- **Con**: #6D4AFF at full saturation may cause eye fatigue in clinical use. Pure purple can read as "fashion/beauty" rather than "medical professional."
- **Risk**: Accessibility — purple on dark backgrounds can fail WCAG AA contrast ratios.

**My recommendation**: Shift the primary slightly toward indigo (#5B4AFF → #6366F1 range) which:
1. Maintains the purple identity
2. Improves contrast on dark surfaces (#0B1220)
3. Reads more "medical tech" than "fashion brand"
4. Better WCAG compliance with white text

The final palette should be validated with a contrast checker before Phase 1 implementation. If the user insists on exact #6D4AFF, we proceed but MUST verify contrast ratios.

**Full recommended palette**:

| Token | Light | Dark | Purpose |
|-------|-------|------|---------|
| primary | #6D4AFF | #9B8AFF | Primary actions |
| onPrimary | #FFFFFF | #1A0F3D | Text on primary |
| primaryContainer | #EDE8FF | #2D1F6E | Subtle primary bg |
| secondary | #3DD9A5 | #6EE7B7 | Accent/success |
| background | #F5F7FA | #0B1220 | Page background |
| surface | #FFFFFF | #172033 | Card/sheet bg |
| surfaceVariant | #E8EAF0 | #1E293B | Muted surfaces |
| onSurfaceVariant | #475569 | #94A3B8 | Secondary text |
| outline | #CBD5E1 | #334155 | Borders |
| outlineVariant | #E2E8F0 | #1E293B | Subtle borders |
| error | #DC2626 | #F87171 | Error states |

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/theme/Color.kt` | Modified | New token definitions, deprecated old color vals |
| `ui/theme/Theme.kt` | Modified | Fill all M3 color slots, add shapes |
| `ui/theme/Type.kt` | Minor | Audit against M3 type scale (already reasonable) |
| `ui/theme/` (new) | New | `OptoTokens.kt` — spacing, shape, elevation tokens |
| `ui/components/` (new) | New | 9 new component files (OptoCard, OptoButton, etc.) |
| `ui/components/OptoTextField.kt` | Modified | Upgrade with new props |
| `ui/components/CommonComponents.kt` | Modified | Migrate DropdownField to use tokens |
| `ui/components/evaluacion/RefraccionSection.kt` | Modified | Extract QuickAddChip, NumericAddStepper to shared |
| `ui/screens/*.kt` (25 files) | Modified | Replace inline TopAppBar, Cards, colors, shapes |
| `build.gradle.kts` | Unchanged | material-icons-extended already included |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Visual regression during token migration | Medium | Phase 1 delivers with zero visual change; use screenshot testing or manual diff |
| Purple palette fails WCAG contrast | Medium | Validate contrast ratios in Phase 1 before any screen changes |
| Component API design doesn't match actual screen needs | Medium | Build OptoCard/OptoButton first, migrate 2-3 screens as proof, then build rest |
| RefraccionSection refactor breaks clinical data entry | Low | Extract components first (Phase 3), migrate section last; TDD for behavior |
| Merge conflicts with parallel feature work | Low | Design system is additive; old code works until migrated |
| Scope creep into new features | Medium | Strict "no new screens/features" rule; if user requests, defer to separate change |

## Rollback Plan

- **Phase 1**: Revert `Color.kt`, `Theme.kt`, delete `OptoTokens.kt`. Zero screen changes = zero impact.
- **Phase 2-3**: Delete new component files. Screens still use inline code — no breakage.
- **Phase 4**: This is the riskiest phase. Each screen migration is independent — revert individual screen files if needed. Git branch per phase recommended.
- **Full rollback**: `git revert` the merge commit for each phase.

## Dependencies

- Compose BOM 2024.12.01 (already in use, includes M3)
- `material-icons-extended` (already in build.gradle)
- No new external dependencies required

## Success Criteria

- [ ] All M3 color scheme slots filled (surfaceVariant, onSurfaceVariant, outline, outlineVariant, surfaceTint, inverseSurface, inverseOnSurface, inversePrimary, scrim, outlineVariant)
- [ ] Zero direct color imports outside `ui/theme/` (grep confirms no `SurfaceDarkMuted`, `PrimaryDark`, etc. in screens/components)
- [ ] Zero hardcoded `RoundedCornerShape` values (all replaced with `MaterialTheme.shapes`)
- [ ] Zero hardcoded `fontSize = N.sp` values (all use `MaterialTheme.typography`)
- [ ] All 15+ TopAppBar usages replaced with `OptoTopAppBar`
- [ ] At least 5 new reusable components with `@Preview` and composable tests
- [ ] Dark mode uses `MaterialTheme.colorScheme` consistently (no leaked light-mode values)
- [ ] Purple palette passes WCAG AA contrast (4.5:1 normal text, 3:1 large text)
- [ ] App builds and all existing tests pass after each phase
