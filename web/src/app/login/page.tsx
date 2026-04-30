import { LoginForm } from "@/components/access/login-form";

export default async function LoginPage({
  searchParams
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const q = await searchParams;
  const configuracionIncompleta = q.error === "configuracion";
  const oauthError = q.error === "oauth";

  return (
    <LoginForm
      configuracionIncompleta={configuracionIncompleta}
      oauthError={oauthError}
    />
  );
}
