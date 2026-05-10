import type { NextResponse } from "next/server";

export const PIN_VERIFIED_COOKIE = "optoapp_pin_verified";

// middleware.ts la lee en page routes; se necesita "/" para cubrir todas las rutas
const COOKIE_PATH = "/";
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 8;

export function setPinVerifiedCookie(res: NextResponse) {
  res.cookies.set(PIN_VERIFIED_COOKIE, "1", {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: COOKIE_PATH,
    maxAge: COOKIE_MAX_AGE_SECONDS
  });
}

export function deletePinVerifiedCookie(res: NextResponse) {
  res.cookies.set(PIN_VERIFIED_COOKIE, "", {
    path: COOKIE_PATH,
    maxAge: 0,
  });
}
