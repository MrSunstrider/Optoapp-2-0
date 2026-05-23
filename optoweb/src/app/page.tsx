import Link from "next/link";
import { Eye, Download, Smartphone, Cloud, FileText, Package, ChevronRight, ArrowRight } from "lucide-react";
import { createClient } from "@/lib/supabase/server";

async function getLatestRelease() {
  try {
    const res = await fetch(
      "https://api.github.com/repos/MrSunstrider/Optoapp-2-0/releases/latest",
      { next: { revalidate: 3600 } }
    );
    if (!res.ok) return null;
    const data = await res.json();
    return {
      version: data.tag_name ?? "v1.0",
      downloadUrl: data.assets?.find((a: { name: string }) => a.name.endsWith(".apk"))?.browser_download_url ?? null,
    };
  } catch {
    return null;
  }
}

export default async function HomePage() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  const release = await getLatestRelease();

  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* ─── Navbar ─────────────────────────────────── */}
      <nav className="sticky top-0 z-50 border-b border-border/40 bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary">
              <Eye className="h-5 w-5 text-primary-foreground" />
            </div>
            <span className="font-heading text-xl font-black text-foreground">OptoApp</span>
          </div>
          <div className="flex items-center gap-4">
            {user ? (
              <Link
                href="/dashboard"
                className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
              >
                Ir al Dashboard
              </Link>
            ) : (
              <>
                <Link
                  href="/login"
                  className="text-sm font-bold text-muted-foreground transition-colors hover:text-foreground"
                >
                  Iniciar sesión
                </Link>
                <Link
                  href="/login"
                  className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
                >
                  Ingresar
                </Link>
              </>
            )}
          </div>
        </div>
      </nav>

      {/* ─── Hero ───────────────────────────────────── */}
      <section className="relative overflow-hidden border-b border-border/20">
        <div className="absolute inset-0 bg-gradient-to-b from-primary/5 via-transparent to-transparent" />
        <div className="mx-auto max-w-7xl px-6 pb-32 pt-24 text-center relative">
          <div className="mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-3xl bg-primary shadow-2xl shadow-primary/30">
            <Eye className="h-12 w-12 text-primary-foreground" />
          </div>
          <h1 className="font-heading text-5xl font-black tracking-tight sm:text-7xl">
            Gestión clínica{" "}
            <span className="text-primary">optométrica</span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg font-medium text-muted-foreground">
            Software completo para tu óptica: evaluaciones, refracción, inventario y más.
            App Android + Web sincronizadas en la nube.
          </p>
          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            {release?.downloadUrl ? (
              <Link
                href={release.downloadUrl}
                className="flex items-center gap-2 rounded-xl bg-primary px-8 py-4 text-base font-bold text-primary-foreground shadow-2xl shadow-primary/30 transition-all hover:scale-[1.02] active:scale-95"
              >
                <Download className="h-5 w-5" />
                Descargar APK {release.version}
              </Link>
            ) : null}
            {!user && (
              <Link
                href="/login"
                className="flex items-center gap-2 rounded-xl border border-border bg-card px-8 py-4 text-base font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95"
              >
                Ingresar al sistema
                <ArrowRight className="h-5 w-5" />
              </Link>
            )}
          </div>
          {release && (
            <p className="mt-4 text-xs font-medium text-muted-foreground/60">
              Versión {release.version} · Android 8+ · APK directo
            </p>
          )}
        </div>
      </section>

      {/* ─── Features ──────────────────────────────── */}
      <section className="border-b border-border/20 py-24">
        <div className="mx-auto max-w-7xl px-6">
          <h2 className="text-center font-heading text-3xl font-black tracking-tight sm:text-4xl">
            Todo lo que necesitás en un solo lugar
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-center font-medium text-muted-foreground">
            Desde la evaluación clínica hasta la gestión del inventario.
          </p>
          <div className="mt-16 grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3">
            <FeatureCard
              icon={<Eye className="h-6 w-6" />}
              title="Evaluaciones completas"
              desc="AV SC/CC, refracción objetiva y subjetiva, contactología, diagnóstico automático, y más."
            />
            <FeatureCard
              icon={<FileText className="h-6 w-6" />}
              title="PDF profesional"
              desc="Generá la fórmula optométrica con tabla de prescripción, prismas y diagnóstico en un PDF listo para entregar."
            />
            <FeatureCard
              icon={<Package className="h-6 w-6" />}
              title="Inventario + Servicios"
              desc="Control de stock, monturas, servicios varios con pagos parciales, dispensación y cierre de caja."
            />
            <FeatureCard
              icon={<Smartphone className="h-6 w-6" />}
              title="App Android nativa"
              desc="OptoApp Android con soporte offline y sincronización cloud. Usala desde tu tablet o celular."
            />
            <FeatureCard
              icon={<Cloud className="h-6 w-6" />}
              title="Cloud Sync"
              desc="Datos sincronizados en tiempo real con Supabase. Accedé desde cualquier dispositivo."
            />
            <FeatureCard
              icon={<Download className="h-6 w-6" />}
              title="Actualizaciones automáticas"
              desc="La app Android te avisa cuando hay una versión nueva. Descargá e instalá con un toque."
            />
          </div>
        </div>
      </section>

      {/* ─── CTA final ─────────────────────────────── */}
      <section className="py-24">
        <div className="mx-auto max-w-3xl px-6 text-center">
          <div className="rounded-3xl border border-border bg-card p-12 shadow-sm">
            <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary">
              <Eye className="h-8 w-8 text-primary-foreground" />
            </div>
            <h2 className="font-heading text-3xl font-black tracking-tight">
              Empezá hoy
            </h2>
            <p className="mt-4 text-lg font-medium text-muted-foreground">
              Descargá la app, creá tu óptica y empezá a gestionar tus pacientes.
              Plan gratuito incluido.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
              {release?.downloadUrl ? (
                <Link
                  href={release.downloadUrl}
                  className="flex items-center gap-2 rounded-xl bg-primary px-8 py-4 text-base font-bold text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
                >
                  <Download className="h-5 w-5" />
                  Descargar APK {release.version}
                </Link>
              ) : null}
              {!user && (
                <Link
                  href="/login"
                  className="flex items-center gap-2 rounded-xl border border-border bg-card px-8 py-4 text-base font-bold text-foreground transition-all hover:bg-foreground/5 active:scale-95"
                >
                  Ingresar
                  <ChevronRight className="h-5 w-5" />
                </Link>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* ─── Footer ────────────────────────────────── */}
      <footer className="border-t border-border/20 py-12">
        <div className="mx-auto max-w-7xl px-6 text-center">
          <div className="flex items-center justify-center gap-2">
            <Eye className="h-4 w-4 text-primary" />
            <span className="font-heading text-sm font-black text-foreground">OptoApp</span>
          </div>
          <p className="mt-3 text-xs font-medium text-muted-foreground/60">
            Clinical Software 2026 · v{release?.version?.replace("v", "") ?? "1.0"}
          </p>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({ icon, title, desc }: { icon: React.ReactNode; title: string; desc: string }) {
  return (
    <div className="group rounded-2xl border border-border bg-card p-6 shadow-sm transition-all hover:shadow-xl hover:border-primary/20">
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
        {icon}
      </div>
      <h3 className="font-heading text-lg font-bold text-foreground">{title}</h3>
      <p className="mt-2 text-sm font-medium leading-relaxed text-muted-foreground">{desc}</p>
    </div>
  );
}
