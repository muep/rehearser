#!/usr/bin/env python3
"""Prepare test dependencies for rehearser on Debian 13 (trixie).

Installs JDK 21, the Clojure CLI tools, and a loopback-only PostgreSQL 17,
then warms the project dependencies and runs the test suite like CI does.
"""

import os
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request

REPO_DIR = os.environ.get("REPO_DIR", os.path.dirname(os.path.abspath(__file__)))

CLOJURE_INSTALL_URL = (
    "https://github.com/clojure/brew-install/releases/latest/download/posix-install.sh"
)


def run(cmd, *, check=True, capture=False, **kwargs):
    print(f"+ {' '.join(cmd)}")
    return subprocess.run(
        cmd,
        check=check,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        **kwargs,
    )


def have(cmd):
    return shutil.which(cmd) is not None


def install_packages(*packages):
    run(["sudo", "apt-get", "update", "-qq"])
    run(["sudo", "apt-get", "install", "-y", "-qq", *packages])


def ensure_jdk():
    if have("java"):
        return
    install_packages("openjdk-21-jdk-headless")
    run(["java", "-version"])


def ensure_clojure_cli():
    """Debian's "clojure" package is NOT the CLI tools we need."""
    if have("clojure") and run(
        ["clojure", "-M", "-e", "(clojure-version)"],
        check=False,
        capture=True,
    ).returncode == 0:
        return
    with tempfile.NamedTemporaryFile(suffix=".sh", delete=False) as f:
        urllib.request.urlretrieve(CLOJURE_INSTALL_URL, f.name)
        installer = f.name
    try:
        run(["sudo", "bash", installer])
    finally:
        os.unlink(installer)
    run(["clojure", "-M", "-e", '(println "clojure" (clojure-version))'])


def ensure_postgres():
    if not have("pg_isready"):
        install_packages("postgresql")
    if not run(["pg_isready", "-q"], check=False).returncode == 0:
        run(["sudo", "pg_ctlcluster", "17", "main", "start"])
        for _ in range(10):
            if run(["pg_isready", "-q"], check=False).returncode == 0:
                break
            time.sleep(1)
    run(["pg_isready"])
    # Tests connect as postgres/postgres (matches CI's service container).
    run(["sudo", "-u", "postgres", "psql", "-c", "ALTER USER postgres PASSWORD 'postgres';"])


def main():
    ensure_jdk()
    ensure_clojure_cli()
    ensure_postgres()

    os.chdir(REPO_DIR)
    run(["./check-deps.sh"])
    # Same command as the clojure.yml workflow.
    result = run(["clojure", "-M:test"])
    sys.exit(result.returncode)


if __name__ == "__main__":
    main()
