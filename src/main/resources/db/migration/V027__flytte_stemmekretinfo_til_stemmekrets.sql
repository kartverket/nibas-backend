-- Nytt felt informasjon in stemmekrets tabell
ALTER TABLE stemmekrets ADD COLUMN IF NOT EXISTS informasjon TEXT;

-- Kopiere data far stemmekrets_informasjon til stemmekerts 
UPDATE stemmekrets s 
SET informasjon = (
    SELECT si.informasjon 
    FROM stemmekrets_informasjon si 
    WHERE si.stemmekrets_fk = s.objid 
    AND si.informasjon IS NOT NULL 
);

DROP TABLE IF EXISTS stemmekrets_informasjon;
