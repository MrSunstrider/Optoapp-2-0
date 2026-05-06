import { SupabaseClient } from "@supabase/supabase-js";
import { IPacienteRepository } from "@/domain/repositories/PacienteRepository";
import { Paciente, PacienteSchema } from "@/domain/models/Paciente";

export class SupabasePacienteRepository implements IPacienteRepository {
  constructor(private supabase: SupabaseClient) {}

  async getAll(opticaId: string, options?: { search?: string; page?: number; pageSize?: number }): Promise<Paciente[]> {
    let query = this.supabase
      .from("pacientes")
      .select("*")
      .eq("optica_id", opticaId)
      .order("fecha_creacion", { ascending: false });

    if (options?.search) {
      const s = options.search.trim();
      query = query.or(`nombre_completo.ilike.%${s}%,telefono.ilike.%${s}%,historia_optometrica.ilike.%${s}%`);
    }

    if (options?.page && options?.pageSize) {
      const from = (options.page - 1) * options.pageSize;
      const to = from + options.pageSize - 1;
      query = query.range(from, to);
    }

    const { data, error } = await query;
    if (error) throw error;

    return (data || []).map((row) => PacienteSchema.parse(row));
  }

  async getById(id: string, opticaId: string): Promise<Paciente | null> {
    const { data, error } = await this.supabase
      .from("pacientes")
      .select("*")
      .eq("id", id)
      .eq("optica_id", opticaId)
      .maybeSingle();

    if (error) throw error;
    if (!data) return null;

    return PacienteSchema.parse(data);
  }

  async save(paciente: Paciente): Promise<void> {
    const { error } = await this.supabase
      .from("pacientes")
      .upsert(paciente);
    
    if (error) throw error;
  }

  async delete(id: string, opticaId: string): Promise<void> {
    const { error } = await this.supabase
      .from("pacientes")
      .delete()
      .eq("id", id)
      .eq("optica_id", opticaId);
    
    if (error) throw error;
  }
}
