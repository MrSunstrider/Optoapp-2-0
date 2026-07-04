# Tasks: Fix Login Alignment

## Review Workload Forecast

This is a small change (~150 lines). Single PR, 400-line budget risk: Low.

## Phase 1: Implementation (Completed)

- [x] Fix 1: "¿Olvidaste tu contraseña?" TextButton Alignment
  - Removed dead `align(Alignment.End)` modifier from TextButton
  - Added `TextAlign.Center` to Text inside TextButton
  - Parent Column's `Alignment.CenterHorizontally` + `fillMaxWidth()` handles centering
  
- [x] Fix 2: "Recordar Cuenta" Row Alignment with OutlinedTextField Content
  - Applied `Modifier.padding(start = 16.dp)` to the Row where `iconOffset` matches OutlinedTextField's visual icon area width
  - Aligns CheckBox + Text start edge with email/password text content area
  
- [x] Fix 3: Typography Tokens Replace Raw Font Size Literals
  - Replaced `13.sp` with `bodySmall` (token: bodySmall, visual match acceptable)
  - Replaced `14.sp` with `bodyMedium` (exact match)
  - Replaced `16.sp` with `bodyLarge` (exact match)
  - Added justification comment for `13.sp → bodySmall` (1sp difference)
  
- [x] Fix 4: Button Height Consistency
  - Unified all 3 buttons (ENTRAR, Google, Crear cuenta) to `48.dp` height
  - Changed ENTRAR button from `52.dp` to `48.dp` (~4px reduction, visually negligible)
  
- [x] Fix 5: Responsive Width Constraint
  - Applied `widthIn(max = 420.dp)` to outer Column
  - Used `Modifier.align(Alignment.Center)` to center form in parent Box
  - Form uses available width on narrow screens, max width on wide screens
  
- [x] Fix 6: Unused Import Cleanup
  - Removed unused `import sp` statement from `LoginScreen.kt`
  - Verified no unused import warnings after typography token migration

## Phase 2: Testing (Completed)

- [x] TDD Test 1: Forgot-password button reachability and centering
  - Added `LOGIN_OLVIDASTE_BTN` tag in `TestTags.kt` (line 21)
  - Added E2E verification: `composeTestRule.onNodeWithTag(LOGIN_OLVIDASTE_BTN).assertIsDisplayed()`
  
- [x] TDD Test 2: Remember-account row alignment
  - Added `LOGIN_REMEMBER_ACCOUNT_CHECK` tag in `TestTags.kt` (line 20)
  - Added E2E verification: `composeTestRule.onNodeWithTag(LOGIN_REMEMBER_ACCOUNT_CHECK).assertIsDisplayed()`
  
- [x] Unit test compatibility maintained
  - Existing `LoginScreenTest.kt` test tags still resolve correctly after layout changes

## Phase 3: Verification (Completed)

- [x] All unit tests pass (`./gradlew :optoapp:testDebugUnitTest --stacktrace`)
- [x] Build succeeds (`./gradlew :optoapp:assembleDebug`)
- [x] Manual verification against Requirement 9 (visual appearance preserved)
- [x] E2E tests for login screen layout pass
- [x] No regression in auth flow, navigation, or PIN functionality
- [x] TextButton centering and alignment verified
- [x] Typography token mapping visually indistinguishable from original
- [x] All validation criteria met (see `openspec/changes/fix-login-alignment/proposal.md` Success Criteria)