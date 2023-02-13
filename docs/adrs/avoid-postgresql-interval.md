Avoid using the `interval` type from postgresql

While on the surface the `interval` type seems like intended for this
kind of use, it uses a complex calendar-style structure for tracking
its value.

Database-sid `interval` translates to jdbc as
[org.postgresql.util.PGInterval](https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/util/PGInterval.html),
which can be complicated to translate into a plain number of seconds
that we expect to prefer to work with.
