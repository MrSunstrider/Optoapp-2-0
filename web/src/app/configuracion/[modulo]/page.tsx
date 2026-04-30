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
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-5xl">
          <ConfigSectionShell title={moduleMeta.title} description={moduleMeta.description}>
            {moduleMeta.key === "seguridad" && (
              <>
                <SecurityAccessCard initialPinRequired={isPinRequiredFromUser(user)} />
                <GeneralSettingsCard
                  initialAutomaticReminders={
                    typeof metadata.optoapp_automatic_reminders === "boolean"
                      ? metadata.optoapp_automatic_reminders
                      : true
                  }
                />
              </>
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
      <MessageCard message="Este módulo es solo para staff interno autorizado." tone="warn" />
    );
  }
  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Administración de plan</h2>
      <p className="mt-2 text-sm text-zinc-300">
        Rol activo: {role}. Ajustes internos y notas operativas por óptica.
      </p>
      <pre className="mt-3 overflow-x-auto rounded-xl bg-black/20 p-3 text-xs text-zinc-300">
        {JSON.stringify(currentOpticaConfig.planAdmin ?? {}, null, 2)}
      </pre>
      {query.msg ? <p className="mt-2 text-xs text-emerald-300">{query.msg}</p> : null}
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
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Usuario y roles</h2>
      {!canManage ? (
        <p className="mt-2 text-sm text-amber-300">Acceso solo lectura para tu rol.</p>
      ) : null}
      {query.msg ? <p className="mt-2 text-sm text-emerald-300">{query.msg}</p> : null}
      {query.error ? <p className="mt-2 text-sm text-red-300">{query.error}</p> : null}
      {error ? (
        <p className="mt-3 text-sm text-red-300">
          No se pudo cargar miembros de la óptica.
        </p>
      ) : (
        <div className="mt-3 space-y-2">
          {members.map((m) => (
            <div
              key={m.user_id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-zinc-700 bg-black/10 p-3"
            >
              <div>
                <p className="text-sm text-zinc-100">{m.user_profiles?.email ?? m.user_id}</p>
                <p className="text-xs text-zinc-400">ID: {m.user_id}</p>
              </div>
              {canManage ? (
                <form action={updateRoleAction} className="flex items-center gap-2">
                  <input type="hidden" name="userId" value={m.user_id} />
                  <select
                    name="rol"
                    defaultValue={m.rol}
                    className="rounded border border-zinc-600 bg-zinc-900 px-2 py-1 text-sm"
                  >
                    <option value="admin">admin</option>
                    <option value="gerente">gerente</option>
                    <option value="colaborador">colaborador</option>
                    <option value="invitado">invitado</option>
                  </select>
                  <button className="rounded border border-zinc-600 px-2 py-1 text-xs">
                    Guardar
                  </button>
                </form>
              ) : null}
            </div>
          ))}
        </div>
      )}
      {canManage ? (
        <form action={inviteMemberAction} className="mt-4 flex flex-wrap items-end gap-2">
          <label className="text-sm">
            <span className="text-zinc-300">ID de usuario a vincular</span>
            <input
              name="userId"
              className="mt-1 w-full rounded border border-zinc-600 bg-zinc-900 px-2 py-1"
              placeholder="uuid usuario"
            />
          </label>
          <label className="text-sm">
            <span className="text-zinc-300">Rol</span>
            <select
              name="rol"
              className="mt-1 rounded border border-zinc-600 bg-zinc-900 px-2 py-1"
              defaultValue="colaborador"
            >
              <option value="admin">admin</option>
              <option value="gerente">gerente</option>
              <option value="colaborador">colaborador</option>
              <option value="invitado">invitado</option>
            </select>
          </label>
          <button className="rounded-full bg-[#8AB4F8] px-4 py-2 text-sm font-semibold text-zinc-900">
            Vincular usuario
          </button>
        </form>
      ) : null}
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
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Suscripciones y límites</h2>
      <div className="mt-3 grid grid-cols-1 gap-2 md:grid-cols-2">
        <LimitRow
          label="Pacientes"
          used={limits.pacientesActuales}
          max={limits.maxPacientes}
          state={patientState}
        />
        <LimitRow
          label="Usuarios"
          used={userCount.count ?? 0}
          max={null}
          state="normal"
        />
        <LimitRow label="Sucursales" used={branchCount.count ?? 0} max={null} state="normal" />
      </div>
      <p className="mt-3 text-xs text-zinc-300">
        Si requieres ampliar capacidad, contacta al administrador del plan para upgrade.
      </p>
    </section>
  );
}

async function DiagnosticoSyncSection({ opticaId }: { opticaId: string }) {
  const supabase = await createClient();
  const snapshot = await fetchDashboardKpis(supabase, opticaId);
  const report = JSON.stringify(snapshot, null, 2);
  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Diagnóstico de sincronización</h2>
      <p className="mt-2 text-sm text-zinc-200">
        Estado general:{" "}
        <span className={snapshot.status.overall === "ok" ? "text-emerald-300" : "text-amber-300"}>
          {snapshot.status.overall === "ok" ? "OK" : "Degradado"}
        </span>
      </p>
      <p className="text-xs text-zinc-400">Último sync: {snapshot.status.lastUpdatedIso}</p>
      <p className="mt-2 text-sm text-zinc-300">
        Pendientes: {snapshot.kpis.saldosPendientes.toFixed(2)} · Reintentos:{" "}
        {snapshot.status.overall === "ok" ? 0 : 1} · Fallidos:{" "}
        {snapshot.status.overall === "ok" ? 0 : 1}
      </p>
      <DiagnosticoCard reportText={report} />
    </section>
  );
}

function DataManagementSection({ role }: { role: string }) {
  const canManage = canManageOpticaSettings(role);
  return (
    <section className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <h2 className="text-xl font-semibold text-[#8AB4F8]">Gestión de datos</h2>
      <p className="mt-2 text-sm text-zinc-300">
        Exporta información operativa y usa acciones sensibles con confirmación doble.
      </p>
      <div className="mt-3 flex flex-wrap gap-2">
        <Link className={btn} href="/api/dashboard/export?type=pendientes">
          Exportar pendientes CSV
        </Link>
        <Link className={btn} href="/api/dashboard/export?type=cierre">
          Exportar cierre CSV
        </Link>
        <Link className={btn} href="/api/dashboard/export?type=inventario-csv">
          Exportar inventario CSV
        </Link>
      </div>
      <p className="mt-3 text-xs text-zinc-400">
        Importación y borrado seguro se habilitan solo para administración avanzada.
      </p>
      {!canManage ? (
        <p className="mt-2 text-xs text-amber-300">
          Tu rol actual no permite acciones destructivas.
        </p>
      ) : null}
    </section>
  );
}

function MessageCard({ message, tone }: { message: string; tone: "warn" | "info" }) {
  return (
    <div className="rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-5">
      <p className={tone === "warn" ? "text-amber-300" : "text-zinc-300"}>{message}</p>
    </div>
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
    state === "error" ? "text-red-300" : state === "warning" ? "text-amber-300" : "text-emerald-300";
  return (
    <div className="rounded-xl border border-zinc-700 bg-black/10 p-3 text-sm">
      <p className="text-zinc-300">{label}</p>
      <p className={`font-semibold ${color}`}>
        {used} / {max ?? "Ilimitado"}
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
  "rounded-full border border-zinc-600 px-4 py-2 text-sm text-zinc-100 hover:bg-white/5";

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
