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
    const qp = new URLSearchParams({
      opticaId: only.opticaId,
      rol: only.rol,
      nombre: only.nombre
    });
    redirect(`/auth/select-optica?${qp.toString()}`);
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-6">
      <div className="w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-sm">
        <h1 className="mb-1 text-2xl font-semibold tracking-tight">
          Seleccionar optica
        </h1>
        <p className="mb-4 text-sm text-muted-foreground">
          Elige la optica activa para empezar a operar.
        </p>
        {memberships.length === 0 ? (
          <p className="text-sm text-destructive">
            No tienes membresias activas en `usuario_optica`.
          </p>
        ) : (
          <div className="space-y-2">
            {memberships.map((m) => (
              <form key={m.opticaId} action={selectOpticaAction}>
                <input type="hidden" name="opticaId" value={m.opticaId} />
                <input type="hidden" name="rol" value={m.rol} />
                <input type="hidden" name="nombre" value={m.nombre} />
                <button
                  type="submit"
                  className="w-full rounded-lg border border-border bg-background px-3 py-3 text-left transition-colors hover:bg-accent hover:text-accent-foreground"
                >
                  <p className="font-medium">{m.nombre}</p>
                  <p className="text-xs text-muted-foreground">Rol: {m.rol}</p>
                </button>
              </form>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
