import { SupabaseClient } from "@supabase/supabase-js";
import { IFinancialRepository } from "@/domain/repositories/FinancialRepository";
import { 
  Dispensacion, DispensacionSchema, 
  Pago, PagoSchema, 
  ServicioExtra, ServicioExtraSchema 
} from "@/domain/models/Financial";

export class SupabaseFinancialRepository implements IFinancialRepository {
  constructor(private supabase: SupabaseClient) {}

  // Dispensaciones
  async getDispensaciones(opticaId: string, options?: { pacienteId?: string, start?: string, end?: string }): Promise<Dispensacion[]> {
    let query = this.supabase
      .from("dispensaciones")
      .select("id,ot,paciente_id,fecha,optica_id,tipo_montura,material_montura,tipo_lente,material_lente,tratamientos,color_lente,notas_diseno,origen_montura,tipo_aro,descripcion_montura,monto_total,metodo_pago,monto_pagado,estado_entrega,fecha_vencimiento_garantia,distancia_lente,altura,sub_tipo_bifocal,montura_id")
      .eq("optica_id", opticaId)
      .order("fecha", { ascending: false });

    if (options?.pacienteId) query = query.eq("paciente_id", options.pacienteId);
    if (options?.start) query = query.gte("fecha", options.start);
    if (options?.end) query = query.lte("fecha", options.end);

    const { data, error } = await query;
    if (error) throw error;
    return (data || []).map(row => DispensacionSchema.parse(row));
  }

  async getDispensacionById(id: string, opticaId: string): Promise<Dispensacion | null> {
    const { data, error } = await this.supabase
      .from("dispensaciones")
      .select("id,ot,paciente_id,fecha,optica_id,tipo_montura,material_montura,tipo_lente,material_lente,tratamientos,color_lente,notas_diseno,origen_montura,tipo_aro,descripcion_montura,monto_total,metodo_pago,monto_pagado,estado_entrega,fecha_vencimiento_garantia,distancia_lente,altura,sub_tipo_bifocal,montura_id")
      .eq("id", id)
      .eq("optica_id", opticaId)
      .maybeSingle();

    if (error) throw error;
    return data ? DispensacionSchema.parse(data) : null;
  }

  async saveDispensacion(dispensacion: Dispensacion): Promise<void> {
    const { error } = await this.supabase
      .from("dispensaciones")
      .upsert(dispensacion);
    if (error) throw error;
  }

  // Pagos
  async getPagos(opticaId: string, options?: { dispensacionId?: string, servicioExtraId?: string, start?: string, end?: string }): Promise<Pago[]> {
    let query = this.supabase
      .from("pagos")
      .select("id,dispensacion_id,servicio_extra_id,fecha,tipo,monto,metodo_pago,nota,optica_id")
      .eq("optica_id", opticaId)
      .order("fecha", { ascending: false });

    if (options?.dispensacionId) query = query.eq("dispensacion_id", options.dispensacionId);
    if (options?.servicioExtraId) query = query.eq("servicio_extra_id", options.servicioExtraId);
    if (options?.start) query = query.gte("fecha", options.start);
    if (options?.end) query = query.lte("fecha", options.end);

    const { data, error } = await query;
    if (error) throw error;
    return (data || []).map(row => PagoSchema.parse(row));
  }

  async savePago(pago: Pago): Promise<void> {
    const { error } = await this.supabase
      .from("pagos")
      .upsert(pago);
    if (error) throw error;
  }

  // Servicios
  async getServiciosExtra(opticaId: string, options?: { pacienteId?: string }): Promise<ServicioExtra[]> {
    let query = this.supabase
      .from("servicios_extra")
      .select("id,ot,descripcion,monto_total,a_cuenta,estado,fecha,paciente_id,metodo_pago,optica_id")
      .eq("optica_id", opticaId)
      .order("fecha", { ascending: false });

    if (options?.pacienteId) query = query.eq("paciente_id", options.pacienteId);

    const { data, error } = await query;
    if (error) throw error;
    return (data || []).map(row => ServicioExtraSchema.parse(row));
  }

  async saveServicioExtra(servicio: ServicioExtra): Promise<void> {
    const { error } = await this.supabase
      .from("servicios_extra")
      .upsert(servicio);
    if (error) throw error;
  }
}
