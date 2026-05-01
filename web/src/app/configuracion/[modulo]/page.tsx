import Link from "next/link";
import { redirect } from "next/navigation";

import { AppShell } from "@/components/app-shell";
import { ConfigSectionShell } from "@/components/config/config-section-shell";
import { DiagnosticoCard } from "@/components/config/diagnostico-card";
import { FiscalSettingsCard } from "@/components/config/fiscal-settings-card";
import { GeneralSettingsCard } from "@/components/config/general-settings-card";
import { LaboratorioCard } from "@/components/config/laboratorio-card";
import { SecurityAccessCard } from "@/components/config/security-access-card";
import { SucursalesCard } from "@/components/config/sucursales-card";
import { saveFiscalAction } from "@/lib/config/fiscal-actions";
import { CONFIG_MODULES } from "@/lib/config/modules";
import {
  canAccessConfigModule,
  canManageOpticaSettings,
  isInternalRole
} from "@/lib/config/permissions";
import { fetchDashboardKpis } from "@/lib/dashboard-kpis";
import { fetchOpticaFiscal } from "@/lib/optica-fiscal";
import { getActiveOpticaContext } from "@/lib/optica-context";
import { getOpticaPacienteLimitInfo } from "@/lib/optica-limits";
import { fetchOpticaSettings } from "@/lib/optica-settings";
import { isPinRequiredFromUser } from "@/lib/pin-policy";
import { canManageFiscalConfig } from "@/lib/roles";
import { createClient } from "@/lib/supabase/server";

type Query = { msg?: string; error?: string; detalle?: string };

export default async function ConfigModulePage({
  params,
  searchParams
}: {
  params: Promise<{ modulo: string }>;
  searchParams: Promise<Query>;
}) {
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  const { modulo } = await params;
  const query = await searchParams;

  const moduleMeta = CONFIG_MODULES.find((m) => m.key === modulo);
  if (!moduleMeta) redirect("/configuracion");
  if (!canAccessConfigModule(activeOptica.rol, moduleMeta.key)) {
    redirect("/configuracion?error=permiso");
  }

  const supabase = await createClient();
  const {
    data: { user }
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const metadata = (user.user_metadata ?? {}) as Record<string, unknown>;
  const tableOpticaConfig = await fetchOpticaSettings(supabase, activeOptica.opticaId);
  const legacyOpticaConfig = asRecord(asRecord(metadata.optoapp_optica_config)[activeOptica.opticaId]);
  const currentOpticaConfig = { ...legacyOpticaConfig, ...tableOpticaConfig };

  return (
    <AppShell role={activeOptica.rol} opticaName={activeOptica.nombre}>
      <div className="min-h-screen bg-background p-4 sm:p-8 text-foreground transition-colors duration-300">
        <div className="mx-auto w-full max-w-5xl space-y-6">
          <ConfigSectionShell title={moduleMeta.title} description={moduleMeta.description}>
            {moduleMeta.key === "seguridad" && (
              <div className="space-y-4">
                <SecurityAccessCard initialPinRequired={isPinRequiredFromUser(user)} />
                <GeneralSettingsCard
                  initialAutomaticReminders={
                    typeof metadata.optoapp_automatic_reminders === "boolean"
                      ? metadata.optoapp_automatic_reminders
                      : true
                  }
                />
              </div>
            )}

            {moduleMeta.key === "laboratorio" && (
              <LaboratorioCard
                initial={{
                  preferredLab: asString(currentOpticaConfig.labConfig, "preferredLab"),
                  deliveryDays: asNumber(currentOpticaConfig.labConfig, "deliveryDays", 7),
                  costBase: asNumber(currentOpticaConfig.labConfig, "costBase", 0),
                  priority: asPriority(currentOpticaConfig.labConfig),
                  notes: asString(currentOpticaConfig.labConfig, "notes")
                }}
              />
            )}

            {moduleMeta.key === "fiscal" && (
              <FiscalSection role={activeOptica.rol} query={query} opticaId={activeOptica.opticaId} />
            )}

            {moduleMeta.key === "plan-admin" && (
              <PlanAdminSection
                isInternal={isInternalRole(activeOptica.rol)}
                query={query}
                role={activeOptica.rol}
                currentOpticaConfig={currentOpticaConfig}
              />
            )}

            {moduleMeta.key === "usuarios-roles" && (
              <UserRolesSection
                query={query}
                canManage={canManageOpticaSettings(activeOptica.rol)}
                opticaId={activeOptica.opticaId}
              />
            )}

            {moduleMeta.key === "sucursales" && (
              <SucursalesCard
                initialBranches={asBranches(currentOpticaConfig.branches)}
              />
            )}

            {moduleMeta.key === "suscripciones" && (
              <SubscriptionLimitsSection opticaId={activeOptica.opticaId} />
            )}

            {moduleMeta.key === "diagnostico-sync" && (
              <DiagnosticoSyncSection opticaId={activeOptica.opticaId} />
            )}

            {moduleMeta.key === "gestion-datos" && (
              <DataManagementSection role={activeOptica.rol} />
            )}
          </ConfigSectionShell>
        </div>
      </div>
    </AppShell>
  );
}

async function FiscalSection({
  role,
  query,
  opticaId
}: {
  role: string;
  query: Query;
  opticaId: string;
}) {
  const supabase = await createClient();
  const fiscal = await fetchOpticaFiscal(supabase, opticaId);
  const canManage = canManageFiscalConfig(role);
  return (
    <FiscalSettingsCard
      canManage={canManage}
      fiscal={fiscal}
      query={query}
      saveAction={saveFiscalAction}
    />
  );
}

async function PlanAdminSection({
  isInternal,
  query,
  role,
  currentOpticaConfig
}: {
  isInternal: boolean;
  query: Query;
  role: string;
  currentOpticaConfig: Record<string, unknown>;
}) {
  if (!isInternal) {
    return (
      <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-6 text-sm font-medium text-amber-600">
        Este módulo es solo para staff interno autorizado.
      </div>
    );
  }
  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Administración de plan</h2>
      <p className="mt-2 text-sm font-medium text-muted-foreground">
        Rol activo: <span className="text-primary font-black uppercase tracking-tighter">{role}</span>. Ajustes internos.
      </p>
      <pre className="mt-4 overflow-x-auto rounded-2xl bg-foreground/[0.03] p-4 text-xs font-mono text-foreground/70">
        {JSON.stringify(currentOpticaConfig.planAdmin ?? {}, null, 2)}
      </pre>
      {query.msg ? <p className="mt-4 text-xs font-bold text-emerald-500 uppercase tracking-widest">{query.msg}</p> : null}
    </section>
  );
}

async function UserRolesSection({
  query,
  canManage,
  opticaId
}: {
  query: Query;
  canManage: boolean;
  opticaId: string;
}) {
  const supabase = await createClient();
  const { data, error } = await supabase
    .from("usuario_optica")
    .select("user_id,rol,user_profiles(email)")
    .eq("optica_id", opticaId);

  const members =
    !error && Array.isArray(data)
      ? (data as Array<{
          user_id: string;
          rol: string;
          user_profiles?: { email?: string | null } | null;
        }>)
      : [];

  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Usuarios y Roles</h2>
      {!canManage ? (
        <p className="mt-2 text-xs font-bold text-amber-500 uppercase tracking-widest">Acceso solo lectura para tu rol.</p>
      ) : null}
      
      <div className="mt-6 space-y-3">
        {members.map((m) => (
          <div
            key={m.user_id}
            className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-border bg-foreground/[0.02] p-4 transition-all hover:bg-foreground/[0.04]"
          >
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-foreground">{m.user_profiles?.email ?? m.user_id}</p>
              <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/50">ID: {m.user_id}</p>
            </div>
            {canManage ? (
              <form action={updateRoleAction} className="flex items-center gap-2">
                <input type="hidden" name="userId" value={m.user_id} />
                <select
                  name="rol"
                  defaultValue={m.rol}
                  className="rounded-xl border border-border bg-background px-3 py-1.5 text-xs font-bold text-foreground outline-none focus:ring-2 focus:ring-primary/20"
                >
                  <option value="admin">admin</option>
                  <option value="gerente">gerente</option>
                  <option value="colaborador">colaborador</option>
                  <option value="invitado">invitado</option>
                </select>
                <button className="rounded-xl bg-primary px-3 py-1.5 text-[10px] font-black uppercase tracking-widest text-primary-foreground hover:scale-105 transition-all">
                  Guardar
                </button>
              </form>
            ) : (
               <span className="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-primary">
                 {m.rol}
               </span>
            )}
          </div>
        ))}
      </div>

      {canManage && (
        <form action={inviteMemberAction} className="mt-8 rounded-2xl border border-dashed border-border p-6 bg-foreground/[0.01]">
          <h3 className="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground/60 mb-4">Vincular nuevo usuario</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-end">
            <div className="space-y-1">
              <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">ID del Usuario (UUID)</span>
              <input
                name="userId"
                className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/20"
                placeholder="00000000-0000-0000-0000-000000000000"
              />
            </div>
            <div className="space-y-1">
              <span className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60">Asignar Rol</span>
              <select
                name="rol"
                className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/20"
                defaultValue="colaborador"
              >
                <option value="admin">admin</option>
                <option value="gerente">gerente</option>
                <option value="colaborador">colaborador</option>
                <option value="invitado">invitado</option>
              </select>
            </div>
          </div>
          <button className="mt-4 w-full rounded-2xl bg-primary py-4 text-xs font-black uppercase tracking-[0.2em] text-primary-foreground shadow-xl shadow-primary/20 hover:scale-[1.01] transition-all active:scale-95">
            Vincular Usuario a la Óptica
          </button>
        </form>
      )}
    </section>
  );
}

async function SubscriptionLimitsSection({ opticaId }: { opticaId: string }) {
  const supabase = await createClient();
  const [limits, userCount, branchCount] = await Promise.all([
    getOpticaPacienteLimitInfo(supabase, opticaId),
    supabase
      .from("usuario_optica")
      .select("user_id", { count: "exact", head: true })
      .eq("optica_id", opticaId),
    Promise.resolve({ count: null as number | null })
  ]);
  const patientState = stateByUsage(limits.pacientesActuales, limits.maxPacientes);
  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Suscripción y Límites</h2>
      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <LimitRow
          label="Pacientes Registrados"
          used={limits.pacientesActuales}
          max={limits.maxPacientes}
          state={patientState}
        />
        <LimitRow
          label="Usuarios Activos"
          used={userCount.count ?? 0}
          max={null}
          state="normal"
        />
        <LimitRow label="Sucursales" used={branchCount.count ?? 0} max={null} state="normal" />
      </div>
      <p className="mt-6 text-[10px] font-black uppercase tracking-widest text-muted-foreground/40 text-center">
        Para ampliar capacidad contacta con el soporte de OptoApp SaaS.
      </p>
    </section>
  );
}

async function DiagnosticoSyncSection({ opticaId }: { opticaId: string }) {
  const supabase = await createClient();
  const snapshot = await fetchDashboardKpis(supabase, opticaId);
  const report = JSON.stringify(snapshot, null, 2);
  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Diagnóstico del Sistema</h2>
      <div className="mt-6 space-y-4">
        <div className="flex items-center justify-between p-4 rounded-2xl bg-foreground/[0.02] border border-border/50">
          <div className="flex items-center gap-3">
            <div className={`h-3 w-3 rounded-full animate-pulse ${snapshot.status.overall === "ok" ? "bg-emerald-500 shadow-[0_0_12px_rgba(16,185,129,0.5)]" : "bg-amber-500"}`} />
            <span className="text-sm font-bold text-foreground">Integridad de Sincronización</span>
          </div>
          <span className={`text-xs font-black uppercase tracking-widest ${snapshot.status.overall === "ok" ? "text-emerald-500" : "text-amber-500"}`}>
            {snapshot.status.overall === "ok" ? "OPERATIVO" : "DEGRADADO"}
          </span>
        </div>
        <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/40 ml-1">ÚLTIMO SYNC: {snapshot.status.lastUpdatedIso}</p>
        <DiagnosticoCard reportText={report} />
      </div>
    </section>
  );
}

function DataManagementSection({ role }: { role: string }) {
  const canManage = canManageOpticaSettings(role);
  return (
    <section className="rounded-3xl border border-border bg-card p-6 shadow-sm">
      <h2 className="font-heading text-xl font-bold text-foreground">Gestión de Datos Maestros</h2>
      <p className="mt-2 text-sm font-medium text-muted-foreground">
        Exportaciones seguras de la base de datos operativa.
      </p>
      <div className="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-3">
        <Link className={btn} href="/api/dashboard/export?type=pendientes">
          📋 Pendientes CSV
        </Link>
        <Link className={btn} href="/api/dashboard/export?type=cierre">
          💰 Cierre Caja CSV
        </Link>
        <Link className={btn} href="/api/dashboard/export?type=inventario-csv">
          📦 Inventario CSV
        </Link>
      </div>
      {!canManage && (
        <p className="mt-6 text-center text-[10px] font-black uppercase tracking-widest text-amber-500">
          Tu rol actual tiene restricciones en acciones destructivas.
        </p>
      )}
    </section>
  );
}

function LimitRow({
  label,
  used,
  max,
  state
}: {
  label: string;
  used: number;
  max: number | null;
  state: "normal" | "warning" | "error";
}) {
  const color =
    state === "error" ? "text-rose-500" : state === "warning" ? "text-amber-500" : "text-emerald-500";
  return (
    <div className="rounded-2xl border border-border bg-foreground/[0.02] p-4 text-center">
      <p className="text-[10px] font-black uppercase tracking-widest text-muted-foreground/60 mb-2">{label}</p>
      <p className={`font-heading text-2xl font-black ${color}`}>
        {used} <span className="text-muted-foreground/30 text-sm font-light">/</span> {max ?? "∞"}
      </p>
    </div>
  );
}

function stateByUsage(used: number, max: number | null): "normal" | "warning" | "error" {
  if (max === null) return "normal";
  if (used > max) return "error";
  if (used >= Math.floor(max * 0.9)) return "warning";
  return "normal";
}

function asRecord(value: unknown): Record<string, unknown> {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return {};
}

function asString(value: unknown, key: string): string {
  const obj = asRecord(value);
  return typeof obj[key] === "string" ? String(obj[key]) : "";
}

function asNumber(value: unknown, key: string, fallback: number): number {
  const obj = asRecord(value);
  const n = Number(obj[key]);
  return Number.isFinite(n) ? n : fallback;
}

function asPriority(value: unknown): "normal" | "urgente" {
  const v = asString(value, "priority");
  return v === "urgente" ? "urgente" : "normal";
}

function asBranches(value: unknown) {
  if (!Array.isArray(value)) return [];
  return value
    .map((item) => asRecord(item))
    .filter((item) => typeof item.id === "string" && typeof item.nombre === "string")
    .map((item) => ({
      id: String(item.id),
      nombre: String(item.nombre),
      direccion: typeof item.direccion === "string" ? item.direccion : "",
      contacto: typeof item.contacto === "string" ? item.contacto : "",
      activa: item.activa !== false,
      principal: item.principal === true
    }));
}

const btn =
  "flex items-center justify-center rounded-2xl border border-border bg-card p-4 text-xs font-bold text-foreground shadow-sm transition-all hover:bg-primary hover:text-primary-foreground hover:border-primary active:scale-95";

async function updateRoleAction(formData: FormData) {
  "use server";
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManageOpticaSettings(activeOptica.rol)) {
    redirect("/configuracion/usuarios-roles?error=Sin permiso");
  }
  const userId = String(formData.get("userId") ?? "").trim();
  const rol = String(formData.get("rol") ?? "").trim().toLowerCase();
  if (!userId || !rol) redirect("/configuracion/usuarios-roles?error=Datos inválidos");
  const supabase = await createClient();
  const { data: admins } = await supabase
    .from("usuario_optica")
    .select("user_id,rol")
    .eq("optica_id", activeOptica.opticaId);
  const adminCount =
    (admins ?? []).filter((m) => String((m as { rol: string }).rol).toLowerCase() === "admin").length;
  const current = (admins ?? []).find((m) => (m as { user_id: string }).user_id === userId) as
    | { rol: string }
    | undefined;
  if (current && current.rol.toLowerCase() === "admin" && rol !== "admin" && adminCount <= 1) {
    redirect("/configuracion/usuarios-roles?error=Debe existir al menos un admin");
  }
  const { error } = await supabase
    .from("usuario_optica")
    .update({ rol })
    .eq("optica_id", activeOptica.opticaId)
    .eq("user_id", userId);
  if (error) redirect("/configuracion/usuarios-roles?error=No se pudo actualizar rol");
  redirect("/configuracion/usuarios-roles?msg=Rol actualizado");
}

async function inviteMemberAction(formData: FormData) {
  "use server";
  const activeOptica = await getActiveOpticaContext();
  if (!activeOptica) redirect("/seleccion-optica");
  if (!canManageOpticaSettings(activeOptica.rol)) {
    redirect("/configuracion/usuarios-roles?error=Sin permiso");
  }
  const userId = String(formData.get("userId") ?? "").trim();
  const rol = String(formData.get("rol") ?? "").trim().toLowerCase();
  if (!userId || !rol) redirect("/configuracion/usuarios-roles?error=Datos inválidos");
  const supabase = await createClient();
  const { error } = await supabase.from("usuario_optica").upsert(
    {
      user_id: userId,
      optica_id: activeOptica.opticaId,
      rol
    },
    { onConflict: "user_id,optica_id" }
  );
  if (error) redirect("/configuracion/usuarios-roles?error=No se pudo vincular usuario");
  redirect("/configuracion/usuarios-roles?msg=Usuario vinculado");
}
