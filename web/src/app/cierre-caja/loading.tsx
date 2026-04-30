import { AppShell } from "@/components/app-shell";

export default function CierreCajaLoading() {
  return (
    <AppShell>
      <div className="-m-6 min-h-screen animate-pulse bg-[#121214] p-6">
        <div className="mx-auto w-full max-w-4xl space-y-4">
          <div className="h-4 w-2/3 rounded bg-zinc-700/60" />
          <div className="h-10 w-1/2 rounded bg-zinc-700/60" />
          <div className="h-24 rounded-xl bg-zinc-700/60" />
          <div className="h-24 rounded-xl bg-zinc-700/60" />
          <div className="h-24 rounded-xl bg-zinc-700/60" />
        </div>
      </div>
    </AppShell>
  );
}
