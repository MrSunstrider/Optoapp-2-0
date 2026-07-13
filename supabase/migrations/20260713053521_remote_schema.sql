drop policy "opticas_select_member" on "public"."opticas";

drop policy "opticas_update_member" on "public"."opticas";

drop policy "usuario_optica_select_member_scope" on "public"."usuario_optica";

drop index if exists "public"."idx_oc_estado";

drop index if exists "public"."idx_oc_numero";

alter table "public"."categorias_montura" enable row level security;

alter table "public"."dispensaciones" alter column "altura" set default ''::text;

alter table "public"."dispensaciones" alter column "altura" set not null;

alter table "public"."dispensaciones" alter column "color_lente" set default ''::text;

alter table "public"."dispensaciones" alter column "color_lente" set not null;

alter table "public"."dispensaciones" alter column "descripcion_montura" set default ''::text;

alter table "public"."dispensaciones" alter column "descripcion_montura" set not null;

alter table "public"."dispensaciones" alter column "distancia_lente" set default ''::text;

alter table "public"."dispensaciones" alter column "distancia_lente" set not null;

alter table "public"."dispensaciones" alter column "fecha" set not null;

alter table "public"."dispensaciones" alter column "material_lente" set default ''::text;

alter table "public"."dispensaciones" alter column "material_lente" set not null;

alter table "public"."dispensaciones" alter column "material_montura" set default ''::text;

alter table "public"."dispensaciones" alter column "material_montura" set not null;

alter table "public"."dispensaciones" alter column "metodo_pago" set default ''::text;

alter table "public"."dispensaciones" alter column "metodo_pago" set not null;

alter table "public"."dispensaciones" alter column "monto_pagado" set default 0.00;

alter table "public"."dispensaciones" alter column "monto_pagado" set not null;

alter table "public"."dispensaciones" alter column "monto_pagado" set data type numeric(10,2) using "monto_pagado"::numeric(10,2);

alter table "public"."dispensaciones" alter column "monto_total" set default 0.00;

alter table "public"."dispensaciones" alter column "monto_total" set not null;

alter table "public"."dispensaciones" alter column "monto_total" set data type numeric(10,2) using "monto_total"::numeric(10,2);

alter table "public"."dispensaciones" alter column "notas_diseno" set default ''::text;

alter table "public"."dispensaciones" alter column "notas_diseno" set not null;

alter table "public"."dispensaciones" alter column "origen_montura" set default ''::text;

alter table "public"."dispensaciones" alter column "origen_montura" set not null;

alter table "public"."dispensaciones" alter column "ot" set default ''::text;

alter table "public"."dispensaciones" alter column "ot" set not null;

alter table "public"."dispensaciones" alter column "paciente_id" drop default;

alter table "public"."dispensaciones" alter column "sub_tipo_bifocal" set default ''::text;

alter table "public"."dispensaciones" alter column "sub_tipo_bifocal" set not null;

alter table "public"."dispensaciones" alter column "tipo_aro" set default ''::text;

alter table "public"."dispensaciones" alter column "tipo_aro" set not null;

alter table "public"."dispensaciones" alter column "tipo_lente" set default ''::text;

alter table "public"."dispensaciones" alter column "tipo_lente" set not null;

alter table "public"."dispensaciones" alter column "tipo_montura" set default ''::text;

alter table "public"."dispensaciones" alter column "tipo_montura" set not null;

alter table "public"."dispensaciones" alter column "tratamientos" set default ''::text;

alter table "public"."dispensaciones" alter column "tratamientos" set not null;

alter table "public"."dispensaciones" enable row level security;

alter table "public"."evaluaciones" drop column "av_cc_ao";

alter table "public"."evaluaciones" drop column "created_at";

alter table "public"."evaluaciones" add column "alergias" text default ''::text;

alter table "public"."evaluaciones" add column "antecedentes_familiares_oculares" text default ''::text;

alter table "public"."evaluaciones" add column "antecedentes_familiares_sistemicos" text default ''::text;

alter table "public"."evaluaciones" add column "antecedentes_personales_oculares" text default ''::text;

alter table "public"."evaluaciones" add column "antecedentes_personales_sistemicos" text default ''::text;

alter table "public"."evaluaciones" add column "av_cc_ao_px" text default ''::text;

alter table "public"."evaluaciones" add column "medicacion" text default ''::text;

alter table "public"."evaluaciones" add column "motivo_consulta" text default ''::text;

alter table "public"."evaluaciones" add column "necesidad_visual" text default ''::text;

alter table "public"."evaluaciones" add column "receta_od_av" text default ''::text;

alter table "public"."evaluaciones" add column "receta_oi_av" text default ''::text;

alter table "public"."evaluaciones" add column "sintomas" text default ''::text;

alter table "public"."evaluaciones" alter column "add_av" set default ''::text;

alter table "public"."evaluaciones" alter column "add_cerca_od" set default ''::text;

alter table "public"."evaluaciones" alter column "add_cerca_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "add_intermedia_od" set default ''::text;

alter table "public"."evaluaciones" alter column "add_intermedia_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "amsler" set default ''::text;

alter table "public"."evaluaciones" alter column "auto_ambliopia" set default false;

alter table "public"."evaluaciones" alter column "auto_ambliopia" set data type boolean using "auto_ambliopia"::boolean;

alter table "public"."evaluaciones" alter column "auto_anisometropia" set default false;

alter table "public"."evaluaciones" alter column "auto_anisometropia" set data type boolean using "auto_anisometropia"::boolean;

alter table "public"."evaluaciones" alter column "auto_presbicia" set default false;

alter table "public"."evaluaciones" alter column "auto_presbicia" set data type boolean using "auto_presbicia"::boolean;

alter table "public"."evaluaciones" alter column "av_cc_ao_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "av_cc_od_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "av_cc_od_lejos" set default ''::text;

alter table "public"."evaluaciones" alter column "av_cc_oi_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "av_cc_oi_lejos" set default ''::text;

alter table "public"."evaluaciones" alter column "av_sc_ao" set default ''::text;

alter table "public"."evaluaciones" alter column "av_sc_ao_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "av_sc_od_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "av_sc_od_lejos" set default ''::text;

alter table "public"."evaluaciones" alter column "av_sc_oi_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "av_sc_oi_lejos" set default ''::text;

alter table "public"."evaluaciones" alter column "balance_od" set default false;

alter table "public"."evaluaciones" alter column "balance_od" set data type boolean using "balance_od"::boolean;

alter table "public"."evaluaciones" alter column "balance_oi" set default false;

alter table "public"."evaluaciones" alter column "balance_oi" set data type boolean using "balance_oi"::boolean;

alter table "public"."evaluaciones" alter column "campo_visual" set default ''::text;

alter table "public"."evaluaciones" alter column "campo_visual_descripcion" set default ''::text;

alter table "public"."evaluaciones" alter column "cover_test_10cm" set default ''::text;

alter table "public"."evaluaciones" alter column "cover_test_40cm" set default ''::text;

alter table "public"."evaluaciones" alter column "cover_test_6m" set default ''::text;

alter table "public"."evaluaciones" alter column "diagnostico" set default ''::text;

alter table "public"."evaluaciones" alter column "diagnostico_od" set default ''::text;

alter table "public"."evaluaciones" alter column "diagnostico_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "diagnostico_otros" set default ''::text;

alter table "public"."evaluaciones" alter column "dip_cerca" set default ''::text;

alter table "public"."evaluaciones" alter column "dip_intermedio" set default ''::text;

alter table "public"."evaluaciones" alter column "dip_lejos" set default ''::text;

alter table "public"."evaluaciones" alter column "dip_lejos_mm" set data type numeric(6,2) using "dip_lejos_mm"::numeric(6,2);

alter table "public"."evaluaciones" alter column "dip_total_mm" set data type numeric(6,2) using "dip_total_mm"::numeric(6,2);

alter table "public"."evaluaciones" alter column "dnp_od_mm" set data type numeric(6,2) using "dnp_od_mm"::numeric(6,2);

alter table "public"."evaluaciones" alter column "dnp_oi_mm" set data type numeric(6,2) using "dnp_oi_mm"::numeric(6,2);

alter table "public"."evaluaciones" alter column "ducciones_od" set default ''::text;

alter table "public"."evaluaciones" alter column "ducciones_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "estereopsis_segundos" set default ''::text;

alter table "public"."evaluaciones" alter column "estereopsis_valor" set default ''::text;

alter table "public"."evaluaciones" alter column "farnsworth" set default ''::text;

alter table "public"."evaluaciones" alter column "fecha" set not null;

alter table "public"."evaluaciones" alter column "hirshberg" set default ''::text;

alter table "public"."evaluaciones" alter column "ishihara" set default ''::text;

alter table "public"."evaluaciones" alter column "k1_od" set default ''::text;

alter table "public"."evaluaciones" alter column "k1_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "k2_od" set default ''::text;

alter table "public"."evaluaciones" alter column "k2_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "kappa_od" set default ''::text;

alter table "public"."evaluaciones" alter column "kappa_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "lang" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_diametro_od" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_diametro_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_laboratorio" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_material" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_observaciones" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_od_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_od_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_od_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_oi_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_oi_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_oi_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_radio_base_od" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_radio_base_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "lc_tipo_lente" set default ''::text;

alter table "public"."evaluaciones" alter column "obj_od_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "obj_od_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "obj_od_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "obj_oi_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "obj_oi_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "obj_oi_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "observaciones" set default ''::text;

alter table "public"."evaluaciones" alter column "osdi_clasificacion" set default ''::text;

alter table "public"."evaluaciones" alter column "osdi_puntuacion" set data type integer using "osdi_puntuacion"::integer;

alter table "public"."evaluaciones" alter column "otros_ambliopia" set default false;

alter table "public"."evaluaciones" alter column "otros_ambliopia" set data type boolean using "otros_ambliopia"::boolean;

alter table "public"."evaluaciones" alter column "otros_anisometropia" set default false;

alter table "public"."evaluaciones" alter column "otros_anisometropia" set data type boolean using "otros_anisometropia"::boolean;

alter table "public"."evaluaciones" alter column "otros_presbicia" set default false;

alter table "public"."evaluaciones" alter column "otros_presbicia" set data type boolean using "otros_presbicia"::boolean;

alter table "public"."evaluaciones" alter column "paciente_id" drop default;

alter table "public"."evaluaciones" alter column "ph_od" set default ''::text;

alter table "public"."evaluaciones" alter column "ph_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "plan_tratamiento" set default ''::text;

alter table "public"."evaluaciones" alter column "ppc_frl" set default ''::text;

alter table "public"."evaluaciones" alter column "ppc_luz" set default ''::text;

alter table "public"."evaluaciones" alter column "ppc_or" set default ''::text;

alter table "public"."evaluaciones" alter column "prisma_od_base" set default ''::text;

alter table "public"."evaluaciones" alter column "prisma_od_valor" set default ''::text;

alter table "public"."evaluaciones" alter column "prisma_oi_base" set default ''::text;

alter table "public"."evaluaciones" alter column "prisma_oi_valor" set default ''::text;

alter table "public"."evaluaciones" alter column "proxima_fecha_control" set data type date using "proxima_fecha_control"::date;

alter table "public"."evaluaciones" alter column "receta_od_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "receta_od_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "receta_od_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "receta_oi_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "receta_oi_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "receta_oi_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "reflejo_acomodativo" set default ''::text;

alter table "public"."evaluaciones" alter column "reflejo_consensual" set default ''::text;

alter table "public"."evaluaciones" alter column "reflejo_fotomotor" set default ''::text;

alter table "public"."evaluaciones" alter column "schirmer_od" set default ''::text;

alter table "public"."evaluaciones" alter column "schirmer_oi" set default ''::text;

alter table "public"."evaluaciones" alter column "sensibilidad_contraste" set default ''::text;

alter table "public"."evaluaciones" alter column "sensibilidad_frecuencia" set default ''::text;

alter table "public"."evaluaciones" alter column "subj_od_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "subj_od_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "subj_od_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "subj_oi_cil" set default ''::text;

alter table "public"."evaluaciones" alter column "subj_oi_eje" set default ''::text;

alter table "public"."evaluaciones" alter column "subj_oi_esf" set default ''::text;

alter table "public"."evaluaciones" alter column "versiones_ao" set default ''::text;

alter table "public"."evaluaciones" alter column "worth" set default ''::text;

alter table "public"."evaluaciones" enable row level security;

alter table "public"."inventario_fisico" enable row level security;

alter table "public"."inventario_fisico_detalle" enable row level security;

alter table "public"."montura_movimientos" alter column "cantidad" set not null;

alter table "public"."montura_movimientos" alter column "nota" set default ''::text;

alter table "public"."montura_movimientos" alter column "nota" set not null;

alter table "public"."montura_movimientos" alter column "optica_id" set not null;

alter table "public"."montura_movimientos" alter column "referencia_id" set default ''::text;

alter table "public"."montura_movimientos" alter column "referencia_id" set not null;

alter table "public"."montura_movimientos" alter column "stock_nuevo" set not null;

alter table "public"."montura_movimientos" alter column "stock_previo" set not null;

alter table "public"."montura_movimientos" alter column "tipo" set not null;

alter table "public"."montura_proveedor" enable row level security;

alter table "public"."monturas" alter column "color" set default ''::text;

alter table "public"."monturas" alter column "color" set not null;

alter table "public"."monturas" alter column "costo" set default 0.00;

alter table "public"."monturas" alter column "costo" set not null;

alter table "public"."monturas" alter column "marca" set default ''::text;

alter table "public"."monturas" alter column "marca" set not null;

alter table "public"."monturas" alter column "modelo" set default ''::text;

alter table "public"."monturas" alter column "modelo" set not null;

alter table "public"."monturas" alter column "precio" set default 0.00;

alter table "public"."monturas" alter column "precio" set not null;

alter table "public"."monturas" alter column "sku" set default ''::text;

alter table "public"."monturas" alter column "sku" set not null;

alter table "public"."monturas" alter column "stock_actual" set default 0;

alter table "public"."monturas" alter column "stock_actual" set not null;

alter table "public"."monturas" alter column "stock_minimo" set default 0;

alter table "public"."monturas" alter column "stock_minimo" set not null;

alter table "public"."monturas" alter column "talla" set default ''::text;

alter table "public"."monturas" alter column "talla" set not null;

alter table "public"."orden_compra_items" enable row level security;

alter table "public"."ordenes_compra" enable row level security;

alter table "public"."pacientes" alter column "edad" set default 0;

alter table "public"."pacientes" alter column "edad" set not null;

alter table "public"."pacientes" alter column "fecha_creacion" set not null;

alter table "public"."pacientes" alter column "nombre_completo" drop default;

alter table "public"."pacientes" alter column "telefono" set default ''::text;

alter table "public"."pacientes" alter column "telefono" set not null;

alter table "public"."pacientes" alter column "ultimas_etiquetas" set default ''::text;

alter table "public"."pacientes" alter column "ultimas_etiquetas" set not null;

alter table "public"."pacientes" enable row level security;

alter table "public"."pacientes_delete_audit" enable row level security;

alter table "public"."pagos" alter column "fecha" set not null;

alter table "public"."pagos" alter column "metodo_pago" set default ''::text;

alter table "public"."pagos" alter column "monto" set default 0.00;

alter table "public"."pagos" alter column "monto" set not null;

alter table "public"."pagos" alter column "monto" set data type numeric(10,2) using "monto"::numeric(10,2);

alter table "public"."pagos" alter column "nota" set default ''::text;

alter table "public"."pagos" alter column "nota" set not null;

alter table "public"."pagos" alter column "tipo" set default ''::text;

alter table "public"."pagos" enable row level security;

alter table "public"."proveedores" enable row level security;

alter table "public"."schema_migrations_flags" enable row level security;

alter table "public"."servicios_extra" alter column "a_cuenta" set default 0.00;

alter table "public"."servicios_extra" alter column "a_cuenta" set not null;

alter table "public"."servicios_extra" alter column "a_cuenta" set data type numeric(10,2) using "a_cuenta"::numeric(10,2);

alter table "public"."servicios_extra" alter column "descripcion" set default ''::text;

alter table "public"."servicios_extra" alter column "fecha" set not null;

alter table "public"."servicios_extra" alter column "metodo_pago" set default ''::text;

alter table "public"."servicios_extra" alter column "monto_total" set default 0.00;

alter table "public"."servicios_extra" alter column "monto_total" set not null;

alter table "public"."servicios_extra" alter column "monto_total" set data type numeric(10,2) using "monto_total"::numeric(10,2);

alter table "public"."servicios_extra" enable row level security;

CREATE INDEX idx_categorias_montura_optica_id ON public.categorias_montura USING btree (optica_id);

CREATE INDEX idx_inventario_fisico_optica_id ON public.inventario_fisico USING btree (optica_id);

CREATE UNIQUE INDEX idx_monturas_sku_optica ON public.monturas USING btree (optica_id, sku);

CREATE INDEX idx_ordenes_compra_estado ON public.ordenes_compra USING btree (estado);

CREATE INDEX idx_ordenes_compra_optica_id ON public.ordenes_compra USING btree (optica_id);

CREATE INDEX idx_pacientes_optica_id ON public.pacientes USING btree (optica_id);

CREATE INDEX idx_proveedores_optica_id ON public.proveedores USING btree (optica_id);

alter table "public"."dispensaciones" add constraint "dispensaciones_estado_entrega_not_blank_chk" CHECK ((btrim(estado_entrega) <> ''::text)) not valid;

alter table "public"."dispensaciones" validate constraint "dispensaciones_estado_entrega_not_blank_chk";

alter table "public"."dispensaciones" add constraint "dispensaciones_monto_pagado_chk" CHECK ((monto_pagado >= (0)::numeric)) not valid;

alter table "public"."dispensaciones" validate constraint "dispensaciones_monto_pagado_chk";

alter table "public"."dispensaciones" add constraint "dispensaciones_monto_total_chk" CHECK ((monto_total >= (0)::numeric)) not valid;

alter table "public"."dispensaciones" validate constraint "dispensaciones_monto_total_chk";

alter table "public"."dispensaciones" add constraint "dispensaciones_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."dispensaciones" validate constraint "dispensaciones_optica_id_fkey";

alter table "public"."dispensaciones" add constraint "dispensaciones_paciente_id_fkey" FOREIGN KEY (paciente_id) REFERENCES public.pacientes(id) ON DELETE CASCADE not valid;

alter table "public"."dispensaciones" validate constraint "dispensaciones_paciente_id_fkey";

alter table "public"."evaluaciones" add constraint "evaluaciones_dip_lejos_mm_chk" CHECK (((dip_lejos_mm IS NULL) OR (dip_lejos_mm >= (0)::numeric))) not valid;

alter table "public"."evaluaciones" validate constraint "evaluaciones_dip_lejos_mm_chk";

alter table "public"."evaluaciones" add constraint "evaluaciones_dip_total_mm_chk" CHECK (((dip_total_mm IS NULL) OR (dip_total_mm >= (0)::numeric))) not valid;

alter table "public"."evaluaciones" validate constraint "evaluaciones_dip_total_mm_chk";

alter table "public"."evaluaciones" add constraint "evaluaciones_dnp_od_mm_chk" CHECK (((dnp_od_mm IS NULL) OR (dnp_od_mm >= (0)::numeric))) not valid;

alter table "public"."evaluaciones" validate constraint "evaluaciones_dnp_od_mm_chk";

alter table "public"."evaluaciones" add constraint "evaluaciones_dnp_oi_mm_chk" CHECK (((dnp_oi_mm IS NULL) OR (dnp_oi_mm >= (0)::numeric))) not valid;

alter table "public"."evaluaciones" validate constraint "evaluaciones_dnp_oi_mm_chk";

alter table "public"."evaluaciones" add constraint "evaluaciones_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."evaluaciones" validate constraint "evaluaciones_optica_id_fkey";

alter table "public"."evaluaciones" add constraint "evaluaciones_paciente_id_fkey" FOREIGN KEY (paciente_id) REFERENCES public.pacientes(id) ON DELETE CASCADE not valid;

alter table "public"."evaluaciones" validate constraint "evaluaciones_paciente_id_fkey";

alter table "public"."montura_movimientos" add constraint "montura_movimientos_montura_id_fkey" FOREIGN KEY (montura_id) REFERENCES public.monturas(id) ON DELETE CASCADE not valid;

alter table "public"."montura_movimientos" validate constraint "montura_movimientos_montura_id_fkey";

alter table "public"."montura_movimientos" add constraint "montura_movimientos_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."montura_movimientos" validate constraint "montura_movimientos_optica_id_fkey";

alter table "public"."monturas" add constraint "monturas_costo_chk" CHECK ((costo >= (0)::numeric)) not valid;

alter table "public"."monturas" validate constraint "monturas_costo_chk";

alter table "public"."monturas" add constraint "monturas_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."monturas" validate constraint "monturas_optica_id_fkey";

alter table "public"."monturas" add constraint "monturas_precio_chk" CHECK ((precio >= (0)::numeric)) not valid;

alter table "public"."monturas" validate constraint "monturas_precio_chk";

alter table "public"."monturas" add constraint "monturas_stock_actual_chk" CHECK ((stock_actual >= 0)) not valid;

alter table "public"."monturas" validate constraint "monturas_stock_actual_chk";

alter table "public"."monturas" add constraint "monturas_stock_minimo_chk" CHECK ((stock_minimo >= 0)) not valid;

alter table "public"."monturas" validate constraint "monturas_stock_minimo_chk";

alter table "public"."pacientes" add constraint "pacientes_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."pacientes" validate constraint "pacientes_optica_id_fkey";

alter table "public"."pagos" add constraint "pagos_dispensacion_id_fkey" FOREIGN KEY (dispensacion_id) REFERENCES public.dispensaciones(id) ON DELETE CASCADE not valid;

alter table "public"."pagos" validate constraint "pagos_dispensacion_id_fkey";

alter table "public"."pagos" add constraint "pagos_monto_chk" CHECK ((monto >= (0)::numeric)) not valid;

alter table "public"."pagos" validate constraint "pagos_monto_chk";

alter table "public"."pagos" add constraint "pagos_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."pagos" validate constraint "pagos_optica_id_fkey";

alter table "public"."pagos" add constraint "pagos_origen_xor_chk" CHECK ((((dispensacion_id IS NOT NULL) AND (servicio_extra_id IS NULL)) OR ((dispensacion_id IS NULL) AND (servicio_extra_id IS NOT NULL)))) not valid;

alter table "public"."pagos" validate constraint "pagos_origen_xor_chk";

alter table "public"."pagos" add constraint "pagos_servicio_extra_id_fkey" FOREIGN KEY (servicio_extra_id) REFERENCES public.servicios_extra(id) ON DELETE CASCADE not valid;

alter table "public"."pagos" validate constraint "pagos_servicio_extra_id_fkey";

alter table "public"."servicios_extra" add constraint "servicios_extra_a_cuenta_chk" CHECK ((a_cuenta >= (0)::numeric)) not valid;

alter table "public"."servicios_extra" validate constraint "servicios_extra_a_cuenta_chk";

alter table "public"."servicios_extra" add constraint "servicios_extra_monto_total_chk" CHECK ((monto_total >= (0)::numeric)) not valid;

alter table "public"."servicios_extra" validate constraint "servicios_extra_monto_total_chk";

alter table "public"."servicios_extra" add constraint "servicios_extra_optica_id_fkey" FOREIGN KEY (optica_id) REFERENCES public.opticas(id) ON DELETE RESTRICT not valid;

alter table "public"."servicios_extra" validate constraint "servicios_extra_optica_id_fkey";

alter table "public"."servicios_extra" add constraint "servicios_extra_paciente_id_fkey" FOREIGN KEY (paciente_id) REFERENCES public.pacientes(id) ON DELETE SET NULL not valid;

alter table "public"."servicios_extra" validate constraint "servicios_extra_paciente_id_fkey";

set check_function_bodies = off;

CREATE OR REPLACE FUNCTION public.assert_backup_operation_allowed(p_action text, p_source_optica_id text, p_target_optica_id text)
 RETURNS void
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
declare
  v_uid uuid;
  v_is_admin_target boolean;
  v_is_admin_source boolean;
  v_action text := lower(btrim(coalesce(p_action, '')));
begin
  v_uid := auth.uid();
  if v_uid is null then
    raise exception 'Sesión inválida.';
  end if;

  if v_action not in ('export', 'restore') then
    raise exception 'Acción no válida: %', p_action;
  end if;

  select exists (
    select 1
    from public.usuario_optica uo
    where uo.user_id = v_uid
      and uo.optica_id = p_target_optica_id
      and lower(trim(uo.rol)) = 'admin'
  ) into v_is_admin_target;

  if not v_is_admin_target then
    raise exception 'Solo admin de la óptica activa puede realizar esta operación.';
  end if;

  if v_action = 'restore' then
    if btrim(coalesce(p_source_optica_id, '')) = '' then
      raise exception 'Respaldo sin óptica de origen.';
    end if;

    if p_source_optica_id <> p_target_optica_id then
      raise exception 'No se permite restaurar respaldos de otra óptica.';
    end if;

    select exists (
      select 1
      from public.usuario_optica uo
      where uo.user_id = v_uid
        and uo.optica_id = p_source_optica_id
        and lower(trim(uo.rol)) = 'admin'
    ) into v_is_admin_source;

    if not v_is_admin_source then
      raise exception 'Sin permisos admin sobre la óptica origen del respaldo.';
    end if;
  end if;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.assign_optica_role_by_email(p_optica_id text, p_email text, p_rol text)
 RETURNS void
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
declare
  v_email text;
  v_uid uuid;
  v_rol text;
  v_is_allowed boolean;
begin
  v_email := lower(btrim(coalesce(p_email, '')));
  v_rol := lower(btrim(coalesce(p_rol, '')));

  if v_email = '' then
    raise exception 'Email requerido';
  end if;

  if v_rol not in ('admin', 'gerente', 'especialista', 'asesor', 'asesora', 'ventas', 'invitado') then
    raise exception 'Rol no permitido: %', p_rol;
  end if;

  select exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = p_optica_id
      and lower(trim(self.rol)) in ('admin', 'gerente')
  ) into v_is_allowed;

  if not v_is_allowed then
    raise exception 'Sin permisos para gestionar roles en esta óptica';
  end if;

  select up.user_id into v_uid
  from public.user_profiles up
  where up.email = v_email;

  if v_uid is null then
    raise exception 'No existe una cuenta con ese email. Debe registrarse primero.';
  end if;

  insert into public.usuario_optica (user_id, optica_id, rol)
  values (v_uid, p_optica_id, v_rol)
  on conflict (user_id, optica_id)
  do update set rol = excluded.rol;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.enforce_admin_role_assignment_guard()
 RETURNS trigger
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
declare
  v_actor_is_admin boolean;
  v_bootstrap_allowed boolean;
begin
  if lower(trim(coalesce(new.rol, ''))) <> 'admin' then
    return new;
  end if;

  select exists (
    select 1
    from public.usuario_optica self
    where self.user_id = auth.uid()
      and self.optica_id = new.optica_id
      and lower(trim(self.rol)) = 'admin'
  ) into v_actor_is_admin;

  if v_actor_is_admin then
    return new;
  end if;

  select (
    new.user_id = auth.uid()
    and not exists (
      select 1 from public.usuario_optica u where u.optica_id = new.optica_id
    )
  ) into v_bootstrap_allowed;

  if v_bootstrap_allowed then
    return new;
  end if;

  raise exception 'Solo un admin actual de la óptica puede asignar rol admin';
end;
$function$
;

CREATE OR REPLACE FUNCTION public.enforce_dev_owner_guard()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
begin
  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' and not app_private.is_internal_owner() then
    raise exception 'El plan dev_owner está restringido al owner interno';
  end if;

  if lower(trim(coalesce(new.plan_code, ''))) = 'dev_owner' then
    new.plan_source := 'internal';
  end if;

  return new;
end;
$function$
;

CREATE OR REPLACE FUNCTION public.has_optica_role(p_user_id uuid, p_optica_id text, p_roles text[] DEFAULT ARRAY['admin'::text, 'gerente'::text])
 RETURNS boolean
 LANGUAGE sql
 STABLE
 SET search_path TO 'public'
AS $function$
  select app_private.has_optica_role(p_user_id, p_optica_id, p_roles);
$function$
;

CREATE OR REPLACE FUNCTION public.is_internal_owner()
 RETURNS boolean
 LANGUAGE sql
 STABLE
 SET search_path TO 'public'
AS $function$
  select app_private.is_internal_owner();
$function$
;

CREATE OR REPLACE FUNCTION public.rls_auto_enable()
 RETURNS event_trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog'
AS $function$
DECLARE
  cmd record;
BEGIN
  FOR cmd IN
    SELECT *
    FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE', 'CREATE TABLE AS', 'SELECT INTO')
      AND object_type IN ('table','partitioned table')
  LOOP
     IF cmd.schema_name IS NOT NULL AND cmd.schema_name IN ('public') AND cmd.schema_name NOT IN ('pg_catalog','information_schema') AND cmd.schema_name NOT LIKE 'pg_toast%' AND cmd.schema_name NOT LIKE 'pg_temp%' THEN
      BEGIN
        EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity);
        RAISE LOG 'rls_auto_enable: enabled RLS on %', cmd.object_identity;
      EXCEPTION
        WHEN OTHERS THEN
          RAISE LOG 'rls_auto_enable: failed to enable RLS on %', cmd.object_identity;
      END;
     ELSE
        RAISE LOG 'rls_auto_enable: skip % (either system schema or not in enforced list: %.)', cmd.object_identity, cmd.schema_name;
     END IF;
  END LOOP;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.rpc_adjust_montura_stock(p_montura_id text, p_optica_id text, p_delta integer, p_reference_id text, p_note text, p_tipo text, p_fecha text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_new_stock integer;
    v_old_stock integer;
BEGIN
    UPDATE public.monturas
    SET stock_actual = stock_actual + p_delta
    WHERE id = p_montura_id
      AND optica_id = p_optica_id
    RETURNING stock_actual, stock_actual - p_delta
    INTO v_new_stock, v_old_stock;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_found');
    END IF;

    -- If the CHECK constraint didn't catch it (unlikely but safe),
    -- revert and signal insufficient.
    IF v_new_stock < 0 THEN
        UPDATE public.monturas
        SET stock_actual = v_old_stock
        WHERE id = p_montura_id
          AND optica_id = p_optica_id;
        RETURN jsonb_build_object('ok', false, 'error', 'insufficient');
    END IF;

    INSERT INTO public.montura_movimientos (
        id, montura_id, fecha, tipo, cantidad,
        stock_previo, stock_nuevo, referencia_id, nota, optica_id
    ) VALUES (
        gen_random_uuid()::text,
        p_montura_id,
        CAST(p_fecha AS date),
        p_tipo,
        ABS(p_delta),
        v_old_stock,
        v_new_stock,
        p_reference_id,
        p_note,
        p_optica_id
    );

    RETURN jsonb_build_object('ok', true, 'new_stock', v_new_stock);
END;
$function$
;

CREATE OR REPLACE FUNCTION public.rpc_cierre_caja_resumen(p_optica_id text, p_from date, p_to date)
 RETURNS jsonb
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_efectivo numeric;
    v_movil_trans numeric;
    v_tarjeta numeric;
    v_total numeric;
BEGIN
    SELECT
        COALESCE(SUM(CASE
            WHEN metodo_pago = 'Efectivo' THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(CASE
            WHEN metodo_pago IN ('Transferencia', 'Yape', 'Plin', 'Móvil')
            THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(CASE
            WHEN metodo_pago = 'Tarjeta' THEN monto ELSE 0
        END), 0),
        COALESCE(SUM(monto), 0)
    INTO
        v_efectivo,
        v_movil_trans,
        v_tarjeta,
        v_total
    FROM public.pagos
    WHERE optica_id = p_optica_id
      AND fecha >= p_from
      AND fecha < p_to;

    RETURN jsonb_build_object(
        'efectivo', v_efectivo,
        'movil_trans', v_movil_trans,
        'tarjeta', v_tarjeta,
        'total', v_total
    );
END;
$function$
;

CREATE OR REPLACE FUNCTION public.suggest_next_ho(p_optica_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_year text := EXTRACT(YEAR FROM NOW())::text;
    v_max_num int;
BEGIN
    SELECT MAX(
        NULLIF(regexp_replace(
            historia_optometrica,
            '^HO-' || v_year || '-(\d+)$',
            '\1'
        ), historia_optometrica)::int
    ) INTO v_max_num
    FROM pacientes
    WHERE optica_id = p_optica_id
      AND historia_optometrica ~* ('^HO-' || v_year || '-\d+$');

    v_max_num := COALESCE(v_max_num, 0);
    RETURN jsonb_build_object(
        'next_ho', 'HO-' || v_year || '-' || LPAD((v_max_num + 1)::text, 4, '0')
    );
END;
$function$
;

CREATE OR REPLACE FUNCTION public.sync_snapshot(p_optica_id uuid)
 RETURNS jsonb
 LANGUAGE plpgsql
 SET search_path TO 'public'
AS $function$
DECLARE
    v_pacientes_total int;
    v_disp_total int;
    v_disp_pendientes int;
    v_serv_total int;
    v_serv_pendientes int;
    v_eval_total int;
    v_eval_pendientes int;
    v_inv_total int;
    v_inv_critico int;
BEGIN
    -- Pacientes: total count
    SELECT COUNT(*) INTO v_pacientes_total
    FROM pacientes WHERE optica_id = p_optica_id;

    -- Dispensaciones: total + pendientes
    SELECT COUNT(*), COUNT(*) FILTER (WHERE estado_entrega = 'Pendiente')
    INTO v_disp_total, v_disp_pendientes
    FROM dispensaciones WHERE optica_id = p_optica_id;

    -- Servicios extra: total + pendientes
    SELECT COUNT(*), COUNT(*) FILTER (WHERE estado = 'Pendiente')
    INTO v_serv_total, v_serv_pendientes
    FROM servicios_extra WHERE optica_id = p_optica_id;

    -- Evaluaciones: total (con próxima cita) + pendientes (no atendidas ni canceladas)
    SELECT
        COUNT(*),
        COUNT(*) FILTER (
            WHERE cita_estado IS NULL OR (cita_estado <> 'atendida' AND cita_estado <> 'cancelada')
        )
    INTO v_eval_total, v_eval_pendientes
    FROM evaluaciones
    WHERE optica_id = p_optica_id
      AND proxima_cita IS NOT NULL;

    -- Inventario (monturas activas): total + críticas (stock <= 2)
    SELECT
        COUNT(*),
        COUNT(*) FILTER (WHERE stock_actual <= 2)
    INTO v_inv_total, v_inv_critico
    FROM monturas
    WHERE optica_id = p_optica_id
      AND activo = true;

    RETURN jsonb_build_object(
        'pacientes', jsonb_build_object('total', v_pacientes_total, 'pending', 0),
        'dispensaciones', jsonb_build_object('total', v_disp_total, 'pending', v_disp_pendientes),
        'servicios_extra', jsonb_build_object('total', v_serv_total, 'pending', v_serv_pendientes),
        'evaluaciones', jsonb_build_object('total', v_eval_total, 'pending', v_eval_pendientes),
        'inventario', jsonb_build_object('total', v_inv_total, 'pending', v_inv_critico)
    );
END;
$function$
;

grant delete on table "public"."app_releases" to "anon";

grant insert on table "public"."app_releases" to "anon";

grant select on table "public"."app_releases" to "anon";

grant update on table "public"."app_releases" to "anon";

grant delete on table "public"."app_releases" to "authenticated";

grant insert on table "public"."app_releases" to "authenticated";

grant select on table "public"."app_releases" to "authenticated";

grant update on table "public"."app_releases" to "authenticated";

grant delete on table "public"."app_releases" to "service_role";

grant insert on table "public"."app_releases" to "service_role";

grant select on table "public"."app_releases" to "service_role";

grant update on table "public"."app_releases" to "service_role";

grant delete on table "public"."categorias_montura" to "anon";

grant insert on table "public"."categorias_montura" to "anon";

grant select on table "public"."categorias_montura" to "anon";

grant update on table "public"."categorias_montura" to "anon";

grant delete on table "public"."categorias_montura" to "authenticated";

grant insert on table "public"."categorias_montura" to "authenticated";

grant select on table "public"."categorias_montura" to "authenticated";

grant update on table "public"."categorias_montura" to "authenticated";

grant delete on table "public"."categorias_montura" to "service_role";

grant insert on table "public"."categorias_montura" to "service_role";

grant select on table "public"."categorias_montura" to "service_role";

grant update on table "public"."categorias_montura" to "service_role";

grant delete on table "public"."categorias_producto" to "anon";

grant insert on table "public"."categorias_producto" to "anon";

grant select on table "public"."categorias_producto" to "anon";

grant update on table "public"."categorias_producto" to "anon";

grant delete on table "public"."categorias_producto" to "authenticated";

grant insert on table "public"."categorias_producto" to "authenticated";

grant select on table "public"."categorias_producto" to "authenticated";

grant update on table "public"."categorias_producto" to "authenticated";

grant delete on table "public"."categorias_producto" to "service_role";

grant insert on table "public"."categorias_producto" to "service_role";

grant select on table "public"."categorias_producto" to "service_role";

grant update on table "public"."categorias_producto" to "service_role";

grant delete on table "public"."cierres_caja" to "anon";

grant insert on table "public"."cierres_caja" to "anon";

grant select on table "public"."cierres_caja" to "anon";

grant update on table "public"."cierres_caja" to "anon";

grant delete on table "public"."cierres_caja" to "authenticated";

grant insert on table "public"."cierres_caja" to "authenticated";

grant select on table "public"."cierres_caja" to "authenticated";

grant update on table "public"."cierres_caja" to "authenticated";

grant delete on table "public"."cierres_caja" to "service_role";

grant insert on table "public"."cierres_caja" to "service_role";

grant select on table "public"."cierres_caja" to "service_role";

grant update on table "public"."cierres_caja" to "service_role";

grant delete on table "public"."configuracion_financiera" to "anon";

grant insert on table "public"."configuracion_financiera" to "anon";

grant select on table "public"."configuracion_financiera" to "anon";

grant update on table "public"."configuracion_financiera" to "anon";

grant delete on table "public"."configuracion_financiera" to "authenticated";

grant insert on table "public"."configuracion_financiera" to "authenticated";

grant select on table "public"."configuracion_financiera" to "authenticated";

grant update on table "public"."configuracion_financiera" to "authenticated";

grant delete on table "public"."configuracion_financiera" to "service_role";

grant insert on table "public"."configuracion_financiera" to "service_role";

grant select on table "public"."configuracion_financiera" to "service_role";

grant update on table "public"."configuracion_financiera" to "service_role";

grant delete on table "public"."costos_biselado" to "anon";

grant insert on table "public"."costos_biselado" to "anon";

grant select on table "public"."costos_biselado" to "anon";

grant update on table "public"."costos_biselado" to "anon";

grant delete on table "public"."costos_biselado" to "authenticated";

grant insert on table "public"."costos_biselado" to "authenticated";

grant select on table "public"."costos_biselado" to "authenticated";

grant update on table "public"."costos_biselado" to "authenticated";

grant delete on table "public"."costos_biselado" to "service_role";

grant insert on table "public"."costos_biselado" to "service_role";

grant select on table "public"."costos_biselado" to "service_role";

grant update on table "public"."costos_biselado" to "service_role";

grant delete on table "public"."costos_productos" to "anon";

grant insert on table "public"."costos_productos" to "anon";

grant select on table "public"."costos_productos" to "anon";

grant update on table "public"."costos_productos" to "anon";

grant delete on table "public"."costos_productos" to "authenticated";

grant insert on table "public"."costos_productos" to "authenticated";

grant select on table "public"."costos_productos" to "authenticated";

grant update on table "public"."costos_productos" to "authenticated";

grant delete on table "public"."costos_productos" to "service_role";

grant insert on table "public"."costos_productos" to "service_role";

grant select on table "public"."costos_productos" to "service_role";

grant update on table "public"."costos_productos" to "service_role";

grant delete on table "public"."dispensacion_items" to "anon";

grant insert on table "public"."dispensacion_items" to "anon";

grant select on table "public"."dispensacion_items" to "anon";

grant update on table "public"."dispensacion_items" to "anon";

grant delete on table "public"."dispensacion_items" to "authenticated";

grant insert on table "public"."dispensacion_items" to "authenticated";

grant select on table "public"."dispensacion_items" to "authenticated";

grant update on table "public"."dispensacion_items" to "authenticated";

grant delete on table "public"."dispensacion_items" to "service_role";

grant insert on table "public"."dispensacion_items" to "service_role";

grant select on table "public"."dispensacion_items" to "service_role";

grant update on table "public"."dispensacion_items" to "service_role";

grant delete on table "public"."dispensaciones" to "anon";

grant insert on table "public"."dispensaciones" to "anon";

grant select on table "public"."dispensaciones" to "anon";

grant update on table "public"."dispensaciones" to "anon";

grant delete on table "public"."dispensaciones" to "authenticated";

grant insert on table "public"."dispensaciones" to "authenticated";

grant select on table "public"."dispensaciones" to "authenticated";

grant update on table "public"."dispensaciones" to "authenticated";

grant delete on table "public"."dispensaciones" to "service_role";

grant insert on table "public"."dispensaciones" to "service_role";

grant select on table "public"."dispensaciones" to "service_role";

grant update on table "public"."dispensaciones" to "service_role";

grant delete on table "public"."evaluaciones" to "anon";

grant insert on table "public"."evaluaciones" to "anon";

grant select on table "public"."evaluaciones" to "anon";

grant update on table "public"."evaluaciones" to "anon";

grant delete on table "public"."evaluaciones" to "authenticated";

grant insert on table "public"."evaluaciones" to "authenticated";

grant select on table "public"."evaluaciones" to "authenticated";

grant update on table "public"."evaluaciones" to "authenticated";

grant delete on table "public"."evaluaciones" to "service_role";

grant insert on table "public"."evaluaciones" to "service_role";

grant select on table "public"."evaluaciones" to "service_role";

grant update on table "public"."evaluaciones" to "service_role";

grant delete on table "public"."feedback_recomendaciones" to "anon";

grant insert on table "public"."feedback_recomendaciones" to "anon";

grant select on table "public"."feedback_recomendaciones" to "anon";

grant update on table "public"."feedback_recomendaciones" to "anon";

grant delete on table "public"."feedback_recomendaciones" to "authenticated";

grant insert on table "public"."feedback_recomendaciones" to "authenticated";

grant select on table "public"."feedback_recomendaciones" to "authenticated";

grant update on table "public"."feedback_recomendaciones" to "authenticated";

grant delete on table "public"."feedback_recomendaciones" to "service_role";

grant insert on table "public"."feedback_recomendaciones" to "service_role";

grant select on table "public"."feedback_recomendaciones" to "service_role";

grant update on table "public"."feedback_recomendaciones" to "service_role";

grant delete on table "public"."gastos_operativos" to "anon";

grant insert on table "public"."gastos_operativos" to "anon";

grant select on table "public"."gastos_operativos" to "anon";

grant update on table "public"."gastos_operativos" to "anon";

grant delete on table "public"."gastos_operativos" to "authenticated";

grant insert on table "public"."gastos_operativos" to "authenticated";

grant select on table "public"."gastos_operativos" to "authenticated";

grant update on table "public"."gastos_operativos" to "authenticated";

grant delete on table "public"."gastos_operativos" to "service_role";

grant insert on table "public"."gastos_operativos" to "service_role";

grant select on table "public"."gastos_operativos" to "service_role";

grant update on table "public"."gastos_operativos" to "service_role";

grant delete on table "public"."inventario_fisico" to "anon";

grant insert on table "public"."inventario_fisico" to "anon";

grant select on table "public"."inventario_fisico" to "anon";

grant update on table "public"."inventario_fisico" to "anon";

grant delete on table "public"."inventario_fisico" to "authenticated";

grant insert on table "public"."inventario_fisico" to "authenticated";

grant select on table "public"."inventario_fisico" to "authenticated";

grant update on table "public"."inventario_fisico" to "authenticated";

grant delete on table "public"."inventario_fisico" to "service_role";

grant insert on table "public"."inventario_fisico" to "service_role";

grant select on table "public"."inventario_fisico" to "service_role";

grant update on table "public"."inventario_fisico" to "service_role";

grant delete on table "public"."inventario_fisico_detalle" to "anon";

grant insert on table "public"."inventario_fisico_detalle" to "anon";

grant select on table "public"."inventario_fisico_detalle" to "anon";

grant update on table "public"."inventario_fisico_detalle" to "anon";

grant delete on table "public"."inventario_fisico_detalle" to "authenticated";

grant insert on table "public"."inventario_fisico_detalle" to "authenticated";

grant select on table "public"."inventario_fisico_detalle" to "authenticated";

grant update on table "public"."inventario_fisico_detalle" to "authenticated";

grant delete on table "public"."inventario_fisico_detalle" to "service_role";

grant insert on table "public"."inventario_fisico_detalle" to "service_role";

grant select on table "public"."inventario_fisico_detalle" to "service_role";

grant update on table "public"."inventario_fisico_detalle" to "service_role";

grant delete on table "public"."invitaciones" to "anon";

grant insert on table "public"."invitaciones" to "anon";

grant select on table "public"."invitaciones" to "anon";

grant update on table "public"."invitaciones" to "anon";

grant delete on table "public"."invitaciones" to "authenticated";

grant insert on table "public"."invitaciones" to "authenticated";

grant select on table "public"."invitaciones" to "authenticated";

grant update on table "public"."invitaciones" to "authenticated";

grant delete on table "public"."invitaciones" to "service_role";

grant insert on table "public"."invitaciones" to "service_role";

grant select on table "public"."invitaciones" to "service_role";

grant update on table "public"."invitaciones" to "service_role";

grant delete on table "public"."margen_por_categoria" to "anon";

grant insert on table "public"."margen_por_categoria" to "anon";

grant select on table "public"."margen_por_categoria" to "anon";

grant update on table "public"."margen_por_categoria" to "anon";

grant delete on table "public"."margen_por_categoria" to "authenticated";

grant insert on table "public"."margen_por_categoria" to "authenticated";

grant select on table "public"."margen_por_categoria" to "authenticated";

grant update on table "public"."margen_por_categoria" to "authenticated";

grant delete on table "public"."margen_por_categoria" to "service_role";

grant insert on table "public"."margen_por_categoria" to "service_role";

grant select on table "public"."margen_por_categoria" to "service_role";

grant update on table "public"."margen_por_categoria" to "service_role";

grant delete on table "public"."montura_movimientos" to "anon";

grant insert on table "public"."montura_movimientos" to "anon";

grant select on table "public"."montura_movimientos" to "anon";

grant update on table "public"."montura_movimientos" to "anon";

grant delete on table "public"."montura_movimientos" to "authenticated";

grant insert on table "public"."montura_movimientos" to "authenticated";

grant select on table "public"."montura_movimientos" to "authenticated";

grant update on table "public"."montura_movimientos" to "authenticated";

grant delete on table "public"."montura_movimientos" to "service_role";

grant insert on table "public"."montura_movimientos" to "service_role";

grant select on table "public"."montura_movimientos" to "service_role";

grant update on table "public"."montura_movimientos" to "service_role";

grant delete on table "public"."montura_proveedor" to "anon";

grant insert on table "public"."montura_proveedor" to "anon";

grant select on table "public"."montura_proveedor" to "anon";

grant update on table "public"."montura_proveedor" to "anon";

grant delete on table "public"."montura_proveedor" to "authenticated";

grant insert on table "public"."montura_proveedor" to "authenticated";

grant select on table "public"."montura_proveedor" to "authenticated";

grant update on table "public"."montura_proveedor" to "authenticated";

grant delete on table "public"."montura_proveedor" to "service_role";

grant insert on table "public"."montura_proveedor" to "service_role";

grant select on table "public"."montura_proveedor" to "service_role";

grant update on table "public"."montura_proveedor" to "service_role";

grant delete on table "public"."monturas" to "anon";

grant insert on table "public"."monturas" to "anon";

grant select on table "public"."monturas" to "anon";

grant update on table "public"."monturas" to "anon";

grant delete on table "public"."monturas" to "authenticated";

grant insert on table "public"."monturas" to "authenticated";

grant select on table "public"."monturas" to "authenticated";

grant update on table "public"."monturas" to "authenticated";

grant delete on table "public"."monturas" to "service_role";

grant insert on table "public"."monturas" to "service_role";

grant select on table "public"."monturas" to "service_role";

grant update on table "public"."monturas" to "service_role";

grant delete on table "public"."optica_settings" to "anon";

grant insert on table "public"."optica_settings" to "anon";

grant select on table "public"."optica_settings" to "anon";

grant update on table "public"."optica_settings" to "anon";

grant delete on table "public"."optica_settings" to "authenticated";

grant insert on table "public"."optica_settings" to "authenticated";

grant select on table "public"."optica_settings" to "authenticated";

grant update on table "public"."optica_settings" to "authenticated";

grant delete on table "public"."optica_settings" to "service_role";

grant insert on table "public"."optica_settings" to "service_role";

grant select on table "public"."optica_settings" to "service_role";

grant update on table "public"."optica_settings" to "service_role";

grant delete on table "public"."opticas" to "anon";

grant insert on table "public"."opticas" to "anon";

grant select on table "public"."opticas" to "anon";

grant update on table "public"."opticas" to "anon";

grant delete on table "public"."opticas" to "authenticated";

grant insert on table "public"."opticas" to "authenticated";

grant select on table "public"."opticas" to "authenticated";

grant update on table "public"."opticas" to "authenticated";

grant delete on table "public"."opticas" to "service_role";

grant insert on table "public"."opticas" to "service_role";

grant select on table "public"."opticas" to "service_role";

grant update on table "public"."opticas" to "service_role";

grant delete on table "public"."orden_compra_items" to "anon";

grant insert on table "public"."orden_compra_items" to "anon";

grant select on table "public"."orden_compra_items" to "anon";

grant update on table "public"."orden_compra_items" to "anon";

grant delete on table "public"."orden_compra_items" to "authenticated";

grant insert on table "public"."orden_compra_items" to "authenticated";

grant select on table "public"."orden_compra_items" to "authenticated";

grant update on table "public"."orden_compra_items" to "authenticated";

grant delete on table "public"."orden_compra_items" to "service_role";

grant insert on table "public"."orden_compra_items" to "service_role";

grant select on table "public"."orden_compra_items" to "service_role";

grant update on table "public"."orden_compra_items" to "service_role";

grant delete on table "public"."ordenes_compra" to "anon";

grant insert on table "public"."ordenes_compra" to "anon";

grant select on table "public"."ordenes_compra" to "anon";

grant update on table "public"."ordenes_compra" to "anon";

grant delete on table "public"."ordenes_compra" to "authenticated";

grant insert on table "public"."ordenes_compra" to "authenticated";

grant select on table "public"."ordenes_compra" to "authenticated";

grant update on table "public"."ordenes_compra" to "authenticated";

grant delete on table "public"."ordenes_compra" to "service_role";

grant insert on table "public"."ordenes_compra" to "service_role";

grant select on table "public"."ordenes_compra" to "service_role";

grant update on table "public"."ordenes_compra" to "service_role";

grant delete on table "public"."pacientes" to "anon";

grant insert on table "public"."pacientes" to "anon";

grant select on table "public"."pacientes" to "anon";

grant update on table "public"."pacientes" to "anon";

grant delete on table "public"."pacientes" to "authenticated";

grant insert on table "public"."pacientes" to "authenticated";

grant select on table "public"."pacientes" to "authenticated";

grant update on table "public"."pacientes" to "authenticated";

grant delete on table "public"."pacientes" to "service_role";

grant insert on table "public"."pacientes" to "service_role";

grant select on table "public"."pacientes" to "service_role";

grant update on table "public"."pacientes" to "service_role";

grant delete on table "public"."pacientes_delete_audit" to "service_role";

grant insert on table "public"."pacientes_delete_audit" to "service_role";

grant select on table "public"."pacientes_delete_audit" to "service_role";

grant update on table "public"."pacientes_delete_audit" to "service_role";

grant delete on table "public"."pagos" to "anon";

grant insert on table "public"."pagos" to "anon";

grant select on table "public"."pagos" to "anon";

grant update on table "public"."pagos" to "anon";

grant delete on table "public"."pagos" to "authenticated";

grant insert on table "public"."pagos" to "authenticated";

grant select on table "public"."pagos" to "authenticated";

grant update on table "public"."pagos" to "authenticated";

grant delete on table "public"."pagos" to "service_role";

grant insert on table "public"."pagos" to "service_role";

grant select on table "public"."pagos" to "service_role";

grant update on table "public"."pagos" to "service_role";

grant delete on table "public"."pin_attempts" to "anon";

grant insert on table "public"."pin_attempts" to "anon";

grant select on table "public"."pin_attempts" to "anon";

grant update on table "public"."pin_attempts" to "anon";

grant delete on table "public"."pin_attempts" to "authenticated";

grant insert on table "public"."pin_attempts" to "authenticated";

grant select on table "public"."pin_attempts" to "authenticated";

grant update on table "public"."pin_attempts" to "authenticated";

grant delete on table "public"."pin_attempts" to "service_role";

grant insert on table "public"."pin_attempts" to "service_role";

grant select on table "public"."pin_attempts" to "service_role";

grant update on table "public"."pin_attempts" to "service_role";

grant delete on table "public"."proveedores" to "anon";

grant insert on table "public"."proveedores" to "anon";

grant select on table "public"."proveedores" to "anon";

grant update on table "public"."proveedores" to "anon";

grant delete on table "public"."proveedores" to "authenticated";

grant insert on table "public"."proveedores" to "authenticated";

grant select on table "public"."proveedores" to "authenticated";

grant update on table "public"."proveedores" to "authenticated";

grant delete on table "public"."proveedores" to "service_role";

grant insert on table "public"."proveedores" to "service_role";

grant select on table "public"."proveedores" to "service_role";

grant update on table "public"."proveedores" to "service_role";

grant delete on table "public"."regalos_dispensacion" to "anon";

grant insert on table "public"."regalos_dispensacion" to "anon";

grant select on table "public"."regalos_dispensacion" to "anon";

grant update on table "public"."regalos_dispensacion" to "anon";

grant delete on table "public"."regalos_dispensacion" to "authenticated";

grant insert on table "public"."regalos_dispensacion" to "authenticated";

grant select on table "public"."regalos_dispensacion" to "authenticated";

grant update on table "public"."regalos_dispensacion" to "authenticated";

grant delete on table "public"."regalos_dispensacion" to "service_role";

grant insert on table "public"."regalos_dispensacion" to "service_role";

grant select on table "public"."regalos_dispensacion" to "service_role";

grant update on table "public"."regalos_dispensacion" to "service_role";

grant delete on table "public"."resumen_diario" to "anon";

grant insert on table "public"."resumen_diario" to "anon";

grant select on table "public"."resumen_diario" to "anon";

grant update on table "public"."resumen_diario" to "anon";

grant delete on table "public"."resumen_diario" to "authenticated";

grant insert on table "public"."resumen_diario" to "authenticated";

grant select on table "public"."resumen_diario" to "authenticated";

grant update on table "public"."resumen_diario" to "authenticated";

grant delete on table "public"."resumen_diario" to "service_role";

grant insert on table "public"."resumen_diario" to "service_role";

grant select on table "public"."resumen_diario" to "service_role";

grant update on table "public"."resumen_diario" to "service_role";

grant delete on table "public"."schema_migrations_flags" to "anon";

grant insert on table "public"."schema_migrations_flags" to "anon";

grant select on table "public"."schema_migrations_flags" to "anon";

grant update on table "public"."schema_migrations_flags" to "anon";

grant delete on table "public"."schema_migrations_flags" to "authenticated";

grant insert on table "public"."schema_migrations_flags" to "authenticated";

grant select on table "public"."schema_migrations_flags" to "authenticated";

grant update on table "public"."schema_migrations_flags" to "authenticated";

grant delete on table "public"."schema_migrations_flags" to "service_role";

grant insert on table "public"."schema_migrations_flags" to "service_role";

grant select on table "public"."schema_migrations_flags" to "service_role";

grant update on table "public"."schema_migrations_flags" to "service_role";

grant delete on table "public"."servicios_extra" to "anon";

grant insert on table "public"."servicios_extra" to "anon";

grant select on table "public"."servicios_extra" to "anon";

grant update on table "public"."servicios_extra" to "anon";

grant delete on table "public"."servicios_extra" to "authenticated";

grant insert on table "public"."servicios_extra" to "authenticated";

grant select on table "public"."servicios_extra" to "authenticated";

grant update on table "public"."servicios_extra" to "authenticated";

grant delete on table "public"."servicios_extra" to "service_role";

grant insert on table "public"."servicios_extra" to "service_role";

grant select on table "public"."servicios_extra" to "service_role";

grant update on table "public"."servicios_extra" to "service_role";

grant delete on table "public"."sync_telemetry_optica" to "service_role";

grant insert on table "public"."sync_telemetry_optica" to "service_role";

grant select on table "public"."sync_telemetry_optica" to "service_role";

grant update on table "public"."sync_telemetry_optica" to "service_role";

grant delete on table "public"."user_profiles" to "anon";

grant insert on table "public"."user_profiles" to "anon";

grant select on table "public"."user_profiles" to "anon";

grant update on table "public"."user_profiles" to "anon";

grant delete on table "public"."user_profiles" to "authenticated";

grant insert on table "public"."user_profiles" to "authenticated";

grant select on table "public"."user_profiles" to "authenticated";

grant update on table "public"."user_profiles" to "authenticated";

grant delete on table "public"."user_profiles" to "service_role";

grant insert on table "public"."user_profiles" to "service_role";

grant select on table "public"."user_profiles" to "service_role";

grant update on table "public"."user_profiles" to "service_role";

grant delete on table "public"."usuario_optica" to "anon";

grant insert on table "public"."usuario_optica" to "anon";

grant select on table "public"."usuario_optica" to "anon";

grant update on table "public"."usuario_optica" to "anon";

grant delete on table "public"."usuario_optica" to "authenticated";

grant insert on table "public"."usuario_optica" to "authenticated";

grant select on table "public"."usuario_optica" to "authenticated";

grant update on table "public"."usuario_optica" to "authenticated";

grant delete on table "public"."usuario_optica" to "service_role";

grant insert on table "public"."usuario_optica" to "service_role";

grant select on table "public"."usuario_optica" to "service_role";

grant update on table "public"."usuario_optica" to "service_role";


  create policy "opticas_select_member"
  on "public"."opticas"
  as permissive
  for select
  to public
using (((id IN ( SELECT uo.optica_id
   FROM public.usuario_optica uo
  WHERE (uo.user_id = auth.uid()))) AND ((lower(TRIM(BOTH FROM plan_code)) <> 'dev_owner'::text) OR app_private.is_internal_owner())));



  create policy "opticas_update_member"
  on "public"."opticas"
  as permissive
  for update
  to public
using (((id IN ( SELECT uo.optica_id
   FROM public.usuario_optica uo
  WHERE (uo.user_id = auth.uid()))) AND ((lower(TRIM(BOTH FROM plan_code)) <> 'dev_owner'::text) OR app_private.is_internal_owner())))
with check (((id IN ( SELECT uo.optica_id
   FROM public.usuario_optica uo
  WHERE (uo.user_id = auth.uid()))) AND ((lower(TRIM(BOTH FROM plan_code)) <> 'dev_owner'::text) OR app_private.is_internal_owner())));



  create policy "usuario_optica_select_member_scope"
  on "public"."usuario_optica"
  as permissive
  for select
  to public
using (((user_id = auth.uid()) OR app_private.has_optica_role(auth.uid(), optica_id, ARRAY['admin'::text, 'gerente'::text])));


CREATE TRIGGER dispensaciones_updated_at BEFORE UPDATE ON public.dispensaciones FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

CREATE TRIGGER evaluaciones_updated_at BEFORE UPDATE ON public.evaluaciones FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

CREATE TRIGGER pacientes_updated_at BEFORE UPDATE ON public.pacientes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

CREATE TRIGGER pagos_updated_at BEFORE UPDATE ON public.pagos FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();

CREATE TRIGGER servicios_extra_updated_at BEFORE UPDATE ON public.servicios_extra FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();


