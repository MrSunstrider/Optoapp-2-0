/**
 * Shared date formatting utilities.
 * WHY: dateOnly/toDateOnly was duplicated in cierre-caja, dashboard-kpis,
 * reportes-financieros, agenda, and export/route.ts (M5).
 */

/**
 * Today's date as YYYY-MM-DD using local timezone.
 */
export function today(): string {
  return dateOnly(new Date());
}

/**
 * Format a Date as YYYY-MM-DD using the given IANA timezone.
 * If timeZone is omitted or "auto", uses the local system timezone.
 */
export function dateOnly(date: Date, timeZone?: string | null): string {
  if (timeZone && timeZone !== "auto") {
    try {
      const formatter = new Intl.DateTimeFormat("en-CA", {
        timeZone,
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      });
      return formatter.format(date);
    } catch (e) {
      console.warn("[dateOnly] Zona horaria inválida:", timeZone, e);
    }
  }
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}