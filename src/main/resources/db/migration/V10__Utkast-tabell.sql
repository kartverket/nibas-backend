CREATE TABLE UTKAST
(
    id               text  NOT NULL,
    version          smallint NOT NULL,
    navn             text  NOT NULL,
    gyldig_fra       date,
    endringstype     text  NOT NULL,
    status           text  NOT NULL,
    utkast           jsonb NOT NULL,
    opprettet_dato   timestamp NOT NULL,
    oppdateringsdato date  NOT NULL,
    endret_av        text,
    primary key (id)
);

