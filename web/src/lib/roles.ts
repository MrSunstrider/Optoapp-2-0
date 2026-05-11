const ROLES_CON_BI = new Set(["admin", "especialista", "gerente"]);
const ROLES_COMERCIALES = new Set(["asesor", "asesora", "ventas"]);
const ROLES_SIN_EXPORTACION = new Set(["invitado"]);

function norm(role: string): string {
  return role.toLowerCase().trim();
}

export function canViewBiAndReports(role: string): boolean {
  const r = norm(role);
  if (ROLES_CON_BI.has(r)) return true;
  if (ROLES_COMERCIALES.has(r)) return false;
  if (ROLES_SIN_EXPORTACION.has(r)) return false;
  return false;
}

export function canAccessModule(role: string, moduleName: string): boolean {
  const moduleKey = moduleName.trim().toLowerCase();
  if (moduleKey === "pacientes") return canReadPacientes(role);
  if (moduleKey === "servicios-varios") return canReadPacientes(role);
  if (moduleKey === "reportes" || moduleKey === "estadisticas")
    return canViewBiAndReports(role);
  if (moduleKey === "inventario") return canReadPacientes(role);
  return true;
}

export function canReadPacientes(role: string): boolean {
  const r = norm(role);
  return r !== "invitado";
}

export function canManagePacientes(role: string): boolean {
  const r = norm(role);
  return r !== "invitado";
}

export function canManageFiscalConfig(role: string): boolean {
  const r = norm(role);
  return r === "admin" || r === "gerente";
}

/** Alineado con RLS/trigger `guard_pacientes_delete` en Supabase. */
export function canDeletePaciente(role: string): boolean {
  const r = norm(role);
  return r === "admin" || r === "gerente";
}
