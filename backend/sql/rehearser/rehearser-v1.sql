drop table if exists rehearsal;
drop index if exists sesn_single_null_idx;
drop table if exists sesn;
drop table if exists task;
drop table if exists account;
drop table if exists rehearser_schema;

create table rehearser_schema (version integer unique not null);

create table account (
    id serial unique not null,
    name text unique not null,
    pwhash text not null
);

create table task (
    id serial unique not null,
    account_id integer not null references account(id),
    title text not null,
    description text not null,
    unique (account_id, title)
);

create table sesn (
    id serial unique not null,
    account_id integer not null references account(id),
    start_time timestamptz not null,
    duration interval,
    title text not null,
    description text not null
);

create unique index sesn_single_null_idx
    on sesn (account_id, (duration is null))
    where duration is null;

create table rehearsal (
    id serial unique not null,
    sesn_id integer not null references sesn(id),
    task_id integer not null references task(id),
    rehearse_time timestamptz not null,
    remarks text not null
);

insert into rehearser_schema(version) values (1);
