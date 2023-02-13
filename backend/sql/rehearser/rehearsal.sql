-- name: rehearsal-insert<!
insert into rehearsal (
    "account-id",
    "start-time",
    duration,
    title,
    description
) values (
    :account-id,
    :start-time,
    :duration,
    :title,
    :description
)
returning
    id,
    "account-id",
    "start-time",
    "start-time" + duration * interval '1 second' as "end-time",
    duration,
    duration is null as "is-open",
    title,
    description;

--name: rehearsal-select
select
    id,
    "account-id",
    "start-time",
    "start-time" + duration * interval '1 second' as "end-time",
    duration,
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
    "start-time",
    "start-time" + duration * interval '1 second' as "end-time",
    duration,
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
    "entry-time",
    remarks,

    rehearsal."start-time" as "rehearsal-start-time",
    rehearsal."start-time" + rehearsal.duration * interval '1 second' as "rehearsal-end-time",
    rehearsal.duration as "rehearsal-duration",
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
