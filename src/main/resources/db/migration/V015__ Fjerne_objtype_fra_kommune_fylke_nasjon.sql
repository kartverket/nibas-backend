ALTER TABLE kommune
    DROP COLUMN IF EXISTS objtype,
    DROP COLUMN IF EXISTS maalemetode,
    DROP COLUMN IF EXISTS maalemetodehoeyde,
    DROP COLUMN IF EXISTS noeyaktighet,
    DROP COLUMN IF EXISTS noeyaktighethoeyde,
    DROP COLUMN IF EXISTS synbarhet,
    DROP COLUMN IF EXISTS maksimaltavvik;

ALTER TABLE fylke
    DROP COLUMN IF EXISTS objtype,
    DROP COLUMN IF EXISTS maalemetode,
    DROP COLUMN IF EXISTS maalemetodehoeyde,
    DROP COLUMN IF EXISTS noeyaktighet,
    DROP COLUMN IF EXISTS noeyaktighethoeyde,
    DROP COLUMN IF EXISTS synbarhet,
    DROP COLUMN IF EXISTS maksimaltavvik;

ALTER TABLE nasjon
    DROP COLUMN IF EXISTS objtype,
    DROP COLUMN IF EXISTS maalemetode,
    DROP COLUMN IF EXISTS maalemetodehoeyde,
    DROP COLUMN IF EXISTS noeyaktighet,
    DROP COLUMN IF EXISTS noeyaktighethoeyde,
    DROP COLUMN IF EXISTS synbarhet,
    DROP COLUMN IF EXISTS maksimaltavvik;
