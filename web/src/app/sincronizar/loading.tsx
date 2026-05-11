import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function SincronizarLoading() {
  return (
    <AppShell>
      <div className="mx-auto w-full max-w-6xl space-y-8 py-4">
        <div className="mx-auto w-full max-w-5xl space-y-4">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-3 w-64" />
          <Skeleton className="h-32 w-full" />
          <div className="space-y-2">
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
            <Skeleton className="h-12 w-full" />
          </div>
          <Skeleton className="h-12 w-40" />
        </div>
      </div>
    </AppShell>
  );
}
