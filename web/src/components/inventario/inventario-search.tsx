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
    <div className="group relative rounded-2xl border border-border bg-card p-1 shadow-sm transition-all focus-within:border-primary focus-within:ring-4 focus-within:ring-primary/10">
      <div className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground/50 transition-colors group-focus-within:text-primary">
        🔍
      </div>
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") submit(value);
        }}
        placeholder="Buscar por SKU, marca o modelo..."
        className="w-full bg-transparent py-3 pl-11 pr-4 text-base font-medium text-foreground placeholder:text-muted-foreground/40 focus:outline-none"
        aria-label="Buscar por SKU, marca o modelo"
      />
    </div>
  );
}
