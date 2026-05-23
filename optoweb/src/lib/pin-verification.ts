import { createHash, timingSafeEqual } from "node:crypto";

export type PinVerifyResult =
  | { ok: true }
  | { ok: false; code: "mismatch" | "unconfigured" };

/**
 * Verificación server-side. El hash esperado (`optoapp_web_pin_sha256` en user_metadata)
 * debe ser el SHA-256 en hexadecimal de: `${PEPPER}|${userId}|${pin}` (utf8),
 * donde PEPPER = env OPTOAPP_WEB_PIN_PEPPER (obligatorio en producción si se usa hash).
 *
 * Desarrollo: si no hay hash en metadata, se puede aceptar OPTOAPP_WEB_PIN_DEV (6 dígitos).
 */
export function verifyWebPinAgainstMetadata(
  pin: string,
  userId: string,
  userMetadata: Record<string, unknown>
): PinVerifyResult {
  const pepper = process.env.OPTOAPP_WEB_PIN_PEPPER ?? "";
  const storedHex = userMetadata.optoapp_web_pin_sha256;

  if (typeof storedHex === "string" && /^[a-f0-9]{64}$/i.test(storedHex)) {
    const actual = createHash("sha256")
      .update(`${pepper}|${userId}|${pin}`, "utf8")
      .digest("hex");
    try {
      const a = Buffer.from(storedHex.toLowerCase(), "hex");
      const b = Buffer.from(actual, "hex");
      if (a.length === b.length && timingSafeEqual(a, b)) {
        return { ok: true };
      }
    } catch {
      return { ok: false, code: "mismatch" };
    }
    return { ok: false, code: "mismatch" };
  }

  if (
    process.env.NODE_ENV === "development" &&
    process.env.OPTOAPP_WEB_PIN_DEV &&
    pin === process.env.OPTOAPP_WEB_PIN_DEV
  ) {
    return { ok: true };
  }

  return { ok: false, code: "unconfigured" };
}

export function hashWebPin(pin: string, userId: string): string {
  const pepper = process.env.OPTOAPP_WEB_PIN_PEPPER ?? "";
  return createHash("sha256")
    .update(`${pepper}|${userId}|${pin}`, "utf8")
    .digest("hex");
}
