# Result Type Unification Specification

## Purpose

Audit and document the relationship between the custom `Resource<T>` type and `kotlin.Result`, producing a clear decision on whether to migrate, consolidate, or keep both with documented rationale.

## Requirements

### Requirement: Audit Resource<T> usage

The system MUST document all current usages of `Resource<T>` across the codebase, categorized by usage pattern (success/loading/error, suspend vs non-suspend, error mapping).

#### Scenario: Complete usage inventory produced

- GIVEN the audit begins
- WHEN a search for `Resource<` and `Resource.Success`/`Resource.Error`/`Resource.Loading` is performed
- THEN a complete list of files using Resource<T> MUST be documented
- AND the count of usages per file MUST be recorded

#### Scenario: Usage patterns categorized

- GIVEN the usage inventory exists
- WHEN each usage is analyzed
- THEN it MUST be categorized as: Loading+Error state, Error-only, or Error+Data wrapper
- AND the category distribution MUST be documented

### Requirement: Decision document produced

A decision document MUST be produced that compares `Resource<T>` vs `kotlin.Result` on the following axes: expressiveness, error typing, loading state support, codebase migration cost, and team familiarity.

| Axis | Resource<T> | kotlin.Result |
|------|-------------|---------------|
| Error typing | Typed (custom sealed class) | `Throwable` only |
| Loading state | Built-in | Not built-in |
| Migration cost | N/A (current) | High (touch every call site) |
| Standard library | No | Yes |

#### Scenario: Decision document includes tradeoffs

- GIVEN the audit is complete
- WHEN the decision document is written
- THEN it MUST include a comparison table of at least 4 dimensions
- AND it MUST state the recommended approach with rationale

#### Scenario: Decision is actionable

- GIVEN the decision document exists
- WHEN a developer reads it
- THEN it MUST clearly state one of: (a) migrate to kotlin.Result, (b) keep Resource<T>, or (c) consolidate to a hybrid approach
- AND the rationale MUST reference specific codebase evidence

### Requirement: Consistency enforcement

If the decision is to keep `Resource<T>`, the system MUST document the standard usage pattern so all developers follow the same convention. If migrating, a migration guide MUST be produced.

#### Scenario: Convention documented for chosen approach

- GIVEN a decision has been made
- WHEN the documentation is complete
- THEN a standard usage pattern MUST be documented
- AND examples of correct usage MUST be provided
- AND anti-patterns MUST be listed with "why" explanations

### Requirement: No code changes in this scope

This deliverable is an audit and documentation task. No code changes SHALL be made to existing files as part of this requirement. Code migration (if decided) is out of scope for this change.

#### Scenario: No source files modified

- GIVEN the audit and decision are complete
- WHEN the deliverable is reviewed
- THEN zero existing source files MUST have been modified
- AND the output is limited to documentation artifacts
