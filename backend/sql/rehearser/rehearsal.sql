-- name: rehearsal-insert<!
insert into rehearsal (
    "account-id",
    "start-time",
    duration,
    title,
    description
) values (
    :account-id,
    to_timestamp(:start-time),
    :duration,
    :title,
    :description
)
returning
    id,
    "account-id",
    extract(epoch from  "start-time")::bigint as "start-time",
    extract(epoch from "start-time" + duration)::bigint as "end-time",
    extract(seconds from duration)::bigint as duration,
    duration is null as "is-open",
    title,
    description;

--name: rehearsal-select
select
    id,
    "account-id",
    extract(epoch from "start-time")::bigint as "start-time",
    extract(epoch from "start-time" + duration)::bigint as "end-time",
    extract(epoch from coalesce(duration, now() - "start-time"))::bigint as duration,
    duration is null as "is-open",
    title,
    description
from
    rehearsal
where
    "account-id" = :account-id
order by "start-time" asc;

--name: rehearsal-select-open
select
    id,
    "account-id",
    extract(epoch from "start-time")::bigint as "start-time",
    extract(epoch from "start-time" + duration)::bigint as "end-time",
    extract(epoch from coalesce(duration, now() - "start-time")) as duration,
    duration is null as "is-open",
    title,
    description
from
    rehearsal
where
    "account-id" = :account-id and duration is null;

--name: rehearsal-select-by-id
select
    entry.id,
    rehearsal."account-id",
    rehearsal.id as "rehearsal-id",
    "exercise-id",
    "variant-id",
    extract(epoch from entry."entry-time") as "entry-time",
    remarks,

    extract(epoch from rehearsal."start-time")::bigint as "rehearsal-start-time",
    extract(epoch from rehearsal."start-time" + rehearsal.duration)::bigint as "rehearsal-end-time",
    extract(epoch from coalesce(rehearsal.duration, now() - rehearsal."start-time"))::bigint as "rehearsal-duration",
    duration is null as "is-open",
    rehearsal.title as "rehearsal-title",
    rehearsal.description as "rehearsal-description"
from
    rehearsal left outer join entry
        on rehearsal.id = entry."rehearsal-id"
where
    rehearsal."account-id" = :account-id and
    rehearsal.id = :id;

--name: rehearsal-delete-by-id!
delete from rehearsal
where
    "account-id" = :account-id and
    id = :id;

--name: entry-insert<!
insert into entry (
    "account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    "entry-time",
    remarks
)
values (
    :account-id,
    :rehearsal-id,
    :exercise-id,
    :variant-id,
    now(),
    :remarks
)
returning
    id,
    "account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    extract(epoch from  "entry-time") as "entry-time",
    remarks;

--name: entry-select
select
    id,
    "account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    extract(epoch from  "entry-time") as "entry-time",
    remarks
from entry
where
    "account-id" = :account-id and
    id = :id;

--name: entry-delete-by-rehearsal-id!
delete from entry
where
    "account-id" = :account-id and
    "rehearsal-id" = :id;

--name: entry-delete!
delete from entry
where
    "account-id" = :account-id and
    "rehearsal-id" = :rehearsal-id and
    id = :id;
