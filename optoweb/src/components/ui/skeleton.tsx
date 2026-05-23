export function Skeleton({ className }: { className?: string }) {
  return <div className={`animate-pulse rounded-2xl bg-foreground/[0.05] ${className ?? ""}`} />;
}
