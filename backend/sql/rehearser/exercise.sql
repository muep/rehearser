-- name: exercise-insert<!
insert into exercise (
    "account-id",
    title,
    description
) values (
    :account-id,
    :title,
    :description
)
returning id, "account-id", title, description;

-- name: exercise-select-all
select id, "account-id", title, description
from exercise
where "account-id" = :account-id;

--name: exercise-by-id
select id, "account-id", title, description
from exercise
where "account-id" = :account-id and
      id = :id;

--name: exercise-delete!
delete from exercise
where "account-id" = :account-id and
      id = :id;

--name: exercise-update<!
update exercise
set
  title = coalesce(:title, title),
  description = coalesce(:description, description)
where "account-id" = :account-id and
      id = :id
returning id, "account-id", title, description;
