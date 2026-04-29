import { LoginForm } from "./login-form";

export default async function LoginPage({
  searchParams
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const q = await searchParams;
  const configuracionIncompleta = q.error === "configuracion";

  return <LoginForm configuracionIncompleta={configuracionIncompleta} />;
}
