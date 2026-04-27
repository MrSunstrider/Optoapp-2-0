import Link from "next/link";

const modules = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/pacientes", label: "Pacientes" },
  { href: "/evaluaciones", label: "Evaluaciones" },
  { href: "/dispensaciones", label: "Dispensaciones" },
  { href: "/servicios", label: "Servicios" },
  { href: "/agenda", label: "Agenda" },
  { href: "/inventario", label: "Inventario" },
  { href: "/configuracion", label: "Configuracion" },
  { href: "/reportes", label: "Reportes" }
];

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen grid grid-cols-[240px_1fr]">
      <aside className="bg-slate-900 text-slate-100 p-4">
        <h2 className="text-lg font-semibold mb-4">OptoApp Web</h2>
        <nav className="flex flex-col gap-2 text-sm">
          {modules.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="rounded px-2 py-1 hover:bg-slate-800"
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <form action="/auth/logout" method="post" className="mt-6">
          <button
            type="submit"
            className="w-full rounded border border-slate-700 px-2 py-1 text-sm hover:bg-slate-800"
          >
            Cerrar sesion
          </button>
        </form>
      </aside>
      <main className="p-6">{children}</main>
    </div>
  );
}
