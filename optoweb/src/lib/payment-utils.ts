import { z } from "zod";
import { today } from "@/lib/date-utils";

export const PagoInputSchema = z.object({
  id: z.string(),
  fecha: z.string(),
  monto: z.number(),
  metodoPago: z.string(),
  nota: z.string(),
});

export type PagoInput = z.infer<typeof PagoInputSchema>;

export function parsePagos(raw: string): PagoInput[] {
  const arr = JSON.parse(raw);
  const parsed = z.array(PagoInputSchema).safeParse(arr);
  if (!parsed.success) throw new Error("Invalid pagos data");
  return parsed.data;
}

export function parseDeletedIds(raw: string): string[] {
  const arr = JSON.parse(raw);
  const parsed = z.array(z.string()).safeParse(arr);
  if (!parsed.success) throw new Error("Invalid deleted-ids data");
  return parsed.data.map((x) => x.trim()).filter(Boolean);
}

export function parseMonto(raw: string): number {
  const n = Number(raw.replace(",", "."));
  return Number.isFinite(n) ? n : NaN;
}

export function todayIsoDate(): string {
  return today();
}
