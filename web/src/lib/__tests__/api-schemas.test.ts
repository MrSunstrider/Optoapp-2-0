import { describe, it, expect } from "vitest";
import {
  ChangePinSchema,
  VerifyPinSchema,
  UserSettingsSchema,
  CierreCajaSchema,
} from "@/lib/api-schemas";

describe("ChangePinSchema", () => {
  it("accepts valid 6-digit PINs", () => {
    const result = ChangePinSchema.safeParse({
      currentPin: "123456",
      newPin: "654321",
      confirmPin: "654321",
    });
    expect(result.success).toBe(true);
  });

  it("rejects non-digit characters", () => {
    const result = ChangePinSchema.safeParse({
      currentPin: "12345a",
      newPin: "654321",
      confirmPin: "654321",
    });
    expect(result.success).toBe(false);
  });

  it("rejects PINs shorter than 6 digits", () => {
    const result = ChangePinSchema.safeParse({
      currentPin: "12345",
      newPin: "654321",
      confirmPin: "654321",
    });
    expect(result.success).toBe(false);
  });

  it("rejects PINs longer than 6 digits", () => {
    const result = ChangePinSchema.safeParse({
      currentPin: "1234567",
      newPin: "654321",
      confirmPin: "654321",
    });
    expect(result.success).toBe(false);
  });

  it("rejects empty strings", () => {
    const result = ChangePinSchema.safeParse({
      currentPin: "",
      newPin: "654321",
      confirmPin: "654321",
    });
    expect(result.success).toBe(false);
  });

  it("rejects missing fields", () => {
    const result = ChangePinSchema.safeParse({
      currentPin: "123456",
    });
    expect(result.success).toBe(false);
  });
});

describe("VerifyPinSchema", () => {
  it("accepts valid 6-digit PIN", () => {
    const result = VerifyPinSchema.safeParse({ pin: "123456" });
    expect(result.success).toBe(true);
  });

  it("rejects non-digit PIN", () => {
    const result = VerifyPinSchema.safeParse({ pin: "abc123" });
    expect(result.success).toBe(false);
  });

  it("rejects short PIN", () => {
    const result = VerifyPinSchema.safeParse({ pin: "12345" });
    expect(result.success).toBe(false);
  });

  it("rejects missing pin", () => {
    const result = VerifyPinSchema.safeParse({});
    expect(result.success).toBe(false);
  });
});

describe("UserSettingsSchema", () => {
  it("accepts empty object (all optional)", () => {
    const result = UserSettingsSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it("accepts partial settings", () => {
    const result = UserSettingsSchema.safeParse({
      pinRequired: true,
    });
    expect(result.success).toBe(true);
  });

  it("accepts full settings", () => {
    const result = UserSettingsSchema.safeParse({
      pinRequired: false,
      automaticReminders: true,
      timezone: "America/Argentina/Buenos_Aires",
      opticaConfigPatch: { theme: "dark" },
    });
    expect(result.success).toBe(true);
  });

  it("rejects non-boolean pinRequired", () => {
    const result = UserSettingsSchema.safeParse({
      pinRequired: "yes",
    });
    expect(result.success).toBe(false);
  });

  it("accepts null timezone", () => {
    const result = UserSettingsSchema.safeParse({
      timezone: null,
    });
    expect(result.success).toBe(true);
  });
});

describe("CierreCajaSchema", () => {
  it("accepts empty object (all optional)", () => {
    const result = CierreCajaSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it("accepts close action", () => {
    const result = CierreCajaSchema.safeParse({ action: "close" });
    expect(result.success).toBe(true);
  });

  it("accepts reopen action", () => {
    const result = CierreCajaSchema.safeParse({ action: "reopen" });
    expect(result.success).toBe(true);
  });

  it("accepts fecha", () => {
    const result = CierreCajaSchema.safeParse({ fecha: "2026-05-09" });
    expect(result.success).toBe(true);
  });

  it("rejects invalid action", () => {
    const result = CierreCajaSchema.safeParse({ action: "invalid" });
    expect(result.success).toBe(false);
  });
});
