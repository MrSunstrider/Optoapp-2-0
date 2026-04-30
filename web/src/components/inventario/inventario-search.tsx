"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

export function InventarioSearch({ initial }: { initial: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [value, setValue] = useState(initial);

  function submit(next: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (next.trim()) params.set("q", next.trim());
    else params.delete("q");
    router.replace(`/inventario?${params.toString()}`);
  }

  return (
    <div className="rounded-2xl border border-zinc-600 bg-[#121214] p-3">
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") submit(value);
        }}
        placeholder="Buscar por SKU, marca o modelo"
        className="w-full bg-transparent text-lg text-zinc-100 placeholder:text-zinc-400 focus-visible:outline-none"
        aria-label="Buscar por SKU, marca o modelo"
      />
    </div>
  );
}
