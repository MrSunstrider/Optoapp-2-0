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
      className="block rounded-2xl border border-zinc-700/80 bg-[#3F3D4A]/75 p-4 transition-colors hover:bg-[#4a4758]"
      aria-disabled={blocked}
    >
      <p className="text-base font-semibold text-[#8AB4F8]">{title}</p>
      <p className="mt-1 text-sm text-zinc-300">{description}</p>
      {blocked ? (
        <p className="mt-2 text-xs text-amber-300">Acceso restringido por rol.</p>
      ) : null}
    </Link>
  );
}
