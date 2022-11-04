ALTER TABLE stemmekrets add column utledetfra text;
ALTER TABLE stemmekrets ADD FOREIGN KEY (utledetfra) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE grunnkrets add column utledetfra text;
ALTER TABLE grunnkrets ADD FOREIGN KEY (utledetfra) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE kommune add column utledetfra text;
ALTER TABLE kommune ADD FOREIGN KEY (utledetfra) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE fylke add column utledetfra text;
ALTER TABLE fylke ADD FOREIGN KEY (utledetfra) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;



