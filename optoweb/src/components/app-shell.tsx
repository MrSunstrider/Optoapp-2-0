import { AppShellNav } from "./app-shell-nav";
import { LogoOptoArc } from "./nav-icons";

export function AppShell({
  children,
  role,
  opticaName
}: {
  children: React.ReactNode;
  role?: string;
  opticaName?: string;
}) {

  return (
    <div className="grid min-h-screen grid-cols-1 md:grid-cols-[280px_1fr] bg-background text-foreground">
      <aside className="flex min-h-[auto] flex-col border-r border-border bg-card/30 backdrop-blur-2xl text-foreground md:min-h-screen z-20 shadow-xl transition-all duration-300">
        <header className="shrink-0 border-b border-border/50 px-6 pb-6 pt-8">
          <div className="flex flex-col items-center text-center">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 shadow-inner">
              <LogoOptoArc className="h-8 w-8 text-primary" />
            </div>
            <span className="text-2xl font-heading font-bold tracking-tight text-foreground">
              OptoApp
            </span>
            <span className="mt-1 text-[10px] font-bold uppercase tracking-widest text-muted-foreground/80">
              Salud Visual & Preventiva
            </span>
          </div>
          {(opticaName || role) && (
            <div className="mt-6 rounded-xl bg-foreground/5 p-3 text-center text-[11px] leading-snug text-muted-foreground backdrop-blur-sm">
              {opticaName && (
                <p className="truncate">
                  <span className="font-semibold text-foreground/70">Óptica:</span>{" "}
                  {opticaName}
                </p>
              )}
              {role && (
                <p className="mt-1">
                  <span className="font-semibold text-foreground/70">Rol:</span>{" "}
                  {role}
                </p>
              )}
            </div>
          )}
        </header>

        <AppShellNav />
      </aside>

      <main className="relative min-h-screen overflow-y-auto bg-background p-6 lg:p-10">{children}</main>
    </div>
  );
}
