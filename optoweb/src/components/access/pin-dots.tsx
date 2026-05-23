"use client";

const PIN_LENGTH = 6;

type PinDotsProps = {
  filled: number;
};

/** Indicador de 6 posiciones para el PIN ingresado. */
export function PinDots({ filled }: PinDotsProps) {
  return (
    <div
      className="flex justify-center gap-3"
      role="status"
      aria-label={`${filled} de ${PIN_LENGTH} dígitos ingresados`}
    >
      {Array.from({ length: PIN_LENGTH }).map((_, i) => (
        <span
          key={i}
          className={`h-3 w-3 rounded-full transition-colors ${
            i < filled ? "bg-optoapp-brand" : "bg-zinc-700"
          }`}
          aria-hidden
        />
      ))}
    </div>
  );
}
