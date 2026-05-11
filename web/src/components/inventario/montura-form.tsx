"use client";

import { useActionState } from "react";

type SaveAction = (
  prevState: { error?: string } | null,
  formData: FormData
) => Promise<{ error?: string } | null>;

type Field = {
  name: string;
  label: string;
  type?: string;
  min?: number;
  defaultValue?: string;
};

type Props = {
  action: SaveAction;
  fields: Field[];
  submitLabel: string;
  disabled: boolean;
  hiddenInputs?: { name: string; value: string }[];
};

export function MonturaForm({
  action,
  fields,
  submitLabel,
  disabled,
  hiddenInputs,
}: Props) {
  const [state, formAction, isPending] = useActionState(action, null);

  return (
    <form action={formAction} className="space-y-3 rounded-2xl bg-[#4A4856] p-4">
      {state?.error && (
        <p className="rounded-lg border border-red-900/50 bg-red-950/40 px-3 py-2 text-sm text-red-300">
          {state.error}
        </p>
      )}
      {hiddenInputs?.map((h) => (
        <input key={h.name} type="hidden" name={h.name} value={h.value} />
      ))}
      {fields.map((f) => (
        <label key={f.name} className="block">
          <span className="text-sm text-zinc-200">{f.label}</span>
          <input
            name={f.name}
            type={f.type ?? "text"}
            min={f.min}
            defaultValue={f.defaultValue}
            className="mt-1 w-full rounded-xl border border-zinc-600 bg-[#121214] px-3 py-2 text-zinc-100"
          />
        </label>
      ))}
      <button
        type="submit"
        disabled={disabled || isPending}
        className="rounded-full bg-[#8AB4F8] px-5 py-2 text-sm font-semibold text-zinc-900 disabled:opacity-50"
      >
        {isPending ? "Guardando…" : submitLabel}
      </button>
    </form>
  );
}
