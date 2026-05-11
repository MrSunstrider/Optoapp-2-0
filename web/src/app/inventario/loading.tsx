import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function InventarioLoading() {
  return (
    <AppShell>
      <div className="min-h-screen bg-background p-4 sm:p-8 text-foreground transition-colors duration-300">
        <div className="mx-auto w-full max-w-6xl space-y-8">
          <div className="space-y-2">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-12 w-48" />
          </div>
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
          <div className="space-y-3">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        </div>
      </div>
    </AppShell>
  );
}
