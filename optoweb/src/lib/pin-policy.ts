export function isPinRequiredFromUser(user: {
  user_metadata?: Record<string, unknown> | null;
} | null): boolean {
  if (!user?.user_metadata) return true;
  const v = user.user_metadata.optoapp_pin_required;
  return v !== false;
}
