import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = { ...loadEnv(mode, process.cwd(), ""), ...process.env };
  const apiTarget = env.VITE_API_PROXY_TARGET || "http://127.0.0.1:4040";
  const isLoopback = ["127.0.0.1", "localhost", "[::1]"].includes(new URL(apiTarget).hostname);
  const apiProxy = {
    "/api": {
      target: apiTarget,
      // Let the local backend see the same origin as the browser, including LAN IPs.
      changeOrigin: !isLoopback
    }
  };
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url))
      }
    },
    build: {
      outDir: "dist",
      emptyOutDir: true
    },
    server: {
      host: env.FRONTEND_HOST || "127.0.0.1",
      port: Number(env.FRONTEND_PORT || 5173),
      strictPort: true,
      proxy: apiProxy
    },
    preview: {
      host: env.FRONTEND_HOST || "127.0.0.1",
      port: Number(env.FRONTEND_PORT || 5173),
      strictPort: true,
      proxy: apiProxy
    }
  };
});
