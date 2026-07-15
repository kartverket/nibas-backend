ALTER TABLE bopliktomraade
    ADD COLUMN har_usikker_avgrensning BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN gjelder_for_brukt_som_helarsbolig BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN gjelder_for_bolig_ikke_tatt_i_bruk BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN gjelder_for_ubebygd_boligtomt BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN har_unntak_fra_slektskapsunntak BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN andre_lokale_avgrensninger TEXT;

UPDATE bopliktomraade SET
    har_usikker_avgrensning = usikker_avgrensning,
    gjelder_for_brukt_som_helarsbolig = ikke_helaarsbolig_under_oppforing,
    gjelder_for_bolig_ikke_tatt_i_bruk = bebygd_eiendom,
    gjelder_for_ubebygd_boligtomt = ubebygd_tomt,
    har_unntak_fra_slektskapsunntak = unntak_fra_slektskapsunntak,
    andre_lokale_avgrensninger = andre_avgrensninger;
