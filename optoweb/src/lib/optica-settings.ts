import type { SupabaseClient } from "@supabase/supabase-js";

export async function fetchOpticaSettings(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Record<string, unknown>> {
  const { data, error } = await supabase
    .from("optica_settings")
    .select("config_json")
    .eq("optica_id", opticaId)
    .maybeSingle();

  if (error?.code === "42P01") return {};
  if (error) return {};
  const json = (data as { config_json?: unknown } | null)?.config_json;
  if (json && typeof json === "object" && !Array.isArray(json)) {
    return json as Record<string, unknown>;
  }
  return {};
}

export async function upsertOpticaSettingsPatch(
  supabase: SupabaseClient,
  opticaId: string,
  patch: Record<string, unknown>
): Promise<{ ok: boolean; missingTable?: boolean }> {
  const current = await fetchOpticaSettings(supabase, opticaId);
  const merged = { ...current, ...patch };

  const { error } = await supabase.from("optica_settings").upsert(
    {
      optica_id: opticaId,
      config_json: merged
    },
    { onConflict: "optica_id" }
  );
  if (error?.code === "42P01") return { ok: false, missingTable: true };
  if (error) return { ok: false };
  return { ok: true };
}
