# ViewModel Test Coverage Specification

## Purpose

Add unit test coverage for SettingsViewModel and SubscriptionViewModel, which currently have 0% test coverage. Follow existing test patterns from DispensacionViewModel and PacienteViewModel tests.

## Requirements

### Requirement: SettingsViewModel test coverage

The system MUST have at least 5 unit tests for `SettingsViewModel`. Tests MUST cover the core state transitions and business logic paths.

| Test Category | Minimum Tests | What to Cover |
|--------------|---------------|---------------|
| Initial state | 1 | Default UiState on creation |
| Settings load | 1 | Successful load populates state |
| Settings load failure | 1 | Error state on load failure |
| Setting update | 1 | Optimistic update and persistence |
| Setting update failure | 1 | Rollback or error on persistence failure |

#### Scenario: SettingsViewModel initial state

- GIVEN SettingsViewModel is instantiated
- WHEN no actions are performed
- THEN the UiState MUST reflect loading or default values
- AND no error state MUST be present

#### Scenario: Settings load success

- GIVEN SettingsViewModel is instantiated
- WHEN settings are loaded successfully from repository
- THEN UiState MUST contain the loaded settings
- AND loading indicator MUST be false

#### Scenario: Settings load failure

- GIVEN SettingsViewModel is instantiated
- WHEN settings load throws an IOException
- THEN UiState MUST contain an error message
- AND loading indicator MUST be false

### Requirement: SubscriptionViewModel test coverage

The system MUST have at least 5 unit tests for `SubscriptionViewModel`. Tests MUST cover membership status checks and subscription-related operations.

| Test Category | Minimum Tests | What to Cover |
|--------------|---------------|---------------|
| Initial state | 1 | Default UiState on creation |
| Subscription status check | 1 | Active subscription state |
| Expired subscription | 1 | Expired/cancelled subscription state |
| Subscription error | 1 | Network error during status check |
| Subscription action | 1 | Subscribe/renew action and state transition |

#### Scenario: SubscriptionViewModel initial state

- GIVEN SubscriptionViewModel is instantiated
- WHEN no actions are performed
- THEN the UiState MUST reflect loading state

#### Scenario: Active subscription detected

- GIVEN SubscriptionViewModel is instantiated
- WHEN membership status is fetched and subscription is active
- THEN UiState MUST indicate active subscription with expiry date

#### Scenario: Expired subscription detected

- GIVEN SubscriptionViewModel is instantiated
- WHEN membership status is fetched and subscription is expired
- THEN UiState MUST indicate expired subscription
- AND a renewal prompt MUST be available in state

### Requirement: Test conventions

Tests MUST follow existing project conventions: JUnit 4, MockK for mocking, descriptive camelCase method names, mirror package structure under `app/src/test/java/`.

#### Scenario: Test file structure matches convention

- GIVEN a new test file is created
- WHEN the file is reviewed
- THEN it MUST use JUnit 4 annotations
- AND MockK MUST be used for mocking (not Mockito)
- AND method names MUST be descriptive camelCase
- AND the package MUST mirror the source package
