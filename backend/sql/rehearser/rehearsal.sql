-- :name rehearsal-insert! :<! :1
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
    title,
    description,
    "start-time",
    duration,
    duration is not null as "is-open";

-- :name rehearsal-end! :! :n
update rehearsal
set duration = now() - "start-time"
where
    duration is null and
    "account-id" = :account-id;

-- :name rehearsal-select :? :*
select
    id,
    "account-id",
    "start-time",
    "start-time" + (duration * interval '1 second') as "end-time",
    duration,
    duration is null as "is-open",
    title,
    description
from
    rehearsal
where
    "account-id" = :account-id
order by "start-time" asc;

-- :name rehearsal-update! :<! :1
update rehearsal
set
    "start-time" = :start-time,
    duration = :duration,
    title = :title,
    description = :description
where
    "account-id" = :account-id and
    id = :id
returning
    "account-id",
    "start-time",
    "start-time" + (duration * interval '1 second') as "end-time",
    duration,
    duration is null as "is-open",
    title,
    description;

-- :name entry-insert! :<! :1
insert into entry (
    "account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    "entry-time",
    remarks
) values (
    :account-id,
    :rehearsal-id,
    :exercise-id,
    :variant-id,
    :entry-time,
    :remarks
)
returning
    id,
    "account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    "entry-time",
    remarks;

-- :name entry-select :? :*
select
    id,
    "account-id",
    "rehearsal-id",
    "exercise-id",
    "variant-id",
    "entry-time",
    remarks
from
    entry
where
    "account-id" = :account-id and
    "rehearsal-id" = :rehearsal-id
order by "entry-time" asc;
