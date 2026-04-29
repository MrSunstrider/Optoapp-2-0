import { redirect } from "next/navigation";
import { AppShell } from "@/components/app-shell";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { canManageFiscalConfig } from "@/lib/roles";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";

async function saveFiscalAction(formData: FormData) {
  "use server";

  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManageFiscalConfig(activeOptica.rol)) {
    redirect("/configuracion?error=permiso");
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
    redirect("/configuracion?error=validacion");
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
    redirect(`/configuracion?error=guardar&detalle=${detalle}`);
  }
  if (!data) {
    const detalle = encodeURIComponent(
      "Sin filas actualizadas (revisa RLS o que la óptica exista)."
    );
    redirect(`/configuracion?error=guardar&detalle=${detalle}`);
  }

  const persisted =
    data.nombre?.trim() === nombreComercial &&
    (data.fiscal_doc_tipo ?? "").trim().toUpperCase() === docTipo &&
    (data.fiscal_doc_numero ?? "").trim() === docNumero &&
    (data.razon_social ?? "").trim() === razonSocial &&
    (data.direccion_fiscal ?? "").trim() === direccionFiscal;

  if (!persisted) {
    const detalle = encodeURIComponent(
      "Los datos guardados no coinciden con lo esperado (posible RLS o lectura tras update)."
    );
    redirect(`/configuracion?error=guardar&detalle=${detalle}`);
  }
  redirect("/configuracion?msg=guardado");
}

export default async function ConfiguracionPage({
  searchParams
}: {
  searchParams: Promise<{ error?: string; msg?: string; detalle?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  const query = await searchParams;
  const supabase = await createClient();
  const fiscal = await fetchOpticaFiscal(supabase, activeOptica.opticaId);
  const canManage = canManageFiscalConfig(activeOptica.rol);

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <h1 className="text-2xl font-semibold mb-4">Configuracion fiscal</h1>
      {!canManage && (
        <p className="mb-3 text-sm text-amber-700">
          Tu rol tiene acceso de solo lectura. Solo admin/gerente puede guardar cambios.
        </p>
      )}
      {query.msg === "guardado" && (
        <p className="mb-3 text-sm text-emerald-700">Datos fiscales guardados correctamente.</p>
      )}
      {query.error === "permiso" && (
        <p className="mb-3 text-sm text-red-700">No tienes permisos para modificar datos fiscales.</p>
      )}
      {query.error === "validacion" && (
        <p className="mb-3 text-sm text-red-700">
          Completa campos obligatorios: nombre comercial, tipo/número documento, razón social y dirección.
        </p>
      )}
      {query.error === "guardar" && (
        <div className="mb-3 text-sm text-red-700 space-y-1">
          <p>No se pudo guardar.</p>
          {query.detalle && (
            <p className="text-xs font-mono text-red-800 whitespace-pre-wrap break-words">
              {safeDecode(query.detalle)}
            </p>
          )}
        </div>
      )}

      <form action={saveFiscalAction} className="max-w-2xl grid grid-cols-1 md:grid-cols-2 gap-3">
        <Field label="Nombre comercial">
          <input
            name="nombreComercial"
            defaultValue={fiscal?.nombre ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Tipo documento (RUC/RUS)">
          <input
            name="docTipo"
            defaultValue={fiscal?.fiscal_doc_tipo ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Numero documento">
          <input
            name="docNumero"
            defaultValue={fiscal?.fiscal_doc_numero ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Razon social">
          <input
            name="razonSocial"
            defaultValue={fiscal?.razon_social ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Direccion fiscal">
          <input
            name="direccionFiscal"
            defaultValue={fiscal?.direccion_fiscal ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Distrito/Ciudad/Departamento">
          <input
            name="distrito"
            defaultValue={fiscal?.distrito_ciudad_departamento ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Moneda">
          <input
            name="moneda"
            defaultValue={fiscal?.moneda ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="Pais">
          <input
            name="pais"
            defaultValue={fiscal?.pais ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <Field label="WhatsApp/Telefono">
          <input
            name="contacto"
            defaultValue={fiscal?.contacto_whatsapp_telefono ?? ""}
            disabled={!canManage}
            className="w-full rounded border px-3 py-2 text-sm disabled:bg-slate-100"
          />
        </Field>
        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={!canManage}
            className="rounded bg-slate-900 text-white px-4 py-2 text-sm disabled:opacity-50"
          >
            Guardar datos fiscales
          </button>
        </div>
      </form>
    </AppShell>
  );
}

function safeDecode(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

function Field({
  label,
  children
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block">
      <span className="text-sm text-slate-700">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
