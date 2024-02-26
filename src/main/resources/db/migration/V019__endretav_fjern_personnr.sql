-- Erstatter personnummerdelen i fødseslnummer med XXXXX i endretav-feltet. eks: 01017012345 -> 010170XXXXX

UPDATE administrativenhetnavn SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE administrativtnivaa SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE administrativenhet SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE grunnkrets SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE fylke SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE grense SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE dokumentasjonsreferanse SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE delomraade SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE nasjon SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE stemmekrets SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE kommune SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';
UPDATE utkast SET endretav = regexp_replace(endretav, '^(\d{6})\d{5}$', '\1XXXXX') WHERE endretav ~ '^\d{11}$';

