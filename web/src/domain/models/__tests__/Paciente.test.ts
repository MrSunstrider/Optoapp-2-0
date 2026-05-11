import { describe, it, expect } from "vitest";
import { PacienteSchema } from "@/domain/models/Paciente";

describe("PacienteSchema", () => {
  it("accepts valid paciente", () => {
    const result = PacienteSchema.safeParse({
      id: "p1",
      nombre_completo: "Juan Perez",
      edad: 30,
      telefono: "123456789",
      fecha_creacion: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.success).toBe(true);
  });

  it("accepts paciente with all optional fields", () => {
    const result = PacienteSchema.safeParse({
      id: "p1",
      nombre_completo: "Juan Perez",
      edad: 30,
      telefono: "123456789",
      fecha_creacion: "2026-05-09",
      optica_id: "optica1",
      dni: "12345678",
      fecha_nacimiento: "1990-01-01",
      sexo: "M",
      email: "juan@example.com",
      historia_optometrica: "H-001",
      direccion: "Av. Siempre Viva 123",
      distrito: "Lima",
      ocupacion: "Ingeniero",
      acompanante: "Maria Perez",
      hobbies: "Leer",
      ultimas_etiquetas: "tag1,tag2",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing id", () => {
    const result = PacienteSchema.safeParse({
      nombre_completo: "Juan Perez",
      edad: 30,
      telefono: "123456789",
      fecha_creacion: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.success).toBe(false);
  });

  it("rejects non-number edad", () => {
    const result = PacienteSchema.safeParse({
      id: "p1",
      nombre_completo: "Juan Perez",
      edad: "treinta",
      telefono: "123456789",
      fecha_creacion: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.success).toBe(false);
  });

  it("allows null optional fields", () => {
    const result = PacienteSchema.safeParse({
      id: "p1",
      nombre_completo: "Juan Perez",
      edad: 30,
      telefono: "123456789",
      fecha_creacion: "2026-05-09",
      optica_id: "optica1",
      dni: null,
      email: null,
    });
    expect(result.success).toBe(true);
  });
});
