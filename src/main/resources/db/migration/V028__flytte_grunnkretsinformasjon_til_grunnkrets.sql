-- Nytt felt informasjon in grunnkrets tabell
ALTER TABLE grunnkrets ADD COLUMN IF NOT EXISTS informasjon TEXT;

-- Kopiere data fra grunnkrets_informasjon til grunnkrets 
UPDATE grunnkrets s 
SET informasjon = (
    SELECT si.informasjon 
    FROM grunnkrets_informasjon si 
    WHERE si.grunnkrets_fk = s.objid 
    AND si.informasjon IS NOT NULL 
);

DROP TABLE IF EXISTS grunnkrets_informasjon;
