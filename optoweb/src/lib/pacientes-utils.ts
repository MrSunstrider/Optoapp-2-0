import type { ListChipFilter } from "@/lib/pacientes";

export type PacientesSearchParams = {
  q: string;
  page: number;
  minEdad?: number;
  maxEdad?: number;
  chip: ListChipFilter;
  msg?: string;
  restantesValid: number | null;
};

export function parseChip(raw: string | undefined): ListChipFilter {
  if (raw === "saldo") return "saldo";
  if (raw === "entrega") return "entrega";
  return "none";
}

export function parsePacientesSearchParams(params: {
  q?: string;
  page?: string;
  minEdad?: string;
  maxEdad?: string;
  chip?: string;
  msg?: string;
  restantes?: string;
}): PacientesSearchParams {
  const restantesRaw = params.restantes?.trim();
  const restantesElim =
    restantesRaw !== undefined && restantesRaw !== ""
      ? Number(restantesRaw)
      : null;
  const restantesValid =
    restantesElim !== null && Number.isFinite(restantesElim)
      ? Math.max(0, Math.floor(restantesElim))
      : null;

  return {
    q: params.q ?? "",
    page: Math.max(1, Number(params.page ?? "1") || 1),
    minEdad: params.minEdad ? Number(params.minEdad) : undefined,
    maxEdad: params.maxEdad ? Number(params.maxEdad) : undefined,
    chip: parseChip(params.chip),
    msg: params.msg,
    restantesValid,
  };
}

export function baseParamsWithoutChip(
  sp: PacientesSearchParams
): URLSearchParams {
  const qp = new URLSearchParams();
  if (sp.q) qp.set("q", sp.q);
  if (sp.minEdad !== undefined) qp.set("minEdad", String(sp.minEdad));
  if (sp.maxEdad !== undefined) qp.set("maxEdad", String(sp.maxEdad));
  return qp;
}

export function baseParamsFull(sp: PacientesSearchParams): URLSearchParams {
  const qp = baseParamsWithoutChip(sp);
  if (sp.chip !== "none") qp.set("chip", sp.chip);
  return qp;
}

export function hrefPage(sp: PacientesSearchParams, p: number): string {
  const qp = baseParamsFull(sp);
  qp.set("page", String(p));
  return `/pacientes?${qp.toString()}`;
}

export function hrefChipToggle(
  sp: PacientesSearchParams,
  which: "saldo" | "entrega"
): string {
  const qp = baseParamsWithoutChip(sp);
  if (which === "saldo") {
    if (sp.chip !== "saldo") qp.set("chip", "saldo");
  } else if (sp.chip !== "entrega") {
    qp.set("chip", "entrega");
  }
  qp.set("page", "1");
  return `/pacientes?${qp.toString()}`;
}

export function shortIdPreview(id: string, len = 8): string {
  return id.length <= len ? id : `${id.slice(0, len)}…`;
}
