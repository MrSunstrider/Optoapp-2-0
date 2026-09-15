-- JD reportes-cluster S2:
-- Keep half-open [p_from, p_to) for optoweb callers (toExclusive / endExclusive).
-- Same-day queries MUST pass p_to = selectedDate + 1 day. Do NOT switch to inclusive.

COMMENT ON FUNCTION public.rpc_cierre_caja_resumen(TEXT, DATE, DATE) IS
  'Cash-close aggregates for [p_from, p_to). Upper bound exclusive. Same calendar day: p_from=D, p_to=D+1. Matches optoweb toExclusive/endExclusive callers.';
