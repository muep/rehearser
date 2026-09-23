#!/bin/sh
# Report dependencies in deps.edn that have a newer release available.
#
# Prints one line per dependency: the current version and the latest
# stable version found in Maven Central or Clojars. Pre-release versions
# (alpha, beta, rc, snapshot, milestone, preview) are ignored.
#
# Requires the Clojure CLI tools. Reads deps.edn but never modifies it.
#
# Usage: ./check-outdated-deps.sh [path-to-deps.edn]

set -eu

DEPS_FILE="${1:-deps.edn}"

if [ ! -f "$DEPS_FILE" ]; then
    echo "No such file: $DEPS_FILE" >&2
    exit 1
fi

exec clojure -M:dev -m outdated-deps.core "$DEPS_FILE"
