"use client";

import { Eye, EyeOff, Lock, Mail } from "lucide-react";
import Link from "next/link";
import { FormEvent, useMemo, useState } from "react";
import { createClient } from "@/lib/supabase/client";

function isValidEmail(value: string): boolean {
  const v = value.trim();
  if (!v) return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}

export function LoginForm({
  configuracionIncompleta,
  oauthError
}: {
  configuracionIncompleta: boolean;
  oauthError: boolean;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [oauthLoading, setOauthLoading] = useState(false);

  const signupUrl = process.env.NEXT_PUBLIC_SIGNUP_URL?.trim();

  const canSubmit = useMemo(() => {
    if (configuracionIncompleta) return false;
    return isValidEmail(email) && password.length > 0;
  }, [email, password, configuracionIncompleta]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;
    setLoading(true);
    setError(null);

    try {
      const supabase = createClient();
      const { error: signInError } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password
      });

      if (signInError) {
        setError(translateAuthError(signInError.message));
        return;
      }

      window.location.assign("/dashboard");
    } catch (err) {
      const msg =
        err instanceof Error ? err.message : "Error inesperado al iniciar sesión.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  async function onGoogle() {
    if (configuracionIncompleta) return;
    setOauthLoading(true);
    setError(null);
    try {
      const supabase = createClient();
      const origin = window.location.origin;
      const { error: oauthErr } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: {
          redirectTo: `${origin}/auth/callback`
        }
      });
      if (oauthErr) setError(oauthErr.message);
    } catch (err) {
      const msg =
        err instanceof Error ? err.message : "No se pudo iniciar sesión con Google.";
      setError(msg);
    } finally {
      setOauthLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-optoapp-bg px-4 py-10 font-inter">
      <div className="flex w-full max-w-[400px] flex-col items-center gap-8">
        <header className="space-y-1 text-center">
          <h1 className="text-4xl font-bold text-optoapp-brand">OptoApp</h1>
          <p className="text-[15px] text-zinc-400">Acceso a tu cuenta</p>
        </header>

        <div className="w-full rounded-2xl bg-optoapp-card p-6 shadow-xl">
          <form className="flex flex-col gap-4" onSubmit={onSubmit} noValidate>
            {configuracionIncompleta && (
              <p className="rounded-xl border border-red-500/40 bg-red-950/40 p-3 text-sm text-red-200">
                Configuración incompleta: defina las variables de Supabase en{" "}
                <code className="text-xs">web/.env.local</code>.
              </p>
            )}
            {oauthError && (
              <p className="rounded-xl border border-amber-500/40 bg-amber-950/40 p-3 text-sm text-amber-100">
                No se pudo completar el acceso con Google. Intente de nuevo.
              </p>
            )}

            <div className="space-y-2">
              <label
                htmlFor="login-email"
                className="sr-only"
              >
                Correo electrónico
              </label>
              <div className="relative">
                <Mail
                  className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-zinc-500"
                  aria-hidden
                />
                <input
                  id="login-email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  inputMode="email"
                  placeholder="Correo electrónico"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={configuracionIncompleta}
                  className="w-full rounded-xl border border-zinc-600 bg-zinc-900/80 py-3 pl-11 pr-3 text-[15px] text-white placeholder:text-zinc-500 focus-visible:border-optoapp-brand focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-optoapp-brand disabled:opacity-50"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="login-password" className="sr-only">
                Contraseña
              </label>
              <div className="relative">
                <Lock
                  className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-zinc-500"
                  aria-hidden
                />
                <input
                  id="login-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="Contraseña"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={configuracionIncompleta}
                  className="w-full rounded-xl border border-zinc-600 bg-zinc-900/80 py-3 pl-11 pr-12 text-[15px] text-white placeholder:text-zinc-500 focus-visible:border-optoapp-brand focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-optoapp-brand disabled:opacity-50"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-zinc-400 hover:bg-white/5 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-optoapp-brand"
                  aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                  disabled={configuracionIncompleta}
                >
                  {showPassword ? (
                    <EyeOff className="h-[18px] w-[18px]" aria-hidden />
                  ) : (
                    <Eye className="h-[18px] w-[18px]" aria-hidden />
                  )}
                </button>
              </div>
            </div>

            {error && (
              <p className="text-sm text-red-400" role="alert" aria-live="assertive">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={!canSubmit || loading || configuracionIncompleta}
              className={
                !canSubmit || configuracionIncompleta
                  ? "w-full rounded-xl bg-zinc-600 py-3 text-[15px] font-medium text-zinc-400"
                  : "w-full rounded-xl bg-optoapp-brand py-3 text-[15px] font-semibold text-zinc-900 hover:brightness-105 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-optoapp-brand disabled:opacity-70"
              }
            >
              {loading ? "Ingresando…" : "Ingresar"}
            </button>

            <button
              type="button"
              onClick={() => void onGoogle()}
              disabled={oauthLoading || configuracionIncompleta}
              className="w-full rounded-xl border border-zinc-600 bg-transparent py-3 text-[15px] font-medium text-optoapp-brand hover:bg-white/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-optoapp-brand disabled:opacity-50"
            >
              {oauthLoading ? "Redirigiendo…" : "Continuar con Google"}
            </button>

            {signupUrl ? (
              <Link
                href={signupUrl}
                className="block w-full rounded-xl border border-zinc-600 py-3 text-center text-[15px] font-medium text-zinc-400 hover:bg-white/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-zinc-400"
              >
                Crear cuenta
              </Link>
            ) : null}
          </form>
        </div>

        <p className="max-w-[360px] text-center text-xs leading-relaxed text-zinc-500">
          Si no tienes cuenta, contacta al administrador de tu óptica.
        </p>
      </div>
    </div>
  );
}

function translateAuthError(message: string): string {
  const lower = message.toLowerCase();
  if (lower.includes("invalid login credentials")) {
    return "Credenciales incorrectas. Revise correo y contraseña.";
  }
  if (lower.includes("network") || lower.includes("fetch")) {
    return "Error de red. Compruebe su conexión.";
  }
  return message;
}
