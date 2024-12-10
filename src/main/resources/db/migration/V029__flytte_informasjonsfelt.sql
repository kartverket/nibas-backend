ALTER TABLE grense RENAME COLUMN info TO informasjon;
-- Grense har allerede ett felt for informasjon 
DROP TABLE IF EXISTS grense_informasjon;

-- Nytt felt informasjon in kommune tabell
ALTER TABLE kommune ADD COLUMN IF NOT EXISTS informasjon TEXT;
-- Kopiere data fra kommune_informasjon til kommune
UPDATE kommune s
SET informasjon = (
    SELECT si.informasjon
    FROM kommune_informasjon si
    WHERE si.kommune_fk = s.objid
    AND si.informasjon IS NOT NULL
);
DROP TABLE IF EXISTS kommune_informasjon;

ALTER TABLE fylke ADD COLUMN IF NOT EXISTS informasjon TEXT;
UPDATE fylke s
SET informasjon = (
    SELECT si.informasjon
    FROM fylke_informasjon si
    WHERE si.fylke_fk = s.objid
    AND si.informasjon IS NOT NULL
);
DROP TABLE IF EXISTS fylke_informasjon;

ALTER TABLE nasjon ADD COLUMN IF NOT EXISTS informasjon TEXT;
UPDATE nasjon s
SET informasjon = (
    SELECT si.informasjon
    FROM nasjon_informasjon si
    WHERE si.nasjon_fk = s.objid
    AND si.informasjon IS NOT NULL
);
DROP TABLE IF EXISTS nasjon_informasjon;
