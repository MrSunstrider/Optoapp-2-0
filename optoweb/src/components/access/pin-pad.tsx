"use client";

type PinPadProps = {
  onDigit: (digit: string) => void;
  onClear: () => void;
  onConfirm: () => void;
  confirmDisabled: boolean;
};

/**
 * Teclado 3×4 al estilo móvil: última fila C, 0, OK.
 */
export function PinPad({
  onDigit,
  onClear,
  onConfirm,
  confirmDisabled
}: PinPadProps) {
  const keys = [
    ["1", "2", "3"],
    ["4", "5", "6"],
    ["7", "8", "9"],
    ["C", "0", "OK"]
  ];

  return (
    <div className="mx-auto grid w-full max-w-[280px] grid-cols-3 gap-3 sm:max-w-[320px] sm:gap-4">
      {keys.flatMap((row, ri) =>
        row.map((label, ci) => {
          const isClear = label === "C";
          const isOk = label === "OK";

          const base =
            "flex aspect-square max-h-[76px] max-w-[76px] items-center justify-center rounded-full text-lg font-semibold transition-opacity focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-optoapp-brand disabled:opacity-40 sm:h-[72px] sm:w-[72px]";

          if (isClear) {
            return (
              <button
                key={`${ri}-${ci}`}
                type="button"
                className={`${base} bg-optoapp-key-clear text-zinc-900`}
                onClick={onClear}
                aria-label="Borrar último dígito"
              >
                C
              </button>
            );
          }

          if (isOk) {
            return (
              <button
                key={`${ri}-${ci}`}
                type="button"
                className={`${base} bg-optoapp-key-ok text-zinc-900`}
                onClick={onConfirm}
                disabled={confirmDisabled}
                aria-label="Confirmar PIN"
              >
                <span className="flex items-center justify-center font-bold tracking-tight">
                  OK
                </span>
              </button>
            );
          }

          return (
            <button
              key={`${ri}-${ci}`}
              type="button"
              className={`${base} bg-optoapp-key text-white`}
              onClick={() => onDigit(label)}
              aria-label={`Dígito ${label}`}
            >
              {label}
            </button>
          );
        })
      )}
    </div>
  );
}
