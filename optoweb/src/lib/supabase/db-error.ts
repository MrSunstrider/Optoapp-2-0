/**
 * Errores devueltos por PostgREST (consultas `.from()`).
 * Si hay error, se lanza con contexto para que llegue al error boundary o se muestre en UI.
 */
export function assertNoDbError(
  error: { message: string; code?: string } | null,
  contexto: string
): asserts error is null {
  if (error) {
    const code = error.code ? ` [${error.code}]` : "";
    throw new Error(`${contexto}: ${error.message}${code}`);
  }
}
