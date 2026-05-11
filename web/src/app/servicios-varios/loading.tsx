import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function ServiciosVariosLoading() {
  return (
    <AppShell>
      <div className="mx-auto w-full max-w-6xl space-y-8 py-4">
        <div className="space-y-1">
          <Skeleton className="h-3 w-32" />
          <Skeleton className="h-8 w-48" />
        </div>
        <div className="space-y-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      </div>
    </AppShell>
  );
}
