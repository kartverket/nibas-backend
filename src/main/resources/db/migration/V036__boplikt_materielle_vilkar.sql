ALTER TABLE bopliktomraade
    ADD COLUMN bebygd_eiendom BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ikke_helaarsbolig_under_oppforing BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ubebygd_tomt BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN unntak_fra_slektskapsunntak BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN andre_avgrensninger TEXT DEFAULT NULL;
