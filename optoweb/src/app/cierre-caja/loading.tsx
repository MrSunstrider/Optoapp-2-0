import { AppShell } from "@/components/app-shell";
import { Skeleton } from "@/components/ui/skeleton";

export default function CierreCajaLoading() {
  return (
    <AppShell>
      <div className="min-h-screen bg-background p-4 sm:p-8">
        <div className="mx-auto w-full max-w-4xl space-y-8">
          <div className="space-y-2">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-10 w-2/3" />
          </div>
          <Skeleton className="h-48 w-full" />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        </div>
      </div>
    </AppShell>
  );
}
