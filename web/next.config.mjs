/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  /**
   * En desarrollo, la caché persistente de Webpack a veces queda inconsistente tras
   * errores/HMR y dispara runtime `__webpack_modules__[moduleId] is not a function`.
   * Desactivarla aquí penaliza algo el rebuild pero evita ese estado corrupto.
   */
  webpack: (config, { dev }) => {
    if (dev) {
      config.cache = false;
    }
    return config;
  }
};

export default nextConfig;
