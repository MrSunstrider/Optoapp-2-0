## Exploration: WindowInsets snackbar/nav-bar fix across all screens

### Current State

The app uses `contentWindowInsets = WindowInsets(0, 0, 0, 0)` in 13 Scaffold screens and `WindowInsets(0.dp)` in 2 screens. This zeroes out system bar insets, causing snackbars, bottom content, and FABs to render BEHIND the device's navigation bar buttons.

Two screens have been fixed as proof of concept:
- **ServiciosExtraScreen.kt** (ALREADY FIXED): Added `.navigationBarsPadding()` to LazyColumn and FAB
- **NuevoServicioScreen.kt** (ALREADY FIXED): Added `.navigationBarsPadding()` to SnackbarHost and scrollable Column

### Affected Screens — Detailed Audit

| # | Screen | Has Snackbar | Snackbar Fixed? | Scroll Content | Scroll Fixed? | FAB | FAB Fixed? | Needs Fix? |
|---|--------|-------------|-----------------|----------------|---------------|-----|------------|------------|
| 1 | ServiciosExtraScreen | Yes | Partial* | LazyColumn | **Yes** | Yes | **Yes** | Partial |
| 2 | NuevoServicioScreen | Yes | **Yes** | Column+scroll | **Yes** | No | N/A | No |
| 3 | ReportesScreen | No | N/A | LazyColumn | **No** | No | N/A | **Yes** |
| 4 | AnalisisDetalleScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |
| 5 | NuevoPacienteScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |
| 6 | NuevaEvaluacionScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |
| 7 | NuevaDispensacionScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |
| 8 | OperacionHoyScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |
| 9 | AnalisisNegocioScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |
| 10 | PacientesListScreen | No | N/A | LazyColumn | **No** | Yes | **Yes** | **Yes** |
| 11 | DetallePacienteScreen | No | N/A | Tab content (inner) | **No** | Yes | **Yes** | **Yes** |
| 12 | GastosScreen | No | N/A | LazyColumn | **No** | Yes | **Yes** | **Yes** |
| 13 | InformacionFinancieraScreen | No | N/A | Column+scroll | **Yes** | No | N/A | No |

*\* ServiciosExtraScreen: LazyColumn and FAB are fixed, but SnackbarHost (line 94) lacks `.navigationBarsPadding()` — the snackbar may still render behind the nav bar.*

### Screens that need fixes (summary)

1. **ReportesScreen.kt** — LazyColumn (lines 123-128) missing `.navigationBarsPadding()`.
   - Modifier at line 124: `Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()`
   - Add `.navigationBarsPadding()` after `.fillMaxSize()`.

2. **PacientesListScreen.kt** — Wrapper Column (lines 111-113) or LazyColumn (lines 179-180) missing `.navigationBarsPadding()`.
   - Column: `Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)`
   - LazyColumn: `Modifier.fillMaxSize()` with `contentPadding = PaddingValues(bottom = 88.dp)` (88dp covers the FAB but not the nav bar)
   - Recommend adding `.navigationBarsPadding()` to the wrapper Column (line 111) since the LazyColumn sits inside and the Column uses `.fillMaxSize()`.

3. **DetallePacienteScreen.kt** — Content Column (lines 183-185) missing `.navigationBarsPadding()`.
   - Modifier: `Modifier.fillMaxSize().padding(padding)`
   - Contains PacienteInfoHeader, ScrollableTabRow, and a Box with tab content (scrollable lists).
   - The tab content at the bottom can be obscured by the nav bar.
   - Add `.navigationBarsPadding()` after `.padding(padding)`.

4. **GastosScreen.kt** — LazyColumn (lines 64-65) missing `.navigationBarsPadding()`.
   - Modifier: `Modifier.fillMaxSize().padding(padding)`
   - Has `contentPadding = PaddingValues(16.dp)` and bottom spacer of 80dp (for FAB), but nav bar can still obscure content.
   - Add `.navigationBarsPadding()` after `.fillMaxSize().padding(padding)`.

### Screens that DON'T need fixes (with reasoning)

1. **AnalisisDetalleScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 88).
2. **NuevoPacienteScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 118).
3. **NuevaEvaluacionScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 206).
4. **NuevaDispensacionScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 93). Uses `WindowInsets(0.dp)` variant.
5. **OperacionHoyScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 95).
6. **AnalisisNegocioScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 83).
7. **InformacionFinancieraScreen.kt** — Already has `.navigationBarsPadding()` on the scrollable Column (line 75). Uses `WindowInsets(0.dp)` variant.
8. **NuevoServicioScreen.kt** (ALREADY FIXED) — SnackbarHost has `.navigationBarsPadding()` (line 58), scrollable Column has `.navigationBarsPadding()` (line 85).

### Additional observation: ServiciosExtraScreen snackbar (already-fixed screen)

While ServiciosExtraScreen is considered fixed for the LazyColumn and FAB, the `SnackbarHost` at line 94 does NOT have `.navigationBarsPadding()`. Snackbar messages (e.g., delete errors at lines 70-75) may still render behind the nav bar. Consider adding `.navigationBarsPadding()` to the SnackbarHost's modifier to fully resolve the issue.

### Pattern for fixes

All 4 screens needing fixes follow the same pattern:
- The content modifier currently has: `Modifier.fillMaxSize().padding(padding)` (or similar)
- Add: `.navigationBarsPadding()` at the end of the modifier chain

The existing imports already include `androidx.compose.foundation.layout.*` and `androidx.compose.material3.*` which contain `WindowInsets` and related APIs, but `.navigationBarsPadding()` comes from `androidx.compose.foundation.layout` which is already imported via `import androidx.compose.foundation.layout.*`.

### Risk: No test coverage concern

None of the affected screens have UI tests that assert on specific padding values. The change is purely a modifier addition. No behavior changes, no logic changes. Low risk.

### Recommendation

Fix all 4 screens in a single PR. The change is mechanical and the same pattern applies everywhere: add `.navigationBarsPadding()` to the scrollable content modifier. This is the same approach already proven in the fixed screens.

### Ready for Proposal

Yes.
