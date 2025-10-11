drop table if exists entry;
drop index if exists rehearsal_single_null_idx;
drop table if exists rehearsal;
drop table if exists variant;
drop table if exists exercise;
drop table if exists account;
drop table if exists rehearser_schema;

create table rehearser_schema (version integer unique not null);

create table account (
    id integer generated always as identity,
    name text unique not null,
    pwhash text not null,
    primary key (id)
);

-- Something that can be practiced
create table exercise (
    id integer generated always as identity,
    "account-id" integer not null references account(id),
    title text not null,
    description text not null,
    primary key ("account-id", id),
    unique (id),
    unique ("account-id", title)
);

-- Different ways of performing an exercise - e.g. different
-- instruments.
create table variant (
    id integer generated always as identity,
    "account-id" integer not null references account(id),
    title text not null,
    description text not null,
    primary key ("account-id", id),
    unique (id),
    unique ("account-id", title)
);

-- This is kind of a collection of rehearsal entries that
-- occurred together
create table rehearsal (
    id integer generated always as identity,
    "account-id" integer not null references account(id),
    "start-time" timestamptz not null,
    -- Number of seconds. Null means the rehearsal is still ongoing
    duration integer,
    title text not null,
    description text not null,
    primary key ("account-id", id),
    unique (id)
);

-- Only one ongoing rehearsal per account
create unique index rehearsal_single_null_idx
    on rehearsal ("account-id", (duration is null))
    where duration is null;

-- Entry for marking that some variant of some exercise was performed
-- exactly then and then as part of some rehearsal.
create table entry (
    id integer generated always as identity,
    "account-id" integer not null,
    "rehearsal-id" integer not null,
    "exercise-id" integer not null,
    "variant-id" integer not null,
    "entry-time" timestamptz not null,
    remarks text not null,
    primary key ("account-id", id),
    foreign key ("account-id") references account(id),
    foreign key ("rehearsal-id", "account-id") references rehearsal(id, "account-id"),
    foreign key ("exercise-id", "account-id") references exercise(id, "account-id"),
    foreign key ("variant-id", "account-id") references variant(id, "account-id")
);

insert into rehearser_schema(version) values (1);
