export type SaveAction = (
  prevState: { error?: string } | null,
  formData: FormData
) => Promise<{ error?: string } | null>;

export type MonturaOption = {
  id: string;
  sku: string | null;
  marca: string | null;
  modelo: string | null;
  stock_actual: number | null;
  activo: boolean | null;
};

export type PagoDraft = {
  id: string;
  fecha: string;
  monto: string;
  metodoPago: string;
  nota: string;
};

export type DispensacionFormProps = {
  pacienteId: string;
  dispensacionId?: string;
  mode: "create" | "edit";
  backHref: string;
  opticaLine: string;
  todayDate: string;
  initial: {
    fecha: string;
    ot: string;
    tipoLente: string;
    subTipoBifocal: string;
    distanciaLente: string;
    altura: string;
    materialLente: string;
    tratamientos: string[];
    colorLente: string;
    notasDiseno: string;
    origenMontura: string;
    monturaId: string;
    previousMonturaId: string;
    tipoAro: string;
    materialMontura: string;
    descripcionMontura: string;
    montoTotal: string;
    estadoEntrega: string;
    pagos: PagoDraft[];
  };
  monturas: MonturaOption[];
  saveAction: SaveAction;
};

export const TIPO_LENTE = [
  "Monofocal",
  "Bifocal",
  "Progresivo",
  "Ocupacional",
] as const;

export const SUB_BIFOCAL = ["Flaptop", "Invisible"] as const;
export const DISTANCIA = ["Lejos", "Intermedia", "Cerca"] as const;
export const MATERIAL_LENTE = [
  "Resina",
  "Policarbonato",
  "Cristal",
  "Trivex",
] as const;

export const ORIGEN_MONTURA = ["Tienda", "Paciente"] as const;
export const TIPO_ARO = ["Aro Completo", "Semi al aire", "Al aire"] as const;
export const MATERIAL_MONTURA = [
  "Acetato",
  "Metal",
  "Carey",
  "Econ",
] as const;
export const METODO_PAGO = [
  "Efectivo",
  "Tarjeta",
  "Transferencia",
] as const;
export const ESTADO_ENTREGA = ["Pendiente", "Entregado"] as const;
export const TRATAMIENTOS = [
  "Ninguno",
  "Antireflejo",
  "Antirayas",
  "Filtro UV 400",
  "Fotocromático",
  "AR Blue Defense",
] as const;

export function parseNum(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : 0;
}
