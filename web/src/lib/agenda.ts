import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

export type AgendaPeriodo = "hoy" | "semana" | "mes";

type EvaluacionAgendaRow = {
  id: string;
  paciente_id: string | null;
  proxima_cita: string | null;
  cita_estado: string | null;
  motivo_consulta: string | null;
  plan_tratamiento: string | null;
};

type PacienteAgendaRow = {
  id: string;
  nombre_completo: string | null;
  telefono: string | null;
};

export type AgendaItem = {
  id: string;
  pacienteId: string | null;
  pacienteNombre: string;
  contacto: string | null;
  fechaIso: string;
  hora: string | null;
  motivo: string | null;
  estado: string;
};

export function normalizeAgendaPeriodo(value?: string): AgendaPeriodo {
  if (value === "hoy" || value === "semana" || value === "mes") return value;
  return "hoy";
}

export function getAgendaPeriodBounds(periodo: AgendaPeriodo): {
  from: string;
  toExclusive: string;
  note: string;
} {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  let end = new Date(start);
  let note = "";
  if (periodo === "hoy") {
    end.setDate(start.getDate() + 1);
    note = "Rango: hoy (inicio de día local a fin de día local).";
  } else if (periodo === "semana") {
    end.setDate(start.getDate() + 7);
    note = "Rango: próximos 7 días desde hoy.";
  } else {
    end.setDate(start.getDate() + 30);
    note = "Rango: próximos 30 días desde hoy.";
  }
  return { from: toDateOnly(start), toExclusive: toDateOnly(end), note };
}

export async function fetchAgendaItems(
  supabase: SupabaseClient,
  opticaId: string,
  periodo: AgendaPeriodo
): Promise<AgendaItem[]> {
  const bounds = getAgendaPeriodBounds(periodo);

  const { data, error } = await supabase
    .from("evaluaciones")
    .select("id,paciente_id,proxima_cita,cita_estado,motivo_consulta,plan_tratamiento")
    .eq("optica_id", opticaId)
    .not("proxima_cita", "is", null)
    .gte("proxima_cita", bounds.from)
    .lt("proxima_cita", bounds.toExclusive)
    .order("proxima_cita", { ascending: true })
    .abortSignal(AbortSignal.timeout(12_000));

  assertNoDbError(error, "Agenda: evaluaciones por rango");
  const rows = (data ?? []) as EvaluacionAgendaRow[];
  if (rows.length === 0) return [];

  const ids = Array.from(
    new Set(rows.map((r) => r.paciente_id).filter((v): v is string => Boolean(v)))
  );

  let patientMap = new Map<string, PacienteAgendaRow>();
  if (ids.length > 0) {
    const { data: pData, error: pErr } = await supabase
      .from("pacientes")
      .select("id,nombre_completo,telefono")
      .eq("optica_id", opticaId)
      .in("id", ids)
      .abortSignal(AbortSignal.timeout(12_000));

    assertNoDbError(pErr, "Agenda: pacientes");
    patientMap = new Map(
      ((pData ?? []) as PacienteAgendaRow[]).map((p) => [p.id, p])
    );
  }

  return rows.map((row) => {
    const fecha = row.proxima_cita ? parseDateTimeSafe(row.proxima_cita) : null;
    const paciente = row.paciente_id ? patientMap.get(row.paciente_id) : null;
    return {
      id: row.id,
      pacienteId: row.paciente_id,
      pacienteNombre: paciente?.nombre_completo?.trim() || "Paciente",
      contacto: paciente?.telefono?.trim() || null,
      fechaIso: row.proxima_cita ?? "",
      hora: fecha?.hour ?? null,
      motivo: row.motivo_consulta?.trim() || row.plan_tratamiento?.trim() || null,
      estado: (row.cita_estado?.trim() || "programada").toLowerCase()
    };
  });
}

export function formatAgendaDate(iso: string): string {
  const date = parseDateTimeSafe(iso)?.date;
  if (!date) return iso;
  return date.toLocaleDateString("es-PE", {
    weekday: "short",
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function toDateOnly(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function parseDateTimeSafe(raw: string): { date: Date; hour: string | null } | null {
  const d = new Date(raw.includes("T") ? raw : `${raw}T12:00:00`);
  if (Number.isNaN(d.getTime())) return null;
  const hasExplicitTime = raw.includes("T");
  const hour = hasExplicitTime
    ? d.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit", hour12: false })
    : null;
  return { date: d, hour };
}
