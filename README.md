Rehearser

This is [Rehearser](https://github.com/muep/rehearser/), a tool for
managing keeping track of things to practice regularly.

# How to run

To run the program based on this source code, these dependencies are
needed:

- Java 21
- [Clojure](https://clojure.org/index)

The build process will additionally fetch dependencies listed in
[deps.edn](deps.edn), as well as their transitive dependencies.

To just run the program in this source directory, one can invoke
`clojure` like this:

```
$ clojure -M -m rehearser.main
Subcommand is required
usage: rehearser [options]
options:
  -h, --help                            Display help and exit
      --database DATABASE Database URL
subcommands:
    account-add: Add an account
    account-list: List accounts
    account-passwd: Reset password of account
    db-check: Check database connectivity
    db-reset: Reset database contents
    serve: Run the http service
$
```

Some subcommands have their own help options, but note that not all of
them have them!

For more options on how to start the backend, see its documentation in
[backend/README.md](backend/README.md).

# Source code layout

The top level directory contains this README and packaging
information. It is also a location where some build output and other
generated stuff goes, though those would be better off elsewhere.

There is a simple front-end in the `front` directory. See its
[README](front/README.md) for details.

The `backend` directory contains the backend, written mainly in
Clojure. When built, it will typically include a copy of the frontend
that it can serve alongside the HTTP API. The frontend could be
delivered through CDN as well.

In case a second attempt at a frontend is started, it should be easy
to add it as a new subdirectory in the project root, and change the
load path to point there.
