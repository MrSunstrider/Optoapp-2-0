import Link from "next/link";

export function ModuleCard({
  href,
  title,
  description,
  blocked
}: {
  href: string;
  title: string;
  description: string;
  blocked?: boolean;
}) {
  return (
    <Link
      href={href}
      className="group block rounded-2xl border border-border bg-card p-6 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md active:scale-95"
      aria-disabled={blocked}
    >
      <p className="font-heading text-lg font-bold text-foreground group-hover:text-primary transition-colors">{title}</p>
      <p className="mt-2 text-sm font-medium text-muted-foreground/80">{description}</p>
      {blocked ? (
        <div className="mt-4 flex items-center gap-2 rounded-lg bg-amber-500/10 px-3 py-1.5 text-[10px] font-bold uppercase tracking-widest text-amber-600 dark:text-amber-400">
          <span>🔒</span> Acceso restringido
        </div>
      ) : null}
    </Link>
  );
}
