"use client";

import { useRouter, useSearchParams } from "next/navigation";

export function DateFilter({ value }: { value: string }) {
  const router = useRouter();
  const params = useSearchParams();

  function onChange(next: string) {
    const q = new URLSearchParams(params.toString());
    if (next) q.set("fecha", next);
    else q.delete("fecha");
    router.replace(`/cierre-caja?${q.toString()}`);
  }

  return (
    <label className="inline-flex items-center gap-2 rounded-lg border border-zinc-600 px-2 py-1 text-sm text-zinc-100">
      <span aria-hidden>📅</span>
      <input
        aria-label="Seleccionar fecha de corte"
        type="date"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="bg-transparent text-zinc-100 focus-visible:outline-none"
      />
    </label>
  );
}
