CREATE TABLE dokumentasjonsreferanse
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Dokumentasjonsreferanse',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    rettskildetittel           text                     NOT NULL,
    rettskildeid               text,
    hjemmel                    text,
    fastsettingsmyndighet      text,
    fastsettingsdato           date                     NOT NULL
)
    WITH (OIDS= FALSE);

CREATE TABLE grense_dokumentasjonsreferanse
(
    grense_fk                  text,
    dokumentasjonsreferanse_fk text
)
    WITH (OIDS= FALSE);

CREATE TABLE fylkesnummer
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);


CREATE TABLE kommunenummer
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);


CREATE TABLE typeavgrensning
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);

INSERT INTO typeavgrensning (identifier, description)
VALUES ('Riksgrense', 'Riksgrense');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('Territorialgrense', 'Territorialgrense');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('AvtaltAvgrensningslinje', 'AvtaltAvgrensningslinje');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('Kommunegrense', 'Kommunegrense');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('Fylkesgrense', 'Fylkesgrense');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('Grunnkretsgrense', 'Grunnkretsgrense');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('Delområdegrense', 'Delområdegrense');
INSERT INTO typeavgrensning (identifier, description)
VALUES ('Stemmekretsgrense', 'Stemmekretsgrense');

CREATE TABLE typeendring
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);

INSERT INTO typeendring (identifier, description)
VALUES ('kvalitetsheving', 'kvalitetsheving');
INSERT INTO typeendring (identifier, description)
VALUES ('retting', 'retting');
INSERT INTO typeendring (identifier, description)
VALUES ('vedtattDeling', 'vedtattDeling');
INSERT INTO typeendring (identifier, description)
VALUES ('vedtattSammenslåing', 'vedtattSammenslåing');
INSERT INTO typeendring (identifier, description)
VALUES ('vedtattGrensejustering', 'vedtattGrensejustering');
INSERT INTO typeendring (identifier, description)
VALUES ('vedtattSletting', 'vedtattSletting');
INSERT INTO typeendring (identifier, description)
VALUES ('fastsetting', 'fastsetting');
INSERT INTO typeendring (identifier, description)
VALUES ('navneendring', 'navneendring');
INSERT INTO typeendring (identifier, description)
VALUES ('nummerendring', 'nummerendring');
INSERT INTO typeendring (identifier, description)
VALUES ('import', 'import');

CREATE TABLE landkode
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);

INSERT INTO landkode (identifier, description)
VALUES ('BV', 'BouvetIsland');
INSERT INTO landkode (identifier, description)
VALUES ('DK', 'Denmark');
INSERT INTO landkode (identifier, description)
VALUES ('FO', 'FaroeIslands');
INSERT INTO landkode (identifier, description)
VALUES ('FI', 'Finland');
INSERT INTO landkode (identifier, description)
VALUES ('GL', 'Greenland');
INSERT INTO landkode (identifier, description)
VALUES ('IS', 'Iceland');
INSERT INTO landkode (identifier, description)
VALUES ('NO', 'Norway');
INSERT INTO landkode (identifier, description)
VALUES ('RU', 'RussianFederation');
INSERT INTO landkode (identifier, description)
VALUES ('SJ', 'SvalbardAndJanMayen');
INSERT INTO landkode (identifier, description)
VALUES ('SE', 'Sweden');
INSERT INTO landkode (identifier, description)
VALUES ('GB', 'UnitedKingdom');

CREATE TABLE stemmekrets
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Stemmekrets',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833) NOT NULL,
    posisjon                   geometry(Point, 25833)   NOT NULL,
    stemmekretsnummer          text                     NOT NULL,
    stemmekretsnavn            text                     NOT NULL,
    tellekretsnummer           text,
    tellekretsnavn             text,
    valgdistriktsnummer        text,
    kommune_fk                 text
)
    WITH (OIDS= FALSE);

CREATE TABLE stemmekrets_stemmekretsgrense
(
    stemmekrets_fk       text,
    stemmekretsgrense_fk text
)
    WITH (OIDS= FALSE);

CREATE TABLE administrativenhet
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'AdministrativEnhet',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833),
    posisjon                   geometry(Point, 25833),
    land                       text,
    administrativkode          text
)
    WITH (OIDS= FALSE);

CREATE TABLE administrativenhetnavn
(
    objid                         text                     NOT NULL,
    objtype                       text DEFAULT 'AdministrativEnhetNavn',
    lokalid                       text                     NOT NULL,
    navnerom                      text                     NOT NULL,
    versjonid                     text,
    gyldigfra                     timestamp with time zone NOT NULL,
    gyldigtil                     timestamp with time zone,
    datafangstdato                timestamp with time zone,
    foerstedigitaliseringsdato    timestamp with time zone NOT NULL,
    maalemetode                   text,
    noeyaktighet                  int,
    synbarhet                     text,
    maalemetodehoeyde             text,
    noeyaktighethoeyde            int,
    maksimaltavvik                int,
    opphav                        text,
    endretdato                    timestamp with time zone NOT NULL,
    endretav                      text                     NOT NULL,
    typeendring                   text                     NOT NULL,
    navn                          text                     NOT NULL,
    spraak                        text                     NOT NULL,
    rekkefoelge                   int,
    administrativenhet_fk         text,
    administrativenhet_fylke_fk   text,
    administrativenhet_nasjon_fk  text,
    administrativenhet_kommune_fk text
)
    WITH (OIDS= FALSE);

CREATE TABLE administrativtnivaa
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'AdministrativtNivå',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    nivaanummer                text                     NOT NULL
)
    WITH (OIDS= FALSE);

CREATE TABLE fylke
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Fylke',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833),
    posisjon                   geometry(Point, 25833),
    land                       text,
    administrativkode          text,
    samiskforvaltningsomraade  boolean,
    fylkesnummer               text                     NOT NULL,
    nasjon_fk                  text
)
    WITH (OIDS= FALSE);

CREATE TABLE kommune
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Kommune',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833),
    posisjon                   geometry(Point, 25833),
    land                       text,
    administrativkode          text,
    samiskforvaltningsomraade  boolean,
    kommunenummer              text                     NOT NULL,
    fylke_fk                   text
)
    WITH (OIDS= FALSE);

CREATE TABLE nasjon
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Nasjon',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833),
    posisjon                   geometry(Point, 25833),
    land                       text,
    administrativkode          text
)
    WITH (OIDS= FALSE);

CREATE TABLE nivaanummer
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);

INSERT INTO nivaanummer (identifier, description)
VALUES ('FørsteNivå', 'FørsteNivå');
INSERT INTO nivaanummer (identifier, description)
VALUES ('AndreNivå', 'AndreNivå');
INSERT INTO nivaanummer (identifier, description)
VALUES ('TredjeNivå', 'TredjeNivå');
INSERT INTO nivaanummer (identifier, description)
VALUES ('FjerdeNivå', 'FjerdeNivå');

CREATE TABLE spraakkode
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);

INSERT INTO spraakkode (identifier, description)
VALUES ('eng', 'English');
INSERT INTO spraakkode (identifier, description)
VALUES ('fkv', 'KvenFinnish');
INSERT INTO spraakkode (identifier, description)
VALUES ('smj', 'LuleSami');
INSERT INTO spraakkode (identifier, description)
VALUES ('sme', 'NorthernSami');
INSERT INTO spraakkode (identifier, description)
VALUES ('nor', 'Norwegian');
INSERT INTO spraakkode (identifier, description)
VALUES ('sma', 'SouthernSami');

CREATE TABLE maalemetode
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);


CREATE TABLE synbarhet
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);

INSERT INTO synbarhet (identifier, description)
VALUES ('fulltSynligOgGjenfinnbarITerrenget', 'fulltSynligOgGjenfinnbarITerrenget');
INSERT INTO synbarhet (identifier, description)
VALUES ('dårligGjenfinnbarITerrenget', 'dårligGjenfinnbarITerrenget');
INSERT INTO synbarhet (identifier, description)
VALUES ('middelsSynligIFlybilde', 'middelsSynligIFlybilde');
INSERT INTO synbarhet (identifier, description)
VALUES ('dårligSynligIFlybilde', 'dårligSynligIFlybilde');

CREATE TABLE maalemetodehoeyde
(
    identifier  text NOT NULL,
    description text
)
    WITH (OIDS= FALSE);


CREATE TABLE grense
(
    objid                      text                        NOT NULL,
    objtype                    text DEFAULT 'Grense',
    lokalid                    text                        NOT NULL,
    navnerom                   text                        NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone    NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone    NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone    NOT NULL,
    endretav                   text                        NOT NULL,
    typeendring                text                        NOT NULL,
    grense                     geometry(Linestring, 25833) NOT NULL,
    avgrensningstype           text                        NOT NULL
)
    WITH (OIDS= FALSE);

CREATE TABLE grunnkrets_grense
(
    grunnkrets_fk text,
    grense_fk     text
)
    WITH (OIDS= FALSE);

CREATE TABLE delomraade
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Delområde',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833),
    posisjon                   geometry(Point, 25833),
    delomraadenummer           text                     NOT NULL,
    delomraadenavn             text,
    kommunenummer              text                     NOT NULL
)
    WITH (OIDS= FALSE);

CREATE TABLE grunnkrets
(
    objid                      text                     NOT NULL,
    objtype                    text DEFAULT 'Grunnkrets',
    lokalid                    text                     NOT NULL,
    navnerom                   text                     NOT NULL,
    versjonid                  text,
    gyldigfra                  timestamp with time zone NOT NULL,
    gyldigtil                  timestamp with time zone,
    datafangstdato             timestamp with time zone,
    foerstedigitaliseringsdato timestamp with time zone NOT NULL,
    maalemetode                text,
    noeyaktighet               int,
    synbarhet                  text,
    maalemetodehoeyde          text,
    noeyaktighethoeyde         int,
    maksimaltavvik             int,
    opphav                     text,
    endretdato                 timestamp with time zone NOT NULL,
    endretav                   text                     NOT NULL,
    typeendring                text                     NOT NULL,
    omraade                    geometry(MultiPolygon, 25833),
    posisjon                   geometry(Point, 25833),
    grunnkretsnummer           text                     NOT NULL,
    grunnkretsnavn             text,
    delomraadeinndeling_fk     text,
    kommune_fk                 text
)
    WITH (OIDS= FALSE);

CREATE TABLE dokumentasjonsreferanse_dokumentlink
(
    objid                      text NOT NULL,
    dokumentasjonsreferanse_fk text,
    dokumentlink               text
)
    WITH (OIDS= FALSE);

CREATE TABLE dokumentasjonsreferanse_internreferansekartverket
(
    objid                      text NOT NULL,
    dokumentasjonsreferanse_fk text,
    internreferansekartverket  text
)
    WITH (OIDS= FALSE);

CREATE TABLE dokumentasjonsreferanse_informasjon
(
    objid                      text NOT NULL,
    dokumentasjonsreferanse_fk text,
    informasjon                text
)
    WITH (OIDS= FALSE);

CREATE TABLE stemmekrets_informasjon
(
    objid          text NOT NULL,
    stemmekrets_fk text,
    informasjon    text
)
    WITH (OIDS= FALSE);

CREATE TABLE administrativenhet_informasjon
(
    objid                 text NOT NULL,
    administrativenhet_fk text,
    informasjon           text
)
    WITH (OIDS= FALSE);

CREATE TABLE administrativenhetnavn_informasjon
(
    objid                     text NOT NULL,
    administrativenhetnavn_fk text,
    informasjon               text
)
    WITH (OIDS= FALSE);

CREATE TABLE administrativtnivaa_informasjon
(
    objid                  text NOT NULL,
    administrativtnivaa_fk text,
    informasjon            text
)
    WITH (OIDS= FALSE);

CREATE TABLE fylke_informasjon
(
    objid       text NOT NULL,
    fylke_fk    text,
    informasjon text
)
    WITH (OIDS= FALSE);

CREATE TABLE kommune_informasjon
(
    objid       text NOT NULL,
    kommune_fk  text,
    informasjon text
)
    WITH (OIDS= FALSE);

CREATE TABLE nasjon_informasjon
(
    objid       text NOT NULL,
    nasjon_fk   text,
    informasjon text
)
    WITH (OIDS= FALSE);

CREATE TABLE grense_informasjon
(
    objid       text NOT NULL,
    grense_fk   text,
    informasjon text
)
    WITH (OIDS= FALSE);

CREATE TABLE delomraade_informasjon
(
    objid         text NOT NULL,
    delomraade_fk text,
    informasjon   text
)
    WITH (OIDS= FALSE);

CREATE TABLE grunnkrets_informasjon
(
    objid         text NOT NULL,
    grunnkrets_fk text,
    informasjon   text
)
    WITH (OIDS= FALSE);

ALTER TABLE dokumentasjonsreferanse
    ADD PRIMARY KEY (objid);
ALTER TABLE grense_dokumentasjonsreferanse
    ADD PRIMARY KEY (grense_fk, dokumentasjonsreferanse_fk);
ALTER TABLE fylkesnummer
    ADD PRIMARY KEY (identifier);
ALTER TABLE kommunenummer
    ADD PRIMARY KEY (identifier);
ALTER TABLE typeavgrensning
    ADD PRIMARY KEY (identifier);
ALTER TABLE typeendring
    ADD PRIMARY KEY (identifier);
ALTER TABLE landkode
    ADD PRIMARY KEY (identifier);
ALTER TABLE stemmekrets
    ADD PRIMARY KEY (objid);
ALTER TABLE stemmekrets_stemmekretsgrense
    ADD PRIMARY KEY (stemmekrets_fk, stemmekretsgrense_fk);
ALTER TABLE administrativenhet
    ADD PRIMARY KEY (objid);
ALTER TABLE administrativenhetnavn
    ADD PRIMARY KEY (objid);
ALTER TABLE administrativtnivaa
    ADD PRIMARY KEY (objid);
ALTER TABLE fylke
    ADD PRIMARY KEY (objid);
ALTER TABLE kommune
    ADD PRIMARY KEY (objid);
ALTER TABLE nasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE nivaanummer
    ADD PRIMARY KEY (identifier);
ALTER TABLE spraakkode
    ADD PRIMARY KEY (identifier);
ALTER TABLE maalemetode
    ADD PRIMARY KEY (identifier);
ALTER TABLE synbarhet
    ADD PRIMARY KEY (identifier);
ALTER TABLE maalemetodehoeyde
    ADD PRIMARY KEY (identifier);
ALTER TABLE grense
    ADD PRIMARY KEY (objid);
ALTER TABLE grunnkrets_grense
    ADD PRIMARY KEY (grunnkrets_fk, grense_fk);
ALTER TABLE delomraade
    ADD PRIMARY KEY (objid);
ALTER TABLE grunnkrets
    ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse_dokumentlink
    ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse_internreferansekartverket
    ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE stemmekrets_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE administrativenhet_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE administrativenhetnavn_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE administrativtnivaa_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE fylke_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE kommune_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE nasjon_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE grense_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE delomraade_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE grunnkrets_informasjon
    ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE dokumentasjonsreferanse
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE dokumentasjonsreferanse
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE dokumentasjonsreferanse
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grense_dokumentasjonsreferanse
    ADD FOREIGN KEY (grense_fk) REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_dokumentasjonsreferanse
    ADD FOREIGN KEY (dokumentasjonsreferanse_fk) REFERENCES dokumentasjonsreferanse (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE stemmekrets
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE stemmekrets
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE stemmekrets
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE stemmekrets
    ADD FOREIGN KEY (kommune_fk) REFERENCES kommune (objid) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE stemmekrets_stemmekretsgrense
    ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_stemmekretsgrense
    ADD FOREIGN KEY (stemmekretsgrense_fk) REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhet
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE administrativenhet
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE administrativenhet
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE administrativenhet
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE administrativenhet
    ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (spraak) REFERENCES spraakkode;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (administrativenhet_fk) REFERENCES administrativenhet (objid) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (administrativenhet_fylke_fk) REFERENCES fylke (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (administrativenhet_nasjon_fk) REFERENCES nasjon (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhetnavn
    ADD FOREIGN KEY (administrativenhet_kommune_fk) REFERENCES kommune (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativtnivaa
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE administrativtnivaa
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE administrativtnivaa
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE administrativtnivaa
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE administrativtnivaa
    ADD FOREIGN KEY (nivaanummer) REFERENCES nivaanummer;
ALTER TABLE fylke
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE fylke
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE fylke
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE fylke
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE fylke
    ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE fylke
    ADD FOREIGN KEY (fylkesnummer) REFERENCES fylkesnummer;
ALTER TABLE fylke
    ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon (objid) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE kommune
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE kommune
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE kommune
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE kommune
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE kommune
    ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE kommune
    ADD FOREIGN KEY (kommunenummer) REFERENCES kommunenummer;
ALTER TABLE kommune
    ADD FOREIGN KEY (fylke_fk) REFERENCES fylke (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE nasjon
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE nasjon
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE nasjon
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE nasjon
    ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE grense
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE grense
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE grense
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE grense
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grense
    ADD FOREIGN KEY (avgrensningstype) REFERENCES typeavgrensning;
ALTER TABLE grunnkrets_grense
    ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_grense
    ADD FOREIGN KEY (grense_fk) REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE delomraade
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE delomraade
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE delomraade
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE delomraade
    ADD FOREIGN KEY (kommunenummer) REFERENCES kommunenummer;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (maalemetode) REFERENCES maalemetode;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (synbarhet) REFERENCES synbarhet;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (maalemetodehoeyde) REFERENCES maalemetodehoeyde;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (delomraadeinndeling_fk) REFERENCES delomraade (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets
    ADD FOREIGN KEY (kommune_fk) REFERENCES kommune (objid) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE dokumentasjonsreferanse_dokumentlink
    ADD FOREIGN KEY (dokumentasjonsreferanse_fk) REFERENCES dokumentasjonsreferanse (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse_internreferansekartverket
    ADD FOREIGN KEY (dokumentasjonsreferanse_fk) REFERENCES dokumentasjonsreferanse (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse_informasjon
    ADD FOREIGN KEY (dokumentasjonsreferanse_fk) REFERENCES dokumentasjonsreferanse (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_informasjon
    ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhet_informasjon
    ADD FOREIGN KEY (administrativenhet_fk) REFERENCES administrativenhet (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhetnavn_informasjon
    ADD FOREIGN KEY (administrativenhetnavn_fk) REFERENCES administrativenhetnavn (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativtnivaa_informasjon
    ADD FOREIGN KEY (administrativtnivaa_fk) REFERENCES administrativtnivaa (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_informasjon
    ADD FOREIGN KEY (fylke_fk) REFERENCES fylke (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_informasjon
    ADD FOREIGN KEY (kommune_fk) REFERENCES kommune (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_informasjon
    ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_informasjon
    ADD FOREIGN KEY (grense_fk) REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_informasjon
    ADD FOREIGN KEY (delomraade_fk) REFERENCES delomraade (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_informasjon
    ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE stemmekrets
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE stemmekrets
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE stemmekrets
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE administrativenhet
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE administrativenhet
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE administrativenhet
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE administrativenhet
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE fylke
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE fylke
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE fylke
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE fylke
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE kommune
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE kommune
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE kommune
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE kommune
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE nasjon
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE nasjon
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE nasjon
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE nasjon
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grense
    ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE grense
    ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE delomraade
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE delomraade
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE delomraade
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE delomraade
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grunnkrets
    ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE grunnkrets
    ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE grunnkrets
    ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grunnkrets
    ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);

