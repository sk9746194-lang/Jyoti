#!/usr/bin/env node
/**
 * Production and Container Entrypoint for MYRA
 * Executes compiled dist/server.js if present, or loads server.ts
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const compiledServer = path.join(__dirname, "dist", "server.js");

if (fs.existsSync(compiledServer)) {
  import("./dist/server.js").catch((err) => {
    console.error("FATAL: Failed to launch compiled server (dist/server.js):", err);
    process.exit(1);
  });
} else {
  import("./server.ts").catch((err) => {
    console.error("FATAL: Failed to launch server.ts:", err);
    process.exit(1);
  });
}
