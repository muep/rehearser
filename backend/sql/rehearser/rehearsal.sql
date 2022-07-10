-- name: rehearsal-start<!
insert into rehearsal (
    "account-id",
    "start-time",
    title,
    description
) values (
    :account-id,
    now(),
    :title,
    :description
)
returning id, title, description, extract(epoch from  "start-time") as "start-time";

-- name: rehearsal-end!
update rehearsal
set duration = now() - "start-time"
where
    duration is null and
    "account-id" = :account-id;

--name: rehearsal-select
select
    "account-id",
    extract(epoch from "start-time") as "start-time",
    extract(epoch from "start-time" + duration) as "end-time",
    extract(epoch from coalesce(duration, now() - "start-time")) as duration,
    duration is null as "is-open",
    title,
    description
from
    rehearsal
where
    "account-id" = :account-id
order by "start-time" asc;
