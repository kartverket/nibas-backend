-- Knytning fra kommune til grunnkrets
ALTER TABLE grunnkrets add column kommune_fk text;
ALTER TABLE grunnkrets ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

-- Knytning fra kommune til stemmekrets
ALTER TABLE stemmekrets add column kommune_fk text;
ALTER TABLE stemmekrets ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

