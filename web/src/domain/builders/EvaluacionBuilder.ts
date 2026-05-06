import { Evaluacion, EvaluacionSchema } from "../models/Evaluacion";
import { v4 as uuidv4 } from "uuid";

export class EvaluacionBuilder {
  private _evaluacion: Partial<Evaluacion> = {
    id: uuidv4(),
    fecha: new Date().toISOString().split("T")[0],
  };

  withPaciente(id: string) {
    this._evaluacion.paciente_id = id;
    return this;
  }

  withOptica(id: string) {
    this._evaluacion.optica_id = id;
    return this;
  }

  withRefraccionOd(esf: string, cil: string, eje: string) {
    const normEsf = this.normalizeSphere(esf);
    const { nEsf, nCil, nEje } = this.transpose(normEsf, cil, eje);
    this._evaluacion.receta_od_esf = nEsf;
    this._evaluacion.receta_od_cil = nCil;
    this._evaluacion.receta_od_eje = nEje;
    this.updateAutoDiagnosis();
    return this;
  }

  withRefraccionOi(esf: string, cil: string, eje: string) {
    const normEsf = this.normalizeSphere(esf);
    const { nEsf, nCil, nEje } = this.transpose(normEsf, cil, eje);
    this._evaluacion.receta_oi_esf = nEsf;
    this._evaluacion.receta_oi_cil = nCil;
    this._evaluacion.receta_oi_eje = nEje;
    this.updateAutoDiagnosis();
    return this;
  }

  build(): Evaluacion {
    return EvaluacionSchema.parse(this._evaluacion);
  }

  private normalizeSphere(value: string): string {
    const v = value.trim().toLowerCase();
    return (v === "plano" || v === "neutro" || v === "pl") ? "0.00" : value;
  }

  private updateAutoDiagnosis() {
    // Aquí iría la lógica de presbicia y anisometropía similar a Android
    const eeOd = this.calculateEE(this._evaluacion.receta_od_esf || "0", this._evaluacion.receta_od_cil || "0");
    const eeOi = this.calculateEE(this._evaluacion.receta_oi_esf || "0", this._evaluacion.receta_oi_cil || "0");
    const diff = Math.abs(eeOd - eeOi);
    // Para simplificar, asumimos campos en el esquema (que deberíamos expandir)
  }

  private calculateEE(esf: string, cil: string): number {
    const e = parseFloat(esf) || 0;
    const c = parseFloat(cil) || 0;
    return e + (c / 2.0);
  }

  private transpose(esf: string, cil: string, eje: string) {
    const c = parseFloat(cil) || 0;
    if (c <= 0) return { nEsf: esf, nCil: cil, nEje: eje };

    const e = parseFloat(esf) || 0;
    const a = parseInt(eje) || 0;

    const nEsf = (e + c).toFixed(2);
    const nCil = (-c).toFixed(2);
    let nEje = a + 90;
    if (nEje > 180) nEje -= 180;

    return { nEsf, nCil, nEje: nEje.toString() };
  }
}
