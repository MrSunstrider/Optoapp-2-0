# Spec: cleanup-dead-code

## Behavior to Preserve

### AppointmentReminderWorker

- GIVEN AppointmentReminderWorker fires a notification
- WHEN POST_NOTIFICATIONS permission is not granted on Android 13+
- THEN the worker SHALL check permission before posting (current behavior: crashes or silently fails)
- AND when permission IS granted, notification SHALL fire with identical content and timing
- AND worker scheduling SHALL remain unchanged

### SecurityManager PIN logic

- GIVEN SecurityManager.kt has PIN migration logic
- WHEN user has legacy PIN format
- THEN migration SHALL convert to new format without data loss
- AND PIN validation SHALL produce identical pass/fail results for all inputs
- AND PIN change flow SHALL function identically

### PlayBillingManager / SubscriptionManager

- GIVEN PlayBillingManager.kt and SubscriptionManager.kt are dead code
- WHEN they are removed or marked @Deprecated
- THEN the build SHALL succeed without references to removed classes
- AND no runtime behavior SHALL change (no caller uses these classes)

### Type.kt typography

- GIVEN Type.kt defines typography scale
- WHEN the typography scale is completed to include all text styles used in the app
- THEN all existing text styles SHALL remain identical (no visual changes)
- AND new styles (if any) SHALL only apply to text that previously used default/inline styles

### SyncCancellation + SyncGate merge

- GIVEN SyncCancellation.kt and SyncGate.kt are separate files with related logic
- WHEN merged into a single module
- THEN all cancellation checks SHALL produce identical results
- AND gate logic SHALL permit/block sync under identical conditions
- AND public API surface SHALL be a superset of both original APIs

### WhatsAppUtils + FileShareUtils merge

- GIVEN WhatsAppUtils.kt and FileShareUtils.kt exist separately
- WHEN WhatsAppUtils is merged into FileShareUtils
- THEN all share intents SHALL produce identical actions
- AND file paths, MIME types, and extras SHALL be identical

### OnboardingOpticaScreen

- GIVEN OnboardingOpticaScreen.kt is unused
- WHEN it is removed or archived
- THEN no navigation route SHALL reference it
- AND the build SHALL succeed

## Acceptance Criteria

- [ ] AppointmentReminderWorker checks POST_NOTIFICATIONS on API 33+ before posting
- [ ] SecurityManager PIN migration handles legacy format correctly
- [ ] PlayBillingManager and SubscriptionManager removal causes no compile errors
- [ ] Type.kt typography changes produce no visual differences
- [ ] SyncCancellation + SyncGate merged API covers all callers
- [ ] WhatsAppUtils merged into FileShareUtils with identical share behavior
- [ ] OnboardingOpticaScreen removal leaves no dangling references
- [ ] `./gradlew assembleDebug` succeeds after all changes
