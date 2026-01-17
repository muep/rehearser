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
    duration is null as "is-open";

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

-- :name rehearsal-select-by-id :? :1
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
    "account-id" = :account-id and
    "id" = :rehearsal-id
limit 1;

-- :name rehearsal-update! :<! :1
update rehearsal
set
    --~ (clojure.string/join ", " (map (fn [[k _]] (str "\"" (name k) "\" = " k )) (dissoc params :id :account-id)))
where
    "account-id" = :account-id and
    id = :id
returning
    id,
    "account-id",
    "start-time",
    "start-time" + (duration * interval '1 second') as "end-time",
    duration,
    duration is null as "is-open",
    title,
    description;

-- :name rehearsal-close! :<! :1
update rehearsal
set
    duration = extract(epoch from (:end-time - "start-time"))
where
    "account-id" = :account-id and
    id = :id;

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

-- :name entry-update! :<! :1
update entry
set
    --~ (clojure.string/join ", " (map (fn [[k _]] (str "\"" (name k) "\" = " k )) (dissoc params :id :account-id)))
where
    "account-id" = :account-id and
    id = :id
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

-- :name entry-select-with-title :? :*
select
    entry.id,
    entry."account-id",
    "rehearsal-id",
    "exercise-id",
    ex.title as "exercise-title",
    "variant-id",
    vr.title as "variant-title",
    "entry-time",
    remarks
from
    entry
        join exercise ex on (ex."account-id" = entry."account-id" and ex.id = entry."exercise-id")
        join variant vr on (vr."account-id" = entry."account-id" and vr.id = entry."variant-id")
where
    entry."account-id" = :account-id and
    entry."rehearsal-id" = :rehearsal-id
order by "entry-time" asc;

-- :name entry-delete! :! :n
delete from entry
where
    "account-id" = :account-id and
    id = :id;
