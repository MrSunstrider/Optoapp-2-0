# Exploration: Responsive Design for OptoApp

## Current State

### Layout Architecture — `MainDrawerScreen.kt`

The root composable uses a **`ModalNavigationDrawer`** wrapping a `Surface` with a `Column` containing:

1. A full-width `LinearProgressIndicator` (sync indicator)
2. A full-width `TextButton` (active optica header)
3. A `Box(weight(1f))` containing:
   - **`NavHost`** (startDestination = "pacientes") with **20 composable routes**
   - A `SnackbarHost` aligned to BottomCenter

The drawer is always **modal** (`ModalNavigationDrawer`). There is **no permanent drawer variant**, no `DrawerValue` based on screen width, no layout changes between phone/tablet.

### Screen Inventory

The app has **~32 screen composables** across 3 navigation contexts:

| # | Screen | File | Type | Layout |
|---|--------|------|------|--------|
| 1 | PacientesListScreen | `ui/screens/PacientesListScreen.kt` | List | Scaffold + LazyColumn, fillMaxWidth |
| 2 | AgendaScreen | `ui/screens/AgendaScreen.kt` | List/Dashboard | Scaffold + LazyColumn, fillMaxWidth |
| 3 | NuevoPacienteScreen | `ui/screens/NuevoPacienteScreen.kt` | Form | Scaffold + Column + verticalScroll |
| 4 | DetallePacienteScreen | `ui/screens/DetallePacienteScreen.kt` | Detail/Tabs | Scaffold + TabRow + Box |
| 5 | NuevaEvaluacionScreen | `ui/screens/NuevaEvaluacionScreen.kt` | Form (tabs) | Scaffold + TabRow + Column + verticalScroll |
| 6 | NuevaDispensacionScreen | `ui/screens/NuevaDispensacionScreen.kt` | Form | Scaffold + Column + verticalScroll |
| 7 | ServiciosExtraScreen | `ui/screens/ServiciosExtraScreen.kt` | List | Scaffold + LazyColumn |
| 8 | MonturasScreen | `ui/screens/MonturasScreen.kt` | List/Manage | Scaffold + Column |
| 9 | ProveedoresScreen | `ui/screens/ProveedoresScreen.kt` | List | Scaffold + LazyColumn |
| 10 | OrdenesCompraScreen | `ui/screens/ordenescompra/OrdenesCompraScreen.kt` | List | Scaffold + LazyColumn |
| 11 | InventarioFisicoScreen | `ui/screens/inventariofisico/InventarioFisicoScreen.kt` | List | Scaffold + LazyColumn |
| 12 | OperacionHoyScreen | `ui/screens/OperacionHoyScreen.kt` | Dashboard | Scaffold + Column + verticalScroll |
| 13 | NuevoServicioScreen | `ui/screens/NuevoServicioScreen.kt` | Form | Scaffold + Column + verticalScroll |
| 14 | ReportesScreen | `ui/screens/ReportesScreen.kt` | List | Scaffold + LazyColumn |
| 15 | CierreCajaScreen | `ui/screens/CierreCajaScreen.kt` | Dashboard | Scaffold + Column + verticalScroll |
| 16 | AnalisisNegocioScreen | `ui/screens/AnalisisNegocioScreen.kt` | Dashboard | Scaffold + Column + verticalScroll |
| 17 | AnalisisDetalleScreen | `ui/screens/AnalisisDetalleScreen.kt` | Dashboard | Scaffold + Column + verticalScroll |
| 18 | ConfiguracionScreen | `ui/screens/ConfiguracionScreen.kt` | Settings | Scaffold + Column + verticalScroll |
| 19 | ConflictosScreen | `ui/screens/ConflictosScreen.kt` | List | Scaffold + LazyColumn |
| 20 | InformacionFinancieraScreen | `ui/screens/InformacionFinancieraScreen.kt` | Form | Scaffold + Column + verticalScroll |
| 21 | MonturaScanScreen | `ui/screens/inventariofisico/MonturaScanScreen.kt` | Scan | Scaffold + LazyColumn |
| 22 | VarianceReportScreen | `ui/screens/inventariofisico/VarianceReportScreen.kt` | List | Scaffold + LazyColumn |
| 23 | LoginScreen | `ui/screens/LoginScreen.kt` | Auth | Box + centered Column |
| 24 | RegisterScreen | `ui/screens/RegisterScreen.kt` | Auth | Scaffold + Column |
| 25 | RecoveryScreen | `ui/screens/RecoveryScreen.kt` | Auth | — |
| 26 | NewPasswordScreen | `ui/screens/NewPasswordScreen.kt` | Auth | — |
| 27 | PinScreen | `ui/screens/PinScreen.kt` | Auth | Box + centered Column |
| 28 | CreatePinScreen | `ui/screens/CreatePinScreen.kt` | Auth | Box + centered Column |
| 29 | SeleccionOpticaScreen | `ui/screens/SeleccionOpticaScreen.kt` | List | Scaffold + LazyColumn |
| 30 | SinOpticaScreen | `ui/screens/SinOpticaScreen.kt` | Onboarding | — |
| 31 | OnboardingOpticaScreen | `ui/screens/OnboardingOpticaScreen.kt` | Onboarding | — |

**Total: ~31 screens in 28 files + 3 subdirectory files.**

### Common Layout Patterns

**Pattern A: List screens** (12 screens)
```
Scaffold(topBar = OptoTopAppBar(...)) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        // search field or header
        LazyColumn(Modifier.fillMaxSize()) { ... }
    }
}
```

**Pattern B: Form screens** (8 screens)
```
Scaffold(topBar = OptoTopAppBar(...)) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        .verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        // form fields, each Modifier.fillMaxWidth()
    }
}
```

**Pattern C: Dashboard/detail screens** (5 screens)
```
Scaffold(topBar = OptoTopAppBar(...)) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)
        .verticalScroll(rememberScrollState()).navigationBarsPadding()) {
        // Cards, each Modifier.fillMaxWidth()
    }
}
```

**Pattern D: Auth screens** (6 screens)
```
Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
    Column(Modifier.widthIn(max = 420.dp).align(Alignment.Center).padding(horizontal = 32.dp)) {
        // centered content
    }
}
```

### Existing Responsive Code

**ZERO responsive adaptation exists.** The codebase search found:

- `widthIn(max = 420.dp)` — **1 occurrence**, only in `LoginScreen.kt`
- `BoxWithConstraints` — **0 occurrences**
- `WindowSizeClass` — **0 occurrences** (no `material3-adaptive` dependency)
- `LocalConfiguration.current.screenWidthDp` — **0 occurrences**
- Landscape/orientation detection — **0 occurrences**
- `Modifier.widthIn()` — only the LoginScreen usage above
- `fillMaxWidth()` — **200+ occurrences** (the universal pattern)
- `contentWindowInsets = WindowInsets(0, 0, 0, 0)` — standard pattern across ~15 screens

### Padding Consistency

- Standard: `padding(horizontal = 16.dp)` — used in ~80% of screens
- LoginScreen: `padding(horizontal = 32.dp)`
- RegisterScreen: `padding(horizontal = 24.dp)`
- PinScreen/CreatePinScreen: `padding(horizontal = 24.dp)`
- ConfiguracionScreen: uses `OptoTokens.spacing.lg` (= 16.dp)
- `navigationBarsPadding()` — applied on ~12 screens, missing on some
- Auth screens use `statusBarsPadding()` + `navigationBarsPadding()`

The Fase 5 standardization is mostly consistent but not 100% uniform.

### Key Pain Points (on tablets / landscape)

1. **Lists stretch infinitely**: LazyColumns with single-column layout become extremely wide on tablets, making text hard to read
2. **Forms waste space**: Single-column forms on a 10" tablet use <40% of available width
3. **Cards grow unbounded**: `OptoCard(modifier = Modifier.fillMaxWidth())` — no max-width cap anywhere except LoginScreen
4. **No master-detail**: DetallePacienteScreen could show list + detail side by side
5. **Drawer is always modal**: No permanent drawer for tablets
6. **No multi-column layouts**: Even list screens could use 2-column grids on tablets
7. **Analytics dashboards**: The `Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp))` with 3 equal-weight columns works okay but doesn't adapt
8. **Long text lines**: No `maxWidth` constraint means text lines can be 800dp+ on tablets

### Component Library

- **OptoCard**: Thin wrapper around Material3 `Card` — no responsive features
- **OptoTopAppBar**: Uses `CenterAlignedTopAppBar` with `fillMaxWidth()` — no adaptation
- **OptoTokens**: Has `spacing` and `shapes` tokens but no responsive/size tokens
- **CommonComponents**: `DropdownField` uses `fillMaxWidth()` — no responsive behavior

---

## Affected Areas

### Directly affected files (~29 files)

All screen files in `ui/screens/` and subdirectories plus key components:

- `ui/screens/MainDrawerScreen.kt` — drawer behavior, root layout
- `ui/screens/DrawerSections.kt` — drawer content
- `ui/screens/PacientesListScreen.kt` — list layout
- `ui/screens/AgendaScreen.kt` — list/dashboard layout
- `ui/screens/NuevoPacienteScreen.kt` — form layout
- `ui/screens/DetallePacienteScreen.kt` — detail/tabs layout
- `ui/screens/NuevaEvaluacionScreen.kt` — form layout
- `ui/screens/NuevaDispensacionScreen.kt` — form layout
- `ui/screens/ServiciosExtraScreen.kt` — list layout
- `ui/screens/MonturasScreen.kt` — list/manage layout
- `ui/screens/ProveedoresScreen.kt` — list layout
- `ui/screens/OrdenesCompraScreen.kt` — list layout
- `ui/screens/InventarioFisicoScreen.kt` — list layout
- `ui/screens/MonturaScanScreen.kt` — scan layout
- `ui/screens/VarianceReportScreen.kt` — list layout
- `ui/screens/OperacionHoyScreen.kt` — dashboard layout
- `ui/screens/NuevoServicioScreen.kt` — form layout
- `ui/screens/ReportesScreen.kt` — list layout
- `ui/screens/CierreCajaScreen.kt` — dashboard layout
- `ui/screens/AnalisisNegocioScreen.kt` — dashboard layout
- `ui/screens/AnalisisDetalleScreen.kt` — dashboard layout
- `ui/screens/ConfiguracionScreen.kt` — settings layout
- `ui/screens/ConflictosScreen.kt` — list layout
- `ui/screens/InformacionFinancieraScreen.kt` — form layout
- `ui/screens/LoginScreen.kt` — auth layout (has existing widthIn)
- `ui/screens/RegisterScreen.kt` — auth layout
- `ui/screens/PinScreen.kt` — auth layout
- `ui/screens/CreatePinScreen.kt` — auth layout
- `ui/components/OptoCard.kt` — core card component
- `ui/components/OptoTopAppBar.kt` — core app bar

### Indirectly affected files

- `ui/theme/OptoTokens.kt` — may need responsive size tokens
- `ui/components/CommonComponents.kt` — shared components
- Component sections under `ui/components/evaluacion/`, `ui/components/dispensacion/`, etc.
- `ui/components/paciente/` — tab content composables

---

## Approaches

### Approach A: Canonical Content Width Wrapper (Lowest Invasive)

Add a single reusable composable `OptoContentWidth` that constrains content width and centers it:

```kotlin
@Composable
fun OptoContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 640.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().widthIn(max = maxWidth).align(Alignment.CenterHorizontally),
        content = content
    )
}
```

Then wrap each screen's inner content with this, replacing the current `padding(horizontal = 16.dp)` approach.

**Pros:**
- Minimal code changes per screen (wrap existing content in `OptoContentWidth`)
- Single point of max-width control
- Zero new dependencies
- Works immediately on tablets
- Does not require WindowSizeClass or configuration detection
- Existing padding pattern (`padding(horizontal = 16.dp)`) moves to the wrapper
- All screens benefit equally

**Cons:**
- Single max-width for all screens doesn't differentiate between form screens (wider ok) and list screens (wider = worse)
- Doesn't solve master-detail or multi-column layouts
- Doesn't address the drawer being modal vs permanent
- Wastes space on tablets (content is centered but still single-column)

**Effort: Medium** (~1-2 days for implementation + testing)
- Create `OptoContentWidth` composable (1 file)
- Modify ~25 screens to use it (replace padding pattern)
- Update OptoCard to optionally respect max-width
- Update tests for any layout changes

---

### Approach B: Adaptive Layout Strategy (Moderate Invasive)

Use `BoxWithConstraints` or `LocalConfiguration.current.screenWidthDp` to detect screen size and apply adaptive behaviors:

- **Small screens (< 600dp)**: Current behavior
- **Medium screens (600-840dp)**: Max-width content with centered layout
- **Large screens (> 840dp)**: Multi-column where appropriate, permanent drawer

```kotlin
// Reusable screen size detection
enum class ScreenSize { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberScreenSize(): ScreenSize {
    val config = LocalConfiguration.current
    return when {
        config.screenWidthDp < 600 -> ScreenSize.COMPACT
        config.screenWidthDp < 840 -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
}
```

**Pros:**
- Truly adaptive: different layouts per screen size
- Can enable master-detail on tablets (patient list + detail side by side)
- Drawer can become permanent navigation on tablets
- Multi-column lists on large screens
- Professional-grade responsive behavior

**Cons:**
- More complex to implement per screen
- Risk of regression on existing phone layouts
- Each screen may need individual adaptation logic
- New composable helpers needed for common patterns (responsive column, responsive grid)
- Testing needs device-configuration matrix coverage

**Effort: High** (~1-2 weeks for full implementation)
- Create responsive utilities: ScreenSize detection, responsive spacing/shapes
- Modify MainDrawerScreen for adaptive drawer
- Modify ~15 list/detail screens for adaptive content
- Modify form screens for multi-column on tablets
- Update OptoCard for responsive sizing
- Comprehensive testing across configurations

---

### Approach C: Hybrid — Content Width Wrapper + Selective Adaptation (Recommended)

**Two-phase approach:**

**Phase 1 (Foundation):** Apply Approach A's `OptoContentWidth` to all screens with configurable max-width per screen type:
- Auth screens: 420dp (already done in LoginScreen)
- Form screens: 600dp (comfortable form width)
- List screens: 480dp (readable list width)
- Dashboard screens: 640dp (more room for data)

**Phase 2 (Enhancement):** Add device-size detection for:
- Drawer: modal on compact, permanent side-sheet on expanded
- List screens: 1 column on compact, staggered/2-column on expanded
- Dashboard: `Arrangement.spacedBy(8.dp)` Row → adaptive grid
- Master-detail: patient list → list + detail side by side on expanded

**Pros:**
- Phase 1 delivers immediate tablet improvement with low risk
- Phase 2 is optional and incremental
- Screen-type-specific max-widths are more intelligent than one-size-fits-all
- Progressive enhancement — no breaking changes
- Can be split into independent PRs
- Testable in isolation

**Cons:**
- Two phases means longer total timeline
- Phase 2 still carries complexity
- Need to decide what "done" means for Phase 1 vs Phase 2

**Effort: Phase 1 Low-Medium (~2-3 days), Phase 2 Medium-High (~1 week)**

---

## Recommendation

**Go with Approach C — Hybrid (Content Width Wrapper + Selective Adaptation).**

### Why:

1. **Phase 1 alone solves 80% of the tablet problem.** The biggest visual issue right now is unconstrained content stretching across the entire screen. A max-width wrapper with centering immediately makes the app look intentional on tablets.

2. **Zero new dependencies.** No `WindowSizeClass`, no `material3-adaptive`, no config changes. Just a composable wrapper and removing/replacing some `fillMaxWidth()` calls.

3. **Incremental delivery.** Phase 1 can ship in days, not weeks. The `ModalNavigationDrawer` remains modal in Phase 1 — that's fine for an MVP-responsive state.

4. **Screen-type max-widths handle the diversity.** Forms can be wider (600dp), lists narrower (480dp), dashboards widest (640dp). This avoids the one-size-fits-all trap.

5. **Phase 2 is optional** and only needed if users explicitly request multi-column or master-detail on tablets.

### Specific Phase 1 Plan:

1. Create `ui/components/OptoContentWidth.kt` — a `Column` with `widthIn(max = ...)`, `fillMaxWidth()`, centered via `Modifier.align(Alignment.CenterHorizontally)` in a parent `Box`
2. Create `ui/components/OptoScreenLayout.kt` — combines Scaffold padding + content width + scroll
3. Update each screen progressively (starting with auth screens, then lists, then forms)
4. Update `OptoCard` default modifier to accept content width context
5. No changes to drawer, navigation, or layout structure

### Phase 2 Candidates (deferred):

- Adaptive drawer (permanent on >840dp)
- Patient list + detail side-by-side
- Multi-column LazyGrid on large screens
- Adaptive spacing token (`OptoTokens.spacing.responsive(compact, medium, expanded)`)

---

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Phase 1 changes break existing phone layouts | Low | High | Wrap-only changes preserve behavior on small screens since `widthIn(max = X)` on a 360dp-wide phone is a no-op |
| Content width wrapper adds nested layout | Low | Low | Single extra Box/Column — negligible performance impact |
| OptoTokens spacing changes affect form density | Low | Medium | Keep existing spacing; responsive tokens only added in Phase 2 |
| NavigationBarsPadding inconsistency | Medium | Low | Fix as part of the wrapper — standardize the padding pattern |
| Tests need updates for new composable wrappers | Medium | Low | Layout-only changes, semantics preserved — mostly test tag location adjustments |
| Feature creep into Phase 2 during Phase 1 | Medium | Medium | Strictly scope Phase 1 to max-width + centering only. No drawer, no grid, no master-detail |
| Existing `WindowInsets(0,0,0,0)` pattern conflicts with content wrapper | Low | Low | Wrapper works inside the Scaffold content; insets handled at the Scaffold level |

---

## Ready for Proposal

Yes. The exploration is complete and the recommendation is clear.

The orchestrator should inform the user:
- **Current state**: 31 screens, all stretch to fillMaxWidth, zero responsive adaptation
- **Recommendation**: Phase 1 — content width wrapper with screen-type max-widths (2-3 days)
- **Phase 2**: Adaptive layouts (deferred, ~1 week if needed)
- **Minimal risk**: The approach preserves phone behavior since `widthIn(max = X)` is a no-op on narrow screens
