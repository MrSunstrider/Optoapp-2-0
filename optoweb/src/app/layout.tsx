import "./globals.css";
import type { Metadata } from "next";
import { Inter, Outfit } from "next/font/google";
import { ThemeProvider } from "@/components/theme-provider";
import { ThemeToggle } from "@/components/theme-toggle";

const outfit = Outfit({
  subsets: ["latin"],
  variable: "--font-outfit"
});

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter"
});

export const metadata: Metadata = {
  title: "OptoApp Web",
  description: "Ecosistema web de OptoApp"
};

export default function RootLayout({
  children
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className="bg-background" suppressHydrationWarning>
      <body
        className={`${outfit.variable} ${inter.variable} font-sans antialiased bg-background text-foreground transition-colors duration-300`}
      >
        <ThemeProvider>
          {children}
          <div className="fixed bottom-4 right-4 z-[70] md:bottom-6 md:right-6">
            <ThemeToggle />
          </div>
        </ThemeProvider>
      </body>
    </html>
  );
}
