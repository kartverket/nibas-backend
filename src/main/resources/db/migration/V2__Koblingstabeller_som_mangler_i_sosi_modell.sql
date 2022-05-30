-- Manglende kobling fra kommune mot territorialgrense
CREATE TABLE kommune_territorialgrense (kommune_fk text, territorialgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE kommune_territorialgrense ADD PRIMARY KEY (kommune_fk, territorialgrense_fk);
ALTER TABLE kommune_territorialgrense ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra kommune mot riksgrense
CREATE TABLE kommune_riksgrense (kommune_fk text, riksgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE kommune_riksgrense ADD PRIMARY KEY (kommune_fk, riksgrense_fk);
ALTER TABLE kommune_riksgrense ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra kommune mot avtaltavgrensningslinje
CREATE TABLE kommune_avtaltavgrensningslinje (kommune_fk text, avtaltavgrensningslinje_fk text) WITH (OIDS=FALSE);
ALTER TABLE kommune_avtaltavgrensningslinje ADD PRIMARY KEY (kommune_fk, avtaltavgrensningslinje_fk);
ALTER TABLE kommune_avtaltavgrensningslinje ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot territorialgrense
CREATE TABLE fylke_territorialgrense (fylke_fk text, territorialgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE fylke_territorialgrense ADD PRIMARY KEY (fylke_fk, territorialgrense_fk);
ALTER TABLE fylke_territorialgrense ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot riksgrense
CREATE TABLE fylke_riksgrense (fylke_fk text, riksgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE fylke_riksgrense ADD PRIMARY KEY (fylke_fk, riksgrense_fk);
ALTER TABLE fylke_riksgrense ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot avtaltavgrensningslinje
CREATE TABLE fylke_avtaltavgrensningslinje (fylke_fk text, avtaltavgrensningslinje_fk text) WITH (OIDS=FALSE);
ALTER TABLE fylke_avtaltavgrensningslinje ADD PRIMARY KEY (fylke_fk, avtaltavgrensningslinje_fk);
ALTER TABLE fylke_avtaltavgrensningslinje ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra nasjon mot territorialgrense
CREATE TABLE nasjon_territorialgrense (nasjon_fk text, territorialgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE nasjon_territorialgrense ADD PRIMARY KEY (nasjon_fk, territorialgrense_fk);
ALTER TABLE nasjon_territorialgrense ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot riksgrense
CREATE TABLE nasjon_riksgrense (nasjon_fk text, riksgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE nasjon_riksgrense ADD PRIMARY KEY (nasjon_fk, riksgrense_fk);
ALTER TABLE nasjon_riksgrense ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot avtaltavgrensningslinje
CREATE TABLE nasjon_avtaltavgrensningslinje (nasjon_fk text, avtaltavgrensningslinje_fk text) WITH (OIDS=FALSE);
ALTER TABLE nasjon_avtaltavgrensningslinje ADD PRIMARY KEY (nasjon_fk, avtaltavgrensningslinje_fk);
ALTER TABLE nasjon_avtaltavgrensningslinje ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra grunnkrets mot territorialgrense
CREATE TABLE grunnkrets_territorialgrense (grunnkrets_fk text, territorialgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE grunnkrets_territorialgrense ADD PRIMARY KEY (grunnkrets_fk, territorialgrense_fk);
ALTER TABLE grunnkrets_territorialgrense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra grunnkrets mot riksgrense
CREATE TABLE grunnkrets_riksgrense (grunnkrets_fk text, riksgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE grunnkrets_riksgrense ADD PRIMARY KEY (grunnkrets_fk, riksgrense_fk);
ALTER TABLE grunnkrets_riksgrense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra grunnkrets mot avtaltavgrensningslinje
CREATE TABLE grunnkrets_avtaltavgrensningslinje (grunnkrets_fk text, avtaltavgrensningslinje_fk text) WITH (OIDS=FALSE);
ALTER TABLE grunnkrets_avtaltavgrensningslinje ADD PRIMARY KEY (grunnkrets_fk, avtaltavgrensningslinje_fk);
ALTER TABLE grunnkrets_avtaltavgrensningslinje ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra stemmekrets mot territorialgrense
CREATE TABLE stemmekrets_territorialgrense (stemmekrets_fk text, territorialgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE stemmekrets_territorialgrense ADD PRIMARY KEY (stemmekrets_fk, territorialgrense_fk);
ALTER TABLE stemmekrets_territorialgrense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra stemmekrets mot riksgrense
CREATE TABLE stemmekrets_riksgrense (stemmekrets_fk text, riksgrense_fk text) WITH (OIDS=FALSE);
ALTER TABLE stemmekrets_riksgrense ADD PRIMARY KEY (stemmekrets_fk, riksgrense_fk);
ALTER TABLE stemmekrets_riksgrense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra stemmekrets mot avtaltavgrensningslinje
CREATE TABLE stemmekrets_avtaltavgrensningslinje (stemmekrets_fk text, avtaltavgrensningslinje_fk text) WITH (OIDS=FALSE);
ALTER TABLE stemmekrets_avtaltavgrensningslinje ADD PRIMARY KEY (stemmekrets_fk, avtaltavgrensningslinje_fk);
ALTER TABLE stemmekrets_avtaltavgrensningslinje ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende informasjon for stemmekrets
CREATE TABLE stemmekretsgrense_informasjon (objid text NOT NULL, stemmekretsgrense_fk text, informasjon text) WITH (OIDS=FALSE);
ALTER TABLE stemmekretsgrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE stemmekretsgrense_informasjon ADD FOREIGN KEY (stemmekretsgrense_fk) REFERENCES stemmekretsgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
