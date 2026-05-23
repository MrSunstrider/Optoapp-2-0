import { type NextRequest } from "next/server";
import { updateSession } from "@/lib/supabase/middleware";

export async function middleware(request: NextRequest) {
  return updateSession(request);
}

export const config = {
  // Incluir `/` explícito: en algunos entornos el patrón solo con `(.*)` no aplica sesión en la raíz.
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico).*)",
    "/"
  ]
};
