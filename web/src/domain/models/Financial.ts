import { z } from "zod";

export const DispensacionSchema = z.object({
  id: z.string(),
  ot: z.string(),
  montura_id: z.string().nullable().optional(),
  paciente_id: z.string(),
  fecha: z.string(),
  optica_id: z.string(),
  tipo_montura: z.string().nullable().optional(),
  material_montura: z.string().nullable().optional(),
  tipo_lente: z.string().nullable().optional(),
  material_lente: z.string().nullable().optional(),
  tratamientos: z.array(z.string()).nullable().optional(),
  color_lente: z.string().nullable().optional(),
  notas_diseno: z.string().nullable().optional(),
  origen_montura: z.string().nullable().optional(),
  tipo_aro: z.string().nullable().optional(),
  descripcion_montura: z.string().nullable().optional(),
  monto_total: z.number(),
  metodo_pago: z.string().nullable().optional(),
  monto_pagado: z.number(),
  estado_entrega: z.string(),
  fecha_vencimiento_garantia: z.string().nullable().optional(),
  distancia_lente: z.string().nullable().optional(),
  altura: z.string().nullable().optional(),
  sub_tipo_bifocal: z.string().nullable().optional(),
});

export type Dispensacion = z.infer<typeof DispensacionSchema>;

export const PagoSchema = z.object({
  id: z.string(),
  dispensacion_id: z.string().nullable().optional(),
  servicio_extra_id: z.string().nullable().optional(),
  fecha: z.string(),
  tipo: z.string(),
  monto: z.number(),
  metodo_pago: z.string(),
  nota: z.string().nullable().optional(),
  optica_id: z.string(),
});

export type Pago = z.infer<typeof PagoSchema>;

export const ServicioExtraSchema = z.object({
  id: z.string(),
  ot: z.string().nullable().optional(),
  descripcion: z.string(),
  monto_total: z.number(),
  a_cuenta: z.number(),
  estado: z.string(),
  fecha: z.string(),
  paciente_id: z.string().nullable().optional(),
  metodo_pago: z.string(),
  optica_id: z.string(),
});

export type ServicioExtra = z.infer<typeof ServicioExtraSchema>;
