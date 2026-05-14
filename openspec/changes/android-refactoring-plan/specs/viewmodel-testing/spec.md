# Spec: viewmodel-testing

## Behavior to Preserve

### AuthViewModel business logic

- GIVEN user submits login credentials
- WHEN AuthViewModel processes authentication
- THEN StateFlow SHALL emit loading, then success/error states in the same sequence
- AND navigation triggers SHALL fire with identical routes and arguments

- GIVEN user session expires or token refresh fails
- WHEN AuthViewModel handles auth state
- THEN the system SHALL transition to logout state identically

### AuthViewModel domain-specific delegates

- GIVEN AuthViewModel is refactored into delegates
- WHEN each delegate handles its domain (auth, profile, session)
- THEN the aggregate StateFlow exposed to the UI SHALL be identical in shape and timing
- AND no delegate SHALL emit states that the original ViewModel did not emit

### OperacionHoyViewModel parallelization

- GIVEN OperacionHoyViewModel loads today's operations
- WHEN multiple data sources are fetched
- THEN the system SHALL use awaitAll to parallelize independent fetches
- AND the final combined state SHALL contain identical data as sequential execution
- AND loading duration SHALL be less than or equal to sequential execution

- GIVEN one data source fails during parallel fetch
- WHEN OperacionHoyViewModel handles partial failure
- THEN successful results SHALL still be displayed
- AND the error state for the failed source SHALL match current behavior

### ViewModel test coverage

- GIVEN existing ViewModels without tests
- WHEN characterization tests are written
- THEN each test SHALL exercise the current behavior as the source of truth
- AND tests SHALL pass against the current (pre-refactor) implementation

## Acceptance Criteria

- [ ] AuthViewModel tests cover: login success, login failure, logout, session expiry, token refresh
- [ ] OperacionHoyViewModel tests cover: all sources succeed, partial failure, all fail, loading state
- [ ] Refactored AuthViewModel delegates produce identical aggregate StateFlow output
- [ ] awaitAll parallelization does not change final state ordering
- [ ] All new tests pass against current implementation before refactor begins
