CREATE TABLE grense (
                              objid text NOT NULL,
                              grensetype text NOT NULL,
                              oppdateringsdato date NOT NULL,
                              endret_av text,
                              produkt text,
                              versjon text,
                              omraadeid int,
                              originaldatavert text,
                              kopidato date,
                              maalemetode text,
                              noeyaktighet int,
                              lokalid text NOT NULL,
                              navnerom text NOT NULL,
                              versjonid text,
                              datafangstdato date,
                              gyldigfra date,
                              gyldigtil date,
                              opphav text,
                              grense geometry(Linestring, 25833) NOT NULL,
                              info text,
                              sluttdato date,
                              datauttaksdato date,
                              omtvistet boolean,
                              gjelderomraade text,
                              virkeomraadenavn text,
                              transformasjonsdato date,
                              kommunenummer text,
                              foelgerterrengdetalj text,
                              noeyaktighetsklasse text,
                              grensestatus text,
                              fastsettingstype text,
                              tidspunktendring date,
                              typeendring text,
                              foelgerteiggrense boolean,
                              foelgergrunnkretsgrense_lokalid text,
                              foelgergrunnkretsgrense_navnerom text,
                              foelgergrunnkretsgrense_versjonid text,
                              beregningsdato date,
                              stedfestingverifisert boolean,
                              grensegatebredde double precision
)
    WITH (OIDS=FALSE);

CREATE TABLE grense_land (
                                              grense_fk text,
                                              landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensepunktsjoe (
                                              grense_fk text,
                                              grensepunktsjoe_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE stemmekrets_grense (
                                          stemmekrets_fk text,
                                          grense_fk text,
                                          retning_med_klokken boolean not null default true,
                                          rekkefoelge         int     not null default 0,
                                          flate_indeks        int     not null default 0,
                                          hull_indeks         int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets_grense (
                                         grunnkrets_fk text,
                                         grense_fk text,
                                         retning_med_klokken boolean not null default true,
                                         rekkefoelge         int     not null default 0,
                                         flate_indeks        int     not null default 0,
                                         hull_indeks         int)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerkeland (
                                              grense_fk text,
                                              grensemerkeland_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grenseboeye (
                                                     grense_fk text,
                                                     grenseboeye_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_ansvarligmyndighet (
                                                            objid text NOT NULL,
                                                            grense_fk text,
                                                            ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_vassdragspunkt (
                                                       grense_fk text,
                                                       grensemerke_vassdragspunkt_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_riksroeys (
                                                  grense_fk text,
                                                  grensemerke_riksroeys_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_profilpunkt (
                                                    grense_fk text,
                                                    grensemerke_profilpunkt_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_brytningspunkt (
                                                       grense_fk text,
                                                       grensemerke_brytningspunkt_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_viserstein (
                                                   grense_fk text,
                                                   grensemerke_viserstein_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_oevrighjelpemerke (
                                                          grense_fk text,
                                                          grensemerke_oevrighjelpemerke_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_storutligger (
                                                     grense_fk text,
                                                     grensemerke_storutligger_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_litenutligger (
                                                      grense_fk text,
                                                      grensemerke_litenutligger_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_grensestolpe (
                                                     grense_fk text,
                                                     grensemerke_grensestolpe_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense_grensemerke_grenseskilt (
                                                    grense_fk text,
                                                    grensemerke_grenseskilt_fk text)
    WITH (OIDS=FALSE);


ALTER TABLE grense ADD PRIMARY KEY (objid);
ALTER TABLE grense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE grense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE grense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE grense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE grense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE grense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);

ALTER TABLE grense_land ADD PRIMARY KEY (grense_fk, landkode_fk);
ALTER TABLE grense_land ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensepunktsjoe ADD PRIMARY KEY (grense_fk, grensepunktsjoe_fk);
ALTER TABLE grense_grensepunktsjoe ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensepunktsjoe ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE stemmekrets_grense ADD PRIMARY KEY (stemmekrets_fk, grense_fk);
ALTER TABLE stemmekrets_grense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_grense ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grunnkrets_grense ADD PRIMARY KEY (grunnkrets_fk, grense_fk);
ALTER TABLE grunnkrets_grense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_grense ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerkeland ADD PRIMARY KEY (grense_fk, grensemerkeland_fk);
ALTER TABLE grense_grensemerkeland ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerkeland ADD FOREIGN KEY (grensemerkeland_fk) REFERENCES grensemerkeland(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grenseboeye ADD PRIMARY KEY (grense_fk, grenseboeye_fk);
ALTER TABLE grense_grenseboeye ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grenseboeye ADD FOREIGN KEY (grenseboeye_fk) REFERENCES grenseboeye(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE grense_ansvarligmyndighet ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_vassdragspunkt ADD PRIMARY KEY (grense_fk, grensemerke_vassdragspunkt_fk);
ALTER TABLE grense_grensemerke_vassdragspunkt ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_vassdragspunkt ADD FOREIGN KEY (grensemerke_vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_riksroeys ADD PRIMARY KEY (grense_fk, grensemerke_riksroeys_fk);
ALTER TABLE grense_grensemerke_riksroeys ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_riksroeys ADD FOREIGN KEY (grensemerke_riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_profilpunkt ADD PRIMARY KEY (grense_fk, grensemerke_profilpunkt_fk);
ALTER TABLE grense_grensemerke_profilpunkt ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_profilpunkt ADD FOREIGN KEY (grensemerke_profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_brytningspunkt ADD PRIMARY KEY (grense_fk, grensemerke_brytningspunkt_fk);
ALTER TABLE grense_grensemerke_brytningspunkt ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_brytningspunkt ADD FOREIGN KEY (grensemerke_brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_viserstein ADD PRIMARY KEY (grense_fk, grensemerke_viserstein_fk);
ALTER TABLE grense_grensemerke_viserstein ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_viserstein ADD FOREIGN KEY (grensemerke_viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_oevrighjelpemerke ADD PRIMARY KEY (grense_fk, grensemerke_oevrighjelpemerke_fk);
ALTER TABLE grense_grensemerke_oevrighjelpemerke ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_oevrighjelpemerke ADD FOREIGN KEY (grensemerke_oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_storutligger ADD PRIMARY KEY (grense_fk, grensemerke_storutligger_fk);
ALTER TABLE grense_grensemerke_storutligger ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_storutligger ADD FOREIGN KEY (grensemerke_storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_litenutligger ADD PRIMARY KEY (grense_fk, grensemerke_litenutligger_fk);
ALTER TABLE grense_grensemerke_litenutligger ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_litenutligger ADD FOREIGN KEY (grensemerke_litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_grensestolpe ADD PRIMARY KEY (grense_fk, grensemerke_grensestolpe_fk);
ALTER TABLE grense_grensemerke_grensestolpe ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_grensestolpe ADD FOREIGN KEY (grensemerke_grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grense_grensemerke_grenseskilt ADD PRIMARY KEY (grense_fk, grensemerke_grenseskilt_fk);
ALTER TABLE grense_grensemerke_grenseskilt ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense_grensemerke_grenseskilt ADD FOREIGN KEY (grensemerke_grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE dokumentasjonsreferanse add column grense_fk text;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grenseboeye add column grense_fk text;
ALTER TABLE grenseboeye ADD FOREIGN KEY (grense_fk) REFERENCES grense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;


-- Sjekk avgrensning_bispedoemme, avgrensning_prosti, avgrensning_sokn som i dag refererer fylkesgrense+kommunegrense
-- sjoeroeys_avtaltavgrensningslinje
-- grensemerkeland_kommunegrense
-- grensepunktsjoe_kommunegrense
-- delomraade_kommunegrense
-- skolekrets_kommunegrense
-- kommune_kommunegrense

