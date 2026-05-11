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
    <section className="rounded-2xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">
        Configuración Fiscal
      </h2>

      <div className="mt-6">
        {!canManage && (
          <div className="mb-6 rounded-xl bg-amber-500/10 p-4">
            <p className="text-sm font-bold text-amber-600 dark:text-amber-400">
              ⚠️ Acceso de solo lectura
            </p>
            <p className="text-xs font-medium text-amber-600/80 dark:text-amber-400/80">
              Solo administradores pueden modificar los datos fiscales de la óptica.
            </p>
          </div>
        )}
        {query.msg === "guardado" && (
          <div className="mb-6 rounded-xl bg-primary/10 p-4">
            <p className="text-sm font-bold text-primary">
              ✨ Datos guardados correctamente
            </p>
          </div>
        )}
        {(query.error === "permiso" || query.error === "validacion" || query.error === "guardar") && (
          <div className="mb-6 rounded-xl bg-destructive/10 p-4">
            <p className="text-sm font-bold text-destructive">
              ❌ {query.error === "permiso" ? "Sin permisos" : query.error === "validacion" ? "Campos incompletos" : "Error al guardar"}
            </p>
            {query.detalle && (
              <p className="mt-1 font-mono text-[10px] text-destructive/80">
                {safeDecode(query.detalle)}
              </p>
            )}
          </div>
        )}

        <form
          action={saveAction}
          className="grid grid-cols-1 gap-4 md:grid-cols-2"
        >
          <Field label="Nombre comercial">
            <input
              name="nombreComercial"
              defaultValue={fiscal?.nombre ?? ""}
              disabled={!canManage}
              className={inputClass}
              placeholder="Ej. Optica Sol y Mar"
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
          <div className="mt-4 md:col-span-2">
            <button
              type="submit"
              disabled={!canManage}
              className="w-full rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.01] active:scale-95 disabled:opacity-50"
            >
              💾 Guardar Datos Fiscales
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}

const inputClass =
  "w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground placeholder:text-muted-foreground/50 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all disabled:opacity-50";

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
    <label className="flex flex-col gap-2">
      <span className="text-xs font-bold uppercase tracking-widest text-muted-foreground/60">{label}</span>
      <div>{children}</div>
    </label>
  );
}
