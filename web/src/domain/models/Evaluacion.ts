import { z } from "zod/v4";

export const EvaluacionSchema = z.object({
  id: z.string().uuid(),
  paciente_id: z.string(),
  fecha: z.string(),
  optica_id: z.string(),
  motivo_consulta: z.string().default(""),
  sintomas: z.string().default(""),
  receta_od_esf: z.string().default(""),
  receta_od_cil: z.string().default(""),
  receta_od_eje: z.string().default(""),
  receta_oi_esf: z.string().default(""),
  receta_oi_cil: z.string().default(""),
  receta_oi_eje: z.string().default(""),
  diagnostico: z.string().default(""),
  observaciones: z.string().default(""),
});

export type Evaluacion = z.infer<typeof EvaluacionSchema>;
