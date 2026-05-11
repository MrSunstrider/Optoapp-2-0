// PostgREST .or() treats % _ and \ as pattern operators — user input
// must be escaped to prevent filter injection that bypasses RLS.
export function escapeIlikeFragment(s: string): string {
  return s.replace(/\\/g, "\\\\").replace(/%/g, "\\%").replace(/_/g, "\\_");
}
