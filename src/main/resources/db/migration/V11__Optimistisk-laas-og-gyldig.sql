-- Optimistisk låsing, gyldig fra og gyldig til
update nasjon set gyldigfra = now() where gyldigfra is null;
alter table nasjon
    alter column gyldigfra set not null,
    add column version smallint NOT NULL default 1;

update fylke set gyldigfra = now() where gyldigfra is null;
alter table fylke
    alter column gyldigfra set not null,
    add column version smallint NOT NULL default 1;

update kommune set gyldigfra = now() where gyldigfra is null;
alter table kommune
    alter column gyldigfra set not null,
    add column version smallint NOT NULL default 1;

update grunnkrets set gyldigfra = now() where gyldigfra is null;
alter table grunnkrets
    alter column gyldigfra set not null,
    add column version smallint NOT NULL default 1;

update stemmekrets set gyldigfra = now() where gyldigfra is null;
alter table stemmekrets
    alter column gyldigfra set not null,
    add column version smallint NOT NULL default 1;

update grense set gyldigfra = now() where gyldigfra is null;
alter table grense
    alter column gyldigfra set not null,
    add column version smallint NOT NULL default 1;

alter table administrativenhetnavn
    add column version smallint NOT NULL default 1,
    add column gyldigfra date default now(),
    add column gyldigtil date;

alter table dokumentasjonsreferanse
    add column version smallint NOT NULL default 1,
    add column gyldigfra date default now(),
    add column gyldigtil date;

-- Fjern default-verdi, den skal settes av koden.
alter table nasjon
    alter column version set default null;

alter table fylke
    alter column version set default null;

alter table kommune
    alter column version set default null;

alter table grunnkrets
    alter column version set default null;

alter table stemmekrets
    alter column version set default null;

alter table grense
    alter column version set default null;

alter table administrativenhetnavn
    alter column gyldigfra set default null,
    alter column version set default null;

alter table dokumentasjonsreferanse
    alter column gyldigfra set default null,
    alter column version set default null;
