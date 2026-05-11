import { describe, it, expect } from "vitest";
import { EvaluacionSchema } from "@/domain/models/Evaluacion";

const VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

describe("EvaluacionSchema", () => {
  it("accepts valid evaluacion with defaults", () => {
    const result = EvaluacionSchema.safeParse({
      id: VALID_UUID,
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.success).toBe(true);
  });

  it("applies defaults for optional fields", () => {
    const result = EvaluacionSchema.parse({
      id: VALID_UUID,
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.motivo_consulta).toBe("");
    expect(result.sintomas).toBe("");
    expect(result.receta_od_esf).toBe("");
    expect(result.diagnostico).toBe("");
    expect(result.observaciones).toBe("");
  });

  it("accepts evaluacion with all fields", () => {
    const result = EvaluacionSchema.safeParse({
      id: VALID_UUID,
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
      motivo_consulta: "Control rutinario",
      sintomas: "Visión borrosa",
      receta_od_esf: "-1.00",
      receta_od_cil: "-0.50",
      receta_od_eje: "180",
      receta_oi_esf: "-1.25",
      receta_oi_cil: "-0.75",
      receta_oi_eje: "175",
      diagnostico: "Miopía",
      observaciones: "Paciente estable",
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing id", () => {
    const result = EvaluacionSchema.safeParse({
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.success).toBe(false);
  });

  it("rejects non-uuid id", () => {
    const result = EvaluacionSchema.safeParse({
      id: "not-a-uuid",
      paciente_id: "p1",
      fecha: "2026-05-09",
      optica_id: "optica1",
    });
    expect(result.success).toBe(false);
  });

  it("rejects missing required fields", () => {
    const result = EvaluacionSchema.safeParse({
      id: VALID_UUID,
    });
    expect(result.success).toBe(false);
  });
});
