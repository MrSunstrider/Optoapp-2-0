# Delta for android-auth-onboarding

JD2-C6 + membership Error≠Empty suspects. Fail-closed blank/missing/null `rol`; null GoTrue user and optics-selector Error MUST NOT look like empty memberships.

## MODIFIED Requirements

### Requirement: Blank Role Fail Closed

A membership row with blank, missing, or null `rol` MUST NOT be treated as admin. Absent or null JSON `rol` MUST NOT coerce or default into `admin`. The app SHALL skip the row or reject it.
(Previously: Covered blank rol only; missing/null JSON could still become admin via decode default.)

#### Scenario: Blank rol is not admin

- GIVEN a membership row whose rol is blank
- WHEN memberships are mapped into session
- THEN that row MUST NOT receive rol `admin`
- AND the row MUST be skipped or rejected

#### Scenario: Missing or null rol is not admin

- GIVEN a membership JSON row with missing or null `rol`
- WHEN memberships are decoded and mapped into session
- THEN that row MUST NOT receive rol `admin`
- AND the row MUST be skipped or rejected

#### Scenario: Valid rol is preserved

- GIVEN a membership row with rol `empleado`
- WHEN memberships are mapped
- THEN the session membership MUST keep rol `empleado`

### Requirement: Membership Fetch Distinguishes Error From Empty

Membership fetch MUST expose distinct outcomes: success with a list, success empty, and fetch error. A fetch error MUST NOT be treated as empty memberships. A null or absent GoTrue user during membership fetch MUST yield Error, not Empty. Optics-selector preparation MUST NOT treat fetch Error as empty memberships or as a single-óptica outcome.
(Previously: Network vs empty only; null session and selector Error collapse were unspecified.)

#### Scenario: Network error is not onboarding

- GIVEN an authenticated session
- WHEN membership fetch fails with a network or IO error
- THEN the app MUST surface an error state
- AND MUST NOT route to the no-óptica surface as if memberships were empty
- AND MUST NOT clear the session

#### Scenario: Empty list is onboarding not error

- GIVEN an authenticated session
- WHEN membership fetch succeeds with zero rows
- THEN the outcome MUST be empty, not error
- AND routing MUST keep the session and show the no-óptica surface

#### Scenario: Null GoTrue user is Error not Empty

- GIVEN membership fetch is requested
- AND GoTrue current user is null
- WHEN memberships are fetched
- THEN the outcome MUST be Error, not Empty
- AND MUST NOT route as empty-membership onboarding

#### Scenario: Selector does not treat Error as single óptica

- GIVEN optics-selector preparation runs
- AND membership fetch returns Error
- WHEN the result is presented
- THEN the app MUST NOT treat it as empty memberships
- AND MUST NOT show a single-óptica success toast
- AND MUST surface an error outcome
