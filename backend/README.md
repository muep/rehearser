Rehearser backend

# Development tasks
How to accomplish routine tasks

## Run tests

Run this:

    clojure -M:test

Or to limit what tests are to be run, additionally pass in a filter string

    clojure -M:test db-url

## Build self-container JAR archive

    ./package.sh
