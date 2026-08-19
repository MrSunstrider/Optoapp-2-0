# Hosted GoTrue password policy

Android register and password-reset screens require a password of at least **6** characters with lower, upper, digit, and symbol classes.

`supabase/config.toml` `[auth]` sets:

- `minimum_password_length = 6`
- `password_requirements = "lower_upper_letters_digits_symbols"`

That file applies to **local** GoTrue (`supabase start`). Production Auth at `sflhtihqdhrlryeyrzdo` is not updated by committing this repo. A human must set the same values in the hosted **dashboard** (Authentication → Providers / password settings) or via the **Management API**. Until that step is done, hosted GoTrue can still accept weaker passwords than the app UI.

## A10 — unused `invitaciones`

The `invitaciones` table (and any `codigo` join path) is **not** product onboarding. Employees join when their email is already registered and an admin runs `assign_optica_role_by_email`. Do not treat `invitaciones` as a live invite-code flow in this program.
