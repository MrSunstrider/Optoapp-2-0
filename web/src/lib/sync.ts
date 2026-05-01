import type { SupabaseClient } from "@supabase/supabase-js";

export type SyncEntityStatus = {
  key: "pacientes" | "evaluaciones" | "dispensaciones" | "servicios_extra" | "inventario";
  label: string;
  status: "ok" | "pending" | "error";
  pendingCount: number;
  failedCount: number;
  successCount: number;
  lastAttemptAt: string | null;
  message: string;
};

export type SyncSnapshot = {
  globalStatus: "ok" | "pending" | "degraded" | "error";
  lastSuccessAt: string | null;
  lastError: string | null;
  updatedAt: string | null;
  entities: SyncEntityStatus[];
};

export async function fetchSyncSnapshot(
  supabase: SupabaseClient,
  opticaId: string
): Promise<SyncSnapshot> {
  const telemetry = await fetchTelemetry(supabase, opticaId);
  const entities = await buildEntityStatuses(supabase, opticaId, telemetry.updatedAt);
  const hasError = entities.some((e) => e.status === "error");
  const hasPending = entities.some((e) => e.status === "pending");
  const globalStatus = hasError
    ? "error"
    : telemetry.lastStatus === "error"
      ? "degraded"
      : "ok";

  return {
    globalStatus,
    lastSuccessAt: telemetry.lastSyncAt,
    lastError: telemetry.lastError || null,
    updatedAt: telemetry.updatedAt,
    entities
  };
}

export async function runManualSync(
  supabase: SupabaseClient,
  opticaId: string,
  actorId: string,
  stage = "manual-sync"
): Promise<{ ok: boolean; message: string }> {
  const entities = await buildEntityStatuses(supabase, opticaId, new Date().toISOString());
  const hasError = entities.some((e) => e.status === "error");
  const status = hasError ? "error" : "ok";
  const errorSummary = hasError
    ? entities
        .filter((e) => e.status === "error")
        .map((e) => `${e.label}: ${e.message}`)
        .join(" | ")
    : "";

  const { error } = await supabase.from("sync_telemetry_optica").upsert(
    {
      optica_id: opticaId,
      last_sync_at: new Date().toISOString(),
      last_status: status,
      last_stage: stage,
      last_error: errorSummary,
      last_actor: actorId
    },
    { onConflict: "optica_id" }
  );

  if (error?.code === "42P01") {
    return {
      ok: false,
      message: "Tabla de telemetría de sync no disponible."
    };
  }
  if (error) {
    return { ok: false, message: "No se pudo registrar la sincronización." };
  }
  return {
    ok: true,
    message: hasError
      ? "Verificación ejecutada con incidencias."
      : "Verificación completada. La base de datos está al día."
  };
}

async function fetchTelemetry(supabase: SupabaseClient, opticaId: string) {
  const { data, error } = await supabase
    .from("sync_telemetry_optica")
    .select("last_sync_at,last_status,last_stage,last_error,updated_at")
    .eq("optica_id", opticaId)
    .maybeSingle();

  if (error?.code === "42P01") {
    return {
      lastSyncAt: null as string | null,
      lastStatus: "idle",
      lastStage: "",
      lastError: "",
      updatedAt: null as string | null
    };
  }

  const row = (data ?? {}) as {
    last_sync_at?: string | null;
    last_status?: string | null;
    last_stage?: string | null;
    last_error?: string | null;
    updated_at?: string | null;
  };

  return {
    lastSyncAt: row.last_sync_at ?? null,
    lastStatus: row.last_status ?? "idle",
    lastStage: row.last_stage ?? "",
    lastError: row.last_error ?? "",
    updatedAt: row.updated_at ?? null
  };
}

async function buildEntityStatuses(
  supabase: SupabaseClient,
  opticaId: string,
  updatedAt: string | null
): Promise<SyncEntityStatus[]> {
  const jobs = [
    countSafe(supabase, "pacientes", "Pacientes", opticaId),
    countEvaluacionesPendientes(supabase, opticaId),
    countDispensacionesPendientes(supabase, opticaId),
    countServiciosPendientes(supabase, opticaId),
    countInventarioCritico(supabase, opticaId)
  ] as const;
  const result = await Promise.all(jobs);

  return result.map((r) => {
    if (!r.ok) {
      return {
        key: r.key,
        label: r.label,
        status: "error",
        pendingCount: 0,
        failedCount: 1,
        successCount: 0,
        lastAttemptAt: updatedAt,
        message: "Error de consulta"
      } satisfies SyncEntityStatus;
    }
    return {
      key: r.key,
      label: r.label,
      status: "ok",
      pendingCount: r.pendingCount,
      failedCount: 0,
      successCount: r.totalCount,
      lastAttemptAt: updatedAt,
      message: r.pendingCount > 0 
        ? `${r.pendingCount} registros operativos activos en la nube` 
        : "Base de datos al día"
    } satisfies SyncEntityStatus;
  });
}

async function countSafe(
  supabase: SupabaseClient,
  table: "pacientes",
  label: string,
  opticaId: string
) {
  const { count, error } = await supabase
    .from(table)
    .select("id", { count: "exact", head: true })
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(12_000));
  if (error) return { ok: false as const, key: "pacientes" as const, label };
  return {
    ok: true as const,
    key: "pacientes" as const,
    label,
    pendingCount: 0,
    totalCount: count ?? 0
  };
}

async function countDispensacionesPendientes(supabase: SupabaseClient, opticaId: string) {
  const { data, error } = await supabase
    .from("dispensaciones")
    .select("id,estado_entrega")
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(12_000));
  if (error) {
    return { ok: false as const, key: "dispensaciones" as const, label: "Dispensaciones" };
  }
  const rows = (data ?? []) as Array<{ id: string; estado_entrega?: string | null }>;
  const pendingCount = rows.filter((r) => String(r.estado_entrega ?? "") === "Pendiente").length;
  return {
    ok: true as const,
    key: "dispensaciones" as const,
    label: "Dispensaciones",
    pendingCount,
    totalCount: rows.length
  };
}

async function countServiciosPendientes(supabase: SupabaseClient, opticaId: string) {
  const { data, error } = await supabase
    .from("servicios_extra")
    .select("id,estado")
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(12_000));
  if (error) {
    return { ok: false as const, key: "servicios_extra" as const, label: "Servicios extra" };
  }
  const rows = (data ?? []) as Array<{ id: string; estado?: string | null }>;
  const pendingCount = rows.filter((r) => String(r.estado ?? "") === "Pendiente").length;
  return {
    ok: true as const,
    key: "servicios_extra" as const,
    label: "Servicios extra",
    pendingCount,
    totalCount: rows.length
  };
}

async function countEvaluacionesPendientes(supabase: SupabaseClient, opticaId: string) {
  const { data, error } = await supabase
    .from("evaluaciones")
    .select("id,cita_estado,proxima_cita")
    .eq("optica_id", opticaId)
    .not("proxima_cita", "is", null)
    .abortSignal(AbortSignal.timeout(12_000));
  if (error) return { ok: false as const, key: "evaluaciones" as const, label: "Evaluaciones" };
  const rows = (data ?? []) as Array<{ cita_estado?: string | null }>;
  const pendingCount = rows.filter((r) => {
    const status = String(r.cita_estado ?? "programada").toLowerCase();
    return status !== "atendida" && status !== "cancelada";
  }).length;
  return {
    ok: true as const,
    key: "evaluaciones" as const,
    label: "Evaluaciones",
    pendingCount,
    totalCount: rows.length
  };
}

async function countInventarioCritico(supabase: SupabaseClient, opticaId: string) {
  const { data, error } = await supabase
    .from("monturas")
    .select("id,stock_actual")
    .eq("optica_id", opticaId)
    .eq("activo", true)
    .abortSignal(AbortSignal.timeout(12_000));
  if (error) return { ok: false as const, key: "inventario" as const, label: "Inventario" };
  const rows = (data ?? []) as Array<{ stock_actual?: number | null }>;
  const pendingCount = rows.filter((r) => Number(r.stock_actual ?? 0) <= 2).length;
  return {
    ok: true as const,
    key: "inventario" as const,
    label: "Inventario",
    pendingCount,
    totalCount: rows.length
  };
}
