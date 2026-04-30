import Link from "next/link";

export function ConfigSectionShell({
  title,
  description,
  children
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-white">{title}</h1>
          {description ? <p className="text-sm text-zinc-400">{description}</p> : null}
        </div>
        <Link
          href="/configuracion"
          className="rounded-lg border border-zinc-600 px-3 py-1.5 text-sm text-zinc-200 hover:bg-white/5"
        >
          Volver
        </Link>
      </div>
      {children}
    </div>
  );
}
