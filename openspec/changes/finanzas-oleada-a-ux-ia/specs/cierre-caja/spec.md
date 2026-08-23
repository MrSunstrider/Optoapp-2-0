# Delta for cierre-caja

## ADDED Requirements

### Requirement: Honest CierreCaja Loading Triad

Loading/empty/error distinct. Loading hides empty. Catch `errorMessage` MUST show error; SHOULD retry. `canViewCierreCaja` + PagoEffect math unchanged.

#### Scenario: Loading hides empty

- GIVEN loading THEN loading on, empty off

#### Scenario: Empty after load

- GIVEN no data loaded THEN empty on, loading off

#### Scenario: Error from catch

- GIVEN errorMessage set THEN error shown; PagoEffect unchanged

#### Scenario: Role gate unchanged

- GIVEN fail canViewCierreCaja THEN restricted UI as before
