export type ConfigModuleKey =
  | "seguridad"
  | "laboratorio"
  | "fiscal"
  | "plan-admin"
  | "usuarios-roles"
  | "sucursales"
  | "suscripciones"
  | "gestion-datos";

function norm(role: string): string {
  return role.trim().toLowerCase();
}

export function canManageOpticaSettings(role: string): boolean {
  const r = norm(role);
  return r === "admin" || r === "gerente";
}

export function isReadOnlyRole(role: string): boolean {
  return norm(role) === "invitado" || norm(role) === "lectura";
}

export function canAccessConfigModule(role: string, module: ConfigModuleKey): boolean {
  if (module === "plan-admin") return isInternalRole(role);
  if (module === "gestion-datos") return canManageOpticaSettings(role);
  if (module === "usuarios-roles" || module === "sucursales") return canManageOpticaSettings(role);
  if (module === "fiscal") return true;
  if (module === "seguridad" || module === "suscripciones" || module === "laboratorio")
    return !isReadOnlyRole(role);
  return true;
}

export function isInternalRole(role: string): boolean {
  const r = norm(role);
  return r === "superadmin" || r === "staff" || r === "interno";
}
