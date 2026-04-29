"use client";

import { FormEvent, useState } from "react";
import { createClient } from "@/lib/supabase/client";
import { useRouter } from "next/navigation";

export function LoginForm({
  configuracionIncompleta
}: {
  configuracionIncompleta: boolean;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const supabase = createClient();
      const { error: signInError } = await supabase.auth.signInWithPassword({
        email,
        password
      });

      if (signInError) {
        setError(signInError.message);
        return;
      }

      router.replace("/dashboard");
      router.refresh();
    } catch (err) {
      const msg =
        err instanceof Error ? err.message : "Error inesperado al iniciar sesión.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-sm"
      >
        <h1 className="mb-1 text-xl font-semibold tracking-tight">
          Ingresar a OptoApp Web
        </h1>
        <p className="mb-6 text-sm text-muted-foreground">
          Usa tu correo y contraseña de Supabase Auth.
        </p>
        {configuracionIncompleta && (
          <p className="mb-4 rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            Configuración incompleta: no están definidas las variables de entorno de
            Supabase en <code className="text-xs">web/.env.local</code>. Sin ellas la
            aplicación no puede autenticar.
          </p>
        )}
        <label className="mb-4 block text-sm font-medium">
          Correo
          <input
            className="mt-1.5 w-full rounded-lg border border-input bg-background px-3 py-2 text-foreground outline-none ring-offset-background transition-shadow focus-visible:ring-2 focus-visible:ring-ring"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            disabled={configuracionIncompleta}
          />
        </label>
        <label className="mb-4 block text-sm font-medium">
          Contrasena
          <input
            className="mt-1.5 w-full rounded-lg border border-input bg-background px-3 py-2 text-foreground outline-none ring-offset-background transition-shadow focus-visible:ring-2 focus-visible:ring-ring"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            disabled={configuracionIncompleta}
          />
        </label>
        {error && (
          <p className="mb-3 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}
        <button
          disabled={loading || configuracionIncompleta}
          className="w-full rounded-lg bg-primary py-2.5 text-sm font-medium text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 disabled:opacity-50"
          type="submit"
        >
          {loading ? "Ingresando..." : "Ingresar"}
        </button>
      </form>
    </div>
  );
}
