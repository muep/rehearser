Rehearser backend

# Development tasks
How to accomplish routine tasks

## Development database

Via docker:

    docker run -e POSTGRES_PASSWORD=postgres \
        -d \
        --name rehearser-db \
        --tmpfs /var/lib/postgresql:size=1g \
        -p 5432:5432 postgres

## Run tests

In a fresh environment, prepare the dependencies first; no root
privileges are needed. This installs JDK 21, the Clojure CLI and a
loopback-only PostgreSQL into a user-writable directory and prints
the environment to use:

    # (in project root)
    ./prepare-test-deps.py

Run all tests:

    # (in project root)
    clojure -M:test

Or run tests matching a specific pattern by passing it as an argument:

    # (in project root)
    clojure -M:test db

This will run any test whose fully qualified name contains "db", such as
`rehearser.db-test` and `rehearser.db-url-test`.

## Build self-container JAR archive

    # (in project root)
    ./package.sh
