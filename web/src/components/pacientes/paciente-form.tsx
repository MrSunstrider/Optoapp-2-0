"use client";

import Link from "next/link";
import { useMemo, useRef, useState, useTransition } from "react";

import type { PacienteDetalleRow } from "@/lib/pacientes";
import { localTodayDateOnly } from "@/lib/pacientes";

import { suggestHistoriaOptometricaAction } from "@/app/pacientes/_actions/paciente-crud";

type ActionFn = (formData: FormData) => void | Promise<void>;

function formatFechaRegistroLabel(iso: string): string {
  if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return "—";
  const d = new Date(iso + "T12:00:00");
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("es-PE", {
    day: "numeric",
    month: "short",
    year: "numeric"
  });
}

export function PacienteForm({
  action,
  defaultValues,
  submitLabel,
  pacienteId,
  cancelHref,
  errorBanner,
  variant = "default",
  mode = "edit"
}: {
  action: ActionFn;
  defaultValues?: Partial<PacienteDetalleRow> | null;
  submitLabel: string;
  pacienteId?: string;
  cancelHref: string;
  errorBanner?: React.ReactNode;
  /** Paridad visual con la app Android (Material oscuro). */
  variant?: "default" | "materialDark";
  mode?: "create" | "edit";
}) {
  const isDark = variant === "materialDark";
  const isCreate = mode === "create";

  const [historia, setHistoria] = useState(
    () => defaultValues?.historia_optometrica ?? ""
  );
  const [pending, startTransition] = useTransition();
  const [suggestErr, setSuggestErr] = useState<string | null>(null);

  const fechaDefault = useMemo(
    () => defaultValues?.fecha_creacion ?? localTodayDateOnly(),
    [defaultValues?.fecha_creacion]
  );

  const [fechaCreacionIso, setFechaCreacionIso] = useState(fechaDefault);
  const fechaPickerRef = useRef<HTMLInputElement>(null);

  function openFechaPicker() {
    const el = fechaPickerRef.current;
    if (!el) return;
    if (typeof el.showPicker === "function") {
      el.showPicker();
    } else {
      el.click();
    }
  }

  function onSugerirHo() {
    setSuggestErr(null);
    startTransition(async () => {
      const r = await suggestHistoriaOptometricaAction();
      if (r.ok && r.value) setHistoria(r.value);
      else setSuggestErr(r.error ?? "No se pudo sugerir HO");
    });
  }

  const sexoDefault = useMemo(() => {
    if (defaultValues?.sexo?.trim()) return defaultValues.sexo;
    if (isCreate) return "Masculino";
    return "";
  }, [defaultValues?.sexo, isCreate]);

  const shell = "max-w-2xl space-y-4";

  const inp = isDark
    ? "w-full rounded-lg border border-zinc-600 bg-[#0a0a0a] px-3 py-2.5 text-sm text-zinc-100 placeholder:text-zinc-600 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
    : "w-full rounded-md border border-input bg-background px-3 py-2 text-sm";

  const lbl = isDark ? "text-xs font-medium text-zinc-400" : "text-sm font-medium text-foreground";

  const btnPrimary = isDark
    ? "rounded-lg bg-violet-600 px-5 py-2.5 text-sm font-medium text-white shadow-md hover:bg-violet-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-violet-400"
    : "rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground";

  const btnGhost = isDark
    ? "rounded-lg border border-zinc-600 bg-transparent px-5 py-2.5 text-sm font-medium text-zinc-200 hover:bg-zinc-900"
    : "rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-accent";

  const suggestLink = isDark
    ? "shrink-0 rounded-lg px-3 py-2.5 text-sm font-medium text-sky-400 hover:bg-zinc-900 hover:text-sky-300 disabled:opacity-50"
    : "shrink-0 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent";

  return (
    <form action={action} className={shell}>
      {pacienteId && <input type="hidden" name="id" value={pacienteId} />}
      <input type="hidden" name="fechaCreacion" value={fechaCreacionIso} />
      {errorBanner}

      {/* 1. Fecha de registro — control tipo renglón (paridad app) */}
      <div className={isDark ? "" : ""}>
        <span className={`${lbl} mb-1.5 block`}>Fecha de registro</span>
        <button
          type="button"
          onClick={openFechaPicker}
          className={
            isDark
              ? "flex w-full items-center justify-center rounded-full border border-zinc-600 bg-[#0c0c0c] py-3 text-center text-sm text-sky-400 transition-colors hover:border-zinc-500"
              : "flex w-full items-center justify-center rounded-full border border-input bg-background py-2.5 text-sm text-primary"
          }
        >
          Fecha de Registro: {formatFechaRegistroLabel(fechaCreacionIso)}
        </button>
        <input
          ref={fechaPickerRef}
          type="date"
          value={fechaCreacionIso}
          onChange={(e) => setFechaCreacionIso(e.target.value)}
          className="sr-only"
          tabIndex={-1}
          aria-hidden
        />
      </div>

      {/* 2. HO + Sugerir */}
      <div className="flex flex-wrap items-end gap-2">
        <div className="min-w-0 flex-1">
          <label className="block">
            <span className={`${lbl} mb-1.5 block`}>N° Historia Optométrica</span>
            <input
              name="historiaOptometrica"
              value={historia}
              onChange={(e) => setHistoria(e.target.value)}
              className={inp}
              placeholder="Opcional"
              autoComplete="off"
            />
          </label>
        </div>
        <button
          type="button"
          onClick={onSugerirHo}
          disabled={pending}
          className={suggestLink}
        >
          {pending ? "…" : "Sugerir HO"}
        </button>
      </div>
      {suggestErr && (
        <p
          className={
            isDark ? "text-sm text-amber-400" : "text-sm text-amber-700 dark:text-amber-300"
          }
        >
          {suggestErr}
        </p>
      )}

      {/* 3. Nombre */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>Nombre Completo *</span>
        <input
          name="nombreCompleto"
          required
          defaultValue={defaultValues?.nombre_completo ?? ""}
          className={inp}
          autoComplete="name"
        />
      </label>

      {/* 4. Edad | Teléfono */}
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className={`${lbl} mb-1.5 block`}>Edad *</span>
          <input
            type="number"
            name="edad"
            min={1}
            required
            defaultValue={defaultValues?.edad ?? ""}
            className={inp}
            inputMode="numeric"
          />
        </label>
        <label className="block">
          <span className={`${lbl} mb-1.5 block`}>Teléfono *</span>
          <input
            name="telefono"
            required
            defaultValue={defaultValues?.telefono ?? ""}
            className={inp}
            inputMode="tel"
            autoComplete="tel"
          />
        </label>
      </div>

      {/* 5. DNI */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>DNI / Cédula</span>
        <input
          name="dni"
          defaultValue={defaultValues?.dni ?? ""}
          className={inp}
          autoComplete="off"
        />
      </label>

      {/* 6. Fecha nacimiento */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>Fecha de Nacimiento</span>
        <input
          type="date"
          name="fechaNacimiento"
          defaultValue={defaultValues?.fecha_nacimiento ?? ""}
          className={inp}
        />
        <span className={isDark ? "mt-1 block text-[11px] text-zinc-500" : "mt-1 block text-[11px] text-muted-foreground"}>
          Opcional. Formato según navegador (equivalente a dd/mm/aaaa en la app).
        </span>
      </label>

      {/* 7. Sexo */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>Sexo</span>
        <select
          name="sexo"
          defaultValue={sexoDefault}
          className={inp + (isDark ? " appearance-auto bg-[#0a0a0a]" : "")}
        >
          {!isCreate && <option value="">—</option>}
          <option value="Masculino">Masculino</option>
          <option value="Femenino">Femenino</option>
        </select>
      </label>

      {/* 8. Correo */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>Correo Electrónico</span>
        <input
          type="email"
          name="email"
          defaultValue={defaultValues?.email ?? ""}
          className={inp}
          autoComplete="email"
        />
      </label>

      {/* Resto del flujo (como en app completa) */}
      <div
        className={
          isDark
            ? "space-y-4 border-t border-zinc-800 pt-4"
            : "space-y-4 border-t border-border pt-4"
        }
      >
        <p className={isDark ? "text-xs text-zinc-500" : "text-xs text-muted-foreground"}>
          Más datos del expediente
        </p>
        <label className="block">
          <span className={`${lbl} mb-1.5 block`}>Dirección</span>
          <input
            name="direccion"
            defaultValue={defaultValues?.direccion ?? ""}
            className={inp}
          />
        </label>

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block">
            <span className={`${lbl} mb-1.5 block`}>Distrito</span>
            <input
              name="distrito"
              defaultValue={defaultValues?.distrito ?? ""}
              className={inp}
            />
          </label>
          <label className="block">
            <span className={`${lbl} mb-1.5 block`}>Ocupación</span>
            <input
              name="ocupacion"
              defaultValue={defaultValues?.ocupacion ?? ""}
              className={inp}
            />
          </label>
        </div>

        <label className="block">
          <span className={`${lbl} mb-1.5 block`}>Acompañante</span>
          <input
            name="acompanante"
            defaultValue={defaultValues?.acompanante ?? ""}
            className={inp}
          />
        </label>

        <label className="block">
          <span className={`${lbl} mb-1.5 block`}>Hobbies / hábitos</span>
          <textarea
            name="hobbies"
            rows={3}
            defaultValue={defaultValues?.hobbies ?? ""}
            className={inp + " min-h-[80px] resize-y"}
          />
        </label>
      </div>

      <div className="flex flex-wrap gap-3 pt-4">
        <button type="submit" className={btnPrimary}>
          {submitLabel}
        </button>
        <Link href={cancelHref} className={btnGhost + " inline-flex items-center justify-center"}>
          Cancelar
        </Link>
      </div>
    </form>
  );
}
