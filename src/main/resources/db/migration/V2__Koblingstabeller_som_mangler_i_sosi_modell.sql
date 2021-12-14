-- Manglende kobling fra kommune mot territorialgrense
CREATE TABLE kommune_territorialgrense (kommune_fk int, territorialgrense_fk int) WITH (OIDS=FALSE);
ALTER TABLE kommune_territorialgrense ADD PRIMARY KEY (kommune_fk, territorialgrense_fk);
ALTER TABLE kommune_territorialgrense ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra kommune mot riksgrense
CREATE TABLE kommune_riksgrense (kommune_fk int, riksgrense_fk int) WITH (OIDS=FALSE);
ALTER TABLE kommune_riksgrense ADD PRIMARY KEY (kommune_fk, riksgrense_fk);
ALTER TABLE kommune_riksgrense ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra kommune mot avtaltavgrensningslinje
CREATE TABLE kommune_avtaltavgrensningslinje (kommune_fk int, avtaltavgrensningslinje_fk int) WITH (OIDS=FALSE);
ALTER TABLE kommune_avtaltavgrensningslinje ADD PRIMARY KEY (kommune_fk, avtaltavgrensningslinje_fk);
ALTER TABLE kommune_avtaltavgrensningslinje ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot territorialgrense
CREATE TABLE fylke_territorialgrense (fylke_fk int, territorialgrense_fk int) WITH (OIDS=FALSE);
ALTER TABLE fylke_territorialgrense ADD PRIMARY KEY (fylke_fk, territorialgrense_fk);
ALTER TABLE fylke_territorialgrense ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot riksgrense
CREATE TABLE fylke_riksgrense (fylke_fk int, riksgrense_fk int) WITH (OIDS=FALSE);
ALTER TABLE fylke_riksgrense ADD PRIMARY KEY (fylke_fk, riksgrense_fk);
ALTER TABLE fylke_riksgrense ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot avtaltavgrensningslinje
CREATE TABLE fylke_avtaltavgrensningslinje (fylke_fk int, avtaltavgrensningslinje_fk int) WITH (OIDS=FALSE);
ALTER TABLE fylke_avtaltavgrensningslinje ADD PRIMARY KEY (fylke_fk, avtaltavgrensningslinje_fk);
ALTER TABLE fylke_avtaltavgrensningslinje ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra nasjon mot territorialgrense
CREATE TABLE nasjon_territorialgrense (nasjon_fk int, territorialgrense_fk int) WITH (OIDS=FALSE);
ALTER TABLE nasjon_territorialgrense ADD PRIMARY KEY (nasjon_fk, territorialgrense_fk);
ALTER TABLE nasjon_territorialgrense ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_territorialgrense ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot riksgrense
CREATE TABLE nasjon_riksgrense (nasjon_fk int, riksgrense_fk int) WITH (OIDS=FALSE);
ALTER TABLE nasjon_riksgrense ADD PRIMARY KEY (nasjon_fk, riksgrense_fk);
ALTER TABLE nasjon_riksgrense ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_riksgrense ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Manglende kobling fra fylke mot avtaltavgrensningslinje
CREATE TABLE nasjon_avtaltavgrensningslinje (nasjon_fk int, avtaltavgrensningslinje_fk int) WITH (OIDS=FALSE);
ALTER TABLE nasjon_avtaltavgrensningslinje ADD PRIMARY KEY (nasjon_fk, avtaltavgrensningslinje_fk);
ALTER TABLE nasjon_avtaltavgrensningslinje ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
