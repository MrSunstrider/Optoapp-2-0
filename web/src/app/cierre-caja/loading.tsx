import { AppShell } from "@/components/app-shell";

export default function CierreCajaLoading() {
  return (
    <AppShell>
      <div className="min-h-screen bg-background p-4 sm:p-8 animate-pulse">
        <div className="mx-auto w-full max-w-4xl space-y-8">
          <div className="space-y-2">
            <div className="h-2 w-24 rounded bg-muted" />
            <div className="h-10 w-2/3 rounded-xl bg-muted" />
          </div>
          <div className="h-48 rounded-3xl bg-muted" />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="h-32 rounded-2xl bg-muted" />
            <div className="h-32 rounded-2xl bg-muted" />
            <div className="h-32 rounded-2xl bg-muted" />
          </div>
        </div>
      </div>
    </AppShell>
  );
}
