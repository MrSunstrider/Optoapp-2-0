import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function AgendaLoading() {
  return (
    <AppShell>
      <div className="-m-6 min-h-screen bg-[#121214] p-6 text-zinc-100">
        <div className="mx-auto w-full max-w-4xl space-y-4">
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-10 w-1/3" />
          <Skeleton className="h-8 w-1/2" />
          <Skeleton className="h-24 w-full rounded-xl" />
          <Skeleton className="h-24 w-full rounded-xl" />
        </div>
      </div>
    </AppShell>
  );
}
