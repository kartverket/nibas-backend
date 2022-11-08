-- Pga ddlauto update har kolonnen Grense.gyldig først blitt opprettet som nullable.
-- I en senere commit ble den gjort not-null i JPA-annotasjonen.
ALTER TABLE grense drop column if exists gyldig;
ALTER TABLE grense add column gyldig boolean NOT NULL DEFAULT 'TRUE';
