import { spawn, execFileSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { setTimeout } from "node:timers/promises";

const DIRNAME = dirname(fileURLToPath(import.meta.url));

const TEST_DB_URL =
  "jdbc:postgresql://localhost:5432/rehearser_e2e?user=rehearser_e2e&password=rehearser_e2e"; // Replace with your actual test DB URL
const PORT = 3000;
const REHEARSER_DIR = join(DIRNAME, ".."); // Path to the directory containing `rehearser`
const SERVER_URL = `http://localhost:${PORT}`;

async function waitForServer(url) {
  const controller = new AbortController();
  setTimeout(30000).then(() => controller.abort());

  while (!controller.signal.aborted) {
    try {
      await fetch(url, { signal: controller.signal });
      return;
    } catch (err) {
      await setTimeout(1000, null, { signal: controller.signal });
    }
  }
  throw new Error(`Timeout waiting for server at ${url}`);
}

async function run() {
  let status = 0;
  let server = undefined;

  console.log("Resetting the database state");
  execFileSync("./rehearser", ["--database", TEST_DB_URL, "db-reset"], {
    cwd: REHEARSER_DIR,
    stdio: "inherit",
  });

  console.log("Starting up the server");
  server = spawn(
    "./rehearser",
    ["--database", TEST_DB_URL, "serve", "--port", PORT.toString()],
    { cwd: REHEARSER_DIR, stdio: "inherit" },
  );

  console.log("Waiting for the server to respond");
  // Wait for the server to be ready
  await waitForServer(SERVER_URL);

  console.log("Executing playwright");

  try {
    execFileSync("npx", ["playwright", "test"], { stdio: "inherit" });
  } catch (error) {
    // Not really interested in the error content, playwright tends to
    // print a lot of stuff anyway.
    status = 1;
  } finally {
    // Terminate the server
    if (server && !server.killed) {
      server.kill();
    }
  }

  process.exit(status);
}

run();
