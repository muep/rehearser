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
