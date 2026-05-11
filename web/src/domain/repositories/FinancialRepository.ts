import { Dispensacion, Pago, ServicioExtra } from "../models/Financial";

export interface IFinancialRepository {
  // Dispensaciones
  getDispensaciones(opticaId: string, options?: { pacienteId?: string, start?: string, end?: string }): Promise<Dispensacion[]>;
  getDispensacionById(id: string, opticaId: string): Promise<Dispensacion | null>;
  saveDispensacion(dispensacion: Dispensacion): Promise<void>;

  // Pagos
  getPagos(opticaId: string, options?: { dispensacionId?: string, servicioExtraId?: string, start?: string, end?: string }): Promise<Pago[]>;
  savePago(pago: Pago): Promise<void>;

  // Servicios
  getServiciosExtra(opticaId: string, options?: { pacienteId?: string }): Promise<ServicioExtra[]>;
  saveServicioExtra(servicio: ServicioExtra): Promise<void>;
}
