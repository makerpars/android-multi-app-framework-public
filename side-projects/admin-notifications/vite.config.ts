import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";
import { execSync } from "node:child_process";

const ROOT_DIR = path.resolve(__dirname, "../..");

export default defineConfig(({ mode }) => {
  // Support both side-projects/admin-notifications/.env and repo root .env.
  const rootEnv = loadEnv(mode, ROOT_DIR, "");
  const localEnv = loadEnv(mode, __dirname, "");
  const mergedEnv = { ...rootEnv, ...localEnv };

  const envDefineEntries = Object.entries(mergedEnv).filter(([key]) =>
    key.startsWith("VITE_"),
  );

  let gitSha = "local";
  try {
    gitSha = execSync("git rev-parse --short HEAD", {
      cwd: ROOT_DIR,
      stdio: ["ignore", "pipe", "ignore"],
    })
      .toString()
      .trim();
  } catch {
    gitSha = "local";
  }

  const buildTime = new Date().toISOString();
  const packageVersion = process.env.npm_package_version ?? "0.1.0";

  const define: Record<string, string> = {};
  for (const [key, value] of envDefineEntries) {
    define[`import.meta.env.${key}`] = JSON.stringify(value);
  }
  define["import.meta.env.VITE_APP_BUILD"] = JSON.stringify(`${packageVersion}-${gitSha}`);
  define["import.meta.env.VITE_APP_BUILD_TIME"] = JSON.stringify(buildTime);

  return {
    envDir: __dirname,
    envPrefix: ["VITE_"],
    define,
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes("node_modules/firebase")) return "firebase-vendor";
            if (id.includes("node_modules/react") || id.includes("node_modules/react-dom")) {
              return "react-vendor";
            }
            if (id.includes("node_modules")) return "vendor";
            return undefined;
          },
        },
      },
    },
    resolve: {
      alias: {
        "@ciapps": path.resolve(__dirname, "../../.ci/apps.json"),
      },
    },
    server: {
      fs: {
        allow: [ROOT_DIR],
      },
    },
  };
});
