# Migrations

Naming convention: `YYYYMMDDHHMMSS_description.sql`

- Prefix with timestamp (16 digits, UTC)
- Use snake_case for description
- Keep descriptions brief but descriptive

Example: `20260515000000_rpc_suggest_next_ho.sql`

Rollback: apply a new migration that reverses the change. Do NOT modify existing migrations after they've been applied to production.
