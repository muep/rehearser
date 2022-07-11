-- name: task-get
select
    id,
    name
from task
where "account-id" = :account-id;

-- name: task-add
insert into task (name) values (:name) returning id;

-- name: sesn-open
insert into sesn ("account-id", "start-time", duration, title)
  values (:account-id, now(), null, :title);

-- name: sesn-close
update sesn
  set duration = now() - "start-time"
  where "account-id" = :account-id and duration is null;

-- name: rehearsal-add
insert into rehearsal ("account-id", "task-id")
values ();
