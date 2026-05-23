import { z } from "zod/v4";

export const ChangePinSchema = z.object({
  currentPin: z.string().regex(/^\d{6}$/, "PIN debe tener 6 dígitos"),
  newPin: z.string().regex(/^\d{6}$/, "PIN debe tener 6 dígitos"),
  confirmPin: z.string().regex(/^\d{6}$/, "PIN debe tener 6 dígitos"),
});

export type ChangePinInput = z.infer<typeof ChangePinSchema>;

export const VerifyPinSchema = z.object({
  pin: z.string().regex(/^\d{6}$/, "PIN debe tener 6 dígitos"),
});

export type VerifyPinInput = z.infer<typeof VerifyPinSchema>;

export const UserSettingsSchema = z.object({
  pinRequired: z.boolean().optional(),
  automaticReminders: z.boolean().optional(),
  timezone: z.string().nullable().optional(),
  opticaConfigPatch: z.record(z.string(), z.unknown()).optional(),
});

export type UserSettingsInput = z.infer<typeof UserSettingsSchema>;

export const CierreCajaSchema = z.object({
  fecha: z.string().optional(),
  action: z.enum(["close", "reopen"]).optional(),
});

export type CierreCajaInput = z.infer<typeof CierreCajaSchema>;
