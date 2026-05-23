import { redirect } from "next/navigation";

/** La app abre por defecto la pestaña Evaluaciones; el resumen va en el layout. */
export default async function PacienteIdRedirectPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { id } = await params;
  const sp = await searchParams;
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(sp)) {
    if (typeof v === "string" && v.length) qs.set(k, v);
    else if (Array.isArray(v) && v[0]) qs.set(k, v[0]);
  }
  const suffix = qs.toString() ? `?${qs.toString()}` : "";
  redirect(`/pacientes/${id}/evaluaciones${suffix}`);
}
