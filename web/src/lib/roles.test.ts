import { describe, it, expect } from "vitest";
import {
  canViewBiAndReports,
  canAccessModule,
  canReadPacientes,
  canManagePacientes,
  canDeletePaciente,
  canManageFiscalConfig,
} from "@/lib/roles";

function describeRoleTests(fn: (role: string) => boolean, label: string) {
  describe(label, () => {
    it("returns true for admin", () => {
      expect(fn("admin")).toBe(true);
    });

    it("returns true for gerente", () => {
      expect(fn("gerente")).toBe(true);
    });

    it("handles case insensitivity", () => {
      expect(fn("Admin")).toBe(true);
      expect(fn("ADMIN")).toBe(true);
      expect(fn("admin")).toBe(true);
    });

    it("handles whitespace", () => {
      expect(fn("  admin  ")).toBe(true);
    });
  });
}

describe("canViewBiAndReports", () => {
  it("returns true for admin", () => {
    expect(canViewBiAndReports("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canViewBiAndReports("gerente")).toBe(true);
  });

  it("returns true for especialista", () => {
    expect(canViewBiAndReports("especialista")).toBe(true);
  });

  it("returns false for asesor", () => {
    expect(canViewBiAndReports("asesor")).toBe(false);
  });

  it("returns false for asesora", () => {
    expect(canViewBiAndReports("asesora")).toBe(false);
  });

  it("returns false for ventas", () => {
    expect(canViewBiAndReports("ventas")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(canViewBiAndReports("invitado")).toBe(false);
  });

  it("is case insensitive", () => {
    expect(canViewBiAndReports("Admin")).toBe(true);
    expect(canViewBiAndReports("ASESOR")).toBe(false);
    expect(canViewBiAndReports("INVITADO")).toBe(false);
  });

  it("handles whitespace", () => {
    expect(canViewBiAndReports("  admin  ")).toBe(true);
    expect(canViewBiAndReports("  invitado  ")).toBe(false);
  });
});

describe("canAccessModule", () => {
  describe("pacientes module", () => {
    it("returns true for admin", () => {
      expect(canAccessModule("admin", "pacientes")).toBe(true);
    });

    it("returns false for invitado", () => {
      expect(canAccessModule("invitado", "pacientes")).toBe(false);
    });
  });

  describe("servicios-varios module", () => {
    it("returns true for admin", () => {
      expect(canAccessModule("admin", "servicios-varios")).toBe(true);
    });

    it("returns false for invitado", () => {
      expect(canAccessModule("invitado", "servicios-varios")).toBe(false);
    });
  });

  describe("reportes module", () => {
    it("returns true for admin", () => {
      expect(canAccessModule("admin", "reportes")).toBe(true);
    });

    it("returns false for invitado", () => {
      expect(canAccessModule("invitado", "reportes")).toBe(false);
    });

    it("returns true for gerente", () => {
      expect(canAccessModule("gerente", "reportes")).toBe(true);
    });

    it("returns false for asesor", () => {
      expect(canAccessModule("asesor", "reportes")).toBe(false);
    });
  });

  describe("estadisticas module", () => {
    it("returns true for admin", () => {
      expect(canAccessModule("admin", "estadisticas")).toBe(true);
    });

    it("returns true for especialista", () => {
      expect(canAccessModule("especialista", "estadisticas")).toBe(true);
    });

    it("returns false for ventas", () => {
      expect(canAccessModule("ventas", "estadisticas")).toBe(false);
    });
  });

  describe("other modules", () => {
    it("returns true for all roles including invitado (fallback)", () => {
      // Non-restricted modules return true for all roles
      expect(canAccessModule("admin", "inventario")).toBe(true);
      expect(canAccessModule("asesor", "inventario")).toBe(true);
      expect(canAccessModule("invitado", "inventario")).toBe(true);
    });

    it("is case insensitive for module name", () => {
      expect(canAccessModule("admin", "PACIENTES")).toBe(true);
      expect(canAccessModule("invitado", "PACIENTES")).toBe(false);
    });
  });
});

describe("canReadPacientes", () => {
  it("returns true for admin", () => {
    expect(canReadPacientes("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canReadPacientes("gerente")).toBe(true);
  });

  it("returns true for especialista", () => {
    expect(canReadPacientes("especialista")).toBe(true);
  });

  it("returns true for asesor", () => {
    expect(canReadPacientes("asesor")).toBe(true);
  });

  it("returns true for asesora", () => {
    expect(canReadPacientes("asesora")).toBe(true);
  });

  it("returns true for ventas", () => {
    expect(canReadPacientes("ventas")).toBe(true);
  });

  it("returns false for invitado", () => {
    expect(canReadPacientes("invitado")).toBe(false);
  });

  it("handles case insensitivity", () => {
    expect(canReadPacientes("Invitado")).toBe(false);
    expect(canReadPacientes("Admin")).toBe(true);
  });
});

describe("canManagePacientes", () => {
  it("returns true for admin", () => {
    expect(canManagePacientes("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canManagePacientes("gerente")).toBe(true);
  });

  it("returns true for especialista", () => {
    expect(canManagePacientes("especialista")).toBe(true);
  });

  it("returns true for asesor", () => {
    expect(canManagePacientes("asesor")).toBe(true);
  });

  it("returns true for asesora", () => {
    expect(canManagePacientes("asesora")).toBe(true);
  });

  it("returns true for ventas", () => {
    expect(canManagePacientes("ventas")).toBe(true);
  });

  it("returns false for invitado", () => {
    expect(canManagePacientes("invitado")).toBe(false);
  });
});

describe("canDeletePaciente", () => {
  it("returns true for admin", () => {
    expect(canDeletePaciente("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canDeletePaciente("gerente")).toBe(true);
  });

  it("returns false for especialista", () => {
    expect(canDeletePaciente("especialista")).toBe(false);
  });

  it("returns false for asesor", () => {
    expect(canDeletePaciente("asesor")).toBe(false);
  });

  it("returns false for asesora", () => {
    expect(canDeletePaciente("asesora")).toBe(false);
  });

  it("returns false for ventas", () => {
    expect(canDeletePaciente("ventas")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(canDeletePaciente("invitado")).toBe(false);
  });

  it("is case insensitive", () => {
    expect(canDeletePaciente("Admin")).toBe(true);
    expect(canDeletePaciente("GERENTE")).toBe(true);
    expect(canDeletePaciente("Invitado")).toBe(false);
  });

  it("handles whitespace", () => {
    expect(canDeletePaciente("  admin  ")).toBe(true);
    expect(canDeletePaciente("  invitado  ")).toBe(false);
  });
});

describe("canManageFiscalConfig", () => {
  it("returns true for admin", () => {
    expect(canManageFiscalConfig("admin")).toBe(true);
  });

  it("returns true for gerente", () => {
    expect(canManageFiscalConfig("gerente")).toBe(true);
  });

  it("returns false for especialista", () => {
    expect(canManageFiscalConfig("especialista")).toBe(false);
  });

  it("returns false for asesor", () => {
    expect(canManageFiscalConfig("asesor")).toBe(false);
  });

  it("returns false for asesora", () => {
    expect(canManageFiscalConfig("asesora")).toBe(false);
  });

  it("returns false for ventas", () => {
    expect(canManageFiscalConfig("ventas")).toBe(false);
  });

  it("returns false for invitado", () => {
    expect(canManageFiscalConfig("invitado")).toBe(false);
  });

  it("is case insensitive", () => {
    expect(canManageFiscalConfig("Admin")).toBe(true);
    expect(canManageFiscalConfig("GERENTE")).toBe(true);
  });

  it("handles whitespace", () => {
    expect(canManageFiscalConfig("  admin  ")).toBe(true);
  });
});
