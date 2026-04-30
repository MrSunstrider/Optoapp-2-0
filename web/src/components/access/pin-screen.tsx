"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import { PinDots } from "@/components/access/pin-dots";
import { PinPad } from "@/components/access/pin-pad";

const PIN_LENGTH = 6;

export function PinScreen() {
  const router = useRouter();
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
      router.replace("/dashboard");
      router.refresh();
    } catch {
      setError("Error de red. Intente de nuevo.");
      clearAll();
    } finally {
      setLoading(false);
    }
  }, [digits, loading, router, clearAll]);

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
    <div
      className="flex min-h-screen flex-col items-center justify-center bg-optoapp-surface px-4 py-10 font-inter text-white"
    >
      <div
        ref={containerRef}
        tabIndex={0}
        role="group"
        aria-label="Ingreso de PIN de seguridad"
        aria-describedby="pin-instructions"
        className="flex w-full max-w-[400px] flex-col items-center gap-10 outline-none"
      >
        <header className="space-y-3 text-center">
          <h1 className="text-4xl font-bold text-optoapp-brand">OptoApp</h1>
          <p id="pin-instructions" className="text-sm text-zinc-300">
            Ingrese su PIN de seguridad (6 dígitos)
          </p>
        </header>

        <PinDots filled={digits.length} />

        <div aria-live="polite" className="min-h-[1.25rem] text-center text-sm">
          {loading ? (
            <span className="text-zinc-400">Verificando…</span>
          ) : null}
        </div>
        {error ? (
          <p
            role="alert"
            aria-live="assertive"
            className="max-w-sm text-center text-sm text-red-400"
          >
            {error}
          </p>
        ) : null}

        <PinPad
          onDigit={append}
          onClear={clearLast}
          onConfirm={() => void submit()}
          confirmDisabled={digits.length !== PIN_LENGTH || loading}
        />
      </div>
    </div>
  );
}
