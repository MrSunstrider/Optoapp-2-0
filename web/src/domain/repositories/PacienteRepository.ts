import { Paciente } from "../models/Paciente";

export interface IPacienteRepository {
  getAll(opticaId: string, options?: { search?: string; page?: number; pageSize?: number }): Promise<Paciente[]>;
  getById(id: string, opticaId: string): Promise<Paciente | null>;
  save(paciente: Paciente): Promise<void>;
  delete(id: string, opticaId: string): Promise<void>;
}
