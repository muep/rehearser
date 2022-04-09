-- name: account-create!
insert into account (name, pwhash)
  values (:name, :pwhash)
  returning id;

-- name: account-force-passwd!
update account
  set pwhash = :pwhash
  where name = :name;

-- name: account-login
select id
from account
where name = :name and
      pwhash = :pwhash;

-- name: select-accounts
select
  id,
  name
from account
order by id;
