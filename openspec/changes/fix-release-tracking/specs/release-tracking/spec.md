# Spec: Release Tracking

## ADDED Requirements

### Requirement: track-release on every push to main

The `track-release` Edge Function SHALL be invoked on every push to `main` in the `build-apk.yml` workflow, regardless of whether the release tag already exists.

#### Scenario: First push creates release
- **Given** a new version tag is pushed to main
- **When** `build-apk.yml` executes the track-release step
- **Then** the Edge Function SHALL insert the new release into `app_releases`

#### Scenario: Re-push of existing tag
- **Given** a tag already exists and is re-pushed to main
- **When** `build-apk.yml` executes the track-release step
- **Then** the Edge Function SHALL handle the call idempotently without errors

#### Scenario: PR push does not trigger track-release
- **Given** a pull request push event
- **When** `build-apk.yml` evaluates the track-release step condition
- **Then** the step SHALL be skipped (PR gate preserved)

### Requirement: Backfill missing releases 1.10.0 to 1.15.8

All releases from 1.10.0 to 1.15.8 that are missing from `app_releases` SHALL be backfilled with correct version, download URL, and timestamp.

#### Scenario: Backfill inserts missing versions
- **Given** `app_releases` is missing entries for versions 1.10.0 through 1.15.8
- **When** the backfill script calls track-release for each missing version
- **Then** each version SHALL appear in `app_releases` with its corresponding GitHub Release URL

#### Scenario: Backfill is idempotent
- **Given** a backfill was already run for a version
- **When** the backfill script runs again for the same version
- **Then** the Edge Function SHALL not create a duplicate entry

### Requirement: track-release Edge Function remains idempotent

The `track-release` Edge Function SHALL remain unchanged. The CI gate fix is a workflow-only change.

#### Scenario: Edge Function handles duplicate calls
- **Given** the Edge Function is called twice with the same version
- **When** the second call arrives
- **Then** the function SHALL upsert (update or no-op) without creating duplicates

### Requirement: GGA review of workflow change

The modified `build-apk.yml` workflow SHALL pass GGA dual-blind review before merge.

#### Scenario: GGA passes
- **Given** the modified `build-apk.yml` is submitted for GGA review
- **When** both reviewers evaluate the change
- **Then** all observations SHALL be resolved before merge
