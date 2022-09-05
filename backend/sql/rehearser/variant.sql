-- name: variant-insert<!
insert into variant (
    "account-id",
    title,
    description
) values (
    :account-id,
    :title,
    :description
)
returning id, "account-id", title, description;

-- name: variant-select-all
select id, "account-id", title, description
from variant
where "account-id" = :account-id;

--name: variant-by-id
select id, "account-id", title, description
from variant
where "account-id" = :account-id and
      id = :id;

--name: variant-delete!
delete from variant
where "account-id" = :account-id and
      id = :id;

--name: variant-update<!
update variant
set
  title = coalesce(:title, title),
  description = coalesce(:description, description)
where "account-id" = :account-id and
      id = :id
returning id, "account-id", title, description;
