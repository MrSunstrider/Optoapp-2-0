import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const nextDir = path.join(__dirname, "..", ".next");

try {
  fs.rmSync(nextDir, { recursive: true, force: true });
  process.stdout.write(`Removed ${nextDir}\n`);
} catch {
  process.exitCode = 0;
}
