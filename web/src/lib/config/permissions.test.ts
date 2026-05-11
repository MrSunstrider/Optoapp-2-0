import { describe, it, expect } from "vitest";
import {
  canAccessConfigModule,
  isReadOnlyRole,
  isInternalRole,
  canManageOpticaSettings,
} from "@/lib/config/permissions";
import type { ConfigModuleKey } from "@/lib/config/permissions";

const ALL_MODULES: ConfigModuleKey[] = [
  "seguridad",
  "laboratorio",
  "fiscal",
  "plan-admin",
  "usuarios-roles",
  "sucursales",
  "suscripciones",
  "gestion-datos",
];

describe("canAccessConfigModule", () => {
  describe("plan-admin module", () => {
    it("allows superadmin", () => {
      expect(canAccessConfigModule("superadmin", "plan-admin")).toBe(true);
    });

    it("allows staff", () => {
      expect(canAccessConfigModule("staff", "plan-admin")).toBe(true);
    });

    it("blocks admin", () => {
      expect(canAccessConfigModule("admin", "plan-admin")).toBe(false);
    });

    it("blocks gerente", () => {
      expect(canAccessConfigModule("gerente", "plan-admin")).toBe(false);
    });

    it("blocks invitado", () => {
      expect(canAccessConfigModule("invitado", "plan-admin")).toBe(false);
    });
  });

  describe("gestion-datos module", () => {
    it("allows admin", () => {
      expect(canAccessConfigModule("admin", "gestion-datos")).toBe(true);
    });

    it("allows gerente", () => {
      expect(canAccessConfigModule("gerente", "gestion-datos")).toBe(true);
    });

    it("blocks invitado", () => {
      expect(canAccessConfigModule("invitado", "gestion-datos")).toBe(false);
    });
  });

  describe("usuarios-roles module", () => {
    it("allows admin", () => {
      expect(canAccessConfigModule("admin", "usuarios-roles")).toBe(true);
    });

    it("allows gerente", () => {
      expect(canAccessConfigModule("gerente", "usuarios-roles")).toBe(true);
    });

    it("blocks invitado", () => {
      expect(canAccessConfigModule("invitado", "usuarios-roles")).toBe(false);
    });

    it("blocks lectura", () => {
      expect(canAccessConfigModule("lectura", "usuarios-roles")).toBe(false);
    });
  });

  describe("sucursales module", () => {
    it("allows admin", () => {
      expect(canAccessConfigModule("admin", "sucursales")).toBe(true);
    });

    it("blocks invitado", () => {
      expect(canAccessConfigModule("invitado", "sucursales")).toBe(false);
    });
  });

  describe("fiscal module", () => {
    it("allows everyone including invitado", () => {
      for (const role of ["admin", "gerente", "invitado", "asesor"]) {
        expect(canAccessConfigModule(role, "fiscal")).toBe(true);
      }
    });
  });

  describe("seguridad, suscripciones, laboratorio modules", () => {
    it("allow admin", () => {
      for (const module of ["seguridad", "suscripciones", "laboratorio"] as ConfigModuleKey[]) {
        expect(canAccessConfigModule("admin", module)).toBe(true);
      }
    });

    it("block invitado", () => {
      for (const module of ["seguridad", "suscripciones", "laboratorio"] as ConfigModuleKey[]) {
        expect(canAccessConfigModule("invitado", module)).toBe(false);
      }
    });

    it("block lectura", () => {
      for (const module of ["seguridad", "suscripciones", "laboratorio"] as ConfigModuleKey[]) {
        expect(canAccessConfigModule("lectura", module)).toBe(false);
      }
    });

    it("allow asesor", () => {
      for (const module of ["seguridad", "suscripciones", "laboratorio"] as ConfigModuleKey[]) {
        expect(canAccessConfigModule("asesor", module)).toBe(true);
      }
    });
  });
});

describe("isReadOnlyRole", () => {
  it("returns true for invitado", () => {
    expect(isReadOnlyRole("invitado")).toBe(true);
  });

  it("returns true for lectura", () => {
    expect(isReadOnlyRole("lectura")).toBe(true);
  });

  it("returns false for admin", () => {
    expect(isReadOnlyRole("admin")).toBe(false);
  });

  it("returns false for gerente", () => {
    expect(isReadOnlyRole("gerente")).toBe(false);
  });

  it("returns false for asesor", () => {
    expect(isReadOnlyRole("asesor")).toBe(false);
  });

  it("handles case insensitivity", () => {
    expect(isReadOnlyRole("Invitado")).toBe(true);
    expect(isReadOnlyRole("LECTURA")).toBe(true);
  });

  it("handles whitespace", () => {
    expect(isReadOnlyRole("  invitado  ")).toBe(true);
  });
});

describe("isInternalRole", () => {
  it("returns true for superadmin", () => {
    expect(isInternalRole("superadmin")).toBe(true);
  });

  it("returns true for staff", () => {
    expect(isInternalRole("staff")).toBe(true);
  });

  it("returns true for interno", () => {
    expect(isInternalRole("interno")).toBe(true);
  });

  it("returns false for admin", () => {
    expect(isInternalRole("admin")).toBe(false);
  });

  it("returns false for gerente", () => {
    expect(isInternalRole("gerente")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(isInternalRole("invitado")).toBe(false);
  });

  it("handles case insensitivity", () => {
    expect(isInternalRole("Superadmin")).toBe(true);
    expect(isInternalRole("STAFF")).toBe(true);
  });
});

describe("canManageOpticaSettings", () => {
  it("returns true for admin", () => {
    expect(canManageOpticaSettings("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canManageOpticaSettings("gerente")).toBe(true);
  });

  it("returns false for especialista", () => {
    expect(canManageOpticaSettings("especialista")).toBe(false);
  });

  it("returns false for asesor", () => {
    expect(canManageOpticaSettings("asesor")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(canManageOpticaSettings("invitado")).toBe(false);
  });

  it("handles case insensitivity", () => {
    expect(canManageOpticaSettings("Admin")).toBe(true);
  });
});
