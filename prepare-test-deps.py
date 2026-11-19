#!/usr/bin/env python3
"""Prepare test dependencies for rehearser without root privileges.

Installs into a user-writable directory (default ~/rehearser-test-deps,
override with the REHEARSER_TEST_DEPS_DIR environment variable):

- JDK 21 (Temurin)
- the Clojure CLI tools
- a loopback-only PostgreSQL 17 (conda-forge packages via micromamba)
  with the postgres superuser password "postgres", matching the CI
  service container

Works on Linux (x86_64 or aarch64) with glibc; tested on Debian 13
(trixie). After running this script, use the printed environment to
run the tests, for example from the project root:

    clojure -M:test
"""

import hashlib
import json
import os
import platform
import shutil
import subprocess
import sys
import tarfile
import tempfile
import time
import urllib.request
from pathlib import Path

DEPS_DIR = Path(
    os.environ.get("REHEARSER_TEST_DEPS_DIR", "~/rehearser-test-deps")
).expanduser()
DOWNLOADS_DIR = DEPS_DIR / "downloads"
JDK_DIR = DEPS_DIR / "jdk"
CLOJURE_PREFIX = DEPS_DIR / "clojure"
MAMBA_DIR = DEPS_DIR / "mamba"
PGENV_DIR = DEPS_DIR / "pgenv"
PGDATA_DIR = DEPS_DIR / "pgdata"
PGSOCKET_DIR = DEPS_DIR / "pgsocket"

TEMURIN_REPO = "adoptium/temurin21-binaries"
JDK_ARCH_PARTS = {"x86_64": "x64", "aarch64": "aarch64"}

CLOJURE_TOOLS_VERSION = "1.12.7.1681"
CLOJURE_TOOLS_SHA256 = "791c12b5f5a0986ae7456cc50f8db7715992ecc330273305846034ed1bacd6da"
CLOJURE_TOOLS_URL = (
    "https://github.com/clojure/brew-install/releases/download/"
    f"{CLOJURE_TOOLS_VERSION}/clojure-tools-{CLOJURE_TOOLS_VERSION}.tar.gz"
)

MICROMAMBA_URLS = {
    "x86_64": "https://micro.mamba.pm/api/micromamba/linux-64/latest",
    "aarch64": "https://micro.mamba.pm/api/micromamba/linux-aarch64/latest",
}

POSTGRES_VERSION = "17"
POSTGRES_PORT = 5432
POSTGRES_PASSWORD = "postgres"

USER_AGENT = "rehearser-prepare-test-deps"


def fail(message):
    print(message, file=sys.stderr)
    sys.exit(1)


def run(cmd, *, check=True, capture=False, cwd=None, env=None):
    cmd = [str(c) for c in cmd]
    print(f"+ {' '.join(cmd)}")
    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        cwd=cwd,
        env=env,
    )
    if result.returncode != 0 and check:
        fail(f"Command failed: {' '.join(cmd)}")
    return result


def fetch(url, dest):
    print(f"+ download {url}")
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request) as response, open(dest, "wb") as out:
        shutil.copyfileobj(response, out)
    return dest


def extract_tar(archive, dest):
    with tarfile.open(archive) as tar:
        try:
            tar.extractall(dest, filter="data")
        except TypeError:
            tar.extractall(dest)


def check_platform():
    if platform.system() != "Linux":
        fail("This script supports Linux only.")
    if platform.machine() not in JDK_ARCH_PARTS or platform.machine() not in MICROMAMBA_URLS:
        fail(f"Unsupported architecture: {platform.machine()}")


def jdk_env():
    env = os.environ.copy()
    env["JAVA_HOME"] = str(JDK_DIR)
    env["PATH"] = f"{JDK_DIR / 'bin'}:{env.get('PATH', '')}"
    return env


def clojure_env():
    env = jdk_env()
    env["PATH"] = f"{CLOJURE_PREFIX / 'bin'}:{env['PATH']}"
    return env


def latest_temurin_jdk_url():
    arch_part = JDK_ARCH_PARTS[platform.machine()]
    api_url = f"https://api.github.com/repos/{TEMURIN_REPO}/releases/latest"
    request = urllib.request.Request(
        api_url,
        headers={"User-Agent": USER_AGENT, "Accept": "application/vnd.github+json"},
    )
    with urllib.request.urlopen(request) as response:
        release = json.load(response)
    for asset in release["assets"]:
        name = asset["name"]
        if (
            name.startswith("OpenJDK21U-jdk")
            and arch_part in name
            and "linux" in name
            and "hotspot" in name
            and "alpine" not in name
            and name.endswith(".tar.gz")
        ):
            return asset["browser_download_url"]
    fail(f"No Temurin JDK 21 glibc linux asset found in release {release['tag_name']}")


def ensure_jdk():
    java = JDK_DIR / "bin" / "java"
    if java.exists():
        run([java, "-version"])
        return
    archive = fetch(latest_temurin_jdk_url(), DOWNLOADS_DIR / "jdk.tar.gz")
    with tempfile.TemporaryDirectory(dir=DOWNLOADS_DIR) as scratch:
        extract_tar(archive, scratch)
        extracted = next(Path(scratch).iterdir())
        if JDK_DIR.exists():
            shutil.rmtree(JDK_DIR)
        shutil.move(str(extracted), str(JDK_DIR))
    run([java, "-version"])


def ensure_clojure_cli():
    clojure = CLOJURE_PREFIX / "bin" / "clojure"
    env = clojure_env()
    if clojure.exists() and run(
        [clojure, "-M", "-e", "(clojure-version)"],
        check=False,
        capture=True,
        cwd=tempfile.gettempdir(),
        env=env,
    ).returncode == 0:
        return
    archive = fetch(
        CLOJURE_TOOLS_URL, DOWNLOADS_DIR / f"clojure-tools-{CLOJURE_TOOLS_VERSION}.tar.gz"
    )
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    if digest != CLOJURE_TOOLS_SHA256:
        fail(f"SHA-256 mismatch for {archive.name}: {digest}")
    lib_dir = CLOJURE_PREFIX / "lib" / "clojure"
    libexec_dir = lib_dir / "libexec"
    bin_dir = CLOJURE_PREFIX / "bin"
    libexec_dir.mkdir(parents=True, exist_ok=True)
    bin_dir.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=DOWNLOADS_DIR) as scratch:
        extract_tar(archive, scratch)
        tools = next(p for p in Path(scratch).iterdir() if p.is_dir())
        for name in ("deps.edn", "example-deps.edn", "tools.edn"):
            shutil.copy2(tools / name, lib_dir / name)
        for name in ("exec.jar", f"clojure-tools-{CLOJURE_TOOLS_VERSION}.jar"):
            shutil.copy2(tools / name, libexec_dir / name)
        clojure_script = (tools / "clojure").read_text().replace("PREFIX", str(lib_dir))
        clj_script = (tools / "clj").read_text().replace("BINDIR", str(bin_dir))
        (bin_dir / "clojure").write_text(clojure_script)
        (bin_dir / "clj").write_text(clj_script)
        os.chmod(bin_dir / "clojure", 0o755)
        os.chmod(bin_dir / "clj", 0o755)
    run(
        [clojure, "-M", "-e", '(println "clojure" (clojure-version))'],
        cwd=tempfile.gettempdir(),
        env=env,
    )


def ensure_micromamba():
    micromamba = MAMBA_DIR / "bin" / "micromamba"
    if micromamba.exists():
        return micromamba
    archive = fetch(
        MICROMAMBA_URLS[platform.machine()], DOWNLOADS_DIR / "micromamba.tar.bz2"
    )
    micromamba.parent.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive, "r:bz2") as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("bin/micromamba"))
        micromamba.write_bytes(tar.extractfile(member).read())
    os.chmod(micromamba, 0o755)
    return micromamba


def ensure_postgres_env(micromamba):
    if (PGENV_DIR / "bin" / "pg_isready").exists():
        return
    env = os.environ.copy()
    env["MAMBA_ROOT_PREFIX"] = str(DEPS_DIR / "mamba-root")
    run(
        [
            micromamba,
            "create",
            "-y",
            "-p",
            PGENV_DIR,
            "-c",
            "conda-forge",
            f"postgresql={POSTGRES_VERSION}",
        ],
        env=env,
    )


def pg_is_ready(pgenv_bin):
    return (
        run(
            [pgenv_bin / "pg_isready", "-q", "-h", "127.0.0.1", "-p", POSTGRES_PORT],
            check=False,
        ).returncode
        == 0
    )


def ensure_postgres():
    micromamba = ensure_micromamba()
    ensure_postgres_env(micromamba)
    pgenv_bin = PGENV_DIR / "bin"
    if not pg_is_ready(pgenv_bin):
        PGSOCKET_DIR.mkdir(parents=True, exist_ok=True)
        if not (PGDATA_DIR / "PG_VERSION").exists():
            with tempfile.NamedTemporaryFile(
                "w", suffix=".pwfile", delete=False
            ) as pwfile:
                pwfile.write(POSTGRES_PASSWORD + "\n")
                pwfile_path = pwfile.name
            try:
                run(
                    [
                        pgenv_bin / "initdb",
                        "-D",
                        PGDATA_DIR,
                        "-U",
                        "postgres",
                        f"--pwfile={pwfile_path}",
                        "--auth-local=trust",
                        "--auth-host=scram-sha-256",
                    ]
                )
            finally:
                os.unlink(pwfile_path)
            with open(PGDATA_DIR / "postgresql.conf", "a", encoding="utf-8") as conf:
                conf.write(
                    f"listen_addresses = 'localhost'\n"
                    f"port = {POSTGRES_PORT}\n"
                    f"unix_socket_directories = '{PGSOCKET_DIR}'\n"
                )
        run(
            [
                pgenv_bin / "pg_ctl",
                "-D",
                PGDATA_DIR,
                "-l",
                DEPS_DIR / "postgres.log",
                "start",
            ]
        )
        for _ in range(30):
            if pg_is_ready(pgenv_bin):
                break
            time.sleep(1)
    run([pgenv_bin / "pg_isready", "-h", "127.0.0.1", "-p", POSTGRES_PORT])
    env = os.environ.copy()
    env["PGPASSWORD"] = POSTGRES_PASSWORD
    run(
        [
            pgenv_bin / "psql",
            "-h",
            "127.0.0.1",
            "-p",
            POSTGRES_PORT,
            "-U",
            "postgres",
            "-d",
            "postgres",
            "-c",
            f"ALTER USER postgres PASSWORD '{POSTGRES_PASSWORD}';",
        ],
        env=env,
    )


def main():
    check_platform()
    DEPS_DIR.mkdir(parents=True, exist_ok=True)
    DOWNLOADS_DIR.mkdir(parents=True, exist_ok=True)
    ensure_jdk()
    ensure_clojure_cli()
    ensure_postgres()
    print()
    print("Test dependencies are ready in", DEPS_DIR)
    print("Run the tests from the project root with:")
    print(f'  export JAVA_HOME="{JDK_DIR}"')
    print(f'  export PATH="{CLOJURE_PREFIX / "bin"}:{JDK_DIR / "bin"}:$PATH"')
    print("  export POSTGRES_HOST=127.0.0.1")
    print("  clojure -M:test")


if __name__ == "__main__":
    main()
