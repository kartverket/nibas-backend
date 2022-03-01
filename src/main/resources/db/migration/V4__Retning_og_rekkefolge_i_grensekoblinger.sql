-- Kommune
ALTER TABLE kommune_kommunegrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE kommune_fylkesgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE kommune_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE kommune_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE kommune_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

-- Fylke
ALTER TABLE fylke_fylkesgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE fylke_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE fylke_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE fylke_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

-- Nasjon
ALTER TABLE nasjon_avtaltavgrensningslinje
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE nasjon_territorialgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;

ALTER TABLE nasjon_riksgrense
    add column retning_med_klokken boolean not null default true,
    add column rekkefoelge        int     not null default 0,
    add column hull_indeks       int;
