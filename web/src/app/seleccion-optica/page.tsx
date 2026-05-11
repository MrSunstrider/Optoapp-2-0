import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { fetchMembershipsForUser } from "@/lib/memberships";
import { setActiveOpticaContext } from "@/lib/optica-context";
import { crearOptica, canjearInvitacion } from "@/lib/invitaciones";
import { CrearOpticaForm } from "@/components/invitaciones/crear-optica-form";
import { CanjearCodigoForm } from "@/components/invitaciones/canjear-codigo-form";

type FormState = { error?: string } | null;

async function selectOpticaAction(formData: FormData) {
  "use server";

  const opticaId = String(formData.get("opticaId") || "").trim();
  const rol = String(formData.get("rol") || "").trim();
  const nombre = String(formData.get("nombre") || "").trim();
  if (!opticaId || !rol || !nombre) return;

  await setActiveOpticaContext({ opticaId, rol, nombre });
  redirect("/dashboard");
}

async function crearOpticaAction(
  _prevState: FormState,
  formData: FormData,
): Promise<FormState> {
  "use server";

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "No autenticado." };

  const nombre = String(formData.get("nombre") ?? "").trim();
  if (!nombre) return { error: "El nombre es obligatorio." };

  const result = await crearOptica(supabase, user.id, nombre);
  if (!result.ok) return { error: result.error };

  await setActiveOpticaContext({
    opticaId: result.opticaId,
    rol: "admin",
    nombre,
  });
  redirect("/dashboard");
}

async function canjearInvitacionAction(
  _prevState: FormState,
  formData: FormData,
): Promise<FormState> {
  "use server";

  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return { error: "No autenticado." };

  const codigo = String(formData.get("codigo") ?? "").trim();
  if (!codigo) return { error: "El código es obligatorio." };

  const result = await canjearInvitacion(supabase, codigo, user.id);
  if (!result.ok) return { error: result.error };

  const memberships = await fetchMembershipsForUser(supabase, user.id);
  const assigned = memberships.find(
    (m) => m.opticaId === result.opticaId && m.rol === result.rol,
  );

  await setActiveOpticaContext({
    opticaId: result.opticaId,
    rol: result.rol,
    nombre: assigned?.nombre ?? result.opticaId,
  });
  redirect("/dashboard");
}

export default async function SeleccionOpticaPage() {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (!user) redirect("/login");

  const memberships = await fetchMembershipsForUser(supabase, user.id);

  if (memberships.length === 1) {
    const only = memberships[0];
    const qp = new URLSearchParams({
      opticaId: only.opticaId,
      rol: only.rol,
      nombre: only.nombre,
    });
    redirect(`/auth/select-optica?${qp.toString()}`);
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-6">
      <div className="w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-sm">
        <h1 className="mb-1 text-2xl font-semibold tracking-tight">
          {memberships.length === 0
            ? "Bienvenido a OptoApp"
            : "Seleccionar optica"}
        </h1>
        <p className="mb-4 text-sm text-muted-foreground">
          {memberships.length === 0
            ? "Empezá creando tu propia óptica o ingresá un código de invitación."
            : "Elegí la óptica activa para empezar a operar."}
        </p>

        {memberships.length > 0 ? (
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
        ) : (
          <div className="space-y-4">
            <CrearOpticaForm action={crearOpticaAction} />
            <CanjearCodigoForm action={canjearInvitacionAction} />
          </div>
        )}
      </div>
    </div>
  );
}
