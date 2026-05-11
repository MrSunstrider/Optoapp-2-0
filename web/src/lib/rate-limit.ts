import { createClient } from "@/lib/supabase/server";
import { z } from "zod";

const WINDOW_MS = 60_000;
const MAX_ATTEMPTS = 5;

const RateLimitResultSchema = z.object({
  allowed: z.boolean(),
  remaining: z.number(),
});

export async function checkRateLimit(
  key: string
): Promise<{ allowed: boolean; remaining: number }> {
  const supabase = await createClient();
  const { data, error } = await supabase.rpc("check_rate_limit", {
    p_limit_key: key,
    p_window_ms: WINDOW_MS,
    p_max_attempts: MAX_ATTEMPTS,
  });
  if (error) throw error;
  const parsed = RateLimitResultSchema.safeParse(data);
  if (!parsed.success) {
    throw new Error(`Unexpected rate-limit response shape: ${parsed.error.message}`);
  }
  return parsed.data;
}
