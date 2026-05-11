"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { PinDots } from "@/components/access/pin-dots";
import { PinPad } from "@/components/access/pin-pad";

const PIN_LENGTH = 6;

export function PinScreen() {
  const [digits, setDigits] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const append = useCallback((d: string) => {
    setError(null);
    setDigits((prev) =>
      prev.length >= PIN_LENGTH ? prev : prev + d
    );
  }, []);

  const clearLast = useCallback(() => {
    setError(null);
    setDigits((prev) => prev.slice(0, -1));
  }, []);

  const clearAll = useCallback(() => {
    setError(null);
    setDigits("");
  }, []);

  const submit = useCallback(async () => {
    if (digits.length !== PIN_LENGTH || loading) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch("/api/auth/verify-pin", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pin: digits })
      });
      const data = (await res.json().catch(() => ({}))) as {
        error?: string;
        ok?: boolean;
      };
      if (!res.ok) {
        setError(data.error ?? "No se pudo verificar el PIN.");
        clearAll();
        return;
      }
      window.location.assign("/dashboard");
    } catch {
      setError("Error de red. Intente de nuevo.");
      clearAll();
    } finally {
      setLoading(false);
    }
  }, [digits, loading, clearAll]);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const onKeyDown = (e: KeyboardEvent) => {
      if (loading) return;
      if (e.key >= "0" && e.key <= "9") {
        e.preventDefault();
        append(e.key);
        return;
      }
      if (e.key === "Backspace") {
        e.preventDefault();
        clearLast();
        return;
      }
      if (e.key === "Enter" && digits.length === PIN_LENGTH) {
        e.preventDefault();
        void submit();
      }
    };

    el.addEventListener("keydown", onKeyDown);
    return () => el.removeEventListener("keydown", onKeyDown);
  }, [append, clearLast, digits.length, loading, submit]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-6 font-sans selection:bg-primary/30">
      <div className="relative w-full max-w-md">
        {/* Abstract background blur elements */}
        <div className="absolute -left-20 -top-20 h-64 w-64 rounded-full bg-primary/10 blur-[100px]" />
        <div className="absolute -bottom-20 -right-20 h-64 w-64 rounded-full bg-brand-blue/10 blur-[100px]" />

        <div
          ref={containerRef}
          tabIndex={0}
          role="group"
          aria-label="Ingreso de PIN de seguridad"
          aria-describedby="pin-instructions"
          className="relative overflow-hidden rounded-[2.5rem] border border-border/50 bg-card/70 p-8 shadow-2xl backdrop-blur-3xl outline-none sm:p-12"
        >
          <header className="mb-10 flex flex-col items-center space-y-4 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-primary/10 shadow-inner">
              <span className="text-4xl">🔐</span>
            </div>
            <div className="space-y-1">
              <h1 className="font-heading text-4xl font-black tracking-tight text-foreground">
                OptoApp
              </h1>
              <p id="pin-instructions" className="text-sm font-medium text-muted-foreground/80">
                Ingrese su PIN de seguridad de 6 dígitos
              </p>
            </div>
          </header>

          <div className="mb-8 flex flex-col items-center gap-8">
            <PinDots filled={digits.length} />

            <div aria-live="polite" className="h-6 text-center text-sm font-semibold tracking-wide">
              {loading ? (
                <span className="animate-pulse text-primary">Verificando acceso...</span>
              ) : error ? (
                <p role="alert" aria-live="assertive" className="text-destructive">
                  {error}
                </p>
              ) : null}
            </div>
          </div>

          <PinPad
            onDigit={append}
            onClear={clearLast}
            onConfirm={() => void submit()}
            confirmDisabled={digits.length !== PIN_LENGTH || loading}
          />
          
          <footer className="mt-10 text-center">
            <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground/40">
              Acceso Seguro · OptoApp 2026
            </p>
          </footer>
        </div>
      </div>
    </div>
  );
}
