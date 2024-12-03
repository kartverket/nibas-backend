-- Nytt felt informasjon in stemmekrets tabell
ALTER TABLE stemmekrets ADD COLUMN IF NOT EXISTS informasjon TEXT;

-- Kopiere data far stemmekrets_informasjon til stemmekerts 
UPDATE stemmekrets s 
SET informasjon = (
    SELECT si.informasjon 
    FROM stemmekrets_informasjon si 
    WHERE si.stemmekrets_fk = s.objid 
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM stemmekrets_informasjon si 
    WHERE si.stemmekrets_fk = s.objid
);

DROP TABLE IF EXISTS stemmekrets_informasjon;
