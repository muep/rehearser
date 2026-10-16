# E2E tests

End-to-end tests for Rehearser, based on
[Playwright](https://playwright.dev/). The tests drive a real Rehearser
server through a browser and exercise the application the same way a
user would: signing up, logging in, adding tunes, running rehearsals,
and editing rehearsal entries.

The setup is intentionally thin: there is no separate build step, no
bundled web server, and no mocking. A small Node.js harness resets a
dedicated PostgreSQL database, starts the Rehearser backend from the
parent directory, waits for it to answer, and then hands control over
to Playwright.

## Prerequisites

- Node.js (24 in CI; recent versions work)
- JDK 21 and the [Clojure CLI tools](https://clojure.org/guides/install_clojure),
  because the backend is started from source with `clojure`
- A PostgreSQL server reachable at `localhost:5432`, with a dedicated
  database and role for the tests (see below)
- Playwright's browser binaries (installed on demand, see below)

On Debian 13 (trixie), the JDK, Clojure CLI, and a loopback-only
PostgreSQL can be set up by running
[`prepare-test-deps.py`](../prepare-test-deps.py) from the project
root:

    sudo ./prepare-test-deps.py

Note that the script only prepares the default `postgres` superuser;
the dedicated `rehearser_e2e` role and database used by these tests
still need to be created separately, as described next.

## Test database

The harness expects to find a PostgreSQL database at the URL defined
in `test-harness.js`:

    jdbc:postgresql://localhost:5432/rehearser_e2e?user=rehearser_e2e&password=rehearser_e2e

Create it once, e.g. as the `postgres` superuser:

    sudo -u postgres psql \
      -c "CREATE ROLE rehearser_e2e LOGIN PASSWORD 'rehearser_e2e';" \
      -c "CREATE DATABASE rehearser_e2e OWNER rehearser_e2e;"

The database contents are wiped by the harness on every run, so it
must not be a database you care about. Using a dedicated database
keeps the e2e tests isolated from the development database described
in [backend/README.md](../backend/README.md).

The credentials and port are currently hardcoded in
`test-harness.js`. The test database URL is passed to the backend via
the `--database` option, which accepts the JDBC-style URL shown
above. If your PostgreSQL is elsewhere or you want different
credentials, adjust `TEST_DB_URL` in `test-harness.js` accordingly.

## Running the tests

    # (in this directory)
    npm ci
    npx playwright install --with-deps

Then run everything with the harness:

    node test-harness.js

The harness takes care of the full lifecycle:

1. Reset the test database (`./rehearser --database ... db-reset`)
2. Start the backend (`./rehearser --database ... serve --port 3000`)
3. Wait until the server responds on the port
4. Run `npx playwright test`
5. Kill the server and exit with Playwright's status

The first run is slow, because Clojure downloads and compiles the
backend before the server can start. Subsequent runs start faster.

### Running Playwright against an already-running server

For quicker iteration on a single test, you can skip the harness and
run Playwright directly, reusing a server that is already up:

    npx playwright test signup.spec.js

The backend must be listening on `http://localhost:3000` (or on
whatever `BASE_URL` points to). Remember to reset the database
yourself between runs, or use unique usernames like the tests mostly
do; leftover data from earlier runs can change search results and
break tests that assert on page content.

## What the tests cover

Test specs live in [tests/](tests/):

- `example.spec.js` - smoke test; the front page loads with the right
  title
- `signup.spec.js` - a user can sign up and then log in
- `basic-session.spec.js` - a full user session: add tunes, start a
  rehearsal, add entries to it, and create a new tune on the fly from
  an empty search result
- `entry-edit.spec.js` - rehearsal entries can be edited, including
  changing an entry from one tune to another

## Architecture

    +-------------------------------------------------------------+
    | test-harness.js (Node, entry point)                         |
    |                                                             |
    |  1. ./rehearser --database URL db-reset   (sync, in project |
    |     root)                                                   |
    |  2. spawn ./rehearser --database URL serve --port 3000      |
    |  3. poll http://localhost:3000 until it responds            |
    |  4. npx playwright test                    (sync, inherits   |
    |     stdout/stderr)                                          |
    |  5. kill server, exit with Playwright's status              |
    +------------------------------+------------------------------+
                                   |
                                   v
    +-----------------------------+    +--------------------------+
    | Playwright (tests/*.spec.js)|    | Rehearser backend        |
    | Chromium-driven browser --->|---|> server-rendered pages   |
    | baseURL: localhost:3000     |   | Clojure, from project    |
    +-----------------------------+    | root, via ./rehearser    |
                                     +------------+-------------+
                                                  |
                                                  v
                                     +--------------------------+
                                     | PostgreSQL rehearser_e2e |
                                     | (localhost:5432, wiped   |
                                     | at the start of a run)   |
                                     +--------------------------+

Components:

- `test-harness.js` - the entry point. A plain Node script that owns
  the database reset, the server process, and the Playwright
  invocation. It exits non-zero if any test fails, so it is directly
  usable as a CI step.
- `playwright.config.js` - minimal Playwright configuration; sets the
  base URL (default `http://localhost:3000`, overridable with the
  `BASE_URL` environment variable). There are no projects or browser
  matrices configured; Playwright runs its default browser setup.
- `tests/*.spec.js` - the Playwright test specs. Each spec drives the
  site like a user. Test users are created through the real signup
  flow, with randomized usernames to avoid collisions. Specs commonly
  define small helper functions (`addTune`, `addEntry`,
  `prepareUser`) at the top instead of using page objects or fixtures.
- `./rehearser` (project root) - shell wrapper around
  `clojure -M -m rehearser.main`. The harness runs both the `db-reset`
  and `serve` subcommands through it, so the tests always exercise
  the current source code, not a packaged artifact.
- `backend/sql/rehearser/rehearser-v1.sql` - the schema executed by
  the `db-reset` subcommand. It drops and recreates the tables, which
  is how the harness guarantees a clean database for every run.

The tests share one database and one server process for the entire
run. Isolation between tests comes from randomized usernames (data is
per-account in the schema) rather than from resetting between tests.
This keeps runs fast but means tests are not safe to run in parallel
against data sets that could interact; Playwright's default workers
are used as-is.

### CI

GitHub Actions runs the suite on every push and pull request via
[.github/workflows/e2e-tests.yml](../.github/workflows/e2e-tests.yml).
The workflow provides the `rehearser_e2e` PostgreSQL 16 database as a
service container on `localhost:5432`, installs Node 24, Java, and the
Clojure CLI, and then runs `node test-harness.js` in this directory.
On failure, the Playwright test results are uploaded as an artifact.

## Formatting

Prettier is used for formatting:

    # (in this directory)
    npm run format
