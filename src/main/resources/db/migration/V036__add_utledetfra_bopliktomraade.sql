ALTER TABLE bopliktomraade
    add column utledetfra text;
ALTER TABLE bopliktomraade
    ADD FOREIGN KEY (utledetfra) REFERENCES bopliktomraade (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
