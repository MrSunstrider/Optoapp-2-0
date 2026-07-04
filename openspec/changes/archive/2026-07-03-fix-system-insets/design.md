# Design: Fix system insets (content behind status bar)

## Technical Approach

Three-layer fix targeting the root cause and two secondary sites:

1. **Component-level** (`OptoTopAppBar`): Replace `windowInsets = WindowInsets(0, 0, 0, 0)` with `TopAppBarDefaults.windowInsets`. This propagates correct status bar insets to all ~25 screens using this component automatically.
2. **Scaffold-level** (`ConfiguracionScreen`, `AgendaScreen`): Remove explicit `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from the Scaffold call, letting Material 3 defaults apply.
3. **Non-Scaffold screens** (`LoginScreen`, `PinScreen`, `CreatePinScreen`): Add `Modifier.statusBarsPadding()` to the root `Box` modifier since these screens lack a Scaffold wrapper and cannot inherit insets.

`SeleccionOpticaScreen` uses `OptoTopAppBar` without zeroing Scaffold insets — step 1 alone fixes it.

## Architecture Decisions

### Decision: Fix component root cause vs per-screen insets

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Fix `OptoTopAppBar` once | Touches 1 file, fixes ~25 screens | **Chosen** |
| Add `statusBarsPadding` per screen | Touches ~25 files, fragile to new screens | Rejected |

**Rationale**: The zeroed insets in `OptoTopAppBar` is the root cause. Material 3 `CenterAlignedTopAppBar` already supports `TopAppBarDefaults.windowInsets` — we simply restore the default.

### Decision: Remove explicit `contentWindowInsets` vs replace with `ScaffoldDefaults`

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Remove parameter entirely | Cleaner, Scaffold defaults apply automatically | **Chosen** |
| Replace with `ScaffoldDefaults.contentWindowInsets` | Explicit but redundant | Rejected |

**Rationale**: Omitting the parameter lets Scaffold use its built-in default (`ScaffoldDefaults.contentWindowInsets`), which is equivalent and simpler.

### Decision: `statusBarsPadding()` on root Box for non-Scaffold screens

| Option | Tradeoff | Decision |
|--------|----------|----------|
| `statusBarsPadding()` on root `Box` | Simple, proven pattern (already used in `MainDrawerScreen`) | **Chosen** |
| Wrap in Scaffold just for insets | Overhead, changes visual structure | Rejected |
| Wrap in `WindowInsets` composable | More complex, unnecessary for single-padding case | Rejected |

**Rationale**: These screens use centered `Box` layouts. Adding `statusBarsPadding()` to the root modifier pushes content below the status bar without restructuring the composable tree. Already validated by `MainDrawerScreen` using the same pattern.

## Data Flow

```
WindowCompat.setDecorFitsSystemWindows(window, false)
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
OptoTopAppBar    Scaffold        Non-Scaffold
(uses M3 insets) (inherits M3)   (statusBarsPadding)
    │               │               │
    ▼               ▼               ▼
~25 screens      Config,Agenda   Login, Pin, CreatePin
    │               │               │
    └───────────────┴───────────────┘
                    │
            Content below status bar
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/components/OptoTopAppBar.kt` | Modify | Line 44: replace `WindowInsets(0, 0, 0, 0)` with `TopAppBarDefaults.windowInsets` |
| `ui/screens/ConfiguracionScreen.kt` | Modify | Line 131: remove `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from Scaffold |
| `ui/screens/AgendaScreen.kt` | Modify | Line 122: remove `contentWindowInsets = WindowInsets(0, 0, 0, 0)` from Scaffold |
| `ui/screens/LoginScreen.kt` | Modify | Line 112: add `.statusBarsPadding()` to root Box modifier chain |
| `ui/screens/PinScreen.kt` | Modify | Line 33: add `.statusBarsPadding()` to root Box modifier chain |
| `ui/screens/CreatePinScreen.kt` | Modify | Line 33: add `.statusBarsPadding()` to root Box modifier chain |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Compose preview rendering at different screen sizes | Not feasible for insets — manual verification |
| Visual | Status bar does not overlap content on every affected screen | Manual: emulator with edge-to-edge enabled, verify top padding |
| Regression | `MainDrawerScreen` (already correct) unaffected | Visual check on emulator |
| Build | Debug APK assembles without crash | `./gradlew :optoapp:assembleDebug` |

No unit tests for this change — inset behavior is a system-level Compose rendering concern that cannot be asserted in Robolectric. The spec's Given/When/Then scenarios are verified via manual visual inspection on a real device or emulator.

## Migration / Rollout

No migration required. Pure UI layout fix — no data, schema, or API changes.

## Open Questions

None. The fix is mechanically straightforward and follows existing patterns (`MainDrawerScreen` already uses `statusBarsPadding()`).
