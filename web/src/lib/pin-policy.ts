/**
 * Paridad con Android: `SessionManager.isPinRequired` por defecto true (solo false si se desactiva explícitamente).
 * Web: leer `user_metadata.optoapp_pin_required`; si es `false`, no se exige PIN en esta sesión.
 */
export function isPinRequiredFromUser(user: {
  user_metadata?: Record<string, unknown> | null;
} | null): boolean {
  if (!user?.user_metadata) return true;
  const v = user.user_metadata.optoapp_pin_required;
  return v !== false;
}
