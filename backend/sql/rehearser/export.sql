-- :name account-for-export :? :1
select
  id,
  name
from account
where id = :account-id;

-- :name exercises-for-export :? :*
select
  id,
  title,
  description
from exercise
where "account-id" = :account-id;

-- :name variants-for-export :? :*
select
  id,
  title,
  description
from variant
where "account-id" = :account-id;

-- :name rehearsals-for-export :? :*
select
  id,
  "start-time",
  duration,
  title,
  description
from rehearsal
where "account-id" = :account-id;

-- :name entries-for-export :? :*
select
  id,
  "rehearsal-id",
  "exercise-id",
  "variant-id",
  "entry-time",
  remarks
from entry
where "account-id" = :account-id;
