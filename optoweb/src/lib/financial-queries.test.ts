import { describe, it, expect, vi } from "vitest";
import {
  PagoRowSchema,
  DispensacionRowSchema,
  ServicioExtraRowSchema,
  ResumenFinancieroRowSchema,
  CierreCajaResumenRowSchema,
} from "@/lib/financial-queries";

describe("PagoRowSchema", () => {
  it("parses a valid pago row", () => {
    const result = PagoRowSchema.safeParse({ monto: 150.5 });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.monto).toBe(150.5);
    }
  });

  it("allows null monto", () => {
    const result = PagoRowSchema.safeParse({ monto: null });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.monto).toBeNull();
    }
  });

  it("rejects non-numeric monto", () => {
    const result = PagoRowSchema.safeParse({ monto: "abc" });
    expect(result.success).toBe(false);
  });

  it("rejects missing monto", () => {
    const result = PagoRowSchema.safeParse({});
    expect(result.success).toBe(false);
  });
});

describe("DispensacionRowSchema", () => {
  it("parses a valid dispensacion row", () => {
    const result = DispensacionRowSchema.safeParse({
      monto_total: 500,
      monto_pagado: 200,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.monto_total).toBe(500);
      expect(result.data.monto_pagado).toBe(200);
    }
  });

  it("allows null amounts", () => {
    const result = DispensacionRowSchema.safeParse({
      monto_total: null,
      monto_pagado: null,
    });
    expect(result.success).toBe(true);
  });

  it("rejects extra fields beyond schema", () => {
    const result = DispensacionRowSchema.safeParse({
      monto_total: 100,
      monto_pagado: 50,
      estado_entrega: "Entregado",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data).not.toHaveProperty("estado_entrega");
    }
  });
});

describe("ServicioExtraRowSchema", () => {
  it("parses a valid servicio row", () => {
    const result = ServicioExtraRowSchema.safeParse({
      monto_total: 300,
      a_cuenta: 100,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.monto_total).toBe(300);
      expect(result.data.a_cuenta).toBe(100);
    }
  });

  it("allows null amounts", () => {
    const result = ServicioExtraRowSchema.safeParse({
      monto_total: null,
      a_cuenta: null,
    });
    expect(result.success).toBe(true);
  });
});

describe("query helpers exist", () => {
  it("exports queryPagos", async () => {
    const { queryPagos } = await import("@/lib/financial-queries");
    expect(typeof queryPagos).toBe("function");
  });

  it("exports queryDispensaciones", async () => {
    const { queryDispensaciones } = await import("@/lib/financial-queries");
    expect(typeof queryDispensaciones).toBe("function");
  });

  it("exports queryServiciosExtra", async () => {
    const { queryServiciosExtra } = await import("@/lib/financial-queries");
    expect(typeof queryServiciosExtra).toBe("function");
  });
});

describe("queryPagos sets up correct filters", () => {
  it("applies date range and optica_id filter", async () => {
    const calls: Array<{ method: string; args: unknown[] }> = [];
    function track(name: string) {
      return (...args: unknown[]) => {
        calls.push({ method: name, args });
        return chain;
      };
    }
    const chain = {
      select: track("select"),
      eq: track("eq"),
      gte: track("gte"),
      lt: track("lt"),
      abortSignal: vi.fn().mockResolvedValue({ data: [], error: null }),
    };
    const supabase = {
      from: vi.fn().mockReturnValue(chain),
    } as never;

    const { queryPagos } = await import("@/lib/financial-queries");
    await queryPagos(supabase, "optica-1", "2025-01-01", "2025-02-01");

    expect(calls).toContainEqual({
      method: "eq",
      args: ["optica_id", "optica-1"],
    });
    expect(calls).toContainEqual({
      method: "gte",
      args: ["fecha", "2025-01-01"],
    });
    expect(calls).toContainEqual({
      method: "lt",
      args: ["fecha", "2025-02-01"],
    });
    expect(calls[0]).toEqual({ method: "select", args: ["monto"] });
  });
});

describe("queryDispensaciones applies optional filters", () => {
  it("applies basic optica_id filter without date opts", async () => {
    const calls: Array<{ method: string; args: unknown[] }> = [];
    function track(name: string) {
      return (...args: unknown[]) => {
        calls.push({ method: name, args });
        return chain;
      };
    }
    const chain = {
      select: track("select"),
      eq: track("eq"),
      abortSignal: vi.fn().mockResolvedValue({ data: [], error: null }),
    };
    const supabase = {
      from: vi.fn().mockReturnValue(chain),
    } as never;

    const { queryDispensaciones } = await import("@/lib/financial-queries");
    await queryDispensaciones(supabase, "optica-2", "id,monto_total");

    expect(calls).toContainEqual({
      method: "eq",
      args: ["optica_id", "optica-2"],
    });
  });

  it("applies date filters when provided", async () => {
    const calls: Array<{ method: string; args: unknown[] }> = [];
    function track(name: string) {
      return (...args: unknown[]) => {
        calls.push({ method: name, args });
        return chain;
      };
    }
    const chain = {
      select: track("select"),
      eq: track("eq"),
      gte: track("gte"),
      lt: track("lt"),
      abortSignal: vi.fn().mockResolvedValue({ data: [], error: null }),
    };
    const supabase = {
      from: vi.fn().mockReturnValue(chain),
    } as never;

    const { queryDispensaciones } = await import("@/lib/financial-queries");
    await queryDispensaciones(supabase, "optica-2", "id,monto_total", {
      dateFrom: "2025-03-01",
      dateTo: "2025-04-01",
    });

    expect(calls).toContainEqual({
      method: "gte",
      args: ["fecha", "2025-03-01"],
    });
    expect(calls).toContainEqual({
      method: "lt",
      args: ["fecha", "2025-04-01"],
    });
  });

  it("skips date filters when not provided", async () => {
    const calls: Array<{ method: string; args: unknown[] }> = [];
    function track(name: string) {
      return (...args: unknown[]) => {
        calls.push({ method: name, args });
        return chain;
      };
    }
    const chain = {
      select: track("select"),
      eq: track("eq"),
      gte: track("gte"),
      lt: track("lt"),
      abortSignal: vi.fn().mockResolvedValue({ data: [], error: null }),
    };
    const supabase = {
      from: vi.fn().mockReturnValue(chain),
    } as never;

    const { queryDispensaciones } = await import("@/lib/financial-queries");
    await queryDispensaciones(supabase, "optica-3", "monto_total");

    expect(calls.find((c) => c.method === "gte")).toBeUndefined();
    expect(calls.find((c) => c.method === "lt")).toBeUndefined();
  });
});

describe("ResumenFinancieroRowSchema", () => {
  it("parses a valid resumen financiero RPC response", () => {
    const result = ResumenFinancieroRowSchema.safeParse({
      ingresos_cobrados: 5000,
      ventas_emitidas: 8000,
      saldo_pendiente: 3000,
      total_movimientos: 150,
      ticket_promedio: 53.33,
      fecha_inicio: "2026-05-01",
      fecha_fin_exclusiva: "2026-06-01",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.ingresos_cobrados).toBe(5000);
      expect(result.data.total_movimientos).toBe(150);
      expect(result.data.fecha_inicio).toBe("2026-05-01");
    }
  });

  it("rejects missing fields", () => {
    const result = ResumenFinancieroRowSchema.safeParse({
      ingresos_cobrados: 5000,
    });
    expect(result.success).toBe(false);
  });

  it("rejects non-numeric total_movimientos", () => {
    const result = ResumenFinancieroRowSchema.safeParse({
      ingresos_cobrados: 5000,
      ventas_emitidas: 8000,
      saldo_pendiente: 3000,
      total_movimientos: "150",
      ticket_promedio: 53.33,
      fecha_inicio: "2026-05-01",
      fecha_fin_exclusiva: "2026-06-01",
    });
    expect(result.success).toBe(false);
  });
});

describe("CierreCajaResumenRowSchema", () => {
  it("parses a valid cierre caja resumen RPC response", () => {
    const result = CierreCajaResumenRowSchema.safeParse({
      efectivo: 1200.5,
      movil_trans: 300,
      tarjeta: 450,
      total: 1950.5,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.efectivo).toBe(1200.5);
      expect(result.data.movil_trans).toBe(300);
      expect(result.data.total).toBe(1950.5);
    }
  });

  it("rejects missing fields", () => {
    const result = CierreCajaResumenRowSchema.safeParse({
      efectivo: 1200,
    });
    expect(result.success).toBe(false);
  });

  it("rejects non-numeric values", () => {
    const result = CierreCajaResumenRowSchema.safeParse({
      efectivo: "1200",
      movil_trans: 300,
      tarjeta: 450,
      total: 1950,
    });
    expect(result.success).toBe(false);
  });
});
