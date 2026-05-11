import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function DashboardLoading() {
  return (
    <AppShell>
      <div className="mx-auto w-full max-w-7xl space-y-10 py-6">
        <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end border-b border-border/50 pb-10">
          <div className="space-y-4">
            <Skeleton className="h-3 w-40" />
            <Skeleton className="h-10 w-64" />
          </div>
          <Skeleton className="h-10 w-32" />
        </header>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Skeleton className="h-64 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    </AppShell>
  );
}
