import type { ConfigModuleKey } from "@/lib/config/permissions";

export type ConfigModuleMeta = {
  key: ConfigModuleKey;
  title: string;
  description: string;
};

export const CONFIG_MODULES: ConfigModuleMeta[] = [
  {
    key: "seguridad",
    title: "Seguridad y acceso",
    description: "PIN, acceso seguro y preferencias de inicio."
  },
  {
    key: "laboratorio",
    title: "Laboratorio (esta óptica)",
    description: "Parámetros operativos de flujo y prioridad."
  },
  {
    key: "fiscal",
    title: "Datos fiscales",
    description: "Documento fiscal, razón social y contacto."
  },
  {
    key: "plan-admin",
    title: "Administración de plan (interno)",
    description: "Ajustes internos de plan e historial."
  },
  {
    key: "usuarios-roles",
    title: "Usuario y roles",
    description: "Miembros por óptica, roles y revocación."
  },
  {
    key: "sucursales",
    title: "Sucursales",
    description: "Sedes de la óptica y sucursal principal."
  },
  {
    key: "suscripciones",
    title: "Suscripciones y límites",
    description: "Consumo del plan y capacidades habilitadas."
  },
  {
    key: "gestion-datos",
    title: "Gestión de datos",
    description: "Exportación, respaldo y acciones sensibles."
  }
];
