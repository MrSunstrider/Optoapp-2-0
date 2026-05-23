"use client";

import Link from "next/link";
import { useActionState, useMemo, useRef, useState, useTransition } from "react";

import type { PacienteDetalleRow } from "@/lib/pacientes";
import { localTodayDateOnly } from "@/lib/pacientes";

import { PacienteBasicFields } from "@/components/pacientes/paciente/PacienteBasicFields";
import { PacienteClinicalFields } from "@/components/pacientes/paciente/PacienteClinicalFields";
import { suggestHistoriaOptometricaAction } from "@/app/pacientes/_actions/paciente-crud";

type ActionFn = (
  prevState: { error?: string } | null,
  formData: FormData
) => Promise<{ error?: string } | null>;

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

  const [formState, formAction, formPending] = useActionState(action, null);

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
    startTransition(() => {
      suggestHistoriaOptometricaAction().then((r) => {
        if (r.ok && r.value) setHistoria(r.value);
        else setSuggestErr(r.error ?? "No se pudo sugerir HO");
      });
    });
  }

  const shell = "max-w-2xl space-y-4";

  const inp = isDark
    ? "w-full rounded-xl border border-border bg-foreground/[0.03] px-4 py-3 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
    : "w-full rounded-md border border-input bg-background px-3 py-2 text-sm";

  const lbl = isDark ? "text-[10px] font-bold uppercase tracking-widest text-muted-foreground/60" : "text-sm font-medium text-foreground";

  const btnPrimary = isDark
    ? "rounded-xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
    : "rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground";

  const btnGhost = isDark
    ? "rounded-xl border border-border bg-card px-6 py-3 text-sm font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95"
    : "rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-accent";

  const suggestLink = isDark
    ? "shrink-0 rounded-xl border border-border bg-card px-4 py-3 text-sm font-bold text-primary transition-all hover:bg-foreground/5 disabled:opacity-50"
    : "shrink-0 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent";

  return (
    <form action={formAction} className={shell}>
      {pacienteId && <input type="hidden" name="id" value={pacienteId} />}
      <input type="hidden" name="fechaCreacion" value={fechaCreacionIso} />
      {errorBanner}
      {formState?.error && (
        <p className="rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {formState.error}
        </p>
      )}

      {/* Fecha de registro */}
      <div>
        <span className={`${lbl} mb-2 block`}>Fecha de registro</span>
        <button
          type="button"
          onClick={openFechaPicker}
          className={
            isDark
              ? "flex w-full items-center justify-center rounded-2xl border border-border bg-foreground/[0.03] py-4 text-center text-sm font-bold text-primary transition-all hover:bg-foreground/[0.05]"
              : "flex w-full items-center justify-center rounded-full border border-input bg-background py-2.5 text-sm text-primary"
          }
        >
          📅 {formatFechaRegistroLabel(fechaCreacionIso)}
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

      {/* HO + Sugerir */}
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

      <PacienteBasicFields defaultValues={defaultValues} isDark={isDark} inp={inp} lbl={lbl} />

      <PacienteClinicalFields
        defaultValues={defaultValues}
        isCreate={isCreate}
        isDark={isDark}
        inp={inp}
        lbl={lbl}
      />

      <div className="flex flex-wrap gap-3 pt-4">
        <button type="submit" disabled={formPending} className={btnPrimary + (formPending ? " opacity-50" : "")}>
          {formPending ? "Guardando…" : submitLabel}
        </button>
        <Link href={cancelHref} className={btnGhost + " inline-flex items-center justify-center"}>
          Cancelar
        </Link>
      </div>
    </form>
  );
}
