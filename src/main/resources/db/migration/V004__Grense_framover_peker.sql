alter table grense
    add column neste_versjon text;
alter table grense_aud
    add column neste_versjon text;

ALTER TABLE grense
    ADD FOREIGN KEY (neste_versjon) REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
