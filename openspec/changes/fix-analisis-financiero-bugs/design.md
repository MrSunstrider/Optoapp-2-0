# Design: fix-analisis-financiero-bugs

## Technical Approach

### Bug 2 — Recommendation Feedback UI

Wire `feedbacksEnviados` from `AnalisisNegocioUiState` into `RecomendacionCard` to show visual confirmation after feedback. Add `feedbackErrorRecId` to support inline per-card errors when the save fails.

Three visual states per card:
1. **No feedback yet** — buttons visible (current behavior, unchanged)
2. **Success** — button row replaced by checkmark + "Gracias por tu valoración"
3. **Error** — buttons stay enabled; inline red text below buttons

### Bug 1 — GastosViewModel Test Coverage

Use a real in-memory Room DB (`Room.inMemoryDatabaseBuilder`) with a real `GastoOperativoDao` + real `OptoRepository`, mirroring the pattern from `OptoRepositoryFinanzasTest`. This gives us a working DAO Flow pipeline without mocking the entire data layer. For diagnostic logging, add `Log.d` call in `GastosViewModel.init` and verify with MockK slot capture.

---

## Architecture Decisions

### AD-1: Pass `feedbacksEnviados` as parameter to RecomendacionCard

**Decision**: Add `feedbacksEnviados: Map<String, Boolean>` parameter to `RecomendacionCard`.

- **Why**: Follows existing unidirectional data flow pattern (state down from Screen → Card). The Screen already collects `uiState` and has access to `feedbacksEnviados`. No ViewModel changes needed for this part.
- **Alternative rejected**: Card-local state with its own ViewModel call. Breaks source-of-truth — card wouldn't know about feedbacks sent from other parts of the screen.
- **Alternative rejected**: `ViewModel`-level conversion to `Set<String>`. Unnecessary indirection; the Map is already available and efficiently checked by `contains(rec.id)`.

### AD-2: Add `feedbackErrorRecId` field for inline errors

**Decision**: Add `feedbackErrorRecId: String? = null` to `AnalisisNegocioUiState`. The ViewModel sets it on feedback failure and clears it on success. `RecomendacionCard` receives it as a parameter.

- **Why**: REQ-FEEDBACK-UI-3 requires inline errors near the card. The current global `error` banner is too far. Tracking per-card errors at the ViewModel level preserves unidirectional flow — the ViewModel is the source of truth for error state.
- **Alternative rejected**: No inline errors, keep only global banner. Violates spec.
- **Alternative rejected**: Card manages its own `mutableStateOf<Boolean?>` for error. Card can't know when a retry succeeds because the error is set by ViewModel during the coroutine — race condition on clearing.

### AD-3: Real in-memory Room DB for GastosViewModel tests

**Decision**: Use `Room.inMemoryDatabaseBuilder` + real `OptoDatabase` + real `OptoRepository` in `GastosViewModelTest`, following the `OptoRepositoryFinanzasTest` pattern.

- **Why**: `allGastos` is populated via `repository.getGastosOperativos(opticaId)` which returns a Room Flow. A mocked Flow can't simulate the DAO → VM data pipeline. A real DAO guarantees the test exercises the actual code path.
- **Alternative rejected**: Mock `repository.getGastosOperativos` with `flowOf(seededList)`. Doesn't test that `save()` → DAO upsert → Flow emission works end-to-end.
- **Tradeoff**: More setup code (~30 lines) vs mocked approach (~5 lines). Worth it for realistic data flow verification.

### AD-4: Diagnostic log without adding Log import to GastosViewModel

**Decision**: Add `android.util.Log.d(TAG, ...)` call in `GastosViewModel.init` with a companion `TAG`. Verify with MockK slot capture in tests.

- **Why**: Debug logging in init helps diagnose empty-gastos issues without spamming production output (DEBUG level, filtered out by default on release builds).
- **Alternative rejected**: Add a callback/event for logging. Overengineered for a simple diagnostic.

---

## Data Flow — feedbacksEnviados

```
User taps "Útil"
  → AnalisisNegocioViewModel.onFeedback(recId, fueUtil=true)
    → FeedbackRecomendacionUseCase.marcarUtil()  // saves to Room
    → _uiState.update { feedbacksEnviados + (recId to true) }
    → StateFlow emits new AnalisisNegocioUiState
      → Screen collects uiState
        → RecomendacionCard(
             rec = rec,
             onFeedback = { ... },
             feedbacksEnviados = uiState.feedbacksEnviados,
             feedbackErrorRecId = uiState.feedbackErrorRecId,
           )
          → feeds feedbacksEnviados.contains(rec.id)?
              YES → render confirmation message (checkmark + text)
              NO  → render buttons (default)
          → feeds feedbackErrorRecId == rec.id?
              YES → render inline error below buttons
              NO  → hide inline error
```

On error:
```
FeedbackRecomendacionUseCase throws
  → _uiState.update { feedbackErrorRecId = recId }
  → Card shows inline error, buttons remain enabled
On retry success:
  → feedbackErrorRecId cleared, feedbacksEnviados updated
  → Card switches from error state to confirmation state
```

---

## File Changes

| File | Change | Lines |
|------|--------|-------|
| `optoapp/src/main/java/.../ui/.../AnalisisNegocioScreen.kt` | Add `feedbacksEnviados` param to `RecomendacionCard` call site (line 162-165). Add conditional rendering inside `RecomendacionCard` for success/error states. Add `feedbackErrorRecId` param to card. | ~20 |
| `optoapp/src/main/java/.../viewmodel/AnalisisNegocioViewModel.kt` | Add `feedbackErrorRecId: String?` to `AnalisisNegocioUiState`. Update `onFeedback()` to set/clear it. | ~5 |
| `optoapp/src/main/java/.../viewmodel/GastosViewModel.kt` | Add `TAG` companion, import `Log`, add `Log.d` in `init` block when `gastos.isEmpty()` on first emission. | ~8 |
| `optoapp/src/test/java/.../viewmodel/GastosViewModelTest.kt` | Add real Room DB setup + 5 new test methods. Keep existing MockK tests for delete error. | ~100 |

---

## Testing Strategy

| Test | Type | Approach |
|------|------|----------|
| Feedback success: buttons replaced by confirmation | Compose UI test | Render `RecomendacionCard` with `feedbacksEnviados = mapOf("r1" to true)`. Assert `ThumbUp`/`ThumbDown` not displayed; assert "Gracias por tu valoración" + CheckCircle icon visible. |
| Feedback error: inline error shown, buttons stay | Compose UI test | Render card with `feedbacksEnviados = emptyMap()`, `feedbackErrorRecId = "r1"`. Assert buttons visible and enabled; assert "No se pudo enviar tu valoración" visible. |
| Other cards unaffected | Compose UI test | Render two cards, first with `feedbacksEnviados = mapOf("r1" to true)`. Assert card 1 shows confirmation; card 2 shows buttons. |
| save() emits through allGastos | ViewModel unit test | Setup in-memory Room + real repo. Pre-insert existing gastos. Call `viewModel.save(nuevoGasto)`. Assert `viewModel.allGastos` contains nuevoGasto after emission. |
| save() sets error on failure | ViewModel unit test | Use mocked repo that throws on `upsertGastoOperativo`. Call `viewModel.save()`. Assert `uiState.error == "Error al guardar"`. |
| delete() removes from allGastos | ViewModel unit test | In-memory Room + real repo. Pre-insert gasto. Call `viewModel.delete()`. Assert `allGastos` no longer contains it. |
| Diagnostic log on empty init | ViewModel unit test | In-memory Room with empty DB. Capture `Log.d` slot. Assert message contains "GastosViewModel", `opticaId`, and "0 gastos". |
| No diagnostic log on non-empty init | ViewModel unit test | In-memory Room with pre-inserted gastos. Verify `Log.d` is NOT called with empty-gastos message. |

---

## Open Questions

1. **Monthly filter test (REQ-GASTOS-TEST-2)**: The ViewModel doesn't filter by month — filtering happens in the Screen composable. Two options: (a) test `allGastos` at ViewModel level for data integrity (expense has correct `fecha`), or (b) test monthly filtering as a Compose UI test. Decision: (a) — simpler, follows spec instruction for ViewModel-level tests. Verify saved expense `fecha` matches expected month.

2. **save() error test with real repo**: With a real in-memory DB, `upsertGastoOperativo` won't throw (SQLite errors are rare). For REQ-GASTOS-TEST-1 save-error scenario, we should use MockK for the repository to simulate the failure — same as existing `delete_when_repository_fails_sets_error_state`. This means the save tests split: save-success uses real repo, save-error uses mocked repo.

