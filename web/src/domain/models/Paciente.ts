import { z } from "zod";

export const PacienteSchema = z.object({
  id: z.string(),
  nombre_completo: z.string(),
  edad: z.number(),
  telefono: z.string(),
  fecha_creacion: z.string(),
  dni: z.string().nullable().optional(),
  fecha_nacimiento: z.string().nullable().optional(),
  sexo: z.string().nullable().optional(),
  email: z.string().nullable().optional(),
  historia_optometrica: z.string().nullable().optional(),
  direccion: z.string().nullable().optional(),
  distrito: z.string().nullable().optional(),
  ocupacion: z.string().nullable().optional(),
  acompanante: z.string().nullable().optional(),
  hobbies: z.string().nullable().optional(),
  ultimas_etiquetas: z.string().nullable().optional(),
  optica_id: z.string(),
});

export type Paciente = z.infer<typeof PacienteSchema>;
