ALTER TABLE grense ALTER CONSTRAINT grense_avgrensningstype_fkey DEFERRABLE INITIALLY IMMEDIATE;
SET CONSTRAINTS grense_avgrensningstype_fkey DEFERRED;

UPDATE typeavgrensning SET identifier = 'Bopliktområdegrense', description = 'Bopliktområdegrense' WHERE identifier = 'Bopliktgrense';
UPDATE grense g SET avgrensningstype = 'Bopliktområdegrense' WHERE avgrensningstype = 'Bopliktgrense';

SET CONSTRAINTS grense_avgrensningstype_fkey IMMEDIATE ;
ALTER TABLE grense ALTER CONSTRAINT grense_avgrensningstype_fkey NOT DEFERRABLE;
