# Proposal: Fix system insets (content behind status bar)

## Intent

Edge-to-edge (`WindowCompat.setDecorFitsSystemWindows(window, false)`) is enabled globally, but `OptoTopAppBar` sets `windowInsets = WindowInsets(0, 0, 0, 0)`, defeating Material 3's default inset handling. This causes the TopAppBar AND content on ALL screens using this component to render behind the status bar.

## Scope

### In Scope

- **Root cause**: Fix `OptoTopAppBar.kt` — replace zero insets with `TopAppBarDefaults.windowInsets`
- **Explicit zero insets**: Remove `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from Scaffolds in `ConfiguracionScreen.kt` and `AgendaScreen.kt` (also affect content padding downstream)
- **Non-Scaffold screens**: Add `Modifier.statusBarsPadding()` to `LoginScreen.kt`, `PinScreen.kt`, `CreatePinScreen.kt`
- Additional screens with zero `contentWindowInsets` (fix propagates from root cause): `DetallePacienteScreen`, `NuevaDispensacionScreen`, `NuevoPacienteScreen`, `NuevaEvaluacionScreen`, `NuevoServicioScreen`, `OperacionHoyScreen`, `PacientesListScreen`, `ReportesScreen`, `ServiciosExtraScreen`, `CierreCajaScreen`, `BIScreen`, `MonturasScreen`, `ProveedoresScreen`, `ConflictosScreen`, `RegisterScreen`, `RecoveryScreen`, `NewPasswordScreen`, `OrdenesCompraScreen` — all use `OptoTopAppBar`, fixed by root cause

### Out of Scope

- Colors, typography, or any behavioral/functional change
- Navigation bar insets (not reported as broken)
- Supabase schema, migrations, RLS, edge functions
- `MainDrawerScreen` (already correct — uses `.statusBarsPadding()`)
- Refactoring screen layouts beyond inset fixes

## Capabilities

### New Capabilities

None — pure layout fix, no spec-level behavior change.

### Modified Capabilities

None — no existing spec requirements change.

## Approach

1. **OptoTopAppBar.kt** — Change `windowInsets = WindowInsets(0, 0, 0, 0)` to `windowInsets = TopAppBarDefaults.windowInsets`. This fixes the status bar overlap for all ~25 screens using this component.
2. **ConfiguracionScreen.kt & AgendaScreen.kt** — Remove `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from Scaffold call. This lets content padding inherit proper insets.
3. **LoginScreen.kt, PinScreen.kt, CreatePinScreen.kt** — Add `Modifier.statusBarsPadding()` to the root `Box` modifier (no Scaffold present, so no inset inheritance).
4. **SeleccionOpticaScreen.kt** — Only uses `OptoTopAppBar` without explicit zero `contentWindowInsets` on Scaffold; fixed by step 1 alone.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/components/OptoTopAppBar.kt` | Modified | Root cause fix |
| `ui/screens/ConfiguracionScreen.kt` | Modified | Remove explicit zero insets |
| `ui/screens/AgendaScreen.kt` | Modified | Remove explicit zero insets |
| `ui/screens/LoginScreen.kt` | Modified | Add statusBarsPadding |
| `ui/screens/PinScreen.kt` | Modified | Add statusBarsPadding |
| `ui/screens/CreatePinScreen.kt` | Modified | Add statusBarsPadding |
| ~19 other screens using OptoTopAppBar | No direct change | Fixed by root cause propagation |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| TopAppBar height changes slightly | Low | Visual check on emulator + real device |
| Content layout shifts from new padding | Low | Test scrollable content areas visually |
| `.statusBarsPadding()` interacts with gradient backgrounds | Low | Already tested in MainDrawerScreen |

## Rollback Plan

Revert individual commit atoms in reverse order (non-Scaffold padding → Scaffold insets → component fix). Each commit is self-contained so partial rollback is safe.

## Dependencies

None.

## Success Criteria

- [ ] `OptoTopAppBar` title text is fully visible below status bar on all screens using it
- [ ] Centered content on Login, Pin, and CreatePin screens starts below the status bar
- [ ] Configuracion and Agenda screen content has proper top padding
- [ ] No regression on MainDrawerScreen (already correct)
- [ ] Debug build assembles and app runs without crash
