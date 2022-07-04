drop table if exists entry;
drop index if exists rehearsal_single_null_idx;
drop table if exists rehearsal;
drop table if exists variant;
drop table if exists exercise;
drop table if exists account;
drop table if exists rehearser_schema;

create table rehearser_schema (version integer unique not null);

create table account (
    id serial unique not null,
    name text unique not null,
    pwhash text not null
);

create table exercise (
    id serial unique not null,
    account_id integer not null references account(id),
    title text not null,
    description text not null,
    unique (account_id, title)
);

create table variant (
    id serial unique not null,
    account_id integer not null references account(id),
    title text not null,
    description text not null,
    unique (account_id, title)
);

-- This is kind of a collection of rehearsal entries that
-- occurred together
create table rehearsal (
    id serial unique not null,
    account_id integer not null references account(id),
    start_time timestamptz not null,
    duration interval,
    title text not null,
    description text not null
);

create unique index rehearsal_single_null_idx
    on rehearsal (account_id, (duration is null))
    where duration is null;

create table entry (
    id serial unique not null,
    rehearsal_id integer not null references rehearsal(id),
    exercise_id integer not null references exercise(id),
    variant_id integer not null references variant(id),
    entry_time timestamptz not null,
    remarks text not null
);

insert into rehearser_schema(version) values (1);
