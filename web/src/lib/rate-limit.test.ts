import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { checkRateLimit } from "@/lib/rate-limit";
import { createClient } from "@/lib/supabase/server";

vi.mock("@/lib/supabase/server", () => ({
  createClient: vi.fn(),
}));

function makeMockRpc() {
  const store = new Map<
    string,
    { attempts: number; windowStart: number }
  >();
  return vi.fn(
    async (
      _fn: string,
      params: {
        p_limit_key: string;
        p_window_ms: number;
        p_max_attempts: number;
      }
    ) => {
      const { p_limit_key, p_window_ms, p_max_attempts } = params;
      const now = Date.now();
      const entry = store.get(p_limit_key);

      if (!entry || now > entry.windowStart + p_window_ms) {
        store.set(p_limit_key, { attempts: 1, windowStart: now });
        return {
          data: { allowed: true, remaining: p_max_attempts - 1 },
          error: null,
        };
      }
      if (entry.attempts >= p_max_attempts) {
        return { data: { allowed: false, remaining: 0 }, error: null };
      }
      entry.attempts++;
      return {
        data: {
          allowed: true,
          remaining: p_max_attempts - entry.attempts,
        },
        error: null,
      };
    }
  );
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date("2025-06-01T00:00:00Z"));
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe("checkRateLimit", () => {
  it("allows first request and returns remaining count", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();
    const result = await checkRateLimit(key);

    expect(result.allowed).toBe(true);
    expect(result.remaining).toBe(4);
  });

  it("allows requests under the max limit within the window", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();

    const r1 = await checkRateLimit(key);
    expect(r1.allowed).toBe(true);
    expect(r1.remaining).toBe(4);

    const r2 = await checkRateLimit(key);
    expect(r2.allowed).toBe(true);
    expect(r2.remaining).toBe(3);

    const r3 = await checkRateLimit(key);
    expect(r3.allowed).toBe(true);
    expect(r3.remaining).toBe(2);
  });

  it("blocks when exceeding max attempts within the window", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      const result = await checkRateLimit(key);
      expect(result.allowed).toBe(true);
    }

    const blocked = await checkRateLimit(key);
    expect(blocked.allowed).toBe(false);
    expect(blocked.remaining).toBe(0);
  });

  it("resets window after expiry", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      await checkRateLimit(key);
    }

    expect((await checkRateLimit(key)).allowed).toBe(false);

    vi.advanceTimersByTime(60_001);

    const result = await checkRateLimit(key);
    expect(result.allowed).toBe(true);
    expect(result.remaining).toBe(4);
  });

  it("handles concurrent keys independently", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const keyA = crypto.randomUUID();
    const keyB = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      await checkRateLimit(keyA);
    }
    expect((await checkRateLimit(keyA)).allowed).toBe(false);

    expect((await checkRateLimit(keyB)).allowed).toBe(true);
    expect((await checkRateLimit(keyB)).remaining).toBe(3);
  });

  it("decrements remaining correctly for sequential requests", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();

    expect((await checkRateLimit(key)).remaining).toBe(4);
    expect((await checkRateLimit(key)).remaining).toBe(3);
    expect((await checkRateLimit(key)).remaining).toBe(2);
    expect((await checkRateLimit(key)).remaining).toBe(1);
    expect((await checkRateLimit(key)).remaining).toBe(0);

    expect((await checkRateLimit(key)).allowed).toBe(false);
    expect((await checkRateLimit(key)).remaining).toBe(0);
  });

  it("exactly at window boundary: last millisecond blocks", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      await checkRateLimit(key);
    }

    vi.advanceTimersByTime(59_999);

    expect((await checkRateLimit(key)).allowed).toBe(false);
  });

  it("one ms after window expiry resets", async () => {
    const mockRpc = makeMockRpc();
    vi.mocked(createClient).mockResolvedValue({
      rpc: mockRpc,
    } as never);

    const key = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      await checkRateLimit(key);
    }

    expect((await checkRateLimit(key)).allowed).toBe(false);

    vi.advanceTimersByTime(60_001);

    const result = await checkRateLimit(key);
    expect(result.allowed).toBe(true);
    expect(result.remaining).toBe(4);
  });
});
