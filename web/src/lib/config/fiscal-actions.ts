import { redirect } from "next/navigation";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManageFiscalConfig } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

export async function saveFiscalAction(formData: FormData) {
  "use server";

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManageFiscalConfig(activeOptica.rol)) {
    redirect("/configuracion/fiscal?error=permiso");
  }

  const docTipo = String(formData.get("docTipo") ?? "").trim().toUpperCase();
  const docNumero = String(formData.get("docNumero") ?? "").trim();
  const razonSocial = String(formData.get("razonSocial") ?? "").trim();
  const direccionFiscal = String(formData.get("direccionFiscal") ?? "").trim();
  const nombreComercial = String(formData.get("nombreComercial") ?? "").trim();
  const distrito = String(formData.get("distrito") ?? "").trim();
  const moneda = String(formData.get("moneda") ?? "").trim();
  const pais = String(formData.get("pais") ?? "").trim();
  const contacto = String(formData.get("contacto") ?? "").trim();

  if (!docTipo || !docNumero || !razonSocial || !direccionFiscal || !nombreComercial) {
    redirect("/configuracion/fiscal?error=validacion");
  }

  const supabase = await createClient();
  const { data, error } = await supabase
    .from("opticas")
    .update({
      nombre: nombreComercial,
      fiscal_doc_tipo: docTipo,
      fiscal_doc_numero: docNumero,
      razon_social: razonSocial,
      direccion_fiscal: direccionFiscal,
      distrito_ciudad_departamento: distrito,
      moneda,
      pais,
      contacto_whatsapp_telefono: contacto
    })
    .eq("id", activeOptica.opticaId)
    .select(
      "id,nombre,fiscal_doc_tipo,fiscal_doc_numero,razon_social,direccion_fiscal,distrito_ciudad_departamento,moneda,pais,contacto_whatsapp_telefono"
    )
    .maybeSingle();

  if (error) {
    const detalle = encodeURIComponent(error.message.slice(0, 900));
    redirect(`/configuracion/fiscal?error=guardar&detalle=${detalle}`);
  }
  if (!data) {
    const detalle = encodeURIComponent(
      "Sin filas actualizadas (revisa RLS o que la óptica exista)."
    );
    redirect(`/configuracion/fiscal?error=guardar&detalle=${detalle}`);
  }

  redirect("/configuracion/fiscal?msg=guardado");
}
