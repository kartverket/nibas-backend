-- Nytt felt informasjon in grunnkrets tabell
ALTER TABLE grunnkrets ADD COLUMN IF NOT EXISTS informasjon TEXT;

-- Kopiere data fra grunnkrets_informasjon til grunnkrets 
UPDATE grunnkrets s 
SET informasjon = (
    SELECT si.informasjon 
    FROM grunnkrets_informasjon si 
    WHERE si.grunnkrets_fk = s.objid 
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM grunnkrets_informasjon si 
    WHERE si.grunnkrets_fk = s.objid
);

DROP TABLE IF EXISTS grunnkrets_informasjon;
