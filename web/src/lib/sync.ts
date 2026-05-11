import type { SupabaseClient } from "@supabase/supabase-js";
import { z } from "zod";

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
  const [telemetry, counts] = await Promise.all([
    fetchTelemetry(supabase, opticaId),
    fetchCountsViaRPC(supabase, opticaId),
  ]);
  const entities = counts;
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

type RpcCounts = {
  pacientes: { total: number; pending: number };
  dispensaciones: { total: number; pending: number };
  servicios_extra: { total: number; pending: number };
  evaluaciones: { total: number; pending: number };
  inventario: { total: number; pending: number };
};

async function fetchCountsViaRPC(
  supabase: SupabaseClient,
  opticaId: string
): Promise<SyncEntityStatus[]> {
  const { data, error } = await supabase
    .rpc("sync_snapshot", { p_optica_id: opticaId });

  if (error) {
    const fallback = (k: SyncEntityStatus["key"], label: string) => ({
      key: k, label, status: "error" as const,
      pendingCount: 0, failedCount: 1, successCount: 0,
      lastAttemptAt: null, message: "Error de consulta",
    });
    return [
      fallback("pacientes", "Pacientes"),
      fallback("dispensaciones", "Dispensaciones"),
      fallback("servicios_extra", "Servicios extra"),
      fallback("evaluaciones", "Evaluaciones"),
      fallback("inventario", "Inventario"),
    ];
  }

  const r = data as unknown as RpcCounts | null;
  if (!r) {
    return [
      { key: "pacientes", label: "Pacientes", status: "ok", pendingCount: 0, failedCount: 0, successCount: 0, lastAttemptAt: null, message: "Base de datos al día" },
      { key: "dispensaciones", label: "Dispensaciones", status: "ok", pendingCount: 0, failedCount: 0, successCount: 0, lastAttemptAt: null, message: "Base de datos al día" },
      { key: "servicios_extra", label: "Servicios extra", status: "ok", pendingCount: 0, failedCount: 0, successCount: 0, lastAttemptAt: null, message: "Base de datos al día" },
      { key: "evaluaciones", label: "Evaluaciones", status: "ok", pendingCount: 0, failedCount: 0, successCount: 0, lastAttemptAt: null, message: "Base de datos al día" },
      { key: "inventario", label: "Inventario", status: "ok", pendingCount: 0, failedCount: 0, successCount: 0, lastAttemptAt: null, message: "Base de datos al día" },
    ];
  }

  const entities: SyncEntityStatus[] = [
    { key: "pacientes", label: "Pacientes", status: "ok", pendingCount: r.pacientes.pending, failedCount: 0, successCount: r.pacientes.total, lastAttemptAt: null, message: r.pacientes.pending > 0 ? "Registros activos en la nube" : "Base de datos al día" },
    { key: "dispensaciones", label: "Dispensaciones", status: "ok", pendingCount: r.dispensaciones.pending, failedCount: 0, successCount: r.dispensaciones.total, lastAttemptAt: null, message: r.dispensaciones.pending > 0 ? "Dispensaciones pendientes de entrega" : "Base de datos al día" },
    { key: "servicios_extra", label: "Servicios extra", status: "ok", pendingCount: r.servicios_extra.pending, failedCount: 0, successCount: r.servicios_extra.total, lastAttemptAt: null, message: r.servicios_extra.pending > 0 ? "Servicios pendientes" : "Base de datos al día" },
    { key: "evaluaciones", label: "Evaluaciones", status: "ok", pendingCount: r.evaluaciones.pending, failedCount: 0, successCount: r.evaluaciones.total, lastAttemptAt: null, message: r.evaluaciones.pending > 0 ? "Evaluaciones con cita pendiente" : "Base de datos al día" },
    { key: "inventario", label: "Inventario", status: "ok", pendingCount: r.inventario.pending, failedCount: 0, successCount: r.inventario.total, lastAttemptAt: null, message: r.inventario.pending > 0 ? "Monturas con stock crítico" : "Base de datos al día" },
  ];

  return entities;
}

export async function runManualSync(
  supabase: SupabaseClient,
  opticaId: string,
  actorId: string,
  stage = "manual-sync"
): Promise<{ ok: boolean; message: string }> {
  const entities = await fetchCountsViaRPC(supabase, opticaId);
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

const TelemetryRowSchema = z.object({
  last_sync_at: z.string().nullable().optional(),
  last_status: z.string().nullable().optional(),
  last_stage: z.string().nullable().optional(),
  last_error: z.string().nullable().optional(),
  updated_at: z.string().nullable().optional(),
});

type TelemetryResult = {
  lastSyncAt: string | null;
  lastStatus: string;
  lastStage: string;
  lastError: string;
  updatedAt: string | null;
};

async function fetchTelemetry(
  supabase: SupabaseClient,
  opticaId: string
): Promise<TelemetryResult> {
  const { data, error } = await supabase
    .from("sync_telemetry_optica")
    .select("last_sync_at,last_status,last_stage,last_error,updated_at")
    .eq("optica_id", opticaId)
    .maybeSingle();

  if (error?.code === "42P01") {
    return {
      lastSyncAt: null,
      lastStatus: "idle",
      lastStage: "",
      lastError: "",
      updatedAt: null,
    };
  }

  const row = TelemetryRowSchema.parse(data ?? {});

  return {
    lastSyncAt: row.last_sync_at ?? null,
    lastStatus: row.last_status ?? "idle",
    lastStage: row.last_stage ?? "",
    lastError: row.last_error ?? "",
    updatedAt: row.updated_at ?? null,
  };
}


