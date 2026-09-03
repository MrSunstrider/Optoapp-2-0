# Design: fix-dashboard-fab-nav-inset

## ADR-1: Approach B — pad FAB only

**Decision:** Add `Modifier.navigationBarsPadding()` to the Dashboard FAB speed-dial `Column`. Keep `contentWindowInsets = WindowInsets(0, 0, 0, 0)`.

**Why:** Matches proven pattern in Pacientes/Gastos/ServiciosExtra. Avoids re-padding TopAppBar/content that already compensate manually. Lowest regression risk.

**Rejected — Approach A:** Restore default Scaffold `contentWindowInsets` on OperacionHoy. Would require re-validating TopAppBar + scroll padding interaction across density modes.

## Implementation

```kotlin
floatingActionButton = {
    var expanded by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.navigationBarsPadding(),
    ) {
        // AnimatedVisibility mini FABs + main FAB unchanged
    }
}
```

Import already covered by `androidx.compose.foundation.layout.*`.

## Testing

- Unit: source characterization — `OperacionHoyScreen.kt` text between `floatingActionButton` and closing of that slot must contain `navigationBarsPadding`.
- Manual/ADB: after debug install, FAB bottom ≤ `navigationBarBackground` top.

## Risks

| Risk | Mitigation |
|------|------------|
| Double padding if Scaffold later restores insets | Out of scope; document in verify |
| Expanded mini FABs still feel tight | Same padding lifts entire Column |

## Rollback

Revert single modifier + test + spec delta.
