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
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 py-10 transition-colors duration-300">
      <div className="flex w-full max-w-[420px] flex-col items-center gap-10">
        <header className="flex flex-col items-center gap-3 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary shadow-xl shadow-primary/20">
             <span className="font-heading text-3xl font-black text-primary-foreground">O</span>
          </div>
          <div className="space-y-1">
            <h1 className="font-heading text-5xl font-black tracking-tight text-foreground">OptoApp</h1>
            <p className="text-sm font-medium text-muted-foreground/60 uppercase tracking-[0.2em]">Clinical Software 2026</p>
          </div>
        </header>

        <div className="w-full rounded-[2.5rem] border border-border bg-card p-8 shadow-2xl shadow-foreground/[0.02]">
          <form className="flex flex-col gap-6" onSubmit={onSubmit} noValidate>
            {configuracionIncompleta && (
              <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-xs font-medium text-destructive">
                ⚠️ Configuración incompleta: defina las variables de Supabase en{" "}
                <code className="font-mono bg-destructive/10 px-1 rounded">web/.env.local</code>.
              </div>
            )}
            {oauthError && (
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs font-medium text-amber-600">
                ⚠️ No se pudo completar el acceso con Google. Intente de nuevo.
              </div>
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
                  className="w-full rounded-2xl border border-border bg-foreground/[0.03] py-4 pl-12 pr-4 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:bg-background focus:ring-2 focus:ring-primary/20 transition-all outline-none disabled:opacity-50"
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
                  className="w-full rounded-2xl border border-border bg-foreground/[0.03] py-4 pl-12 pr-12 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:bg-background focus:ring-2 focus:ring-primary/20 transition-all outline-none disabled:opacity-50"
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
              className="w-full rounded-2xl bg-primary py-4 font-heading text-base font-black text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-30 disabled:hover:scale-100"
            >
              {loading ? "VERIFICANDO..." : "ENTRAR AL SISTEMA"}
            </button>

            <button
              type="button"
              onClick={() => void onGoogle()}
              disabled={oauthLoading || configuracionIncompleta}
              className="w-full rounded-2xl border border-border bg-foreground/[0.02] py-4 text-sm font-bold text-foreground transition-all hover:bg-foreground/[0.05] disabled:opacity-30"
            >
              {oauthLoading ? "Cargando..." : "Continuar con Google"}
            </button>

            <Link
              href={signupUrl || "/register"}
              className="block w-full rounded-2xl border border-dashed border-border py-4 text-center text-xs font-bold text-muted-foreground hover:bg-muted/30 transition-all"
            >
              CREAR UNA CUENTA NUEVA
            </Link>
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
