# Compose UI E2E Tests Specification

## Purpose

Define Compose UI tests for 4 P0 user flows using `androidx.compose.ui.test.junit4`. Tests verify UI interactions with fake repositories, no real Supabase calls. Each test runs against `MainActivity` with `createAndroidComposeRule`.

## Requirements

### Requirement: Test Environment Setup

Each test MUST initialize an in-memory Room database before every test method and destroy it after. Tests SHALL use `@get:Rule` with `createAndroidComposeRule<MainActivity>()`. The database builder MUST call `allowMainThreadQueries()`. Each test MUST clean up all data created during execution.

| Concern | Rule |
|---------|------|
| DB lifecycle | `@Before` creates, `@After` destroys |
| Threading | `allowMainThreadQueries()` required |
| Cleanup | Remove all inserted rows or reset DB |
| Fake repos | MUST inject fake repositories via test module |

#### Scenario: Fresh database per test

- GIVEN a new test method starts
- WHEN the test rule creates the Room database
- THEN the database MUST be empty (zero rows in all tables)

#### Scenario: Database destroyed after test

- GIVEN a test has inserted patients and evaluations
- WHEN the test completes (pass or fail)
- THEN the in-memory database MUST be closed and garbage collected

### Requirement: Login + PIN Flow

Tests MUST verify the login-to-PIN happy path. The test SHALL enter valid credentials, tap login, and assert navigation to PIN screen. Tests SHALL use semantic node queries (`onNodeWithText`, `onNodeWithTag`) — not coordinate-based clicks.

#### Scenario: Valid credentials navigate to PIN screen

- GIVEN the user is on the login screen
- WHEN they enter a valid email and password and tap login
- THEN the PIN creation/verification screen MUST become visible within 5 seconds

#### Scenario: Wrong password shows error

- GIVEN the user is on the login screen
- WHEN they enter an invalid password and tap login
- THEN an error message MUST be displayed and navigation MUST NOT occur

#### Scenario: Empty fields prevent submission

- GIVEN the user is on the login screen
- WHEN they tap login without entering credentials
- THEN the login button MUST be disabled or an inline validation message MUST appear

### Requirement: Patient Creation Flow

Tests MUST verify the full patient creation form. The test SHALL fill all required fields, tap save, and assert success. Tests SHALL use inline seed data — no network calls.

#### Scenario: Create patient with all fields

- GIVEN the user is on the new patient screen
- WHEN they fill name, phone, email, address and tap save
- THEN a success indicator MUST appear and the patient MUST be persisted in Room

#### Scenario: Required fields validation

- GIVEN the user is on the new patient screen
- WHEN they tap save with the name field empty
- THEN an inline validation error MUST appear on the name field

### Requirement: Evaluation Flow

Tests MUST verify evaluation form fill, auto-diagnosis, and save. The test SHALL complete all required fields including DIP and visual acuity inputs.

#### Scenario: Complete evaluation with auto-diagnosis

- GIVEN the user is on the new evaluation screen with a patient selected
- WHEN they fill all required fields (DIP, esfera, cilindro, eje) and tap save
- THEN the auto-diagnosis section MUST display a result and the evaluation MUST persist

#### Scenario: Partial evaluation blocks save

- GIVEN the user is on the new evaluation screen
- WHEN required fields are incomplete and save is tapped
- THEN save MUST be blocked and missing fields MUST show validation errors

### Requirement: Dispensación Flow

Tests MUST verify dispensación with items, OT, lens configuration, and payment. The test SHALL add items, configure lenses, register payment, and save.

#### Scenario: Complete dispensación with payment

- GIVEN the user is on the new dispensación screen with a patient selected
- WHEN they add items, configure OT/lens/mounture, enter payment amount, and tap save
- THEN the dispensación MUST persist in Room with correct payment total

#### Scenario: Empty items prevents save

- GIVEN the user is on the new dispensación screen
- WHEN they tap save without adding any items
- THEN save MUST be blocked and a message indicating no items MUST appear

### Requirement: Navigation Flow

Tests MUST verify bottom navigation and drawer menu navigation between sections.

#### Scenario: Bottom nav switches sections

- GIVEN the user is on the dashboard
- WHEN they tap a bottom navigation item (e.g., "Pacientes")
- THEN the corresponding section screen MUST be displayed

#### Scenario: Drawer menu opens and navigates

- GIVEN the user is on any screen with the drawer available
- WHEN they open the drawer and tap "Dispensaciones"
- THEN the dispensaciones list screen MUST be displayed

### Requirement: Test Tag Annotations

All key composables MUST have `@TestTag` annotations for reliable node identification. Tests SHOULD prefer `onNodeWithTag` over `onNodeWithText` for elements prone to copy changes.

#### Scenario: Test tags exist on core screens

- GIVEN the codebase has LoginScreen, NuevoPacienteScreen, NuevaEvaluacionScreen, NuevaDispensacionScreen
- WHEN a developer inspects these composables
- THEN each MUST have at least one `@TestTag` annotation on its root or key interactive element
