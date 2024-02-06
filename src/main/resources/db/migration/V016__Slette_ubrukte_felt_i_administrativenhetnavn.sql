ALTER TABLE administrativenhetnavn
    DROP COLUMN IF EXISTS objtype,
    DROP COLUMN IF EXISTS versjonid,
    DROP COLUMN IF EXISTS maalemetode,
    DROP COLUMN IF EXISTS maalemetodehoeyde,
    DROP COLUMN IF EXISTS noeyaktighet,
    DROP COLUMN IF EXISTS noeyaktighethoeyde,
    DROP COLUMN IF EXISTS synbarhet,
    DROP COLUMN IF EXISTS maksimaltavvik;
