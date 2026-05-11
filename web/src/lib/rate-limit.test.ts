import { describe, it, expect, beforeEach, vi, afterEach } from "vitest";
import { checkRateLimit } from "@/lib/rate-limit";

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date("2026-05-10T12:00:00Z"));
});

afterEach(() => {
  vi.useRealTimers();
});

describe("checkRateLimit", () => {
  it("allows first request and returns remaining count", () => {
    const key = crypto.randomUUID();
    const result = checkRateLimit(key);

    expect(result.allowed).toBe(true);
    expect(result.remaining).toBe(4);
  });

  it("allows requests under the max limit within the window", () => {
    const key = crypto.randomUUID();

    const r1 = checkRateLimit(key);
    expect(r1.allowed).toBe(true);
    expect(r1.remaining).toBe(4);

    const r2 = checkRateLimit(key);
    expect(r2.allowed).toBe(true);
    expect(r2.remaining).toBe(3);

    const r3 = checkRateLimit(key);
    expect(r3.allowed).toBe(true);
    expect(r3.remaining).toBe(2);
  });

  it("blocks when exceeding max attempts within the window", () => {
    const key = crypto.randomUUID();

    // Consume all 5 allowed attempts
    for (let i = 0; i < 5; i++) {
      const result = checkRateLimit(key);
      expect(result.allowed).toBe(true);
    }

    // 6th should be blocked
    const blocked = checkRateLimit(key);
    expect(blocked.allowed).toBe(false);
    expect(blocked.remaining).toBe(0);
  });

  it("resets window after expiry", () => {
    const key = crypto.randomUUID();

    // Consume all 5 attempts
    for (let i = 0; i < 5; i++) {
      checkRateLimit(key);
    }

    // 6th is blocked
    expect(checkRateLimit(key).allowed).toBe(false);

    // Advance past the 60s window
    vi.advanceTimersByTime(60_001);

    // Should reset and allow again
    const result = checkRateLimit(key);
    expect(result.allowed).toBe(true);
    expect(result.remaining).toBe(4);
  });

  it("handles concurrent keys independently", () => {
    const keyA = crypto.randomUUID();
    const keyB = crypto.randomUUID();

    // Exhaust key A
    for (let i = 0; i < 5; i++) {
      checkRateLimit(keyA);
    }
    expect(checkRateLimit(keyA).allowed).toBe(false);

    // Key B should still be allowed
    expect(checkRateLimit(keyB).allowed).toBe(true);
    expect(checkRateLimit(keyB).remaining).toBe(3);
  });

  it("decrements remaining correctly for sequential requests", () => {
    const key = crypto.randomUUID();

    expect(checkRateLimit(key).remaining).toBe(4);
    expect(checkRateLimit(key).remaining).toBe(3);
    expect(checkRateLimit(key).remaining).toBe(2);
    expect(checkRateLimit(key).remaining).toBe(1);
    expect(checkRateLimit(key).remaining).toBe(0);

    // Now blocked
    expect(checkRateLimit(key).allowed).toBe(false);
    expect(checkRateLimit(key).remaining).toBe(0);
  });

  it("exactly at window boundary: last millisecond blocks", () => {
    const key = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      checkRateLimit(key);
    }

    // Advance to exactly 59999ms (still within window)
    vi.advanceTimersByTime(59_999);

    // Should still be blocked (window not expired yet)
    // But the count check happens before: entry.count (6) > MAX_ATTEMPTS (5) -> blocked
    expect(checkRateLimit(key).allowed).toBe(false);
  });

  it("one ms after window expiry resets (expiry is exclusive: now > resetAt)", () => {
    const key = crypto.randomUUID();

    for (let i = 0; i < 5; i++) {
      checkRateLimit(key);
    }

    expect(checkRateLimit(key).allowed).toBe(false);

    // Advance past exactly 60000ms — code uses > not >=
    vi.advanceTimersByTime(60_001);

    // Should reset and allow
    const result = checkRateLimit(key);
    expect(result.allowed).toBe(true);
    expect(result.remaining).toBe(4);
  });
});
