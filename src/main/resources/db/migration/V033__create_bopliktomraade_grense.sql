CREATE TABLE bopliktomraade_grense
(
    bopliktomraade_fk text NOT NULL,
    grense_fk         text NOT NULL,
    CONSTRAINT bopliktomraade_grense_pkey PRIMARY KEY (bopliktomraade_fk, grense_fk),
    CONSTRAINT bopliktomraade_grense_bopliktomraade_fk_fkey FOREIGN KEY (bopliktomraade_fk)
        REFERENCES bopliktomraade (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT bopliktomraade_grense_grense_fk_fkey FOREIGN KEY (grense_fk)
        REFERENCES grense (objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX bopliktomraade_grense_grense_fk_index ON bopliktomraade_grense (grense_fk);

INSERT INTO typeavgrensning (identifier, description)
VALUES ('Bopliktgrense', 'Bopliktgrense');


