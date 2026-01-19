-- :name exercise-insert! :<! :1
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

-- :name exercise-select-all :? :*
select id, "account-id", title, description
from exercise
where "account-id" = :account-id;

-- :name exercise-by-id :? :*
select id, "account-id", title, description
from exercise
where "account-id" = :account-id and
      id = :id;

-- :name entries-by-exercise-id :? :*
select
    en.*,
    rh.title "rehearsal-title"
from entry en
join exercise ex on en."exercise-id" = ex.id
join rehearsal rh on en."rehearsal-id" = rh.id
where ex."account-id" = :account-id and ex.id = :id;

-- :name exercise-delete! :! :n
delete from exercise
where "account-id" = :account-id and
      id = :id;

-- :name exercise-update! :<! :1
update exercise
set
  title = coalesce(:title, title),
  description = coalesce(:description, description)
where "account-id" = :account-id and
      id = :id
returning id, "account-id", title, description;

-- :name search-exercises :? :*
select id, title
from exercise
where "account-id" = :account-id and
      lower(title) like '%' || lower(:query) || '%'
order by title
limit 50;

-- :name find-recent-exercises :? :*
select e.id, e.title, max(en."entry-time") as "latest-time"
from entry en
join exercise e on en."exercise-id" = e.id
where en."account-id" = :account-id
group by e.id, e.title
order by "latest-time" desc
limit :limit;

-- :name find-frequent-exercises :? :*
select e.id, e.title, count(*) as frequency
from entry en
join exercise e on en."exercise-id" = e.id
where en."account-id" = :account-id
group by e.id, e.title
order by frequency desc
limit :limit;
