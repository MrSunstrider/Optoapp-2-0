import { redirect } from "next/navigation";
import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { fetchServiciosExtraPorPaciente } from "@/lib/paciente-clinico";

function formatMoney(n: number | null): string {
  if (n === null || Number.isNaN(n)) return "—";
  return `S/ ${n.toFixed(2)}`;
}

export default async function PacienteServiciosExtraPage({
  params,
  searchParams
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ msg?: string }>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");

  const { id } = await params;
  const query = await searchParams;
  const supabase = await createClient();
  const rows = await fetchServiciosExtraPorPaciente(supabase, activeOptica.opticaId, id);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-1">
        <h2 className="font-heading text-2xl font-black tracking-tight text-foreground">Servicios Adicionales</h2>
        <p className="text-sm font-medium text-muted-foreground">
          Gestión de otros servicios y cobros vinculados al paciente.
        </p>
      </div>

      {query.msg === "creado" && (
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-xs font-bold text-emerald-600 animate-in fade-in slide-in-from-top-2">
          ✓ Servicio extra registrado con éxito.
        </div>
      )}
      {query.msg === "actualizado" && (
        <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-xs font-bold text-emerald-600 animate-in fade-in slide-in-from-top-2">
          ✓ Cambios guardados correctamente.
        </div>
      )}

      {rows.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-border p-12 text-center">
          <p className="text-sm font-medium text-muted-foreground">No hay servicios adicionales registrados.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {rows.map((r) => {
            const saldo = (r.monto_total ?? 0) - (r.a_cuenta ?? 0);
            return (
              <div key={r.id} className="group rounded-3xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-md">
                <div className="flex justify-between items-start mb-4">
                  <div className="space-y-1">
                    <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50">{r.fecha ?? "Sin fecha"}</p>
                    <p className="font-heading text-xl font-black text-foreground">{r.descripcion || "Servicio General"}</p>
                    {r.ot?.trim() && <p className="text-[10px] font-black text-primary uppercase tracking-tighter mt-1">Ref OT: {r.ot}</p>}
                  </div>
                  <span className={`rounded-full px-3 py-1 text-[10px] font-black uppercase tracking-widest ring-1 ring-inset ${r.estado === "Entregado" ? "bg-emerald-500/10 text-emerald-500 ring-emerald-500/30" : "bg-primary/10 text-primary ring-primary/30"}`}>
                    {r.estado ?? "Pendiente"}
                  </span>
                </div>
                
                <div className="grid grid-cols-2 gap-4 mb-6 p-5 rounded-2xl bg-foreground/[0.02] border border-border/50">
                  <div className="space-y-1">
                    <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">Costo Servicio</p>
                    <p className="font-heading text-lg font-bold text-foreground">{formatMoney(r.monto_total)}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">Saldo Pendiente</p>
                    <p className={`font-heading text-lg font-black ${saldo > 0 ? "text-destructive" : "text-emerald-500"}`}>{formatMoney(saldo)}</p>
                  </div>
                </div>

                <Link
                  href={`/pacientes/${id}/servicios-extra/${r.id}/editar`}
                  className="flex w-full items-center justify-center rounded-2xl bg-primary/10 py-4 text-[10px] font-black uppercase tracking-widest text-primary hover:bg-primary hover:text-primary-foreground transition-all shadow-sm"
                >
                  🛠️ ADMINISTRAR SERVICIO
                </Link>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
