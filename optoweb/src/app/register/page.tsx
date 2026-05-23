"use client";

import { Eye, EyeOff, Lock, Mail, UserCheck } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { createClient } from "@/lib/supabase/client";

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!email.trim()) { setError("Ingresa un correo electrónico"); return; }
    if (password.length < 6) { setError("La contraseña debe tener al menos 6 caracteres"); return; }
    if (!/[a-z]/.test(password)) { setError("Falta una minúscula en la contraseña"); return; }
    if (!/[A-Z]/.test(password)) { setError("Falta una MAYÚSCULA en la contraseña"); return; }
    if (!/[0-9]/.test(password)) { setError("Falta un número en la contraseña"); return; }
    if (!/[^a-zA-Z0-9]/.test(password)) { setError("Falta un símbolo especial en la contraseña"); return; }
    if (password !== confirmPassword) { setError("Las contraseñas no coinciden"); return; }

    setLoading(true);
    try {
      const supabase = createClient();
      const origin = window.location.origin;
      const { error: signUpError } = await supabase.auth.signUp({
        email: email.trim(),
        password,
        options: {
          emailRedirectTo: `${origin}/auth/callback?next=/dashboard`
        }
      });

      if (signUpError) {
        setError(translateError(signUpError.message));
        return;
      }

      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado.");
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 py-10">
        <div className="flex w-full max-w-[420px] flex-col items-center gap-6 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-500 shadow-xl shadow-emerald-500/20">
            <UserCheck className="h-8 w-8 text-white" />
          </div>
          <h1 className="font-heading text-3xl font-black text-foreground">Revisa tu correo</h1>
          <p className="text-sm font-medium text-muted-foreground leading-relaxed">
            Te enviamos un enlace de confirmación a <strong>{email}</strong>.
            Haz clic en el enlace para activar tu cuenta y luego podrás iniciar sesión.
          </p>
          <Link
            href="/login"
            className="rounded-2xl bg-primary px-8 py-4 font-heading text-sm font-black text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
          >
            VOLVER AL INICIO DE SESIÓN
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 py-10 transition-colors duration-300">
      <div className="flex w-full max-w-[420px] flex-col items-center gap-10">
        <header className="flex flex-col items-center gap-3 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary shadow-xl shadow-primary/20">
            <span className="font-heading text-3xl font-black text-primary-foreground">O</span>
          </div>
          <div className="space-y-1">
            <h1 className="font-heading text-4xl font-black tracking-tight text-foreground">Crear Cuenta</h1>
            <p className="text-sm font-medium text-muted-foreground/60">Registro con correo</p>
            <p className="text-xs text-muted-foreground/40 leading-relaxed">
              La contraseña debe tener: minúsculas, MAYÚSCULAS, números y símbolos.
            </p>
          </div>
        </header>

        <div className="w-full rounded-[2.5rem] border border-border bg-card p-8 shadow-2xl shadow-foreground/[0.02]">
          <form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
            <div className="space-y-2">
              <label htmlFor="reg-email" className="sr-only">Correo electrónico</label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-zinc-500" aria-hidden />
                <input
                  id="reg-email"
                  type="email"
                  autoComplete="email"
                  placeholder="Correo electrónico"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full rounded-2xl border border-border bg-foreground/[0.03] py-4 pl-12 pr-4 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:bg-background focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="reg-password" className="sr-only">Contraseña</label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-zinc-500" aria-hidden />
                <input
                  id="reg-password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
                  placeholder="Contraseña (mín. 6 caracteres)"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full rounded-2xl border border-border bg-foreground/[0.03] py-4 pl-12 pr-12 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:bg-background focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-zinc-400 hover:bg-white/5"
                  aria-label={showPassword ? "Ocultar" : "Mostrar"}
                >
                  {showPassword ? <EyeOff className="h-[18px] w-[18px]" /> : <Eye className="h-[18px] w-[18px]" />}
                </button>
              </div>
            </div>

            <div className="space-y-2">
              <label htmlFor="reg-confirm" className="sr-only">Confirmar contraseña</label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-zinc-500" aria-hidden />
                <input
                  id="reg-confirm"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
                  placeholder="Confirmar contraseña"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full rounded-2xl border border-border bg-foreground/[0.03] py-4 pl-12 pr-4 text-sm font-medium text-foreground placeholder:text-muted-foreground/40 focus:bg-background focus:ring-2 focus:ring-primary/20 transition-all outline-none"
                />
              </div>
            </div>

            {error && (
              <p className="text-sm text-red-400" role="alert">{error}</p>
            )}

            <button
              type="submit"
              disabled={loading || !email.trim() || !password || !confirmPassword}
              className="w-full rounded-2xl bg-primary py-4 font-heading text-base font-black text-primary-foreground shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:opacity-30 disabled:hover:scale-100"
            >
              {loading ? "CREANDO CUENTA..." : "CREAR CUENTA"}
            </button>
          </form>
        </div>

        <Link
          href="/login"
          className="text-sm font-bold text-muted-foreground hover:text-foreground transition-colors"
        >
          ¿Ya tienes cuenta? Inicia sesión
        </Link>
      </div>
    </div>
  );
}

function translateError(message: string): string {
  const lower = message.toLowerCase();
  if (lower.includes("already registered") || lower.includes("already exists") || lower.includes("duplicate")) {
    return "Este correo ya está registrado.";
  }
  if (lower.includes("password")) {
    return "La contraseña debe tener: minúsculas, MAYÚSCULAS, números y símbolos especiales.";
  }
  return message;
}
