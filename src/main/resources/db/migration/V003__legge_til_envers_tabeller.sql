create table revinfo
(
    rev      integer not null,
    revtstmp bigint,
    primary key (rev)
);

create table administrativenhetnavn_aud
(
    objid                         varchar(255) not null,
    rev                           integer      not null,
    revtype                       smallint,
    endretav                      varchar(255),
    endretdato                    date,
    typeendring                   varchar(255),
    foerstedigitaliseringsdato    date,
    gyldigfra                     date,
    lokalid                       varchar(255),
    navn                          varchar(255),
    objtype                       varchar(255),
    rekkefoelge                   integer,
    administrativenhet_fylke_fk   varchar(255),
    administrativenhet_kommune_fk varchar(255),
    administrativenhet_nasjon_fk  varchar(255),
    spraak                        varchar(255),
    primary key (objid, rev),
    constraint fk2frsrhwu2rgg15yvxhob04r3y
        foreign key (rev) references revinfo
);

create table delomraade_aud
(
    objid   varchar(255) not null,
    rev     integer      not null,
    revtype smallint,
    primary key (objid, rev),
    constraint fkgj8jde29abq3hfvpaxh4fye4o
        foreign key (rev) references revinfo
);

create table dokumentasjonsreferanse_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    fastsettingsdato           date,
    fastsettingsmyndighet      varchar(255),
    foerstedigitaliseringsdato date,
    gyldigfra                  date,
    hjemmel                    varchar(255),
    lokalid                    varchar(255),
    objtype                    varchar(255),
    rettskildeid               varchar(255),
    rettskildetittel           varchar(255),
    primary key (objid, rev),
    constraint fk5ad063edf139j19avvblgtp1j
        foreign key (rev) references revinfo
);

create table dokumentasjonsreferanse_dokumentlink_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    dokumentlink               varchar(255),
    dokumentasjonsreferanse_fk varchar(255),
    primary key (objid, rev),
    constraint fksi1ixnyg3gcbeni0xiojf95ud
        foreign key (rev) references revinfo
);

create table dokumentasjonsreferanse_internreferansekartverket_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    internreferansekartverket  varchar(255),
    dokumentasjonsreferanse_fk varchar(255),
    primary key (objid, rev),
    constraint fk9kji5hjh6e4tbmioydfx72v4p
        foreign key (rev) references revinfo
);

create table fylke_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    datafangstdato             date,
    foerstedigitaliseringsdato date,
    gyldigfra                  date,
    gyldigtil                  date,
    slettet                    boolean,
    lokalid                    varchar(255),
    objtype                    varchar(255),
    omraade                    geometry,
    opphav                     varchar(255),
    posisjon                   geometry,
    fylkesnummer               varchar(255),
    samiskforvaltningsomraade  boolean,
    nasjon_fk                  varchar(255),
    utledetfra                 varchar(255),
    primary key (objid, rev),
    constraint fkn08jdi5itvsviba6ra63kt4of
        foreign key (rev) references revinfo
);

create table fylke_informasjon_aud
(
    objid       varchar(255) not null,
    rev         integer      not null,
    revtype     smallint,
    informasjon varchar(255),
    fylke_fk    varchar(255),
    primary key (objid, rev),
    constraint fkipodd9r30gl7h11b9arj6lime
        foreign key (rev) references revinfo
);

create table grense_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    datafangstdato             date,
    foerstedigitaliseringsdato date,
    grense                     geometry,
    avgrensningstype           varchar(255),
    gyldigfra                  date,
    gyldigtil                  date,
    slettet                    boolean,
    info                       varchar(255),
    maalemetode                varchar(255),
    noeyaktighet               integer,
    lokalid                    varchar(255),
    objtype                    varchar(255),
    opphav                     varchar(255),
    primary key (objid, rev),
    constraint fkfstkjtpa78w9u2godqdb97ul5
        foreign key (rev) references revinfo
);

create table grense_dokumentasjonsreferanse_aud
(
    rev                        integer      not null,
    grense_fk                  varchar(255) not null,
    dokumentasjonsreferanse_fk varchar(255) not null,
    revtype                    smallint,
    primary key (rev, grense_fk, dokumentasjonsreferanse_fk),
    constraint fkjble697vjv6ck1t0l0r9ogoac
        foreign key (rev) references revinfo
);

create table grense_kommunenummer_aud
(
    rev           integer      not null,
    grense_objid  varchar(255) not null,
    kommunenummer varchar(255) not null,
    revtype       smallint,
    primary key (rev, grense_objid, kommunenummer),
    constraint fkb90lsdtwwpeymst10jljljjrg
        foreign key (rev) references revinfo
);

create table grunnkrets_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    datafangstdato             date,
    foerstedigitaliseringsdato date,
    gyldigfra                  date,
    gyldigtil                  date,
    slettet                    boolean,
    lokalid                    varchar(255),
    objtype                    varchar(255),
    omraade                    geometry,
    opphav                     varchar(255),
    posisjon                   geometry,
    version                    integer,
    grunnkretsnavn             varchar(255),
    grunnkretsnummer           varchar(255),
    delomraadeinndeling_fk     varchar(255),
    kommune_fk                 varchar(255),
    utledetfra                 varchar(255),
    primary key (objid, rev),
    constraint fkeiiset530theewqy9dops9fi0
        foreign key (rev) references revinfo
);

create table grunnkrets_grense_aud
(
    grense_fk           varchar(255) not null,
    grunnkrets_fk       varchar(255) not null,
    rev                 integer      not null,
    revtype             smallint,
    flate_indeks        integer,
    hull_indeks         integer,
    rekkefoelge         integer,
    retning_med_klokken boolean,
    primary key (grense_fk, grunnkrets_fk, rev),
    constraint fk56bckfk707wh1m25gr0f4q7qe
        foreign key (rev) references revinfo
);

create table grunnkrets_informasjon_aud
(
    objid         varchar(255) not null,
    rev           integer      not null,
    revtype       smallint,
    informasjon   varchar(255),
    grunnkrets_fk varchar(255),
    primary key (objid, rev),
    constraint fk3cp75i4y2nibesxxddrydmeqq
        foreign key (rev) references revinfo
);

create table kommune_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    datafangstdato             date,
    foerstedigitaliseringsdato date,
    gyldigfra                  date,
    gyldigtil                  date,
    slettet                    boolean,
    lokalid                    varchar(255),
    objtype                    varchar(255),
    omraade                    geometry,
    opphav                     varchar(255),
    posisjon                   geometry,
    kommunenummer              varchar(255),
    samiskforvaltningsomraade  boolean,
    fylke_fk                   varchar(255),
    utledetfra                 varchar(255),
    primary key (objid, rev),
    constraint fkdv4n6su65qqupcvxcf23sv79u
        foreign key (rev) references revinfo
);

create table kommune_informasjon_aud
(
    objid       varchar(255) not null,
    rev         integer      not null,
    revtype     smallint,
    informasjon varchar(255),
    kommune_fk  varchar(255),
    primary key (objid, rev),
    constraint fkyxnpl8phi6vflamwnywilxis
        foreign key (rev) references revinfo
);

create table landkode_aud
(
    identifier  varchar(255) not null,
    rev         integer      not null,
    revtype     smallint,
    description varchar(255),
    primary key (identifier, rev),
    constraint fkgdih75d9osgxsdvvbsco5n1nx
        foreign key (rev) references revinfo
);

create table nasjon_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    datafangstdato             date,
    foerstedigitaliseringsdato date,
    gyldigfra                  date,
    gyldigtil                  date,
    slettet                    boolean,
    lokalid                    varchar(255),
    objtype                    varchar(255),
    omraade                    geometry,
    opphav                     varchar(255),
    posisjon                   geometry,
    primary key (objid, rev),
    constraint fklg5lsfo43c1c2dg2gr02ei9p1
        foreign key (rev) references revinfo
);

create table nasjon_informasjon_aud
(
    objid       varchar(255) not null,
    rev         integer      not null,
    revtype     smallint,
    informasjon varchar(255),
    nasjon_fk   varchar(255),
    primary key (objid, rev),
    constraint fk677dh8khubbdlbuu79eq54fln
        foreign key (rev) references revinfo
);

create table spraakkode_aud
(
    identifier  varchar(255) not null,
    rev         integer      not null,
    revtype     smallint,
    description varchar(255),
    primary key (identifier, rev),
    constraint fkqyp3gqxwt8njr4wefdaeys0qh
        foreign key (rev) references revinfo
);

create table stemmekrets_aud
(
    objid                      varchar(255) not null,
    rev                        integer      not null,
    revtype                    smallint,
    endretav                   varchar(255),
    endretdato                 date,
    typeendring                varchar(255),
    datafangstdato             date,
    foerstedigitaliseringsdato date,
    gyldigfra                  date,
    gyldigtil                  date,
    slettet                    boolean,
    lokalid                    varchar(255),
    objtype                    varchar(255),
    omraade                    geometry,
    opphav                     varchar(255),
    posisjon                   geometry,
    stemmekretsnavn            varchar(255),
    stemmekretsnummer          varchar(255),
    tellekretsnavn             varchar(255),
    tellekretsnummer           varchar(255),
    valgdistriktsnummer        varchar(255),
    kommune_fk                 varchar(255),
    utledetfra                 varchar(255),
    primary key (objid, rev),
    constraint fk2fn8cn2vk0b86vp6yh215q93c
        foreign key (rev) references revinfo
);

create table stemmekrets_grense_aud
(
    grense_fk           varchar(255) not null,
    stemmekrets_fk      varchar(255) not null,
    rev                 integer      not null,
    revtype             smallint,
    flate_indeks        integer,
    hull_indeks         integer,
    rekkefoelge         integer,
    retning_med_klokken boolean,
    primary key (grense_fk, stemmekrets_fk, rev),
    constraint fkglb6v5bdpyp68wy3gsylmja9o
        foreign key (rev) references revinfo
);

create table stemmekrets_informasjon_aud
(
    objid          varchar(255) not null,
    rev            integer      not null,
    revtype        smallint,
    informasjon    varchar(255),
    stemmekrets_fk varchar(255),
    primary key (objid, rev),
    constraint fkoa1imp3ngshafl0ke5sp2d6u9
        foreign key (rev) references revinfo
);

create table typeendring_aud
(
    identifier varchar(255) not null,
    rev        integer      not null,
    revtype    smallint,
    primary key (identifier, rev),
    constraint fk8xpkv1s8ipodg6k7qp9o3at5u
        foreign key (rev) references revinfo
);

create table utkast_aud
(
    id             varchar(255) not null,
    rev            integer      not null,
    revtype        smallint,
    endretav       varchar(255),
    endretdato     date,
    typeendring    varchar(255),
    gyldig_fra     date,
    navn           varchar(255),
    utkast         jsonb,
    opprettet_dato timestamp,
    status         varchar(255),
    primary key (id, rev),
    constraint fksog1xc3rpn98o67r61tl0j19
        foreign key (rev) references revinfo
);

