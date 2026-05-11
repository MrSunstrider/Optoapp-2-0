import { describe, it, expect, beforeEach, vi } from "vitest";
import { saveDispensacionAction } from "./dispensacion-crud";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManagePacientes } from "@/lib/roles";
import { redirect } from "next/navigation";

vi.mock("@/lib/supabase/server");
vi.mock("@/lib/optica-context");
vi.mock("@/lib/roles");
vi.mock("next/navigation", () => ({
  redirect: vi.fn((url: string) => { throw new Error(url); }),
}));
vi.mock("next/cache", () => ({
  revalidatePath: vi.fn(),
}));

function makeFormData(
  overrides: Record<string, string> = {}
): FormData {
  const fd = new FormData();
  const defaults: Record<string, string> = {
    pacienteId: "pat-1",
    dispensacionId: "",
    fecha: "2025-06-01",
    ot: "OT-001",
    tipoLente: "Monofocal",
    altura: "",
    materialLente: "CR39",
    tratamientosJson: "[]",
    colorLente: "",
    notasDiseno: "",
    origenMontura: "Tienda",
    monturaId: "mont-1",
    previousMonturaId: "",
    tipoAro: "",
    materialMontura: "",
    descripcionMontura: "",
    distanciaLente: "",
    subTipoBifocal: "",
    estadoEntrega: "Pendiente",
    montoTotal: "100",
    pagosJson: "[]",
    pagosDeleteJson: "[]",
    ...overrides,
  };
  for (const [key, value] of Object.entries(defaults)) {
    fd.append(key, value);
  }
  return fd;
}

/** Build a query chain that always resolves successfully. */
function fromChain() {
  const limit = () => Promise.resolve({ data: [], error: null });
  const makeUpdate = () => Promise.resolve({ error: null });
  const insertOrUpsert = () => Promise.resolve({ error: null });

  const chain: Record<string, any> = {
    select: () => chain,
    eq: () => chain,
    neq: () => chain,
    ilike: () => chain,
    limit,
    maybeSingle: () => limit(),
    insert: insertOrUpsert,
    upsert: insertOrUpsert,
    update: () => ({ eq: () => ({ eq: () => makeUpdate() }) }),
    delete: () => ({ eq: () => ({ eq: () => makeUpdate() }) }),
  };
  return chain;
}

describe("saveDispensacionAction — stock adjustment via RPC", () => {
  let mockRpc: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(getActiveOpticaContext).mockResolvedValue({
      opticaId: "optica-1",
      rol: "admin",
      nombre: "Optica Test",
    });

    vi.mocked(canManagePacientes).mockReturnValue(true);
    // No need to re-mock redirect — factory already throws

    mockRpc = vi.fn().mockResolvedValue({
      data: { ok: true, new_stock: 9 },
      error: null,
    });

    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
      from: vi.fn(fromChain),
    } as never);
  });

  it("calls rpc_adjust_montura_stock with delta=-1 for new montura sale", async () => {
    try {
      await saveDispensacionAction(makeFormData());
    } catch {
      // redirect is expected — continue
    }

    expect(mockRpc).toHaveBeenCalledWith(
      "rpc_adjust_montura_stock",
      expect.objectContaining({
        p_montura_id: "mont-1",
        p_delta: -1,
        p_tipo: "SALIDA_VENTA",
        p_optica_id: "optica-1",
      }),
    );
  });

  it("calls rpc twice when montura changes (restore old, decrement new)", async () => {
    try {
      await saveDispensacionAction(
        makeFormData({
          previousMonturaId: "mont-0",
          monturaId: "mont-1",
        }),
      );
    } catch {
      // redirect is expected
    }

    expect(mockRpc).toHaveBeenCalledTimes(2);

    // Restore previous montura
    expect(mockRpc).toHaveBeenCalledWith(
      "rpc_adjust_montura_stock",
      expect.objectContaining({
        p_montura_id: "mont-0",
        p_delta: 1,
        p_tipo: "AJUSTE",
        p_note: expect.stringContaining("Reversion"),
      }),
    );

    // Decrement new montura
    expect(mockRpc).toHaveBeenCalledWith(
      "rpc_adjust_montura_stock",
      expect.objectContaining({
        p_montura_id: "mont-1",
        p_delta: -1,
        p_tipo: "SALIDA_VENTA",
        p_note: expect.stringContaining("Salida"),
      }),
    );
  });

  it("redirects with error=stock when RPC returns insufficient stock", async () => {
    mockRpc.mockResolvedValue({
      data: { ok: false, error: "insufficient" },
      error: null,
    });

    await expect(
      saveDispensacionAction(makeFormData()),
    ).rejects.toThrow("/pacientes/pat-1/dispensaciones/nueva?error=stock");
  });

  it("redirects with error=guardar when RPC returns not_found", async () => {
    mockRpc.mockResolvedValue({
      data: { ok: false, error: "not_found" },
      error: null,
    });

    await expect(
      saveDispensacionAction(makeFormData()),
    ).rejects.toThrow("/pacientes/pat-1/dispensaciones/nueva?error=guardar");
  });

  it("redirects with error=guardar when RPC returns unknown error", async () => {
    mockRpc.mockResolvedValue({
      data: { ok: false, error: "some_other_error" },
      error: null,
    });

    await expect(
      saveDispensacionAction(makeFormData()),
    ).rejects.toThrow("/pacientes/pat-1/dispensaciones/nueva?error=guardar");
  });

  it("does NOT call rpc when origenMontura is not Tienda", async () => {
    try {
      await saveDispensacionAction(
        makeFormData({ origenMontura: "Proveedor" }),
      );
    } catch {
      // redirect is expected (successful save redirect)
    }

    expect(mockRpc).not.toHaveBeenCalled();
  });

  it("does NOT call rpc when previousMonturaId equals finalMonturaId", async () => {
    try {
      await saveDispensacionAction(
        makeFormData({ previousMonturaId: "mont-1", monturaId: "mont-1" }),
      );
    } catch {
      // redirect is expected
    }

    expect(mockRpc).not.toHaveBeenCalled();
  });

  it("redirects with error=guardar when RPC throws a transport error", async () => {
    mockRpc.mockResolvedValue({
      data: null,
      error: { message: "Network error", code: "502" },
    });

    await expect(
      saveDispensacionAction(makeFormData()),
    ).rejects.toThrow("/pacientes/pat-1/dispensaciones/nueva?error=guardar");
  });

  it("passes correct p_fecha to RPC", async () => {
    try {
      await saveDispensacionAction(makeFormData({ fecha: "2025-12-25" }));
    } catch {
      // redirect is expected
    }

    expect(mockRpc).toHaveBeenCalledWith(
      "rpc_adjust_montura_stock",
      expect.objectContaining({ p_fecha: "2025-12-25" }),
    );
  });

  it("passes correct parameters for an edit (dispensacionId set)", async () => {
    try {
      await saveDispensacionAction(
        makeFormData({ dispensacionId: "disp-1", previousMonturaId: "mont-0" }),
      );
    } catch {
      // redirect is expected
    }

    expect(mockRpc).toHaveBeenCalledWith(
      "rpc_adjust_montura_stock",
      expect.objectContaining({
        p_montura_id: "mont-0",
        p_delta: 1,
        p_tipo: "AJUSTE",
      }),
    );

    expect(mockRpc).toHaveBeenCalledWith(
      "rpc_adjust_montura_stock",
      expect.objectContaining({
        p_montura_id: "mont-1",
        p_delta: -1,
        p_tipo: "SALIDA_VENTA",
      }),
    );
  });
});
