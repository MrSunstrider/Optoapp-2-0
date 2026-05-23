import type { EvaluacionDraft } from "./evaluacion-types";

export const TAB_ITEMS = [
  "Anamnesis",
  "Examen Visual",
  "Refraccion",
  "Contactologia",
  "Cierre"
] as const;

export type TabItem = (typeof TAB_ITEMS)[number];

export const ESTEREOPSIS_OPTIONS = ["Normal", "Reducida", "Ausente"] as const;
export const LANG_OPTIONS = ["Positivo", "Negativo"] as const;
export const WORTH_OPTIONS = ["Fusion normal", "Supresion OD", "Supresion OI", "Diplopia"] as const;
export const FARNSWORTH_OPTIONS = ["Normal", "Deutan", "Protan", "Tritan"] as const;
export const SENSIBILIDAD_OPTIONS = ["Normal", "Disminuida"] as const;
export const CAMPO_VISUAL_OPTIONS = ["Normal", "Anomalia detectada"] as const;
export const BASES_PRISMA = ["Nasal", "Temporal", "Superior", "Inferior"] as const;

export const DIAG_OPTIONS = [
  "Emetropia",
  "Miopia",
  "Hipermetropia",
  "Astigmatismo miopico simple",
  "Astigmatismo miopico compuesto",
  "Astigmatismo hipermetropico simple",
  "Astigmatismo hipermetropico compuesto",
  "Astigmatismo mixto",
  "Balance"
] as const;

export const CITA_ESTADOS: Array<{ code: string; label: string }> = [
  { code: "programada", label: "Programada" },
  { code: "confirmada", label: "Confirmada" },
  { code: "asistio", label: "Asistio" },
  { code: "no_asistio", label: "No asistio" },
  { code: "reprogramada", label: "Reprogramada" }
];

export function toEmptyDraft(todayDate: string): EvaluacionDraft {
  return {
    fecha: todayDate,
    motivoConsulta: "",
    sintomasSignos: "",
    antecedentesPersonalesOculares: "",
    antecedentesPersonalesSistemicos: "",
    antecedentesFamiliaresOculares: "",
    antecedentesFamiliaresSistemicos: "",
    medicacion: "",
    alergias: "",
    necesidadVisual: "",
    avScAo: "",
    avScOdLejos: "",
    avScOiLejos: "",
    avCcAoPx: "",
    avCcOdLejos: "",
    avCcOiLejos: "",
    estereopsisValor: "",
    estereopsisSegundos: "",
    lang: "",
    worth: "",
    ishihara: "",
    farnsworth: "",
    schirmerOd: "",
    schirmerOi: "",
    osdiPuntuacion: "",
    osdiClasificacion: "",
    sensibilidadContraste: "",
    sensibilidadFrecuencia: "",
    amsler: "",
    campoVisual: "",
    campoVisualDescripcion: "",
    phOd: "",
    phOi: "",
    kappaOd: "",
    kappaOi: "",
    hirshberg: "",
    duccionesOd: "",
    duccionesOi: "",
    versionesAo: "",
    coverTest6m: "",
    coverTest40cm: "",
    coverTest10cm: "",
    ppcOr: "",
    ppcLuz: "",
    ppcFrl: "",
    reflejoFotomotor: "",
    reflejoConsensual: "",
    reflejoAcomodativo: "",
    objOdEsf: "",
    objOdCil: "",
    objOdEje: "",
    objOiEsf: "",
    objOiCil: "",
    objOiEje: "",
    subjOdEsf: "",
    subjOdCil: "",
    subjOdEje: "",
    subjOiEsf: "",
    subjOiCil: "",
    subjOiEje: "",
    recetaOdEsf: "",
    recetaOdCil: "",
    recetaOdEje: "",
    recetaOdAv: "",
    recetaOiEsf: "",
    recetaOiCil: "",
    recetaOiEje: "",
    recetaOiAv: "",
    addCercaOd: "",
    addCercaOi: "",
    addAv: "",
    isAddAo: "false",
    dipLejos: "",
    dipCerca: "",
    prismaOdValor: "",
    prismaOdBase: "",
    prismaOiValor: "",
    prismaOiBase: "",
    k1Od: "",
    k2Od: "",
    k1Oi: "",
    k2Oi: "",
    lcOdEsf: "",
    lcOdCil: "",
    lcOdEje: "",
    lcOiEsf: "",
    lcOiCil: "",
    lcOiEje: "",
    lcRadioBaseOd: "",
    lcRadioBaseOi: "",
    lcOdDia: "",
    lcOiDia: "",
    lcLaboratorio: "",
    lcTipoLente: "",
    lcMaterial: "",
    lcFechaAdaptacion: "",
    lcObservaciones: "",
    contactologia: "",
    diagnosticoOd: "",
    diagnosticoOi: "",
    balanceOd: "false",
    balanceOi: "false",
    otrosPresbicia: "false",
    otrosAnisometropia: "false",
    otrosAmbliopia: "false",
    autoPresbicia: "true",
    autoAnisometropia: "true",
    autoAmbliopia: "true",
    planTratamiento: "",
    citaEstado: "programada",
    diagnosticoResumen: "",
    indicaciones: "",
    proximaCita: "",
    notasFinales: ""
  };
}

export function parsePower(raw: string): number {
  const t = raw.trim().toLowerCase();
  if (!t) return 0;
  if (["plano", "pl", "neutro", "nt"].includes(t)) return 0;
  const n = Number(t.replace(",", "."));
  return Number.isFinite(n) ? n : 0;
}

export function hasBalanceText(...values: string[]): boolean {
  return values.some((v) => v.toLowerCase().includes("bal"));
}

export function classifyRefraccion(esfRaw: string, cilRaw: string): string {
  let esfera = parsePower(esfRaw);
  let cil = parsePower(cilRaw);
  if (cil > 0) {
    esfera = esfera + cil;
    cil = -cil;
  }
  const m1 = esfera;
  const m2 = esfera + cil;
  const z = 0.0001;
  const eq0 = (x: number) => Math.abs(x) < z;
  if (eq0(m1) && eq0(m2)) return "Emetropia";
  if (esfera < 0 && eq0(cil)) return "Miopia";
  if (esfera > 0 && eq0(cil)) return "Hipermetropia";
  if ((eq0(m1) && m2 < 0) || (eq0(m2) && m1 < 0)) return "Astigmatismo miopico simple";
  if ((eq0(m1) && m2 > 0) || (eq0(m2) && m1 > 0)) return "Astigmatismo hipermetropico simple";
  if (m1 < 0 && m2 < 0) return "Astigmatismo miopico compuesto";
  if (m1 > 0 && m2 > 0) return "Astigmatismo hipermetropico compuesto";
  if ((m1 > 0 && m2 < 0) || (m1 < 0 && m2 > 0)) return "Astigmatismo mixto";
  return "Astigmatismo mixto";
}

export function autoDiagEye(
  esf: string,
  cil: string,
  eje: string,
  balanceChecked: boolean
): string {
  const effBalance = balanceChecked || hasBalanceText(esf, cil, eje);
  if (effBalance) return "Balance";
  const hasData = esf.trim().length > 0 || cil.trim().length > 0;
  if (!hasData) return "";
  return classifyRefraccion(esf, cil);
}

export function autoPresbiciaValue(addOd: string, addOi: string): boolean {
  const src = addOd.trim() !== "" ? addOd : addOi;
  return parsePower(src) > 0;
}

export function autoAnisometropiaValue(
  odEsfRaw: string,
  odCilRaw: string,
  oiEsfRaw: string,
  oiCilRaw: string,
  odBalance: boolean,
  oiBalance: boolean
): boolean {
  if (odBalance || oiBalance) return false;
  if (odEsfRaw.trim() === "" || oiEsfRaw.trim() === "") return false;
  const odEE = parsePower(odEsfRaw) + parsePower(odCilRaw) / 2;
  const oiEE = parsePower(oiEsfRaw) + parsePower(oiCilRaw) / 2;
  return Math.abs(odEE - oiEE) >= 2.0;
}

export function snellenToLogMar(raw: string): number | null {
  const m = raw.trim().match(/(\d+)\s*\/\s*(\d+)/);
  if (!m) return null;
  const den = Number(m[2]);
  if (!Number.isFinite(den) || den <= 0) return null;
  const decimalAV = 20 / den;
  return -Math.log10(decimalAV);
}

export function autoAmbliopiaValue(avOd: string, avOi: string): boolean | null {
  const lOd = snellenToLogMar(avOd);
  const lOi = snellenToLogMar(avOi);
  if (lOd == null || lOi == null) return null;
  return Math.abs(lOd - lOi) >= 0.19;
}
