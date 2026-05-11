import { AppShell } from "@/components/app-shell";

export default function AgendaLoading() {
  return (
    <AppShell>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-4xl animate-pulse space-y-4">
          <div className="h-4 w-2/3 rounded bg-zinc-700/50" />
          <div className="h-10 w-1/3 rounded bg-zinc-700/50" />
          <div className="h-8 w-1/2 rounded bg-zinc-700/50" />
          <div className="h-24 rounded-xl bg-zinc-700/50" />
          <div className="h-24 rounded-xl bg-zinc-700/50" />
        </div>
      </div>
    </AppShell>
  );
}
