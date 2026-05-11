import type { SVGProps } from "react";

const stroke = (props: SVGProps<SVGSVGElement>) => ({
  fill: "none" as const,
  stroke: "currentColor",
  strokeWidth: 1.75,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  ...props
});

export function IconPacientes(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M6 19c0-3.3 2.7-6 6-6s6 2.7 6 6" />
    </svg>
  );
}

export function IconServiciosVarios(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <path d="M6 4h2l1 2h9a1 1 0 0 1 .96 1.27l-1.2 5A2 2 0 0 1 15.8 14H9" />
      <circle cx="9" cy="19" r="1.25" fill="currentColor" stroke="none" />
      <circle cx="17" cy="19" r="1.25" fill="currentColor" stroke="none" />
      <path d="M4 6h2M4 4v4" />
    </svg>
  );
}

export function IconOperacionHoy(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M8 3v4M16 3v4M4 11h16" />
      <path d="M10 15l1.5 1.5L15 13" />
    </svg>
  );
}

export function IconConfiguracion(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <circle cx="12" cy="12" r="3" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
    </svg>
  );
}

export function IconAgenda(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <rect x="4" y="5" width="16" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M4 11h16" />
      <path d="M8 15h.01M12 15h.01M16 15h.01M8 18h.01M12 18h.01" />
    </svg>
  );
}

export function IconInventario(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
      <path d="M3.27 6.96L12 12.01l8.73-5.05M12 22.08V12" />
    </svg>
  );
}

export function IconCierreCaja(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <path d="M4 9a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9z" />
      <path d="M7 6V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1" />
      <path d="M12 14v2" />
    </svg>
  );
}

export function IconEstadisticas(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <path d="M4 20V10M10 20V4M16 20v-6M22 20V14" />
    </svg>
  );
}

export function IconReportes(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M8 3v4M16 3v4M4 11h16" />
      <circle cx="9" cy="16" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="12" cy="16" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="15" cy="16" r="0.9" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function IconSincronizar(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} aria-hidden {...stroke(props)}>
      <path d="M4 14a8 8 0 0 1 8-8 7.8 7.8 0 0 1 5 1.8L21 4v6h-6" />
      <path d="M20 10a8 8 0 0 1-8 8 7.8 7.8 0 0 1-5-1.8L3 20v-6h6" />
    </svg>
  );
}

export function IconLogout(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} aria-hidden {...stroke(props)}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5M21 12H9" />
    </svg>
  );
}

export function LogoOptoArc(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 48 24"
      width={40}
      height={20}
      aria-hidden
      className={props.className}
    >
      <path
        d="M4 20c0-8 8.5-16 20-16s20 8 20 16"
        fill="none"
        stroke="rgb(59 130 246)"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  );
}
