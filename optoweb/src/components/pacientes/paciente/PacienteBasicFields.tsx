"use client";

import type { PacienteDetalleRow } from "@/lib/pacientes";

export function PacienteBasicFields({
  defaultValues,
  isDark,
  inp,
  lbl
}: {
  defaultValues?: Partial<PacienteDetalleRow> | null;
  isDark: boolean;
  inp: string;
  lbl: string;
}) {
  return (
    <>
      {/* Nombre */}
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

      {/* Edad | Teléfono */}
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

      {/* Correo Electrónico */}
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
    </>
  );
}
