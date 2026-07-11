# Delta Spec: Recommendation Feedback UI

## Context

This delta amends the existing `openspec/specs/recomendaciones/spec.md`. The data layer (R10: `FeedbackRecomendacionEntity` + DAO, R11: `FeedbackRecomendacionUseCase`) already works — feedback saves correctly via `@Upsert` and is idempotent. What's missing is **visual confirmation** in the `RecomendacionCard` composable after the user taps "Útil" or "No me sirve".

The ViewModel already populates `feedbacksEnviados: Map<String, Boolean>` in `AnalisisNegocioUiState`. This delta requires the UI to consume that state.

---

## Requirements

### REQ-FEEDBACK-UI-1: Visual Confirmation After Feedback

After a user taps "Útil" or "No me sirve" on a recommendation card, the card MUST show immediate visual confirmation that the feedback was registered.

#### Scenario: Tap "Útil" shows confirmation

```
GIVEN a RecomendacionCard is displayed with feedback buttons visible
 WHEN the user taps the "Útil" button
 AND the feedback save succeeds
 THEN the buttons SHALL be replaced or overlaid with a confirmation message
  AND the confirmation MUST include the text "Gracias por tu valoración"
  AND a checkmark icon SHALL appear next to the confirmation text
```

#### Scenario: Tap "No me sirve" shows confirmation

```
GIVEN a RecomendacionCard is displayed with feedback buttons visible
 WHEN the user taps the "No me sirve" button
 AND the feedback save succeeds
 THEN the card SHALL display the same confirmation message "Gracias por tu valoración"
  AND a checkmark icon SHALL appear
```

---

### REQ-FEEDBACK-UI-2: Buttons Disabled After Submission

After feedback is sent for a recommendation, the card MUST disable both feedback buttons to prevent duplicate submissions for the same recommendation.

#### Scenario: Buttons are disabled post-feedback

```
GIVEN a RecomendacionCard has just sent feedback for recommendation R1
 WHEN the card re-renders after feedbackEnviados is updated
 THEN both the "Útil" and "No me sirve" buttons for that card SHALL be disabled
  AND the buttons SHOULD use a visually distinct disabled style (reduced opacity, grey tint)
  AND clicking a disabled button SHALL NOT trigger onFeedback
```

#### Scenario: Other cards remain interactive

```
GIVEN the screen has two recommendation cards, R1 and R2
 WHEN feedback is sent for R1 only
 THEN R1's buttons are disabled
  AND R2's buttons remain enabled and interactive
```

**Note**: `feedbacksEnviados` is a `Map<String, Boolean>` keyed by recommendation `id`. The check is `feedbacksEnviados.contains(rec.id)` — deterministic, no ViewModel changes needed.

---

### REQ-FEEDBACK-UI-3: Inline Error on Feedback Failure

If sending feedback for a specific recommendation fails, an inline error message MUST appear inside or directly adjacent to that recommendation card.

#### Scenario: Feedback save failure shows inline error

```
GIVEN a RecomendacionCard is displayed
 WHEN the user taps "Útil"
 AND the FeedbackRecomendacionUseCase throws an exception
 THEN the card SHALL display an inline error message containing "No se pudo enviar tu valoración"
  AND the inline error SHALL be visually distinct from the global screen-level error banner
  AND the buttons SHALL remain enabled so the user can retry
```

#### Scenario: Error clears on successful retry

```
GIVEN an inline error is displayed after a failed feedback attempt
 WHEN the user taps the same button again
 AND the retry succeeds
 THEN the inline error SHALL disappear
  AND the confirmation message SHALL appear
```

---

## Out of Scope

- Feedback toggle or change of vote (allow user to switch from "Útil" to "No me sirve"): not in scope for this bugfix.
- Global error banner changes: the existing screen-level error banner remains. This delta adds a **local** error indicator near the card.
- Animations or transitions: confirmation should appear immediately (state-driven), no animation contract.
- Data layer changes: `FeedbackRecomendacionEntity`, `FeedbackRecomendacionDao`, `FeedbackRecomendacionUseCase` are unchanged.

---

## Test Type

All scenarios SHALL be verified via **Compose UI tests** (Robolectric + Compose Test) that render `RecomendacionCard` with controlled `feedbacksEnviados` state and verify the rendering of buttons, confirmation text, and error text.
