import { describe, it, expect, beforeEach, vi } from "vitest";
import { GET } from "./route";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";

vi.mock("@/lib/supabase/server");
vi.mock("@/lib/optica-context");

function resolveEmpty() {
  return Promise.resolve({ data: null, error: null });
}

function fromChain() {
  const chain: Record<string, unknown> = {
    select: () => chain,
    eq: () => chain,
    neq: () => chain,
    lt: () => chain,
    lte: () => chain,
    gte: () => chain,
    order: () => chain,
    limit: () => chain,
    abortSignal: () => resolveEmpty(),
    maybeSingle: () => resolveEmpty(),
  };
  return chain;
}

function makeRequest(type: string): Request {
  return new Request(`http://localhost/api/dashboard/export?type=${type}`);
}

describe("GET /api/dashboard/export", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(createClient).mockResolvedValue({
      auth: {
        getUser: vi
          .fn()
          .mockResolvedValue({ data: { user: { id: "user-1" } } }),
      },
      from: vi.fn(fromChain),
    } as never);
  });

  describe("auth guards", () => {
    it("returns 401 when not authenticated", async () => {
      vi.mocked(createClient).mockResolvedValue({
        auth: {
          getUser: vi
            .fn()
            .mockResolvedValue({ data: { user: null } }),
        },
        from: vi.fn(),
      } as never);

      const response = await GET(makeRequest("pendientes"));
      expect(response.status).toBe(401);

      const body = await response.json();
      expect(body.error).toBeDefined();
    });

    it("returns 400 when no optica context", async () => {
      vi.mocked(getActiveOpticaContext).mockResolvedValue(null);

      const response = await GET(makeRequest("pendientes"));
      expect(response.status).toBe(400);

      const body = await response.json();
      expect(body.error).toBeDefined();
    });
  });

  describe("role-based access control", () => {
    it("returns 400 for invalid export type", async () => {
      vi.mocked(getActiveOpticaContext).mockResolvedValue({
        opticaId: "optica-1",
        rol: "admin",
        nombre: "Optica Test",
      });

      const response = await GET(makeRequest("invalid-type"));
      expect(response.status).toBe(400);
    });

    describe("invitado role", () => {
      beforeEach(() => {
        vi.mocked(getActiveOpticaContext).mockResolvedValue({
          opticaId: "optica-1",
          rol: "invitado",
          nombre: "Optica Test",
        });
      });

      it("gets 403 on pendientes (financial export)", async () => {
        const response = await GET(makeRequest("pendientes"));
        expect(response.status).toBe(403);
      });

      it("gets 403 on cierre (financial export)", async () => {
        const response = await GET(makeRequest("cierre"));
        expect(response.status).toBe(403);
      });

      it("gets 403 on inventario-csv (module export)", async () => {
        const response = await GET(makeRequest("inventario-csv"));
        expect(response.status).toBe(403);
      });

      it("gets 403 on inventario-pdf (module export)", async () => {
        const response = await GET(makeRequest("inventario-pdf"));
        expect(response.status).toBe(403);
      });
    });

    describe("admin role", () => {
      beforeEach(() => {
        vi.mocked(getActiveOpticaContext).mockResolvedValue({
          opticaId: "optica-1",
          rol: "admin",
          nombre: "Optica Test",
        });
      });

      it("is allowed on pendientes", async () => {
        const response = await GET(makeRequest("pendientes"));
        expect(response.status).toBe(200);
      });

      it("is allowed on cierre", async () => {
        const response = await GET(makeRequest("cierre"));
        expect(response.status).toBe(200);
      });

      it("is allowed on inventario-csv", async () => {
        const response = await GET(makeRequest("inventario-csv"));
        expect(response.status).toBe(200);
      });

      it("is allowed on inventario-pdf", async () => {
        const response = await GET(makeRequest("inventario-pdf"));
        expect(response.status).toBe(200);
      });
    });

    describe("asesor role", () => {
      beforeEach(() => {
        vi.mocked(getActiveOpticaContext).mockResolvedValue({
          opticaId: "optica-1",
          rol: "asesor",
          nombre: "Optica Test",
        });
      });

      it("gets 403 on pendientes (financial export)", async () => {
        const response = await GET(makeRequest("pendientes"));
        expect(response.status).toBe(403);
      });

      it("gets 403 on cierre (financial export)", async () => {
        const response = await GET(makeRequest("cierre"));
        expect(response.status).toBe(403);
      });

      it("can access inventario-csv", async () => {
        const response = await GET(makeRequest("inventario-csv"));
        expect(response.status).toBe(200);
      });

      it("can access inventario-pdf", async () => {
        const response = await GET(makeRequest("inventario-pdf"));
        expect(response.status).toBe(200);
      });
    });
  });
});
