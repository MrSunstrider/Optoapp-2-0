import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "OptoApp Web",
  description: "Ecosistema web de OptoApp"
};

export default function RootLayout({
  children
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
