import { IPacienteRepository } from "../repositories/PacienteRepository";
import { Paciente } from "../models/Paciente";

export class LoggingPacienteRepository implements IPacienteRepository {
  constructor(private inner: IPacienteRepository) {}

  async getAll(opticaId: string, options?: any): Promise<Paciente[]> {
    console.log(`[Web Repo] Obteniendo pacientes para: ${opticaId}`);
    return this.inner.getAll(opticaId, options);
  }

  async getById(id: string, opticaId: string): Promise<Paciente | null> {
    console.log(`[Web Repo] Obteniendo paciente ID: ${id}`);
    return this.inner.getById(id, opticaId);
  }

  async save(paciente: Paciente): Promise<void> {
    console.log(`[Web Repo] Guardando paciente: ${paciente.nombre_completo}`);
    return this.inner.save(paciente);
  }

  async delete(id: string, opticaId: string): Promise<void> {
    console.log(`[Web Repo] Eliminando paciente: ${id}`);
    return this.inner.delete(id, opticaId);
  }
}
