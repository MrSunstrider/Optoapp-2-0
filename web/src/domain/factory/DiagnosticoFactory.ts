import { Evaluacion } from "../models/Evaluacion";

export interface DiagnosticoEngine {
  generate(evaluacion: Evaluacion): string;
}

class StandardEngine implements DiagnosticoEngine {
  generate(evaluacion: Evaluacion): string {
    return "Diagnóstico Estándar (Web)";
  }
}

class AdvancedEngine implements DiagnosticoEngine {
  generate(evaluacion: Evaluacion): string {
    return "Diagnóstico Avanzado (Web)";
  }
}

export class DiagnosticoFactory {
  static create(role: string): DiagnosticoEngine {
    if (role === "admin" || role === "optometrista") {
      return new AdvancedEngine();
    }
    return new StandardEngine();
  }
}
