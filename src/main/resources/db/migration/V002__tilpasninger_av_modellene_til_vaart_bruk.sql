-- Optimistisk låsing --
alter table nasjon
    add column version smallint NOT NULL default 1;

alter table fylke
    add column version smallint NOT NULL default 1;

alter table kommune
    add column version smallint NOT NULL default 1;

alter table grunnkrets
    add column version smallint NOT NULL default 1;

alter table stemmekrets
    add column version smallint NOT NULL default 1;

alter table grense
    add column version smallint NOT NULL default 1;

alter table administrativenhetnavn
    add column version smallint NOT NULL default 1;

alter table dokumentasjonsreferanse
    add column version smallint NOT NULL default 1;

-- Fjern default-verdi, den skal settes av koden.
alter table nasjon
    alter column version drop default;

alter table fylke
    alter column version drop default;

alter table kommune
    alter column version drop default;

alter table grunnkrets
    alter column version drop default;

alter table stemmekrets
    alter column version drop default;

alter table grense
    alter column version drop default;

alter table administrativenhetnavn
    alter column version drop default;

alter table dokumentasjonsreferanse
    alter column version drop default;

-- Utledet fra --
ALTER TABLE stemmekrets
    add column utledetfra text;
ALTER TABLE stemmekrets
    ADD FOREIGN KEY (utledetfra) REFERENCES stemmekrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grunnkrets
    add column utledetfra text;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (utledetfra) REFERENCES grunnkrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE kommune
    add column utledetfra text;
ALTER TABLE kommune
    ADD FOREIGN KEY (utledetfra) REFERENCES kommune (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE fylke
    add column utledetfra text;
ALTER TABLE fylke
    ADD FOREIGN KEY (utledetfra) REFERENCES fylke (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- retning og rekkefølge på grensekobling

ALTER TABLE grunnkrets_grense
    ADD COLUMN retning_med_klokken boolean not null default true,
    ADD COLUMN rekkefoelge         int     not null default 0,
    ADD COLUMN flate_indeks        int     not null default 0,
    ADD COLUMN hull_indeks         int;

CREATE TABLE stemmekrets_grense
(
    stemmekrets_fk      text,
    grense_fk           text,
    retning_med_klokken boolean not null default true,
    rekkefoelge         int     not null default 0,
    flate_indeks        int     not null default 0,
    hull_indeks         int
) WITH (OIDS= FALSE);

ALTER TABLE stemmekrets_grense
    ADD PRIMARY KEY (stemmekrets_fk, grense_fk),
    ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
    ADD FOREIGN KEY (grense_fk) REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Fjerne ubrukte egenskaper --
alter table kommune
    drop constraint kommune_land_fkey;
alter table fylke
    drop constraint fylke_land_fkey;
alter table nasjon
    drop constraint nasjon_land_fkey;
alter table kommune
    drop column land;
alter table kommune
    drop column administrativkode;
alter table fylke
    drop column land;
alter table fylke
    drop column administrativkode;
alter table nasjon
    drop column land;
alter table nasjon
    drop column administrativkode;

ALTER TABLE administrativenhetnavn
    drop column navnerom;
ALTER TABLE dokumentasjonsreferanse
    drop column navnerom;
ALTER TABLE delomraade
    drop column navnerom;
ALTER TABLE delomraade
    drop column versjonid;
ALTER TABLE fylke
    drop column navnerom;
ALTER TABLE fylke
    drop column versjonid;
ALTER TABLE grense
    drop column navnerom;
ALTER TABLE grense
    drop column versjonid;
ALTER TABLE grunnkrets
    drop column navnerom;
ALTER TABLE grunnkrets
    drop column versjonid;
ALTER TABLE kommune
    drop column navnerom;
ALTER TABLE kommune
    drop column versjonid;
ALTER TABLE nasjon
    drop column navnerom;
ALTER TABLE nasjon
    drop column versjonid;
ALTER TABLE stemmekrets
    drop column navnerom;
ALTER TABLE stemmekrets
    drop column versjonid;

-- Fjern tabeller som representerer eksterne kodelister --
DROP TABLE fylkesnummer CASCADE;
DROP TABLE kommunenummer CASCADE;
DROP TABLE maalemetode CASCADE;

-- Legger til egenskaper vi trenger --
alter table grense
    add column info text;

-- Slettet-kolonne --
ALTER TABLE administrativenhet add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE administrativenhetnavn add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE dokumentasjonsreferanse add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE dokumentasjonsreferanse_dokumentlink add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE dokumentasjonsreferanse_internreferansekartverket add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE grense add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE fylke add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE kommune add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE nasjon add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE delomraade add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE grunnkrets add column slettet boolean NOT NULL DEFAULT 'FALSE';
ALTER TABLE stemmekrets add column slettet boolean NOT NULL DEFAULT 'FALSE';

-- Utkast --
CREATE TABLE UTKAST
(
    id               text      NOT NULL,
    version          smallint  NOT NULL,
    navn             text      NOT NULL,
    gyldig_fra       date,
    typeendring     text      NOT NULL,
    status           text      NOT NULL,
    utkast           jsonb,
    opprettet_dato   timestamp NOT NULL,
    endretdato       date      NOT NULL,
    endretav         text,
    primary key (id)
);

ALTER TABLE UTKAST
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;

