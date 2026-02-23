import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 5602,
    proxy: {
      "/api": {
        target: "https://localhost",
        changeOrigin: true,
        secure: false, // Set to false for self-signed certificates in development
      },
      "/auth": {
        target: "https://keycloak.localhost",
        changeOrigin: true,
        secure: false, // Set to false for self-signed certificates in development
        rewrite: (path) => path.replace(/^\/auth/, ""),
      },
    },
  },
  plugins: [react(), mode === "development" && componentTagger()].filter(
    Boolean,
  ),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
}));
