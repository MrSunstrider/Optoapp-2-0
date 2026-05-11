import { describe, it, expect } from "vitest";
import {
  DispensacionSchema,
  PagoSchema,
  ServicioExtraSchema,
} from "@/domain/models/Financial";

describe("DispensacionSchema", () => {
  it("accepts valid dispensacion", () => {
    const result = DispensacionSchema.safeParse({
      id: "d1",
      ot: "OT-2026-001",
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
      monto_total: 150.0,
      monto_pagado: 75.0,
      estado_entrega: "Pendiente",
    });
    expect(result.success).toBe(true);
  });

  it("accepts dispensacion with all optional fields", () => {
    const result = DispensacionSchema.safeParse({
      id: "d1",
      ot: "OT-2026-001",
      montura_id: "m1",
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
      tipo_montura: "Metálica",
      material_montura: "Titanio",
      tipo_lente: "Monofocal",
      material_lente: "Vidrio",
      tratamientos: ["AR", "UV"],
      color_lente: "Transparente",
      notas_diseno: "Biselado",
      origen_montura: "Italia",
      tipo_aro: "Completo",
      descripcion_montura: "Modelo clásico",
      monto_total: 150.0,
      metodo_pago: "Efectivo",
      monto_pagado: 75.0,
      estado_entrega: "Pendiente",
      fecha_vencimiento_garantia: "2027-05-09",
      distancia_lente: "65",
      altura: "35",
      sub_tipo_bifocal: "",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing monto_total", () => {
    const result = DispensacionSchema.safeParse({
      id: "d1",
      ot: "OT-2026-001",
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
      monto_pagado: 75.0,
      estado_entrega: "Pendiente",
    });
    expect(result.success).toBe(false);
  });

  it("rejects non-number monto_total", () => {
    const result = DispensacionSchema.safeParse({
      id: "d1",
      ot: "OT-2026-001",
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
      monto_total: "cien",
      monto_pagado: 75.0,
      estado_entrega: "Pendiente",
    });
    expect(result.success).toBe(false);
  });

  it("allows null optional montura_id", () => {
    const result = DispensacionSchema.safeParse({
      id: "d1",
      ot: "OT-2026-001",
      montura_id: null,
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
      monto_total: 150.0,
      monto_pagado: 75.0,
      estado_entrega: "Pendiente",
    });
    expect(result.success).toBe(true);
  });
});

describe("PagoSchema", () => {
  it("accepts valid pago", () => {
    const result = PagoSchema.safeParse({
      id: "pay1",
      fecha: "2026-05-09",
      tipo: "contado",
      monto: 100.0,
      metodo_pago: "Efectivo",
      optica_id: "optica1",
    });
    expect(result.success).toBe(true);
  });

  it("accepts pago with dispensacion reference", () => {
    const result = PagoSchema.safeParse({
      id: "pay1",
      dispensacion_id: "d1",
      fecha: "2026-05-09",
      tipo: "contado",
      monto: 100.0,
      metodo_pago: "Efectivo",
      optica_id: "optica1",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing monto", () => {
    const result = PagoSchema.safeParse({
      id: "pay1",
      fecha: "2026-05-09",
      tipo: "contado",
      metodo_pago: "Efectivo",
      optica_id: "optica1",
    });
    expect(result.success).toBe(false);
  });
});

describe("ServicioExtraSchema", () => {
  it("accepts valid servicio", () => {
    const result = ServicioExtraSchema.safeParse({
      id: "s1",
      descripcion: "Examen visual",
      monto_total: 50.0,
      a_cuenta: 25.0,
      estado: "Pendiente",
      fecha: "2026-05-09",
      metodo_pago: "Efectivo",
      optica_id: "optica1",
    });
    expect(result.success).toBe(true);
  });

  it("accepts servicio with optional fields", () => {
    const result = ServicioExtraSchema.safeParse({
      id: "s1",
      ot: "OT-SRV-001",
      descripcion: "Examen visual",
      monto_total: 50.0,
      a_cuenta: 25.0,
      estado: "Pendiente",
      fecha: "2026-05-09",
      paciente_id: "p1",
      metodo_pago: "Efectivo",
      optica_id: "optica1",
    });
    expect(result.success).toBe(true);
  });

  it("rejects non-number monto_total", () => {
    const result = ServicioExtraSchema.safeParse({
      id: "s1",
      descripcion: "Examen visual",
      monto_total: "cincuenta",
      a_cuenta: 25.0,
      estado: "Pendiente",
      fecha: "2026-05-09",
      metodo_pago: "Efectivo",
      optica_id: "optica1",
    });
    expect(result.success).toBe(false);
  });
});
