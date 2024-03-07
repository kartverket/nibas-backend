-- Fjerner grensekoblingsatributter som rekkefølge, retninged med klokken o.s.v.

ALTER TABLE stemmekrets_grense
    DROP COLUMN retning_med_klokken,
    DROP COLUMN rekkefoelge,
    DROP COLUMN flate_indeks,
    DROP COLUMN hull_indeks;

ALTER TABLE grunnkrets_grense
    DROP COLUMN retning_med_klokken,
    DROP COLUMN rekkefoelge,
    DROP COLUMN flate_indeks,
    DROP COLUMN hull_indeks;
