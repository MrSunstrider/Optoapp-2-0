import { IPacienteRepository } from "../repositories/PacienteRepository";
import { Paciente } from "../models/Paciente";

export class PacienteRepositoryProxy implements IPacienteRepository {
  constructor(
    private realRepository: IPacienteRepository,
    private userRole: string
  ) {}

  async getAll(opticaId: string, options?: any): Promise<Paciente[]> {
    return this.realRepository.getAll(opticaId, options);
  }

  async getById(id: string, opticaId: string): Promise<Paciente | null> {
    return this.realRepository.getById(id, opticaId);
  }

  async save(paciente: Paciente): Promise<void> {
    return this.realRepository.save(paciente);
  }

  async delete(id: string, opticaId: string): Promise<void> {
    if (this.userRole !== "admin") {
      throw new Error("Acceso denegado: Solo administradores pueden eliminar.");
    }
    return this.realRepository.delete(id, opticaId);
  }
}
