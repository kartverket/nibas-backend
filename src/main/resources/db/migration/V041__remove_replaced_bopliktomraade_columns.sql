ALTER TABLE bopliktomraade
    DROP COLUMN IF EXISTS usikker_avgrensning,
    DROP COLUMN IF EXISTS bebygd_eiendom,
    DROP COLUMN IF EXISTS ikke_helaarsbolig_under_oppforing,
    DROP COLUMN IF EXISTS ubebygd_tomt,
    DROP COLUMN IF EXISTS unntak_fra_slektskapsunntak,
    DROP COLUMN IF EXISTS andre_avgrensninger;
