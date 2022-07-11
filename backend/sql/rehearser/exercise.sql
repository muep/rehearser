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
