ALTER TABLE bopliktomraade ALTER COLUMN omraade DROP NOT NULL;
UPDATE bopliktomraade SET omraade = NULL WHERE delvis_boplikt = FALSE;
DELETE FROM bopliktomraade_grense
WHERE bopliktomraade_fk IN (
    SELECT objid FROM bopliktomraade WHERE delvis_boplikt = FALSE
);
ALTER TABLE bopliktomraade DROP COLUMN delvis_boplikt;
