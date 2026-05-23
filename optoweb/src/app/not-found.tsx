import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen grid place-items-center p-6 bg-background">
      <div className="text-center space-y-4">
        <h1 className="font-heading text-6xl font-black text-muted-foreground/20">404</h1>
        <p className="text-foreground font-medium">La página solicitada no existe.</p>
        <Link href="/dashboard" className="inline-flex items-center gap-2 rounded-2xl bg-primary px-6 py-3 text-sm font-bold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95">
          Volver al inicio
        </Link>
      </div>
    </div>
  );
}
