export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.5"
  }
  public: {
    Tables: {
      cierres_caja: {
        Row: {
          cerrado_at: string | null
          cerrado_por: string | null
          closed_at: string | null
          closed_by: string | null
          created_at: string
          estado: string
          fecha_operativa: string
          id: string
          observaciones: string | null
          optica_id: string
          reopened_at: string | null
          reopened_by: string | null
          total_efectivo_cent: number
          total_general_cent: number
          total_plin_cent: number
          total_tarjeta_cent: number
          total_transferencia_cent: number
          total_yape_cent: number
          updated_at: string
        }
        Insert: {
          cerrado_at?: string | null
          cerrado_por?: string | null
          closed_at?: string | null
          closed_by?: string | null
          created_at?: string
          estado: string
          fecha_operativa: string
          id?: string
          observaciones?: string | null
          optica_id: string
          reopened_at?: string | null
          reopened_by?: string | null
          total_efectivo_cent?: number
          total_general_cent?: number
          total_plin_cent?: number
          total_tarjeta_cent?: number
          total_transferencia_cent?: number
          total_yape_cent?: number
          updated_at?: string
        }
        Update: {
          cerrado_at?: string | null
          cerrado_por?: string | null
          closed_at?: string | null
          closed_by?: string | null
          created_at?: string
          estado?: string
          fecha_operativa?: string
          id?: string
          observaciones?: string | null
          optica_id?: string
          reopened_at?: string | null
          reopened_by?: string | null
          total_efectivo_cent?: number
          total_general_cent?: number
          total_plin_cent?: number
          total_tarjeta_cent?: number
          total_transferencia_cent?: number
          total_yape_cent?: number
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "cierres_caja_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      dispensaciones: {
        Row: {
          altura: string
          color_lente: string
          descripcion_montura: string
          distancia_lente: string
          estado_entrega: string
          fecha: string
          fecha_vencimiento_garantia: string | null
          id: string
          material_lente: string
          material_montura: string
          metodo_pago: string
          monto_pagado: number
          monto_total: number
          montura_id: string | null
          notas_diseno: string
          optica_id: string
          origen_montura: string
          ot: string
          paciente_id: string
          sub_tipo_bifocal: string
          tipo_aro: string
          tipo_lente: string
          tipo_montura: string
          tratamientos: string
          updated_at: string
          updated_by: string | null
        }
        Insert: {
          altura?: string
          color_lente?: string
          descripcion_montura?: string
          distancia_lente?: string
          estado_entrega?: string
          fecha: string
          fecha_vencimiento_garantia?: string | null
          id: string
          material_lente?: string
          material_montura?: string
          metodo_pago?: string
          monto_pagado?: number
          monto_total?: number
          montura_id?: string | null
          notas_diseno?: string
          optica_id?: string
          origen_montura?: string
          ot?: string
          paciente_id: string
          sub_tipo_bifocal?: string
          tipo_aro?: string
          tipo_lente?: string
          tipo_montura?: string
          tratamientos?: string
          updated_at?: string
          updated_by?: string | null
        }
        Update: {
          altura?: string
          color_lente?: string
          descripcion_montura?: string
          distancia_lente?: string
          estado_entrega?: string
          fecha?: string
          fecha_vencimiento_garantia?: string | null
          id?: string
          material_lente?: string
          material_montura?: string
          metodo_pago?: string
          monto_pagado?: number
          monto_total?: number
          montura_id?: string | null
          notas_diseno?: string
          optica_id?: string
          origen_montura?: string
          ot?: string
          paciente_id?: string
          sub_tipo_bifocal?: string
          tipo_aro?: string
          tipo_lente?: string
          tipo_montura?: string
          tratamientos?: string
          updated_at?: string
          updated_by?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "dispensaciones_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "dispensaciones_paciente_id_fkey"
            columns: ["paciente_id"]
            isOneToOne: false
            referencedRelation: "pacientes"
            referencedColumns: ["id"]
          },
        ]
      }
      evaluaciones: {
        Row: {
          add_av: string | null
          add_cerca_od: string | null
          add_cerca_oi: string | null
          add_intermedia_od: string | null
          add_intermedia_oi: string | null
          alergias: string | null
          amsler: string | null
          antecedentes_familiares_oculares: string | null
          antecedentes_familiares_sistemicos: string | null
          antecedentes_personales_oculares: string | null
          antecedentes_personales_sistemicos: string | null
          auto_ambliopia: boolean | null
          auto_anisometropia: boolean | null
          auto_presbicia: boolean | null
          av_cc_ao_cerca: string | null
          av_cc_ao_px: string | null
          av_cc_od_cerca: string | null
          av_cc_od_lejos: string | null
          av_cc_oi_cerca: string | null
          av_cc_oi_lejos: string | null
          av_sc_ao: string | null
          av_sc_ao_cerca: string | null
          av_sc_od_cerca: string | null
          av_sc_od_lejos: string | null
          av_sc_oi_cerca: string | null
          av_sc_oi_lejos: string | null
          balance_od: boolean | null
          balance_oi: boolean | null
          campo_visual: string | null
          campo_visual_descripcion: string | null
          cita_estado: string
          cover_test_10cm: string | null
          cover_test_40cm: string | null
          cover_test_6m: string | null
          diagnostico: string | null
          diagnostico_od: string | null
          diagnostico_oi: string | null
          diagnostico_otros: string | null
          dip_cerca: string | null
          dip_intermedio: string | null
          dip_lejos: string | null
          dip_lejos_mm: number | null
          dip_total_mm: number | null
          dnp_od_mm: number | null
          dnp_oi_mm: number | null
          ducciones_od: string | null
          ducciones_oi: string | null
          estereopsis_segundos: string | null
          estereopsis_valor: string | null
          farnsworth: string | null
          fecha: string
          hirshberg: string | null
          id: string
          ishihara: string | null
          k1_od: string | null
          k1_oi: string | null
          k2_od: string | null
          k2_oi: string | null
          kappa_od: string | null
          kappa_oi: string | null
          lang: string | null
          lc_diametro_od: string | null
          lc_diametro_oi: string | null
          lc_fecha_adaptacion: string | null
          lc_laboratorio: string | null
          lc_material: string | null
          lc_observaciones: string | null
          lc_od_cil: string | null
          lc_od_eje: string | null
          lc_od_esf: string | null
          lc_oi_cil: string | null
          lc_oi_eje: string | null
          lc_oi_esf: string | null
          lc_radio_base_od: string | null
          lc_radio_base_oi: string | null
          lc_tipo_lente: string | null
          medicacion: string | null
          motivo_consulta: string | null
          necesidad_visual: string | null
          obj_od_cil: string | null
          obj_od_eje: string | null
          obj_od_esf: string | null
          obj_oi_cil: string | null
          obj_oi_eje: string | null
          obj_oi_esf: string | null
          observaciones: string | null
          optica_id: string
          osdi_clasificacion: string | null
          osdi_puntuacion: number | null
          otros_ambliopia: boolean | null
          otros_anisometropia: boolean | null
          otros_presbicia: boolean | null
          paciente_id: string
          ph_od: string | null
          ph_oi: string | null
          plan_tratamiento: string | null
          ppc_frl: string | null
          ppc_luz: string | null
          ppc_or: string | null
          prisma_od_base: string | null
          prisma_od_valor: string | null
          prisma_oi_base: string | null
          prisma_oi_valor: string | null
          proxima_cita: string | null
          proxima_fecha_control: string | null
          receta_od_av: string | null
          receta_od_cil: string | null
          receta_od_eje: string | null
          receta_od_esf: string | null
          receta_oi_av: string | null
          receta_oi_cil: string | null
          receta_oi_eje: string | null
          receta_oi_esf: string | null
          reflejo_acomodativo: string | null
          reflejo_consensual: string | null
          reflejo_fotomotor: string | null
          schirmer_od: string | null
          schirmer_oi: string | null
          sensibilidad_contraste: string | null
          sensibilidad_frecuencia: string | null
          sintomas: string | null
          subj_od_cil: string | null
          subj_od_eje: string | null
          subj_od_esf: string | null
          subj_oi_cil: string | null
          subj_oi_eje: string | null
          subj_oi_esf: string | null
          updated_at: string
          updated_by: string | null
          versiones_ao: string | null
          worth: string | null
        }
        Insert: {
          add_av?: string | null
          add_cerca_od?: string | null
          add_cerca_oi?: string | null
          add_intermedia_od?: string | null
          add_intermedia_oi?: string | null
          alergias?: string | null
          amsler?: string | null
          antecedentes_familiares_oculares?: string | null
          antecedentes_familiares_sistemicos?: string | null
          antecedentes_personales_oculares?: string | null
          antecedentes_personales_sistemicos?: string | null
          auto_ambliopia?: boolean | null
          auto_anisometropia?: boolean | null
          auto_presbicia?: boolean | null
          av_cc_ao_cerca?: string | null
          av_cc_ao_px?: string | null
          av_cc_od_cerca?: string | null
          av_cc_od_lejos?: string | null
          av_cc_oi_cerca?: string | null
          av_cc_oi_lejos?: string | null
          av_sc_ao?: string | null
          av_sc_ao_cerca?: string | null
          av_sc_od_cerca?: string | null
          av_sc_od_lejos?: string | null
          av_sc_oi_cerca?: string | null
          av_sc_oi_lejos?: string | null
          balance_od?: boolean | null
          balance_oi?: boolean | null
          campo_visual?: string | null
          campo_visual_descripcion?: string | null
          cita_estado?: string
          cover_test_10cm?: string | null
          cover_test_40cm?: string | null
          cover_test_6m?: string | null
          diagnostico?: string | null
          diagnostico_od?: string | null
          diagnostico_oi?: string | null
          diagnostico_otros?: string | null
          dip_cerca?: string | null
          dip_intermedio?: string | null
          dip_lejos?: string | null
          dip_lejos_mm?: number | null
          dip_total_mm?: number | null
          dnp_od_mm?: number | null
          dnp_oi_mm?: number | null
          ducciones_od?: string | null
          ducciones_oi?: string | null
          estereopsis_segundos?: string | null
          estereopsis_valor?: string | null
          farnsworth?: string | null
          fecha: string
          hirshberg?: string | null
          id: string
          ishihara?: string | null
          k1_od?: string | null
          k1_oi?: string | null
          k2_od?: string | null
          k2_oi?: string | null
          kappa_od?: string | null
          kappa_oi?: string | null
          lang?: string | null
          lc_diametro_od?: string | null
          lc_diametro_oi?: string | null
          lc_fecha_adaptacion?: string | null
          lc_laboratorio?: string | null
          lc_material?: string | null
          lc_observaciones?: string | null
          lc_od_cil?: string | null
          lc_od_eje?: string | null
          lc_od_esf?: string | null
          lc_oi_cil?: string | null
          lc_oi_eje?: string | null
          lc_oi_esf?: string | null
          lc_radio_base_od?: string | null
          lc_radio_base_oi?: string | null
          lc_tipo_lente?: string | null
          medicacion?: string | null
          motivo_consulta?: string | null
          necesidad_visual?: string | null
          obj_od_cil?: string | null
          obj_od_eje?: string | null
          obj_od_esf?: string | null
          obj_oi_cil?: string | null
          obj_oi_eje?: string | null
          obj_oi_esf?: string | null
          observaciones?: string | null
          optica_id?: string
          osdi_clasificacion?: string | null
          osdi_puntuacion?: number | null
          otros_ambliopia?: boolean | null
          otros_anisometropia?: boolean | null
          otros_presbicia?: boolean | null
          paciente_id: string
          ph_od?: string | null
          ph_oi?: string | null
          plan_tratamiento?: string | null
          ppc_frl?: string | null
          ppc_luz?: string | null
          ppc_or?: string | null
          prisma_od_base?: string | null
          prisma_od_valor?: string | null
          prisma_oi_base?: string | null
          prisma_oi_valor?: string | null
          proxima_cita?: string | null
          proxima_fecha_control?: string | null
          receta_od_av?: string | null
          receta_od_cil?: string | null
          receta_od_eje?: string | null
          receta_od_esf?: string | null
          receta_oi_av?: string | null
          receta_oi_cil?: string | null
          receta_oi_eje?: string | null
          receta_oi_esf?: string | null
          reflejo_acomodativo?: string | null
          reflejo_consensual?: string | null
          reflejo_fotomotor?: string | null
          schirmer_od?: string | null
          schirmer_oi?: string | null
          sensibilidad_contraste?: string | null
          sensibilidad_frecuencia?: string | null
          sintomas?: string | null
          subj_od_cil?: string | null
          subj_od_eje?: string | null
          subj_od_esf?: string | null
          subj_oi_cil?: string | null
          subj_oi_eje?: string | null
          subj_oi_esf?: string | null
          updated_at?: string
          updated_by?: string | null
          versiones_ao?: string | null
          worth?: string | null
        }
        Update: {
          add_av?: string | null
          add_cerca_od?: string | null
          add_cerca_oi?: string | null
          add_intermedia_od?: string | null
          add_intermedia_oi?: string | null
          alergias?: string | null
          amsler?: string | null
          antecedentes_familiares_oculares?: string | null
          antecedentes_familiares_sistemicos?: string | null
          antecedentes_personales_oculares?: string | null
          antecedentes_personales_sistemicos?: string | null
          auto_ambliopia?: boolean | null
          auto_anisometropia?: boolean | null
          auto_presbicia?: boolean | null
          av_cc_ao_cerca?: string | null
          av_cc_ao_px?: string | null
          av_cc_od_cerca?: string | null
          av_cc_od_lejos?: string | null
          av_cc_oi_cerca?: string | null
          av_cc_oi_lejos?: string | null
          av_sc_ao?: string | null
          av_sc_ao_cerca?: string | null
          av_sc_od_cerca?: string | null
          av_sc_od_lejos?: string | null
          av_sc_oi_cerca?: string | null
          av_sc_oi_lejos?: string | null
          balance_od?: boolean | null
          balance_oi?: boolean | null
          campo_visual?: string | null
          campo_visual_descripcion?: string | null
          cita_estado?: string
          cover_test_10cm?: string | null
          cover_test_40cm?: string | null
          cover_test_6m?: string | null
          diagnostico?: string | null
          diagnostico_od?: string | null
          diagnostico_oi?: string | null
          diagnostico_otros?: string | null
          dip_cerca?: string | null
          dip_intermedio?: string | null
          dip_lejos?: string | null
          dip_lejos_mm?: number | null
          dip_total_mm?: number | null
          dnp_od_mm?: number | null
          dnp_oi_mm?: number | null
          ducciones_od?: string | null
          ducciones_oi?: string | null
          estereopsis_segundos?: string | null
          estereopsis_valor?: string | null
          farnsworth?: string | null
          fecha?: string
          hirshberg?: string | null
          id?: string
          ishihara?: string | null
          k1_od?: string | null
          k1_oi?: string | null
          k2_od?: string | null
          k2_oi?: string | null
          kappa_od?: string | null
          kappa_oi?: string | null
          lang?: string | null
          lc_diametro_od?: string | null
          lc_diametro_oi?: string | null
          lc_fecha_adaptacion?: string | null
          lc_laboratorio?: string | null
          lc_material?: string | null
          lc_observaciones?: string | null
          lc_od_cil?: string | null
          lc_od_eje?: string | null
          lc_od_esf?: string | null
          lc_oi_cil?: string | null
          lc_oi_eje?: string | null
          lc_oi_esf?: string | null
          lc_radio_base_od?: string | null
          lc_radio_base_oi?: string | null
          lc_tipo_lente?: string | null
          medicacion?: string | null
          motivo_consulta?: string | null
          necesidad_visual?: string | null
          obj_od_cil?: string | null
          obj_od_eje?: string | null
          obj_od_esf?: string | null
          obj_oi_cil?: string | null
          obj_oi_eje?: string | null
          obj_oi_esf?: string | null
          observaciones?: string | null
          optica_id?: string
          osdi_clasificacion?: string | null
          osdi_puntuacion?: number | null
          otros_ambliopia?: boolean | null
          otros_anisometropia?: boolean | null
          otros_presbicia?: boolean | null
          paciente_id?: string
          ph_od?: string | null
          ph_oi?: string | null
          plan_tratamiento?: string | null
          ppc_frl?: string | null
          ppc_luz?: string | null
          ppc_or?: string | null
          prisma_od_base?: string | null
          prisma_od_valor?: string | null
          prisma_oi_base?: string | null
          prisma_oi_valor?: string | null
          proxima_cita?: string | null
          proxima_fecha_control?: string | null
          receta_od_av?: string | null
          receta_od_cil?: string | null
          receta_od_eje?: string | null
          receta_od_esf?: string | null
          receta_oi_av?: string | null
          receta_oi_cil?: string | null
          receta_oi_eje?: string | null
          receta_oi_esf?: string | null
          reflejo_acomodativo?: string | null
          reflejo_consensual?: string | null
          reflejo_fotomotor?: string | null
          schirmer_od?: string | null
          schirmer_oi?: string | null
          sensibilidad_contraste?: string | null
          sensibilidad_frecuencia?: string | null
          sintomas?: string | null
          subj_od_cil?: string | null
          subj_od_eje?: string | null
          subj_od_esf?: string | null
          subj_oi_cil?: string | null
          subj_oi_eje?: string | null
          subj_oi_esf?: string | null
          updated_at?: string
          updated_by?: string | null
          versiones_ao?: string | null
          worth?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "evaluaciones_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "evaluaciones_paciente_id_fkey"
            columns: ["paciente_id"]
            isOneToOne: false
            referencedRelation: "pacientes"
            referencedColumns: ["id"]
          },
        ]
      }
      montura_movimientos: {
        Row: {
          cantidad: number
          fecha: string
          id: string
          montura_id: string
          nota: string
          optica_id: string
          referencia_id: string
          stock_nuevo: number
          stock_previo: number
          tipo: string
        }
        Insert: {
          cantidad: number
          fecha?: string
          id: string
          montura_id: string
          nota?: string
          optica_id: string
          referencia_id?: string
          stock_nuevo: number
          stock_previo: number
          tipo: string
        }
        Update: {
          cantidad?: number
          fecha?: string
          id?: string
          montura_id?: string
          nota?: string
          optica_id?: string
          referencia_id?: string
          stock_nuevo?: number
          stock_previo?: number
          tipo?: string
        }
        Relationships: [
          {
            foreignKeyName: "montura_movimientos_montura_id_fkey"
            columns: ["montura_id"]
            isOneToOne: false
            referencedRelation: "monturas"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "montura_movimientos_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      monturas: {
        Row: {
          activo: boolean
          color: string
          costo: number
          id: string
          marca: string
          modelo: string
          optica_id: string
          precio: number
          sku: string
          stock_actual: number
          stock_minimo: number
          talla: string
          updated_at: string
        }
        Insert: {
          activo?: boolean
          color?: string
          costo?: number
          id: string
          marca?: string
          modelo?: string
          optica_id?: string
          precio?: number
          sku?: string
          stock_actual?: number
          stock_minimo?: number
          talla?: string
          updated_at?: string
        }
        Update: {
          activo?: boolean
          color?: string
          costo?: number
          id?: string
          marca?: string
          modelo?: string
          optica_id?: string
          precio?: number
          sku?: string
          stock_actual?: number
          stock_minimo?: number
          talla?: string
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "monturas_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      optica_settings: {
        Row: {
          config_json: Json
          created_at: string
          optica_id: string
          updated_at: string
        }
        Insert: {
          config_json?: Json
          created_at?: string
          optica_id: string
          updated_at?: string
        }
        Update: {
          config_json?: Json
          created_at?: string
          optica_id?: string
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "optica_settings_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: true
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      opticas: {
        Row: {
          contacto_whatsapp_telefono: string
          created_at: string
          current_period_end: string | null
          direccion_fiscal: string
          distrito_ciudad_departamento: string
          fiscal_doc_numero: string
          fiscal_doc_tipo: string
          id: string
          laboratorio_contacto: string
          laboratorio_nombre: string
          max_opticas: number | null
          max_pacientes_por_optica: number | null
          max_usuarios_por_optica: number | null
          moneda: string
          nombre: string
          pais: string
          plan: string
          plan_code: string
          plan_source: string
          plan_status: string
          razon_social: string
        }
        Insert: {
          contacto_whatsapp_telefono?: string
          created_at?: string
          current_period_end?: string | null
          direccion_fiscal?: string
          distrito_ciudad_departamento?: string
          fiscal_doc_numero?: string
          fiscal_doc_tipo?: string
          id: string
          laboratorio_contacto?: string
          laboratorio_nombre?: string
          max_opticas?: number | null
          max_pacientes_por_optica?: number | null
          max_usuarios_por_optica?: number | null
          moneda?: string
          nombre?: string
          pais?: string
          plan?: string
          plan_code?: string
          plan_source?: string
          plan_status?: string
          razon_social?: string
        }
        Update: {
          contacto_whatsapp_telefono?: string
          created_at?: string
          current_period_end?: string | null
          direccion_fiscal?: string
          distrito_ciudad_departamento?: string
          fiscal_doc_numero?: string
          fiscal_doc_tipo?: string
          id?: string
          laboratorio_contacto?: string
          laboratorio_nombre?: string
          max_opticas?: number | null
          max_pacientes_por_optica?: number | null
          max_usuarios_por_optica?: number | null
          moneda?: string
          nombre?: string
          pais?: string
          plan?: string
          plan_code?: string
          plan_source?: string
          plan_status?: string
          razon_social?: string
        }
        Relationships: []
      }
      pacientes: {
        Row: {
          acompanante: string | null
          direccion: string | null
          distrito: string | null
          dni: string | null
          edad: number
          email: string | null
          fecha_creacion: string
          fecha_nacimiento: string | null
          historia_optometrica: string | null
          hobbies: string | null
          id: string
          nombre_completo: string
          ocupacion: string | null
          optica_id: string
          sexo: string | null
          telefono: string
          ultimas_etiquetas: string
          updated_at: string
          updated_by: string | null
        }
        Insert: {
          acompanante?: string | null
          direccion?: string | null
          distrito?: string | null
          dni?: string | null
          edad?: number
          email?: string | null
          fecha_creacion: string
          fecha_nacimiento?: string | null
          historia_optometrica?: string | null
          hobbies?: string | null
          id: string
          nombre_completo: string
          ocupacion?: string | null
          optica_id?: string
          sexo?: string | null
          telefono?: string
          ultimas_etiquetas?: string
          updated_at?: string
          updated_by?: string | null
        }
        Update: {
          acompanante?: string | null
          direccion?: string | null
          distrito?: string | null
          dni?: string | null
          edad?: number
          email?: string | null
          fecha_creacion?: string
          fecha_nacimiento?: string | null
          historia_optometrica?: string | null
          hobbies?: string | null
          id?: string
          nombre_completo?: string
          ocupacion?: string | null
          optica_id?: string
          sexo?: string | null
          telefono?: string
          ultimas_etiquetas?: string
          updated_at?: string
          updated_by?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "pacientes_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      pacientes_delete_audit: {
        Row: {
          deleted_at: string
          deleted_by: string
          id: number
          optica_id: string
          paciente_id: string
        }
        Insert: {
          deleted_at?: string
          deleted_by: string
          id?: number
          optica_id: string
          paciente_id: string
        }
        Update: {
          deleted_at?: string
          deleted_by?: string
          id?: number
          optica_id?: string
          paciente_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "pacientes_delete_audit_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      pagos: {
        Row: {
          dispensacion_id: string | null
          fecha: string
          id: string
          metodo_pago: string
          monto: number
          nota: string
          optica_id: string
          servicio_extra_id: string | null
          tipo: string
          updated_at: string
          updated_by: string | null
        }
        Insert: {
          dispensacion_id?: string | null
          fecha: string
          id: string
          metodo_pago?: string
          monto?: number
          nota?: string
          optica_id?: string
          servicio_extra_id?: string | null
          tipo?: string
          updated_at?: string
          updated_by?: string | null
        }
        Update: {
          dispensacion_id?: string | null
          fecha?: string
          id?: string
          metodo_pago?: string
          monto?: number
          nota?: string
          optica_id?: string
          servicio_extra_id?: string | null
          tipo?: string
          updated_at?: string
          updated_by?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "pagos_dispensacion_id_fkey"
            columns: ["dispensacion_id"]
            isOneToOne: false
            referencedRelation: "dispensaciones"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "pagos_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "pagos_servicio_extra_id_fkey"
            columns: ["servicio_extra_id"]
            isOneToOne: false
            referencedRelation: "servicios_extra"
            referencedColumns: ["id"]
          },
        ]
      }
      schema_migrations_flags: {
        Row: {
          applied_at: string
          key: string
        }
        Insert: {
          applied_at?: string
          key: string
        }
        Update: {
          applied_at?: string
          key?: string
        }
        Relationships: []
      }
      servicios_extra: {
        Row: {
          a_cuenta: number
          descripcion: string
          estado: string
          fecha: string
          id: string
          metodo_pago: string
          monto_total: number
          optica_id: string
          ot: string
          paciente_id: string | null
          updated_at: string
          updated_by: string | null
        }
        Insert: {
          a_cuenta?: number
          descripcion?: string
          estado?: string
          fecha: string
          id: string
          metodo_pago?: string
          monto_total?: number
          optica_id?: string
          ot?: string
          paciente_id?: string | null
          updated_at?: string
          updated_by?: string | null
        }
        Update: {
          a_cuenta?: number
          descripcion?: string
          estado?: string
          fecha?: string
          id?: string
          metodo_pago?: string
          monto_total?: number
          optica_id?: string
          ot?: string
          paciente_id?: string | null
          updated_at?: string
          updated_by?: string | null
        }
        Relationships: [
          {
            foreignKeyName: "servicios_extra_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "servicios_extra_paciente_id_fkey"
            columns: ["paciente_id"]
            isOneToOne: false
            referencedRelation: "pacientes"
            referencedColumns: ["id"]
          },
        ]
      }
      sync_telemetry_optica: {
        Row: {
          last_actor: string | null
          last_error: string
          last_stage: string
          last_status: string
          last_sync_at: string | null
          optica_id: string
          updated_at: string
        }
        Insert: {
          last_actor?: string | null
          last_error?: string
          last_stage?: string
          last_status?: string
          last_sync_at?: string | null
          optica_id: string
          updated_at?: string
        }
        Update: {
          last_actor?: string | null
          last_error?: string
          last_stage?: string
          last_status?: string
          last_sync_at?: string | null
          optica_id?: string
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "sync_telemetry_optica_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: true
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
      user_profiles: {
        Row: {
          created_at: string
          email: string
          user_id: string
        }
        Insert: {
          created_at?: string
          email: string
          user_id: string
        }
        Update: {
          created_at?: string
          email?: string
          user_id?: string
        }
        Relationships: []
      }
      usuario_optica: {
        Row: {
          created_at: string
          optica_id: string
          rol: string
          user_id: string
        }
        Insert: {
          created_at?: string
          optica_id: string
          rol?: string
          user_id: string
        }
        Update: {
          created_at?: string
          optica_id?: string
          rol?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "usuario_optica_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      optica_members: {
        Row: {
          created_at: string | null
          email: string | null
          optica_id: string | null
          rol: string | null
          user_id: string | null
        }
        Relationships: [
          {
            foreignKeyName: "usuario_optica_optica_id_fkey"
            columns: ["optica_id"]
            isOneToOne: false
            referencedRelation: "opticas"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Functions: {
      assert_backup_operation_allowed: {
        Args: {
          p_action: string
          p_source_optica_id: string
          p_target_optica_id: string
        }
        Returns: undefined
      }
      assign_optica_role_by_email: {
        Args: { p_email: string; p_optica_id: string; p_rol: string }
        Returns: undefined
      }
      has_optica_role: {
        Args: { p_optica_id: string; p_roles?: string[]; p_user_id: string }
        Returns: boolean
      }
      is_internal_owner: { Args: never; Returns: boolean }
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  public: {
    Enums: {},
  },
} as const

