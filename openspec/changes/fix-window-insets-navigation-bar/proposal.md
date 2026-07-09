## Proposal: Fix navigation bar insets across screens

### Intent

Snackbars and scrollable content render behind the device navigation bar on 5 screens because zeroed `WindowInsets` remove system bar insets from Scaffolds. Add `.navigationBarsPadding()` to affected composables — same pattern already proven in 2 fixed screens and 7 correctly-handled screens.

### Scope

**In Scope:**
- ReportesScreen — LazyColumn modifier
- PacientesListScreen — wrapper Column modifier
- DetallePacienteScreen — content Column modifier
- GastosScreen — LazyColumn modifier
- ServiciosExtraScreen — SnackbarHost modifier (bonus: LazyColumn already fixed)

**Out of Scope:**
- Removing `contentWindowInsets = WindowInsets(0,0,0,0)` from Scaffolds (Approach A deferred)
- 7 screens already using `.navigationBarsPadding()` — no change needed
- Supabase schema, RLS, edge functions, web companion

### Capabilities

- **New**: None
- **Modified**: None — pure implementation fix, no spec-level behavior change. Existing `system-insets` spec already requires Scaffolds to not zero out insets.

### Approach

Chosen **Approach B: Add `.navigationBarsPadding()` only where needed**. Targeted modifier additions on scrollable content containers and snackbar hosts. Low risk, zero regression, no layout assumption changes.

#### Fixes

| # | File | Target | Add `.navigationBarsPadding()` after |
|---|------|--------|--------------------------------------|
| 1 | `ReportesScreen.kt` | LazyColumn modifier (line 124) | `.fillMaxSize()` |
| 2 | `PacientesListScreen.kt` | Wrapper Column modifier (line 111) | `.padding(horizontal = 16.dp)` |
| 3 | `DetallePacienteScreen.kt` | Content Column modifier (line 183-185) | `.padding(padding)` |
| 4 | `GastosScreen.kt` | LazyColumn modifier (line 64-65) | `.padding(padding)` |
| 5 | `ServiciosExtraScreen.kt` | SnackbarHost modifier (line 94) | End of modifier chain |

All files under `optoapp/src/main/java/com/example/optoapp/ui/screens/`.

### Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Padding changes visible layout on some devices | Low | Same pattern used in 7 other screens — proven. No visual-only test assertions exist. |
| Missed import for `.navigationBarsPadding()` | Low | Comes from `androidx.compose.foundation.layout.*` already imported in all files. |

### Rollback

Revert commit. Pure modifier additions — no logic, data, or behavior changes. One commit per screen or one commit for all.

### Dependencies

None.

### Success Criteria

- [ ] ReportesScreen LazyColumn bottom items visible above nav bar
- [ ] PacientesListScreen patient list fully visible above nav bar
- [ ] DetallePacienteScreen tab content fully visible above nav bar
- [ ] GastosScreen expense list fully visible above nav bar
- [ ] ServiciosExtraScreen snackbar messages visible above nav bar
- [ ] `./gradlew :optoapp:assembleDebug` succeeds
