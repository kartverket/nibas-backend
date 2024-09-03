-- Dropping columns from each table separately
ALTER TABLE dokumentasjonsreferanse DROP COLUMN maalemetode;
ALTER TABLE dokumentasjonsreferanse DROP COLUMN noeyaktighet;
ALTER TABLE dokumentasjonsreferanse DROP COLUMN objtype;

ALTER TABLE administrativtnivaa DROP COLUMN maalemetode;
ALTER TABLE administrativtnivaa DROP COLUMN noeyaktighet;
ALTER TABLE administrativtnivaa DROP COLUMN objtype;

-- Dropping columns from other tables
ALTER TABLE delomraade DROP COLUMN synbarhet;
ALTER TABLE delomraade DROP COLUMN maalemetodehoeyde;
ALTER TABLE delomraade DROP COLUMN noeyaktighethoeyde;
ALTER TABLE delomraade DROP COLUMN maksimaltavvik;
ALTER TABLE delomraade DROP COLUMN objtype;

ALTER TABLE dokumentasjonsreferanse DROP COLUMN synbarhet;
ALTER TABLE dokumentasjonsreferanse DROP COLUMN maalemetodehoeyde;
ALTER TABLE dokumentasjonsreferanse DROP COLUMN noeyaktighethoeyde;
ALTER TABLE dokumentasjonsreferanse DROP COLUMN maksimaltavvik;

ALTER TABLE administrativenhet DROP COLUMN synbarhet;
ALTER TABLE administrativenhet DROP COLUMN maalemetodehoeyde;
ALTER TABLE administrativenhet DROP COLUMN noeyaktighethoeyde;
ALTER TABLE administrativenhet DROP COLUMN maksimaltavvik;
ALTER TABLE administrativenhet DROP COLUMN objtype;

ALTER TABLE administrativtnivaa DROP COLUMN synbarhet;
ALTER TABLE administrativtnivaa DROP COLUMN maalemetodehoeyde;
ALTER TABLE administrativtnivaa DROP COLUMN noeyaktighethoeyde;
ALTER TABLE administrativtnivaa DROP COLUMN maksimaltavvik;

ALTER TABLE grense DROP COLUMN synbarhet;
ALTER TABLE grense DROP COLUMN maalemetodehoeyde;
ALTER TABLE grense DROP COLUMN noeyaktighethoeyde;
ALTER TABLE grense DROP COLUMN maksimaltavvik;
ALTER TABLE grense DROP COLUMN objtype;

-- Dropping tables with CASCADE
DROP TABLE administrativenhet CASCADE;
DROP TABLE synbarhet CASCADE;
DROP TABLE maalemetodehoeyde CASCADE;
