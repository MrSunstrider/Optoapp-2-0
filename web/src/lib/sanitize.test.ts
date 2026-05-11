import { describe, it, expect } from "vitest";
import { escapeIlikeFragment } from "@/lib/sanitize";

describe("escapeIlikeFragment", () => {
  it("returns normal text unchanged (no special chars)", () => {
    expect(escapeIlikeFragment("juan")).toBe("juan");
  });

  it("escapes percent signs", () => {
    expect(escapeIlikeFragment("100%")).toBe("100\\%");
  });

  it("escapes underscores", () => {
    expect(escapeIlikeFragment("test_case")).toBe("test\\_case");
  });

  it("escapes backslashes first, then percent and underscore", () => {
    expect(escapeIlikeFragment("path\\name")).toBe("path\\\\name");
  });

  it("returns empty string when given empty string", () => {
    expect(escapeIlikeFragment("")).toBe("");
  });

  it("handles mixed special characters", () => {
    expect(escapeIlikeFragment("100%_test\\")).toBe("100\\%\\_test\\\\");
  });
});
