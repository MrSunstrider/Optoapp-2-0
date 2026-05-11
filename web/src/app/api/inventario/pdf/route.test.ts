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
    order: () => chain,
    limit: () => chain,
    abortSignal: () => resolveEmpty(),
    maybeSingle: () => resolveEmpty(),
  };
  return chain;
}

describe("GET /api/inventario/pdf", () => {
  const request = new Request("http://localhost/api/inventario/pdf");

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

  it("returns 401 when not authenticated", async () => {
    vi.mocked(createClient).mockResolvedValue({
      auth: {
        getUser: vi
          .fn()
          .mockResolvedValue({ data: { user: null } }),
      },
      from: vi.fn(),
    } as never);

    const response = await GET(request);
    expect(response.status).toBe(401);

    const body = await response.json();
    expect(body.error).toBeDefined();
  });

  describe("role-based access control", () => {
    it("returns 403 for invitado role", async () => {
      vi.mocked(getActiveOpticaContext).mockResolvedValue({
        opticaId: "optica-1",
        rol: "invitado",
        nombre: "Optica Test",
      });

      const response = await GET(request);
      expect(response.status).toBe(403);

      const body = await response.json();
      expect(body.error).toBeDefined();
    });

    it("allows admin role", async () => {
      vi.mocked(getActiveOpticaContext).mockResolvedValue({
        opticaId: "optica-1",
        rol: "admin",
        nombre: "Optica Test",
      });

      const response = await GET(request);
      expect(response.status).toBe(200);
    });

    it("allows asesor role", async () => {
      vi.mocked(getActiveOpticaContext).mockResolvedValue({
        opticaId: "optica-1",
        rol: "asesor",
        nombre: "Optica Test",
      });

      const response = await GET(request);
      expect(response.status).toBe(200);
    });
  });
});
