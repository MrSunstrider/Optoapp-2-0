import Link from "next/link";
import type { AgendaPeriodo } from "@/lib/agenda";

export function AgendaPeriodTabs({ active }: { active: AgendaPeriodo }) {
  const items: Array<{ key: AgendaPeriodo; label: string }> = [
    { key: "hoy", label: "Hoy" },
    { key: "semana", label: "Semana" },
    { key: "mes", label: "Mes" }
  ];

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => {
        const selected = item.key === active;
        return (
          <Link
            key={item.key}
            href={`/agenda?periodo=${item.key}`}
            className={
              selected
                ? "rounded-xl bg-[#8AB4F8] px-5 py-2 text-sm font-medium text-zinc-900"
                : "rounded-xl border border-zinc-500 px-5 py-2 text-sm font-medium text-zinc-100 hover:bg-white/5"
            }
            aria-current={selected ? "page" : undefined}
          >
            {item.label}
          </Link>
        );
      })}
    </div>
  );
}
