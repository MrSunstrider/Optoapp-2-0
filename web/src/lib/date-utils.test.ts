import { describe, it, expect } from "vitest";
import { dateOnly } from "@/lib/date-utils";

describe("dateOnly", () => {
  it("formats a Date as YYYY-MM-DD without timezone", () => {
    const d = new Date(2026, 4, 15);
    expect(dateOnly(d)).toBe("2026-05-15");
  });

  it("pads single-digit month and day", () => {
    const d = new Date(2026, 0, 5);
    expect(dateOnly(d)).toBe("2026-01-05");
  });

  it("formats with explicit timezone", () => {
    const d = new Date("2026-05-15T12:00:00Z");
    expect(dateOnly(d, "America/Lima")).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it("falls back to local formatting for invalid timezone", () => {
    const d = new Date(2026, 4, 15);
    const result = dateOnly(d, "Invalid/Timezone");
    expect(result).toBe("2026-05-15");
  });

  it("uses local formatting when timezone is null", () => {
    const d = new Date(2026, 4, 15);
    expect(dateOnly(d, null)).toBe("2026-05-15");
  });

  it("uses local formatting when timezone is undefined", () => {
    const d = new Date(2026, 4, 15);
    expect(dateOnly(d, undefined)).toBe("2026-05-15");
  });

  it("uses local formatting when timezone is 'auto'", () => {
    const d = new Date(2026, 4, 15);
    expect(dateOnly(d, "auto")).toBe("2026-05-15");
  });

  it("handles year-end dates", () => {
    const d = new Date(2025, 11, 31);
    expect(dateOnly(d)).toBe("2025-12-31");
  });

  it("handles leap-year date", () => {
    const d = new Date(2024, 1, 29);
    expect(dateOnly(d)).toBe("2024-02-29");
  });
});