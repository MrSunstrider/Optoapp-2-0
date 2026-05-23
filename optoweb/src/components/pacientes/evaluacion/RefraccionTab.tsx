"use client";

import type { EvaluacionDraft, DraftUpdateFn } from "@/lib/evaluacion-types";
import { BASES_PRISMA } from "@/lib/evaluacion-constants";

type Props = {
  draft: EvaluacionDraft;
  update: DraftUpdateFn;
  inputCls: string;
};

function normalizeRx(
  esf: string,
  cil: string,
  eje: string
): {
  esf: string;
  cil: string;
  eje: string;
} {
  const cleanEsf = esf.trim().replace(",", ".");
  const cleanCil = cil.trim().replace(",", ".");
  const cleanEje = eje.trim();
  const esfN = Number(cleanEsf);
  const cilN = Number(cleanCil);
  const ejeN = Number(cleanEje);
  if (!Number.isFinite(esfN) || !Number.isFinite(cilN) || !Number.isFinite(ejeN)) {
    return { esf: esf.trim(), cil: cil.trim(), eje: eje.trim() };
  }
  let nextEsf = esfN;
  let nextCil = cilN;
  let nextEje = ejeN;
  if (nextCil > 0) {
    nextEsf = nextEsf + nextCil;
    nextCil = -nextCil;
    nextEje = nextEje + 90;
  }
  while (nextEje > 180) nextEje -= 180;
  while (nextEje <= 0) nextEje += 180;
  return {
    esf: nextEsf.toFixed(2).replace(/\.00$/, ""),
    cil: nextCil.toFixed(2).replace(/\.00$/, ""),
    eje: String(Math.round(nextEje))
  };
}

export function RefraccionTab({ draft, update, inputCls }: Props) {
  const cardCls = "space-y-4 rounded-2xl border border-border bg-foreground/[0.02] p-5 shadow-sm";
  const subtitle = "text-xs font-black uppercase tracking-widest text-primary/80";
  const addAo = draft.isAddAo === "true";

  function normalizeEye(side: "od" | "oi") {
    if (side === "od") {
      const n = normalizeRx(draft.recetaOdEsf, draft.recetaOdCil, draft.recetaOdEje);
      update("recetaOdEsf", n.esf);
      update("recetaOdCil", n.cil);
      update("recetaOdEje", n.eje);
      return;
    }
    const n = normalizeRx(draft.recetaOiEsf, draft.recetaOiCil, draft.recetaOiEje);
    update("recetaOiEsf", n.esf);
    update("recetaOiCil", n.cil);
    update("recetaOiEje", n.eje);
  }

  return (
    <div className="space-y-4">
      <div className={cardCls}>
        <p className={subtitle}>Refraccion Objetiva</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OD Esf
            <input
              value={draft.objOdEsf}
              onChange={(e) => update("objOdEsf", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OD Cil
            <input
              value={draft.objOdCil}
              onChange={(e) => update("objOdCil", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OD Eje
            <input
              value={draft.objOdEje}
              onChange={(e) => update("objOdEje", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OI Esf
            <input
              value={draft.objOiEsf}
              onChange={(e) => update("objOiEsf", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI Cil
            <input
              value={draft.objOiCil}
              onChange={(e) => update("objOiCil", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI Eje
            <input
              value={draft.objOiEje}
              onChange={(e) => update("objOiEje", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Refraccion Subjetiva</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OD Esf
            <input
              value={draft.subjOdEsf}
              onChange={(e) => update("subjOdEsf", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OD Cil
            <input
              value={draft.subjOdCil}
              onChange={(e) => update("subjOdCil", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OD Eje
            <input
              value={draft.subjOdEje}
              onChange={(e) => update("subjOdEje", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OI Esf
            <input
              value={draft.subjOiEsf}
              onChange={(e) => update("subjOiEsf", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI Cil
            <input
              value={draft.subjOiCil}
              onChange={(e) => update("subjOiCil", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI Eje
            <input
              value={draft.subjOiEje}
              onChange={(e) => update("subjOiEje", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>VL Formula Optometrica</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OD Esf
            <input
              value={draft.recetaOdEsf}
              onChange={(e) => update("recetaOdEsf", e.target.value)}
              onBlur={() => normalizeEye("od")}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OD Cil
            <input
              value={draft.recetaOdCil}
              onChange={(e) => update("recetaOdCil", e.target.value)}
              onBlur={() => normalizeEye("od")}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OD Eje
            <input
              value={draft.recetaOdEje}
              onChange={(e) => update("recetaOdEje", e.target.value)}
              onBlur={() => normalizeEye("od")}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            OI Esf
            <input
              value={draft.recetaOiEsf}
              onChange={(e) => update("recetaOiEsf", e.target.value)}
              onBlur={() => normalizeEye("oi")}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI Cil
            <input
              value={draft.recetaOiCil}
              onChange={(e) => update("recetaOiCil", e.target.value)}
              onBlur={() => normalizeEye("oi")}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            OI Eje
            <input
              value={draft.recetaOiEje}
              onChange={(e) => update("recetaOiEje", e.target.value)}
              onBlur={() => normalizeEye("oi")}
              className={inputCls}
            />
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            AV OD
            <input
              value={draft.recetaOdAv}
              onChange={(e) => update("recetaOdAv", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            AV OI
            <input
              value={draft.recetaOiAv}
              onChange={(e) => update("recetaOiAv", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            AV AO
            <input
              value={draft.avCcAoPx}
              onChange={(e) => update("avCcAoPx", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Adicion (ADD)</p>
        <p className="text-xs font-semibold text-zinc-400">VP Cerca/Interm</p>
        <label className="flex items-center gap-2 text-sm text-zinc-300">
          <input
            type="checkbox"
            checked={addAo}
            onChange={(e) => update("isAddAo", e.target.checked ? "true" : "false")}
          />
          A/O
        </label>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <label className="text-sm text-zinc-300">
            Add OD
            <input
              value={draft.addCercaOd}
              onChange={(e) => update("addCercaOd", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Add OI
            <input
              value={draft.addCercaOi}
              onChange={(e) => update("addCercaOi", e.target.value)}
              disabled={addAo}
              className={inputCls + (addAo ? " opacity-50" : "")}
            />
          </label>
          <label className="text-sm text-zinc-300">
            AV VP
            <input
              value={draft.addAv}
              onChange={(e) => update("addAv", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>DIP</p>
        <p className="text-xs text-zinc-500">
          Puedes ingresar DIP total o DNP OD/OI segun disponibilidad.
        </p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            DIP Lejos
            <input
              value={draft.dipLejos}
              onChange={(e) => update("dipLejos", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            DIP Cerca
            <input
              value={draft.dipCerca}
              onChange={(e) => update("dipCerca", e.target.value)}
              className={inputCls}
            />
          </label>
        </div>
      </div>

      <div className={cardCls}>
        <p className={subtitle}>Prismas</p>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            Prisma OD (valor)
            <input
              value={draft.prismaOdValor}
              onChange={(e) => update("prismaOdValor", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Base OD
            <select
              value={draft.prismaOdBase}
              onChange={(e) => update("prismaOdBase", e.target.value)}
              className={inputCls}
            >
              <option value="">Seleccionar...</option>
              {BASES_PRISMA.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <label className="text-sm text-zinc-300">
            Prisma OI (valor)
            <input
              value={draft.prismaOiValor}
              onChange={(e) => update("prismaOiValor", e.target.value)}
              className={inputCls}
            />
          </label>
          <label className="text-sm text-zinc-300">
            Base OI
            <select
              value={draft.prismaOiBase}
              onChange={(e) => update("prismaOiBase", e.target.value)}
              className={inputCls}
            >
              <option value="">Seleccionar...</option>
              {BASES_PRISMA.map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>
    </div>
  );
}
