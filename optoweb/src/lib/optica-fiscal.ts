import type { SupabaseClient } from "@supabase/supabase-js";
import { assertNoDbError } from "@/lib/supabase/db-error";

export type OpticaFiscalRow = {
  id: string;
  nombre: string | null;
  fiscal_doc_tipo: string | null;
  fiscal_doc_numero: string | null;
  razon_social: string | null;
  direccion_fiscal: string | null;
  distrito_ciudad_departamento: string | null;
  moneda: string | null;
  pais: string | null;
  contacto_whatsapp_telefono: string | null;
};

export async function fetchOpticaFiscal(
  supabase: SupabaseClient,
  opticaId: string
): Promise<OpticaFiscalRow | null> {
  const { data, error } = await supabase
    .from("opticas")
    .select(
      "id,nombre,fiscal_doc_tipo,fiscal_doc_numero,razon_social,direccion_fiscal,distrito_ciudad_departamento,moneda,pais,contacto_whatsapp_telefono"
    )
    .eq("id", opticaId)
    .limit(1)
    .maybeSingle();

  assertNoDbError(error, "Datos fiscales de la óptica");
  if (!data) return null;
  return data as OpticaFiscalRow;
}
