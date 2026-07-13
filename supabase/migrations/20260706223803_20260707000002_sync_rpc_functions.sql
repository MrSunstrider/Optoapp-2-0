-- This is a no-op sync migration -- the DB already has these function versions.
-- It only exists to ensure repo and DB are consistent.
SELECT 'sync_rpc_functions: DB already up to date' AS status;;
