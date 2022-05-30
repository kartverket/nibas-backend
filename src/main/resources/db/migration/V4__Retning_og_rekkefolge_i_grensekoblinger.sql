-- Kommune
ALTER TABLE kommune_kommunegrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE kommune_fylkesgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE kommune_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE kommune_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE kommune_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

-- Fylke
ALTER TABLE fylke_fylkesgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE fylke_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE fylke_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE fylke_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

-- Nasjon
ALTER TABLE nasjon_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE nasjon_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE nasjon_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

-- Grunnkrets
ALTER TABLE grunnkrets_kommunegrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE grunnkrets_fylkesgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE grunnkrets_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE grunnkrets_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE grunnkrets_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE grunnkrets_grunnkretsgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE grunnkrets_delomraadegrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

-- Stemmekrets
ALTER TABLE stemmekrets_kommunegrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE stemmekrets_fylkesgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE stemmekrets_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE stemmekrets_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE stemmekrets_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE stemmekrets_valgdistriktsgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

ALTER TABLE stemmekrets_stemmekretsgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge         int     not null default 0,
    add column flate_indeks        int     not null default 0,
    add column hull_indeks         int;

alter table stemmekrets_stemmekretsgrense drop constraint stemmekrets_stemmekretsgrense_pkey;
alter table stemmekrets_stemmekretsgrense add primary key (stemmekrets_fk, stemmekretsgrense_fk, flate_indeks);
