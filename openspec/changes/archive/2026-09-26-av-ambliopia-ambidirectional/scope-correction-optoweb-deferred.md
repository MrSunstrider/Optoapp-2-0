# Scope correction — OptoWeb deferred

**Date**: 2026-09-26  
**Authority**: User preference (post-archive)

OptoWeb helper parity (Unit 2 / tasks 3.1–3.2 / 4.2) was applied during the original SDD cycle, then **reverted** on user request:

> Do not touch OptoWeb; revisit after OptoApp MVP is finished.

## Still in force (Android only)

- `DiagnosticoCalculator.parseAvToLogMar`
- `computeOtrosAuto` wiring
- Android unit tests + archived OpenSpec capability

## Deferred

- OptoWeb `snellenToLogMar` num/den fix
- OptoWeb `avToLogMar` + Vitest parity

Do not re-apply Web changes under this change without an explicit new user authorization after MVP.
