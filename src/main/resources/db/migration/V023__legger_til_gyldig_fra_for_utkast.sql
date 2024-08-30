-- Legger til gyldigFra felt på utkast for å støtte fremtidige endringer.

ALTER TABLE utkast
ADD COLUMN gyldig_fra DATE;

UPDATE utkast
SET gyldig_fra = endretdato;

ALTER TABLE UTKAST
ALTER COLUMN gyldig_fra SET NOT NULL;
