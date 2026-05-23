/** Nombre de cookie httpOnly para contexto de óptica activa (solo constante; usable en Middleware Edge). */
export const ACTIVE_OPTICA_COOKIE = "optoapp_active_optica";

/**
 * Cookie de caché para la validación de pertenencia a óptica en middleware.
 * Evita una query a usuario_optica en cada request. TTL: 5 minutos.
 */
export const OPTICA_VALIDATED_COOKIE = "optoapp_optica_validated";
export const OPTICA_VALIDATED_TTL_SECS = 300; // 5 minutos
