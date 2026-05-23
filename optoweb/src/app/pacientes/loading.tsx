import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function PacientesLoading() {
  return (
    <AppShell>
      <div className="mx-auto w-full max-w-5xl space-y-6 py-4">
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <Skeleton className="h-3 w-32" />
            <Skeleton className="h-10 w-40" />
          </div>
          <Skeleton className="h-12 w-12 rounded-2xl" />
        </div>
        <Skeleton className="h-12 w-full" />
        <div className="space-y-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      </div>
    </AppShell>
  );
}
