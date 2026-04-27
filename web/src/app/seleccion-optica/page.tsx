import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { fetchMembershipsForUser } from "@/lib/memberships";
import { setActiveOpticaContext } from "@/lib/optica-context";

async function selectOpticaAction(formData: FormData) {
  "use server";

  const opticaId = String(formData.get("opticaId") || "").trim();
  const rol = String(formData.get("rol") || "").trim();
  const nombre = String(formData.get("nombre") || "").trim();
  if (!opticaId || !rol || !nombre) return;

  await setActiveOpticaContext({ opticaId, rol, nombre });
  redirect("/dashboard");
}

export default async function SeleccionOpticaPage() {
  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();

  if (!user) redirect("/login");

  const memberships = await fetchMembershipsForUser(supabase, user.id);

  if (memberships.length === 1) {
    const only = memberships[0];
    await setActiveOpticaContext({
      opticaId: only.opticaId,
      rol: only.rol,
      nombre: only.nombre
    });
    redirect("/dashboard");
  }

  return (
    <div className="min-h-screen p-6 flex items-center justify-center">
      <div className="w-full max-w-lg rounded border bg-white p-5 shadow-sm">
        <h1 className="text-2xl font-semibold mb-1">Seleccionar optica</h1>
        <p className="text-sm text-slate-600 mb-4">
          Elige la optica activa para empezar a operar.
        </p>
        {memberships.length === 0 ? (
          <p className="text-sm text-red-600">
            No tienes membresias activas en `usuario_optica`.
          </p>
        ) : (
          <div className="space-y-3">
            {memberships.map((m) => (
              <form key={m.opticaId} action={selectOpticaAction}>
                <input type="hidden" name="opticaId" value={m.opticaId} />
                <input type="hidden" name="rol" value={m.rol} />
                <input type="hidden" name="nombre" value={m.nombre} />
                <button
                  type="submit"
                  className="w-full rounded border px-3 py-2 text-left hover:bg-slate-50"
                >
                  <p className="font-medium">{m.nombre}</p>
                  <p className="text-xs text-slate-500">Rol: {m.rol}</p>
                </button>
              </form>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
