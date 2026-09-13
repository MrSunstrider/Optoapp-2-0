# Tasks: fix-security-definer-grants-contract

- [x] 1.1 Inventory Android + optoweb call sites for six SECURITY DEFINER RPCs
- [x] 1.2 Apply grant hardening migration on remote (already `20260913212248`)
- [x] 1.3 Align local migration filename to remote version
- [x] 2.1 RED→GREEN: `supabase/tests/verify_security_definer_grants.sql` contract DO-block
- [x] 2.2 Execute contract against production via `execute_sql` (GREEN)
- [x] 3.1 GGA R1 Risk on live migration + prod ACL (CLEAN)
- [x] 3.2 GGA R3/R4 parent live analysis (native binding N/A; CLEAN on evidence)
- [x] 4.1 Document intentional advisor WARNs must remain (exploration/design)
- [x] 4.2 RDD note: disabled/unmanaged
