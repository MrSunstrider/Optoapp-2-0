"use client";

import { useMemo } from "react";

import type { PacienteDetalleRow } from "@/lib/pacientes";

export function PacienteClinicalFields({
  defaultValues,
  isCreate,
  isDark,
  inp,
  lbl
}: {
  defaultValues?: Partial<PacienteDetalleRow> | null;
  isCreate: boolean;
  isDark: boolean;
  inp: string;
  lbl: string;
}) {
  const sexoDefault = useMemo(() => {
    if (defaultValues?.sexo?.trim()) return defaultValues.sexo;
    if (isCreate) return "Masculino";
    return "";
  }, [defaultValues?.sexo, isCreate]);

  return (
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

      {/* DNI */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>DNI / Cédula</span>
        <input
          name="dni"
          defaultValue={defaultValues?.dni ?? ""}
          className={inp}
          autoComplete="off"
        />
      </label>

      {/* Fecha de Nacimiento */}
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

      {/* Sexo */}
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

      {/* Dirección */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>Dirección</span>
        <input
          name="direccion"
          defaultValue={defaultValues?.direccion ?? ""}
          className={inp}
        />
      </label>

      {/* Distrito | Ocupación */}
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

      {/* Acompañante */}
      <label className="block">
        <span className={`${lbl} mb-1.5 block`}>Acompañante</span>
        <input
          name="acompanante"
          defaultValue={defaultValues?.acompanante ?? ""}
          className={inp}
        />
      </label>

      {/* Hobbies / hábitos */}
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
  );
}
