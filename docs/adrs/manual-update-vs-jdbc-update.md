# Manual updates or update! function

It seems preferable to use `clojure.java.jdbc/update!` function when
it works. One case where it does not work, though, if there are
columns of type `interval` or `timestamptz` in the table.

This could be addressed by using e.g. `java.time.Duration` and
`java.time.Instant` instead of plain integers, but it takes some work
to set up the coercions both on the HTTP<->clojure interface and also
in clojure<->postgresql one.
