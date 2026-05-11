import type { SupabaseClient } from "@supabase/supabase-js";
import { z } from "zod";
import { assertNoDbError } from "@/lib/supabase/db-error";
import { escapeIlikeFragment } from "@/lib/sanitize";

export const PacienteListRowSchema = z.object({
  id: z.string(),
  nombre_completo: z.string(),
  edad: z.number(),
  telefono: z.string(),
  fecha_creacion: z.string(),
  historia_optometrica: z.string().nullable(),
  ultimas_etiquetas: z.string().nullable(),
});

export type PacienteListRow = z.infer<typeof PacienteListRowSchema>;

export const PacienteDetalleRowSchema = PacienteListRowSchema.extend({
  dni: z.string().nullable(),
  fecha_nacimiento: z.string().nullable(),
  sexo: z.string().nullable(),
  email: z.string().nullable(),
  direccion: z.string().nullable(),
  distrito: z.string().nullable(),
  ocupacion: z.string().nullable(),
  acompanante: z.string().nullable(),
  hobbies: z.string().nullable(),
});

export type PacienteDetalleRow = z.infer<typeof PacienteDetalleRowSchema>;

function validatePacienteListRows(data: unknown): PacienteListRow[] {
  if (!Array.isArray(data)) return [];
  return data.flatMap((row) => {
    const parsed = PacienteListRowSchema.safeParse(row);
    return parsed.success ? [parsed.data] : [];
  });
}

const LIST_SELECT =
  "id,nombre_completo,edad,telefono,fecha_creacion,historia_optometrica,ultimas_etiquetas";

const DETALLE_SELECT = `${LIST_SELECT},dni,fecha_nacimiento,sexo,email,direccion,distrito,ocupacion,acompanante,hobbies`;

export type ListChipFilter = "none" | "saldo" | "entrega";

export type FetchPacientesOptions = {
  search: string;
  page: number;
  pageSize: number;
  minEdad?: number;
  maxEdad?: number;
  chip: ListChipFilter;
};

async function collectIdsSaldoPendiente(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Set<string>> {
  const ids = new Set<string>();
  const { data: d, error: e1 } = await supabase
    .from("dispensaciones")
    .select("paciente_id,monto_total,monto_pagado")
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(15_000));
  assertNoDbError(e1, "Dispensaciones (filtro saldo)");
  for (const row of d ?? []) {
    const total = Number(row.monto_total ?? 0);
    const pag = Number(row.monto_pagado ?? 0);
    if (total - pag > 0.005) ids.add(String(row.paciente_id));
  }
  const { data: s, error: e2 } = await supabase
    .from("servicios_extra")
    .select("paciente_id,monto_total,a_cuenta")
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(15_000));
  assertNoDbError(e2, "Servicios extra (filtro saldo)");
  for (const row of s ?? []) {
    const total = Number(row.monto_total ?? 0);
    const ac = Number(row.a_cuenta ?? 0);
    if (total - ac > 0.005) ids.add(String(row.paciente_id));
  }
  return ids;
}

async function collectIdsEntregaPendiente(
  supabase: SupabaseClient,
  opticaId: string
): Promise<Set<string>> {
  const ids = new Set<string>();
  const { data: d, error: e1 } = await supabase
    .from("dispensaciones")
    .select("paciente_id,estado_entrega")
    .eq("optica_id", opticaId)
    .eq("estado_entrega", "Pendiente")
    .abortSignal(AbortSignal.timeout(15_000));
  assertNoDbError(e1, "Dispensaciones (filtro entrega)");
  for (const row of d ?? []) ids.add(String(row.paciente_id));

  const { data: s, error: e2 } = await supabase
    .from("servicios_extra")
    .select("paciente_id,estado")
    .eq("optica_id", opticaId)
    .eq("estado", "Pendiente")
    .abortSignal(AbortSignal.timeout(15_000));
  assertNoDbError(e2, "Servicios extra (filtro entrega)");
  for (const row of s ?? []) ids.add(String(row.paciente_id));
  return ids;
}

async function chipFilterIds(
  supabase: SupabaseClient,
  opticaId: string,
  chip: ListChipFilter
): Promise<string[] | null> {
  if (chip === "none") return null;
  const set =
    chip === "saldo"
      ? await collectIdsSaldoPendiente(supabase, opticaId)
      : await collectIdsEntregaPendiente(supabase, opticaId);
  return Array.from(set);
}

function applySearchOr<T extends { or: (filter: string) => T }>(
  query: T,
  term: string
): T {
  const t = escapeIlikeFragment(term.trim());
  if (t.length === 0) return query;
  return query.or(
    `nombre_completo.ilike.%${t}%,telefono.ilike.%${t}%,historia_optometrica.ilike.%${t}%,id.ilike.%${t}%`
  );
}

export function parseEtiquetasRecientes(raw: string | null, max = 2): string[] {
  if (!raw?.trim()) return [];
  return raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
    .slice(0, max);
}

export function shortId(id: string, len = 8): string {
  return id.length <= len ? id : id.slice(0, len) + "…";
}

export async function fetchPacientes(
  supabase: SupabaseClient,
  opticaId: string,
  options: FetchPacientesOptions
): Promise<PacienteListRow[]> {
  const { search, page, pageSize, minEdad, maxEdad, chip } = options;
  const from = (page - 1) * pageSize;
  const to = from + pageSize - 1;

  const idList = await chipFilterIds(supabase, opticaId, chip);
  if (idList && idList.length === 0) {
    return [];
  }

  let query = supabase
    .from("pacientes")
    .select(LIST_SELECT)
    .eq("optica_id", opticaId)
    .order("fecha_creacion", { ascending: false })
    .range(from, to)
    .abortSignal(AbortSignal.timeout(12_000));

  query = applySearchOr(query, search);

  if (typeof minEdad === "number" && !Number.isNaN(minEdad)) {
    query = query.gte("edad", minEdad);
  }
  if (typeof maxEdad === "number" && !Number.isNaN(maxEdad)) {
    query = query.lte("edad", maxEdad);
  }
  if (idList && idList.length > 0) {
    query = query.in("id", idList);
  }

  const { data, error } = await query;
  assertNoDbError(error, "Listado de pacientes");
  return validatePacienteListRows(data);
}

export async function countPacientes(
  supabase: SupabaseClient,
  opticaId: string,
  options: Omit<FetchPacientesOptions, "page" | "pageSize">
): Promise<number> {
  const { search, minEdad, maxEdad, chip } = options;

  const idList = await chipFilterIds(supabase, opticaId, chip);
  if (idList && idList.length === 0) {
    return 0;
  }

  let query = supabase
    .from("pacientes")
    .select("id", { count: "exact", head: true })
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(12_000));

  query = applySearchOr(query, search);

  if (typeof minEdad === "number" && !Number.isNaN(minEdad)) {
    query = query.gte("edad", minEdad);
  }
  if (typeof maxEdad === "number" && !Number.isNaN(maxEdad)) {
    query = query.lte("edad", maxEdad);
  }
  if (idList && idList.length > 0) {
    query = query.in("id", idList);
  }

  const { count, error } = await query;
  assertNoDbError(error, "Conteo de pacientes");
  return count ?? 0;
}

export async function fetchPacienteById(
  supabase: SupabaseClient,
  opticaId: string,
  id: string
): Promise<PacienteDetalleRow | null> {
  const { data, error } = await supabase
    .from("pacientes")
    .select(DETALLE_SELECT)
    .eq("optica_id", opticaId)
    .eq("id", id)
    .limit(1)
    .maybeSingle();

  assertNoDbError(error, "Carga de paciente");
  if (!data) return null;
  const parsed = PacienteDetalleRowSchema.safeParse(data);
  return parsed.success ? parsed.data : null;
}

const HistoriaRowSchema = z.object({
  id: z.string(),
  historia_optometrica: z.string().nullable(),
});

function validateHistoriaRows(data: unknown): { id: string; historia_optometrica: string | null }[] {
  if (!Array.isArray(data)) return [];
  return data.flatMap((row) => {
    const parsed = HistoriaRowSchema.safeParse(row);
    return parsed.success ? [parsed.data] : [];
  });
}

// HO lookup for duplicate check — full table scan is H3 in improvement plan.
export async function fetchHistoriaRowsForOptica(
  supabase: SupabaseClient,
  opticaId: string
): Promise<{ id: string; historia_optometrica: string | null }[]> {
  const { data, error } = await supabase
    .from("pacientes")
    .select("id,historia_optometrica")
    .eq("optica_id", opticaId)
    .abortSignal(AbortSignal.timeout(20_000));
  assertNoDbError(error, "Historias optométricas");
  return validateHistoriaRows(data);
}

export function localTodayDateOnly(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export type PacienteRow = PacienteListRow;
