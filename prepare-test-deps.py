#!/usr/bin/env python3
"""Prepare test dependencies for rehearser on Debian 13 (trixie).

Installs JDK 21, the Clojure CLI tools, and a loopback-only PostgreSQL 17.
This is setup only; use the normal developer workflow to run the tests.

Steps that need administrator privileges fail with an instruction to
rerun the script with sudo.
"""

import os
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request

CLOJURE_INSTALL_URL = (
    "https://github.com/clojure/brew-install/releases/latest/download/posix-install.sh"
)


def fail(message):
    print(message, file=sys.stderr)
    sys.exit(1)


def run(cmd, *, check=True, capture=False, cwd=None):
    print(f"+ {' '.join(cmd)}")
    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        cwd=cwd,
    )
    if result.returncode != 0:
        if check and os.geteuid() != 0:
            fail(
                f"Command failed: {' '.join(cmd)}\n"
                "This step most likely needs administrator privileges.\n"
                "Rerun the script as root, for example:\n"
                f"  sudo {sys.executable} {os.path.abspath(__file__)}"
            )
        if check:
            fail(f"Command failed: {' '.join(cmd)}")
    return result


def have(cmd):
    return shutil.which(cmd) is not None


def check_debian_trixie():
    try:
        with open("/etc/os-release", encoding="utf-8") as f:
            os_release = dict(
                line.strip().split("=", 1)
                for line in f
                if "=" in line and not line.lstrip().startswith("#")
            )
    except OSError:
        fail("Cannot read /etc/os-release. This script supports Debian 13 (trixie) only.")
    if os_release.get("ID", "").strip('"') != "debian" or os_release.get(
        "VERSION_CODENAME", ""
    ).strip('"') != "trixie":
        fail(
            "This system does not look like Debian 13 (trixie), "
            "which is the only supported platform."
        )


def install_packages(*packages):
    run(["apt-get", "update", "-qq"])
    run(["apt-get", "install", "-y", "-qq", *packages])


def ensure_jdk():
    if have("java"):
        return
    install_packages("openjdk-21-jdk-headless")
    run(["java", "-version"])


def ensure_clojure_cli():
    """Debian's "clojure" package is NOT the CLI tools we need."""
    # Run clojure in a scratch directory, so that a root run does not leave
    # a root-owned .cpcache in the project directory.
    with tempfile.TemporaryDirectory() as scratch:
        if have("clojure") and run(
            ["clojure", "-M", "-e", "(clojure-version)"],
            check=False,
            capture=True,
            cwd=scratch,
        ).returncode == 0:
            return
        with tempfile.NamedTemporaryFile(suffix=".sh", delete=False) as f:
            urllib.request.urlretrieve(CLOJURE_INSTALL_URL, f.name)
            installer = f.name
        try:
            run(["bash", installer])
        finally:
            os.unlink(installer)
        run(
            ["clojure", "-M", "-e", '(println "clojure" (clojure-version))'],
            cwd=scratch,
        )


def ensure_postgres():
    if not have("pg_isready"):
        install_packages("postgresql")
    if run(["pg_isready", "-q"], check=False).returncode != 0:
        run(["pg_ctlcluster", "17", "main", "start"])
        for _ in range(10):
            if run(["pg_isready", "-q"], check=False).returncode == 0:
                break
            time.sleep(1)
    run(["pg_isready"])
    # The tests connect as postgres/postgres (matches CI's service container).
    run(["su", "postgres", "-c", "psql -c \"ALTER USER postgres PASSWORD 'postgres';\""])


def main():
    check_debian_trixie()
    ensure_jdk()
    ensure_clojure_cli()
    ensure_postgres()


if __name__ == "__main__":
    main()
