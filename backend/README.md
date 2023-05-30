Rehearser backend

# Development tasks
How to accomplish routine tasks

## Development database

Via docker:

    docker run -e POSTGRES_PASSWORD=postgres \
        -d \
        --name rehearser-db \
        -p 5432:5432 postgres

## Run tests

Run this:

    clojure -M:test

Or to limit what tests are to be run, additionally pass in a filter string

    clojure -M:test db-url

## Build self-container JAR archive

    ./package.sh
