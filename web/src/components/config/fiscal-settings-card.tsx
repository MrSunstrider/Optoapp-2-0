import type { OpticaFiscalRow } from "@/lib/optica-fiscal";

type Query = { error?: string; msg?: string; detalle?: string };

export function FiscalSettingsCard({
  canManage,
  fiscal,
  query,
  saveAction
}: {
  canManage: boolean;
  fiscal: OpticaFiscalRow | null;
  query: Query;
  saveAction: (formData: FormData) => Promise<void>;
}) {
  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5 shadow-sm">
      <h2 className="mb-4 text-xl font-semibold text-[#8AB4F8]">
        Configuración Fiscal
      </h2>

      {!canManage && (
        <p className="mb-3 text-sm text-amber-300">
          Tu rol tiene acceso de solo lectura. Solo admin/gerente puede guardar
          cambios.
        </p>
      )}
      {query.msg === "guardado" && (
        <p className="mb-3 text-sm text-emerald-300">
          Datos fiscales guardados correctamente.
        </p>
      )}
      {query.error === "permiso" && (
        <p className="mb-3 text-sm text-red-300">
          No tienes permisos para modificar datos fiscales.
        </p>
      )}
      {query.error === "validacion" && (
        <p className="mb-3 text-sm text-red-300">
          Completa campos obligatorios: nombre comercial, tipo/número documento,
          razón social y dirección.
        </p>
      )}
      {query.error === "guardar" && (
        <div className="mb-3 space-y-1 text-sm text-red-300">
          <p>No se pudo guardar.</p>
          {query.detalle && (
            <p className="break-words whitespace-pre-wrap font-mono text-xs text-red-200/90">
              {safeDecode(query.detalle)}
            </p>
          )}
        </div>
      )}

      <form
        action={saveAction}
        className="grid max-w-4xl grid-cols-1 gap-3 md:grid-cols-2"
      >
        <Field label="Nombre comercial">
          <input
            name="nombreComercial"
            defaultValue={fiscal?.nombre ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="Tipo documento (RUC/RUS)">
          <input
            name="docTipo"
            defaultValue={fiscal?.fiscal_doc_tipo ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="Número documento">
          <input
            name="docNumero"
            defaultValue={fiscal?.fiscal_doc_numero ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="Razón social">
          <input
            name="razonSocial"
            defaultValue={fiscal?.razon_social ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="Dirección fiscal">
          <input
            name="direccionFiscal"
            defaultValue={fiscal?.direccion_fiscal ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="Distrito/Ciudad/Departamento">
          <input
            name="distrito"
            defaultValue={fiscal?.distrito_ciudad_departamento ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="Moneda">
          <input
            name="moneda"
            defaultValue={fiscal?.moneda ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="País">
          <input
            name="pais"
            defaultValue={fiscal?.pais ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <Field label="WhatsApp/Teléfono">
          <input
            name="contacto"
            defaultValue={fiscal?.contacto_whatsapp_telefono ?? ""}
            disabled={!canManage}
            className={inputClass}
          />
        </Field>
        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={!canManage}
            className="rounded-full bg-[#8AB4F8] px-5 py-2.5 text-sm font-semibold text-zinc-900 transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            Guardar datos fiscales
          </button>
        </div>
      </form>
    </section>
  );
}

const inputClass =
  "w-full rounded-xl border border-zinc-500/70 bg-[#4A4756] px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-300/70 disabled:opacity-60";

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
      <span className="text-sm text-zinc-200">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
