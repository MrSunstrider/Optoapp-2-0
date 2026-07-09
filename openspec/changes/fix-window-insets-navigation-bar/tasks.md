## Tasks: fix-window-insets-navigation-bar

### T1: Add navigationBarsPadding to ReportesScreen LazyColumn
- **File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/ReportesScreen.kt`
- **Line**: ~127
- **Change**: Append `.navigationBarsPadding()` to the LazyColumn modifier chain, after `.fillMaxSize()` and before the trailing comma:
  ```kotlin
  modifier = Modifier
      .padding(padding)
      .padding(horizontal = 16.dp)
      .fillMaxSize()
      .navigationBarsPadding(),
  ```
- **Import**: Already present (`import androidx.compose.foundation.layout.*`)
- **Verify**: Build succeeds; scrollable content bottom items visible above nav bar
- **Depends on**: None

### T2: Add navigationBarsPadding to PacientesListScreen wrapper Column
- **File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/PacientesListScreen.kt`
- **Line**: ~112
- **Change**: Append `.navigationBarsPadding()` to the wrapper Column modifier chain (the Column inside the Scaffold content lambda, which wraps the search field + LazyColumn), after `.padding(horizontal = 16.dp)`:
  ```kotlin
  modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).navigationBarsPadding()
  ```
- **Import**: Already present (`import androidx.compose.foundation.layout.*`)
- **Verify**: Build succeeds; patient list fully visible above nav bar
- **Depends on**: None

### T3: Add navigationBarsPadding to DetallePacienteScreen content Column
- **File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/DetallePacienteScreen.kt`
- **Line**: ~186
- **Change**: Append `.navigationBarsPadding()` to the inner content Column modifier (the Column inside the Scaffold content lambda with tab content), after `.padding(padding)`:
  ```kotlin
  modifier = Modifier
      .fillMaxSize()
      .padding(padding)
      .navigationBarsPadding()
  ```
- **Import**: Already present (`import androidx.compose.foundation.layout.*`)
- **Verify**: Build succeeds; tab content fully visible above nav bar
- **Depends on**: None

### T4: Add navigationBarsPadding to GastosScreen LazyColumn
- **File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/GastosScreen.kt`
- **Line**: ~65
- **Change**: Append `.navigationBarsPadding()` to the LazyColumn modifier chain, after `.padding(padding)`:
  ```kotlin
  modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding()
  ```
- **Import**: Already present (`import androidx.compose.foundation.layout.*`)
- **Verify**: Build succeeds; expense list fully visible above nav bar
- **Depends on**: None

### T5: Add navigationBarsPadding to ServiciosExtraScreen SnackbarHost
- **File**: `optoapp/src/main/java/com/example/optoapp/ui/screens/ServiciosExtraScreen.kt`
- **Line**: ~94
- **Change**: Add `modifier = Modifier.navigationBarsPadding()` to the SnackbarHost composable in the snackbarHost lambda:
  ```kotlin
  snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
  ```
- **Note**: `SnackbarHost` uses `hostState` named parameter — keep it explicit to avoid ambiguity when adding the `modifier` parameter.
- **Import**: Already present (`import androidx.compose.foundation.layout.*`)
- **Verify**: Build succeeds; snackbar messages render above nav bar
- **Depends on**: None

---

### Review Workload Guard

**Decision needed before apply**: No
**Chained PRs recommended**: No
**400-line budget risk**: Low — ~5 lines changed total (1 per file), each task is a single modifier append.
**Strategy**: Single PR — all 5 tasks fit in a single commit well under 400 lines. Each is an independent modifier append to a pre-existing modifier chain with zero logic change. Rollback is a single revert.
