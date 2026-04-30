import type { AgendaItem } from "@/lib/agenda";
import { AgendaItemCard } from "@/components/agenda/agenda-item-card";

export function AgendaList({
  items,
  canWrite
}: {
  items: AgendaItem[];
  canWrite: boolean;
}) {
  if (items.length === 0) {
    return (
      <div className="flex min-h-[48vh] items-center justify-center">
        <p className="text-center text-2xl text-zinc-400">
          No hay citas con fecha en este período.
        </p>
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {items.map((item) => (
        <AgendaItemCard key={item.id} item={item} canWrite={canWrite} />
      ))}
    </ul>
  );
}
