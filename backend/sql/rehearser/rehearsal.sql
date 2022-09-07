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
returning
    id,
    title,
    description,
    extract(epoch from  "start-time") as "start-time",
    extract(seconds from duration) as duration;

-- name: rehearsal-end<!
update rehearsal
set duration = now() - "start-time"
where
    duration is null and
    "account-id" = :account-id
returning
    id,
    title,
    description,
    extract(epoch from  "start-time") as "start-time",
    extract(seconds from duration) as duration;

--name: rehearsal-select
select
    id,
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

--name: rehearsal-select-open
select
    id,
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
    "account-id" = :account-id and duration is null;

--name: rehearsal-select-by-id
select
    entry.id,
    entry."account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    extract(epoch from entry."entry-time") as "entry-time",
    remarks,

    extract(epoch from rehearsal."start-time") as "rehearsal-start-time",
    extract(epoch from rehearsal."start-time" + rehearsal.duration) as "rehearsal-end-time",
    extract(epoch from coalesce(rehearsal.duration, now() - rehearsal."start-time")) as "rehearsal-duration",
    rehearsal.title as "rehearsal-title",
    rehearsal.description as "rehearsal-description"
from
    rehearsal join entry
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
