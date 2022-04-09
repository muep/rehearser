-- name: task-get
select
    id,
    name
from task
where account_id = :account-id;

-- name: task-add
insert into task (name) values (:name) returning id;

-- name: sesn-open
insert into sesn (account_id, start_time, duration, title)
  values (:account-id, now(), null, :title);

-- name: sesn-close
update sesn
  set duration = now() - start_time
  where account_id = :account-id and duration is null;

-- name: rehearsal-add
insert into rehearsal (account_id, task_id)
values (
