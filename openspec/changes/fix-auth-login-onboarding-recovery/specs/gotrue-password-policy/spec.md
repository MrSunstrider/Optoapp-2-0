# GoTrue Password Policy Specification

## Purpose

Align local and hosted GoTrue with Android register and reset UI: minimum length 6 and lower, upper, digit, and symbol classes.

## Requirements

### Requirement: Local Config Enforces Complexity

`supabase/config.toml` `[auth]` MUST set `minimum_password_length = 6` and `password_requirements = "lower_upper_letters_digits_symbols"`.

#### Scenario: Config file values

- GIVEN the repo `supabase/config.toml`
- WHEN `[auth]` is read
- THEN `minimum_password_length` MUST be 6
- AND `password_requirements` MUST be `lower_upper_letters_digits_symbols`

#### Scenario: Weak password rejected locally

- GIVEN local GoTrue using that config
- WHEN a password of length 6 with only lowercase letters is submitted
- THEN GoTrue MUST reject the password

#### Scenario: Strong min-6 password accepted locally

- GIVEN local GoTrue using that config
- WHEN a length-6 password containing lower, upper, digit, and symbol is submitted
- THEN GoTrue MUST accept the password as meeting this policy

### Requirement: Hosted Policy Recorded In Repo

Hosted GoTrue MUST use the same min-6 complexity policy. Because `config.toml` does not update hosted Auth, this change MUST record an in-repo dashboard (or Management API) step naming the same requirement string and minimum length so a test can assert the record exists.

#### Scenario: Hosted match step is greppable

- GIVEN the in-repo Auth config or adjacent operational note for this change
- WHEN a test searches for hosted password-policy instructions
- THEN the record MUST mention `lower_upper_letters_digits_symbols`
- AND MUST mention minimum length 6
- AND MUST state that the hosted dashboard (or Management API) must be updated
