CREATE TABLE identifikasjon (
                                objid serial NOT NULL,
                                objtype text DEFAULT 'Identifikasjon',
                                lokalid text NOT NULL,
                                navnerom text NOT NULL,
                                versjonid text,
                                punktreferanse_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE typeendring (
                             identifier text NOT NULL,
                             description text)
    WITH (OIDS=FALSE);

INSERT INTO typeendring (identifier, description) VALUES ('E', 'Endret');
INSERT INTO typeendring (identifier, description) VALUES ('N', 'Nytt');
INSERT INTO typeendring (identifier, description) VALUES ('S', 'Slettet');

CREATE TABLE fylke (
                       objid serial NOT NULL,
                       objtype text DEFAULT 'Fylke',
                       lokalid text NOT NULL,
                       navnerom text NOT NULL,
                       versjonid text,
                       oppdateringsdato timestamp with time zone,
                       sluttdato timestamp with time zone,
                       datauttaksdato timestamp with time zone,
                       opphav text,
                       produkt text,
                       versjon text,
                       gyldigfra timestamp with time zone,
                       gyldigtil timestamp with time zone,
                       omraadeid int,
                       originaldatavert text,
                       kopidato timestamp with time zone,
                       datafangstdato timestamp with time zone,
                       land text,
                       nivaanummer text,
                       navn text,
                       spraak text,
                       rekkefoelge int,
                       administrativkode text,
                       fylkesnummer text NOT NULL,
                       samiskforvaltningsomraade boolean,
                       nasjon_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE fylke_fylkesgrense (
                                    fylke_fk int,
                                    fylkesgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE fylkesgrense (
                              objid serial NOT NULL,
                              objtype text DEFAULT 'Fylkesgrense',
                              lokalid text NOT NULL,
                              navnerom text NOT NULL,
                              versjonid text,
                              oppdateringsdato timestamp with time zone NOT NULL,
                              sluttdato timestamp with time zone,
                              datauttaksdato timestamp with time zone,
                              typeendring text,
                              tidspunktendring timestamp with time zone,
                              maalemetode text,
                              noeyaktighet int,
                              opphav text,
                              produkt text,
                              versjon text,
                              gyldigfra timestamp with time zone,
                              gyldigtil timestamp with time zone,
                              omraadeid int,
                              originaldatavert text,
                              kopidato timestamp with time zone,
                              datafangstdato timestamp with time zone,
                              grense geometry(Linestring, 25833) NOT NULL,
                              omtvistet boolean,
                              grensestatus text,
                              fastsettingstype text,
                              foelgerterrengdetalj text,
                              noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE avgrensning_bispedoemme (
                                         avgrensning_fk int,
                                         bispedoemme_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE delomraade_fylkesgrense (
                                         delomraade_fk int,
                                         fylkesgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE fylkesgrense_grensepunktsjoe (
                                              fylkesgrense_fk int,
                                              grensepunktsjoe_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE avgrensning_prosti (
                                    avgrensning_fk int,
                                    prosti_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE stemmekrets_fylkesgrense (
                                          stemmekrets_fk int,
                                          fylkesgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets_fylkesgrense (
                                         grunnkrets_fk int,
                                         fylkesgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE avgrensning_sokn (
                                  avgrensning_fk int,
                                  sokn_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE skolekrets_fylkesgrense (
                                         skolekrets_fk int,
                                         fylkesgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE fylkesgrense_grensemerkeland (
                                              fylkesgrense_fk int,
                                              grensemerkeland_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE kommune_fylkesgrense (
                                      kommune_fk int,
                                      fylkesgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grensemerkeland (
                                 objid serial NOT NULL,
                                 objtype text DEFAULT 'GrensemerkeLand',
                                 lokalid text NOT NULL,
                                 navnerom text NOT NULL,
                                 versjonid text,
                                 oppdateringsdato timestamp with time zone,
                                 sluttdato timestamp with time zone,
                                 datauttaksdato timestamp with time zone,
                                 typeendring text,
                                 tidspunktendring timestamp with time zone,
                                 maalemetode text,
                                 noeyaktighet int,
                                 opphav text,
                                 produkt text,
                                 versjon text,
                                 gyldigfra timestamp with time zone,
                                 gyldigtil timestamp with time zone,
                                 omraadeid int,
                                 originaldatavert text,
                                 kopidato timestamp with time zone,
                                 datafangstdato timestamp with time zone,
                                 posisjon geometry(Point, 25833) NOT NULL,
                                 grensemerketype text NOT NULL,
                                 beskrivelse text,
                                 etableringsdato date,
                                 egenhoeyde double precision,
                                 land text)
    WITH (OIDS=FALSE);

CREATE TABLE grensemerkeland_kommunegrense (
                                               grensemerkeland_fk int,
                                               kommunegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE kommune (
                         objid serial NOT NULL,
                         objtype text DEFAULT 'Kommune',
                         lokalid text NOT NULL,
                         navnerom text NOT NULL,
                         versjonid text,
                         oppdateringsdato timestamp with time zone,
                         sluttdato timestamp with time zone,
                         datauttaksdato timestamp with time zone,
                         opphav text,
                         produkt text,
                         versjon text,
                         gyldigfra timestamp with time zone,
                         gyldigtil timestamp with time zone,
                         omraadeid int,
                         originaldatavert text,
                         kopidato timestamp with time zone,
                         datafangstdato timestamp with time zone,
                         land text,
                         nivaanummer text,
                         navn text,
                         spraak text,
                         rekkefoelge int,
                         administrativkode text,
                         kommunenummer text NOT NULL,
                         samiskforvaltningsomraade boolean NOT NULL,
                         fylke_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE kommune_kommunegrense (
                                       kommune_fk int,
                                       kommunegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE kommunegrense (
                               objid serial NOT NULL,
                               objtype text DEFAULT 'Kommunegrense',
                               lokalid text NOT NULL,
                               navnerom text NOT NULL,
                               versjonid text,
                               oppdateringsdato timestamp with time zone NOT NULL,
                               sluttdato timestamp with time zone,
                               datauttaksdato timestamp with time zone,
                               typeendring text,
                               tidspunktendring timestamp with time zone,
                               maalemetode text,
                               noeyaktighet int,
                               opphav text,
                               produkt text,
                               versjon text,
                               gyldigfra timestamp with time zone,
                               gyldigtil timestamp with time zone,
                               omraadeid int,
                               originaldatavert text,
                               kopidato timestamp with time zone,
                               datafangstdato timestamp with time zone,
                               grense geometry(Linestring, 25833) NOT NULL,
                               omtvistet boolean,
                               grensestatus text,
                               fastsettingstype text,
                               foelgerterrengdetalj text,
                               noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE grensepunktsjoe_kommunegrense (
                                               grensepunktsjoe_fk int,
                                               kommunegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets_kommunegrense (
                                          grunnkrets_fk int,
                                          kommunegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE delomraade_kommunegrense (
                                          delomraade_fk int,
                                          kommunegrense_fk int)
    WITH (OIDS=FALSE);

--CREATE TABLE avgrensning_prosti (
--avgrensning_fk int,
--prosti_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE skolekrets_kommunegrense (
                                          skolekrets_fk int,
                                          kommunegrense_fk int)
    WITH (OIDS=FALSE);

--CREATE TABLE avgrensning_sokn (
--avgrensning_fk int,
--sokn_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE stemmekrets_kommunegrense (
                                           stemmekrets_fk int,
                                           kommunegrense_fk int)
    WITH (OIDS=FALSE);

--CREATE TABLE avgrensning_bispedoemme (
--avgrensning_fk int,
--bispedoemme_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE nasjon (
                        objid serial NOT NULL,
                        objtype text DEFAULT 'Nasjon',
                        lokalid text NOT NULL,
                        navnerom text NOT NULL,
                        versjonid text,
                        oppdateringsdato timestamp with time zone,
                        sluttdato timestamp with time zone,
                        datauttaksdato timestamp with time zone,
                        opphav text,
                        produkt text,
                        versjon text,
                        gyldigfra timestamp with time zone,
                        gyldigtil timestamp with time zone,
                        omraadeid int,
                        originaldatavert text,
                        kopidato timestamp with time zone,
                        datafangstdato timestamp with time zone,
                        land text,
                        nivaanummer text,
                        navn text,
                        spraak text,
                        rekkefoelge int,
                        administrativkode text)
    WITH (OIDS=FALSE);

CREATE TABLE administrativenhetnavn (
                                        objid serial NOT NULL,
                                        objtype text DEFAULT 'AdministrativEnhetNavn',
                                        navn text NOT NULL,
                                        spraak text NOT NULL,
                                        rekkefoelge int,
                                        fylke_fk int,
                                        kommune_fk int,
                                        nasjon_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grensepunkttypekode (
                                     identifier text NOT NULL,
                                     description text)
    WITH (OIDS=FALSE);

INSERT INTO grensepunkttypekode (identifier, description) VALUES ('83', 'AnnenDetalj');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('66', 'Annet');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('94', 'Asfaltspiker');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('52', 'Bolt');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('69', 'Borehull');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('87', 'Djupålen');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('57', 'Fastmerke');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('99', 'GeometriskHjelpepunkt');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('82', 'Gjerdestolpe');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('64', 'Grensepåle');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('55', 'GrensesteinEllerRøys');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('65', 'Grensetre');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('90', 'Hushjørne');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('67', 'Kamjern');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('53', 'Kors');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('86', 'MidtIBekkElv');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('84', 'MidtIVeg');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('89', 'Murhjørne');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('51', 'OffentligGodkjentGrensemerke');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('98', 'Retningspunkt');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('59', 'RiksgrenseAnnet');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('50', 'Riksgrensemerke');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('58', 'Riksgrenserøys');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('54', 'Rør');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('60', 'Steingjerde');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('63', 'SteinMedKorsOgVitner');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('97', 'Ukjent');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('56', 'Umerket');
INSERT INTO grensepunkttypekode (identifier, description) VALUES ('85', 'VegKant');

CREATE TABLE nivaanummer (
                             identifier text NOT NULL,
                             description text)
    WITH (OIDS=FALSE);

INSERT INTO nivaanummer (identifier, description) VALUES ('FørsteNivå', 'FørsteNivå');
INSERT INTO nivaanummer (identifier, description) VALUES ('AndreNivå', 'AndreNivå');
INSERT INTO nivaanummer (identifier, description) VALUES ('TredjeNivå', 'TredjeNivå');
INSERT INTO nivaanummer (identifier, description) VALUES ('FjerdeNivå', 'FjerdeNivå');

CREATE TABLE noeyaktighetsklassekode (
                                         identifier text NOT NULL,
                                         description text)
    WITH (OIDS=FALSE);

INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('NøyaktigeMålinger', 'NøyaktigeMålinger');
INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('MiddelsNøyaktigeOgTransformerteMålinger', 'MiddelsNøyaktigeOgTransformerteMålinger');
INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('MindreNøyaktigeMålingerOgGrenserFraTK', 'MindreNøyaktigeMålingerOgGrenserFraTK');
INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('MindreNøyaktigeGrenserOgGrenserFraØK', 'MindreNøyaktigeGrenserOgGrenserFraØK');
INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('LiteNøyaktigeGrenserOgGrenserFraN50', 'LiteNøyaktigeGrenserOgGrenserFraN50');
INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('SkisserteGrenser', 'SkisserteGrenser');
INSERT INTO noeyaktighetsklassekode (identifier, description) VALUES ('IngenNøyaktighet', 'IngenNøyaktighet');

CREATE TABLE spraakkode (
                            identifier text NOT NULL,
                            description text)
    WITH (OIDS=FALSE);

INSERT INTO spraakkode (identifier, description) VALUES ('eng', 'English');
INSERT INTO spraakkode (identifier, description) VALUES ('fkv', 'Kven Finnish');
INSERT INTO spraakkode (identifier, description) VALUES ('smj', 'Lule Sami');
INSERT INTO spraakkode (identifier, description) VALUES ('sme', 'Northern Sami');
INSERT INTO spraakkode (identifier, description) VALUES ('nor', 'Norwegian');
INSERT INTO spraakkode (identifier, description) VALUES ('sma', 'Southern Sami');

CREATE TABLE terrengdetaljkode (
                                   identifier text NOT NULL,
                                   description text)
    WITH (OIDS=FALSE);

INSERT INTO terrengdetaljkode (identifier, description) VALUES ('ANN', 'Annet');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('AGJ', 'AnnetGjerde');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('BYG', 'Bygning');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('BYA', 'BygningsAnlegg');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('EBD', 'ElveBekkeDjupål');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('EBK', 'ElveBekkeKant');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('EBM', 'ElveBekkeMidt');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('GGM', 'GeneralisertGeometriskMidtlinje');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('HKO', 'Havkontur');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('ITD', 'IkkeTerrengdetalj');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('IKA', 'InnsjøKant');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('LBR', 'Lavbrekk');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('SGJ', 'Steingjerde');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('SKJ', 'StiKjerreveg');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('VSK', 'Vannskille');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('VKA', 'VegKant');
INSERT INTO terrengdetaljkode (identifier, description) VALUES ('VMI', 'VegMidt');

CREATE TABLE brytningspunkt (
                                objid serial NOT NULL,
                                objtype text DEFAULT 'Brytningspunkt',
                                lokalid text NOT NULL,
                                navnerom text NOT NULL,
                                versjonid text,
                                datafangstdato timestamp with time zone,
                                oppdateringsdato timestamp with time zone,
                                sluttdato timestamp with time zone,
                                gyldigfra timestamp with time zone,
                                gyldigtil timestamp with time zone,
                                datauttaksdato timestamp with time zone,
                                typeendring text,
                                tidspunktendring timestamp with time zone,
                                maalemetode text NOT NULL,
                                noeyaktighet int,
                                opphav text,
                                stedfestingverifisert boolean,
                                produkt text,
                                versjon text,
                                posisjon geometry(Point, 25833) NOT NULL,
                                beskrivelse text,
                                etableringsdato timestamp with time zone,
                                vegbeskrivelse text,
                                andremarkeringer text,
                                egenhoeyde double precision,
                                underlag text,
                                beregningsdato timestamp with time zone,
                                skjema text,
                                grensemerkenummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_vassdragspunkt (
                                                       riksgrense_fk int,
                                                       grensemerke_vassdragspunkt_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_riksroeys (
                                                  riksgrense_fk int,
                                                  grensemerke_riksroeys_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_profilpunkt (
                                                    riksgrense_fk int,
                                                    grensemerke_profilpunkt_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_brytningspunkt (
                                                       riksgrense_fk int,
                                                       grensemerke_brytningspunkt_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_viserstein (
                                                   riksgrense_fk int,
                                                   grensemerke_viserstein_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_oevrighjelpemerke (
                                                          riksgrense_fk int,
                                                          grensemerke_oevrighjelpemerke_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_storutligger (
                                                     riksgrense_fk int,
                                                     grensemerke_storutligger_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_litenutligger (
                                                      riksgrense_fk int,
                                                      grensemerke_litenutligger_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_grensestolpe (
                                                     riksgrense_fk int,
                                                     grensemerke_grensestolpe_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_grensemerke_grenseskilt (
                                                    riksgrense_fk int,
                                                    grensemerke_grenseskilt_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grenseskilt (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Grenseskilt',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             maalemetode text NOT NULL,
                             noeyaktighet int,
                             opphav text,
                             stedfestingverifisert boolean,
                             produkt text,
                             versjon text,
                             posisjon geometry(Point, 25833) NOT NULL,
                             beskrivelse text,
                             etableringsdato timestamp with time zone,
                             vegbeskrivelse text,
                             andremarkeringer text,
                             egenhoeyde double precision,
                             underlag text,
                             beregningsdato timestamp with time zone,
                             skjema text,
                             skilttype text NOT NULL,
                             nummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE grensestolpe (
                              objid serial NOT NULL,
                              objtype text DEFAULT 'Grensestolpe',
                              lokalid text NOT NULL,
                              navnerom text NOT NULL,
                              versjonid text,
                              datafangstdato timestamp with time zone,
                              oppdateringsdato timestamp with time zone,
                              sluttdato timestamp with time zone,
                              gyldigfra timestamp with time zone,
                              gyldigtil timestamp with time zone,
                              datauttaksdato timestamp with time zone,
                              typeendring text,
                              tidspunktendring timestamp with time zone,
                              maalemetode text NOT NULL,
                              noeyaktighet int,
                              opphav text,
                              stedfestingverifisert boolean,
                              produkt text,
                              versjon text,
                              posisjon geometry(Point, 25833) NOT NULL,
                              beskrivelse text,
                              etableringsdato timestamp with time zone,
                              vegbeskrivelse text,
                              andremarkeringer text,
                              egenhoeyde double precision,
                              underlag text,
                              beregningsdato timestamp with time zone,
                              skjema text,
                              grensemerkenummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE litenutligger (
                               objid serial NOT NULL,
                               objtype text DEFAULT 'LitenUtligger',
                               lokalid text NOT NULL,
                               navnerom text NOT NULL,
                               versjonid text,
                               datafangstdato timestamp with time zone,
                               oppdateringsdato timestamp with time zone,
                               sluttdato timestamp with time zone,
                               gyldigfra timestamp with time zone,
                               gyldigtil timestamp with time zone,
                               datauttaksdato timestamp with time zone,
                               typeendring text,
                               tidspunktendring timestamp with time zone,
                               maalemetode text NOT NULL,
                               noeyaktighet int,
                               opphav text,
                               stedfestingverifisert boolean,
                               produkt text,
                               versjon text,
                               posisjon geometry(Point, 25833) NOT NULL,
                               beskrivelse text,
                               etableringsdato timestamp with time zone,
                               vegbeskrivelse text,
                               andremarkeringer text,
                               egenhoeyde double precision,
                               underlag text,
                               beregningsdato timestamp with time zone,
                               skjema text,
                               nummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE profilpunkt (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Profilpunkt',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             maalemetode text NOT NULL,
                             noeyaktighet int,
                             opphav text,
                             stedfestingverifisert boolean,
                             produkt text,
                             versjon text,
                             posisjon geometry(Point, 25833) NOT NULL,
                             beskrivelse text,
                             etableringsdato timestamp with time zone,
                             vegbeskrivelse text,
                             andremarkeringer text,
                             egenhoeyde double precision,
                             underlag text,
                             beregningsdato timestamp with time zone,
                             skjema text,
                             grensemerkenummer text NOT NULL,
                             avstandnorskstolpe double precision,
                             avstandrussiskstolpe double precision,
                             markertpunkt boolean)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Riksgrense',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            maalemetode text NOT NULL,
                            noeyaktighet int,
                            opphav text,
                            stedfestingverifisert boolean,
                            produkt text,
                            versjon text,
                            grense geometry(Linestring, 25833) NOT NULL,
                            foelgerterrengdetalj text,
                            grensegatebredde double precision,
                            grensestatus text,
                            fastsettingstype text)
    WITH (OIDS=FALSE);

--CREATE TABLE avgrensning_prosti (
--avgrensning_fk int,
--prosti_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE riksroeys (
                           objid serial NOT NULL,
                           objtype text DEFAULT 'Riksrøys',
                           lokalid text NOT NULL,
                           navnerom text NOT NULL,
                           versjonid text,
                           datafangstdato timestamp with time zone,
                           oppdateringsdato timestamp with time zone,
                           sluttdato timestamp with time zone,
                           gyldigfra timestamp with time zone,
                           gyldigtil timestamp with time zone,
                           datauttaksdato timestamp with time zone,
                           typeendring text,
                           tidspunktendring timestamp with time zone,
                           maalemetode text NOT NULL,
                           noeyaktighet int,
                           opphav text,
                           stedfestingverifisert boolean,
                           produkt text,
                           versjon text,
                           posisjon geometry(Point, 25833) NOT NULL,
                           beskrivelse text,
                           etableringsdato timestamp with time zone,
                           vegbeskrivelse text,
                           andremarkeringer text,
                           egenhoeyde double precision,
                           underlag text,
                           beregningsdato timestamp with time zone,
                           skjema text,
                           grensemerkenummer text NOT NULL,
                           diameterfot double precision,
                           diametertopp double precision)
    WITH (OIDS=FALSE);

CREATE TABLE sikringspunkt (
                               objid serial NOT NULL,
                               objtype text DEFAULT 'Sikringspunkt',
                               posisjon geometry(Point, 25833) NOT NULL,
                               nummer text NOT NULL,
                               sikringspunkttype text NOT NULL,
                               egenhoeyde double precision,
                               materialebolt text,
                               diameter double precision,
                               merknad text,
                               underlag text,
                               riksroeys_fk int,
                               utligger_storutligger_fk int,
                               utligger_litenutligger_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE storutligger (
                              objid serial NOT NULL,
                              objtype text DEFAULT 'StorUtligger',
                              lokalid text NOT NULL,
                              navnerom text NOT NULL,
                              versjonid text,
                              datafangstdato timestamp with time zone,
                              oppdateringsdato timestamp with time zone,
                              sluttdato timestamp with time zone,
                              gyldigfra timestamp with time zone,
                              gyldigtil timestamp with time zone,
                              datauttaksdato timestamp with time zone,
                              typeendring text,
                              tidspunktendring timestamp with time zone,
                              maalemetode text NOT NULL,
                              noeyaktighet int,
                              opphav text,
                              stedfestingverifisert boolean,
                              produkt text,
                              versjon text,
                              posisjon geometry(Point, 25833) NOT NULL,
                              beskrivelse text,
                              etableringsdato timestamp with time zone,
                              vegbeskrivelse text,
                              andremarkeringer text,
                              egenhoeyde double precision,
                              underlag text,
                              beregningsdato timestamp with time zone,
                              skjema text,
                              nummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE vassdragspunkt (
                                objid serial NOT NULL,
                                objtype text DEFAULT 'Vassdragspunkt',
                                lokalid text NOT NULL,
                                navnerom text NOT NULL,
                                versjonid text,
                                datafangstdato timestamp with time zone,
                                oppdateringsdato timestamp with time zone,
                                sluttdato timestamp with time zone,
                                gyldigfra timestamp with time zone,
                                gyldigtil timestamp with time zone,
                                datauttaksdato timestamp with time zone,
                                typeendring text,
                                tidspunktendring timestamp with time zone,
                                maalemetode text NOT NULL,
                                noeyaktighet int,
                                opphav text,
                                stedfestingverifisert boolean,
                                produkt text,
                                versjon text,
                                posisjon geometry(Point, 25833) NOT NULL,
                                beskrivelse text,
                                etableringsdato timestamp with time zone,
                                vegbeskrivelse text,
                                andremarkeringer text,
                                egenhoeyde double precision,
                                underlag text,
                                beregningsdato timestamp with time zone,
                                skjema text)
    WITH (OIDS=FALSE);

CREATE TABLE viserstein (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Viserstein',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            maalemetode text NOT NULL,
                            noeyaktighet int,
                            opphav text,
                            stedfestingverifisert boolean,
                            produkt text,
                            versjon text,
                            posisjon geometry(Point, 25833) NOT NULL,
                            beskrivelse text,
                            etableringsdato timestamp with time zone,
                            vegbeskrivelse text,
                            andremarkeringer text,
                            egenhoeyde double precision,
                            underlag text,
                            beregningsdato timestamp with time zone,
                            skjema text)
    WITH (OIDS=FALSE);

CREATE TABLE oevrighjelpemerke (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'ØvrigHjelpemerke',
                                   lokalid text NOT NULL,
                                   navnerom text NOT NULL,
                                   versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   maalemetode text NOT NULL,
                                   noeyaktighet int,
                                   opphav text,
                                   stedfestingverifisert boolean,
                                   produkt text,
                                   versjon text,
                                   posisjon geometry(Point, 25833) NOT NULL,
                                   beskrivelse text,
                                   etableringsdato timestamp with time zone,
                                   vegbeskrivelse text,
                                   andremarkeringer text,
                                   egenhoeyde double precision,
                                   underlag text,
                                   beregningsdato timestamp with time zone,
                                   skjema text,
                                   type text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE hjertesteininformasjon (
                                        objid serial NOT NULL,
                                        objtype text DEFAULT 'Hjertesteininformasjon',
                                        egenhoeydehjertestein double precision,
                                        inskripsjonhjertestein text,
                                        etableringsdatohjertestein date,
                                        riksroeys_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE naermestegrensemerke (
                                      objid serial NOT NULL,
                                      objtype text DEFAULT 'NærmesteGrensemerke',
                                      grensemerkenummer text NOT NULL,
                                      grensemerkeavstand double precision,
                                      grensemerkeretning double precision,
                                      brytningspunkt_fk int,
                                      riksroeys_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE registrertekoordinater (
                                        objid serial NOT NULL,
                                        objtype text DEFAULT 'RegistrerteKoordinater',
                                        nordkoordinat double precision NOT NULL,
                                        oestkoordinat double precision NOT NULL,
                                        koordinatsystem text NOT NULL,
                                        kilde text,
                                        brytningspunkt_fk int,
                                        grenseskilt_fk int,
                                        grensestolpe_fk int,
                                        litenutligger_fk int,
                                        profilpunkt_fk int,
                                        riksroeys_fk int,
                                        storutligger_fk int,
                                        vassdragspunkt_fk int,
                                        viserstein_fk int,
                                        oevrighjelpemerke_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE registrerthoeyde (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'RegistrertHøyde',
                                  hoeyde double precision NOT NULL,
                                  hoeydereferansesystem text NOT NULL,
                                  hoeydereferanse text NOT NULL,
                                  kilde text,
                                  brytningspunkt_fk int,
                                  grenseskilt_fk int,
                                  grensestolpe_fk int,
                                  litenutligger_fk int,
                                  profilpunkt_fk int,
                                  riksroeys_fk int,
                                  storutligger_fk int,
                                  vassdragspunkt_fk int,
                                  viserstein_fk int,
                                  oevrighjelpemerke_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE vedlikehold (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Vedlikehold',
                             arbeidutfoertdato date NOT NULL,
                             arbeidutfoertbeskrivelse text,
                             brytningspunkt_fk int,
                             grenseskilt_fk int,
                             grensestolpe_fk int,
                             litenutligger_fk int,
                             profilpunkt_fk int,
                             riksroeys_fk int,
                             storutligger_fk int,
                             vassdragspunkt_fk int,
                             viserstein_fk int,
                             oevrighjelpemerke_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE hoeydereferanse (
                                 identifier text NOT NULL,
                                 description text)
    WITH (OIDS=FALSE);

INSERT INTO hoeydereferanse (identifier, description) VALUES ('FOT', 'Høyden målt til foten av objektet');
INSERT INTO hoeydereferanse (identifier, description) VALUES ('TOP', 'Høyden målt til toppen av objektet');
INSERT INTO hoeydereferanse (identifier, description) VALUES ('UKJENT', 'Ukjent');

CREATE TABLE hoeydereferansesystem (
                                       identifier text NOT NULL,
                                       description text)
    WITH (OIDS=FALSE);

INSERT INTO hoeydereferansesystem (identifier, description) VALUES ('ELLIP', 'Ellipsoide jf. KOORDSYS');
INSERT INTO hoeydereferansesystem (identifier, description) VALUES ('LOKAL', 'Lokal referanseflate');
INSERT INTO hoeydereferansesystem (identifier, description) VALUES ('NKG89', 'Geoide bestemt av NKG i 1989');
INSERT INTO hoeydereferansesystem (identifier, description) VALUES ('NN54', 'Norsk Null av 1954');
INSERT INTO hoeydereferansesystem (identifier, description) VALUES ('NNN57', 'Nord-Norsk Null av 1957');
INSERT INTO hoeydereferansesystem (identifier, description) VALUES ('NN2000', 'Norsk Null av 2000');

CREATE TABLE materialebolt (
                               identifier text NOT NULL,
                               description text)
    WITH (OIDS=FALSE);

INSERT INTO materialebolt (identifier, description) VALUES ('Messing', 'Messing');
INSERT INTO materialebolt (identifier, description) VALUES ('Stål', 'Stål');
INSERT INTO materialebolt (identifier, description) VALUES ('Jern', 'Jern');
INSERT INTO materialebolt (identifier, description) VALUES ('Kopper', 'Kopper');
INSERT INTO materialebolt (identifier, description) VALUES ('Aluminium', 'Aluminium');
INSERT INTO materialebolt (identifier, description) VALUES ('Plast', 'Plast');
INSERT INTO materialebolt (identifier, description) VALUES ('Annet', 'Annet');

CREATE TABLE referansesystemkode (
                                     identifier text NOT NULL,
                                     description text)
    WITH (OIDS=FALSE);

INSERT INTO referansesystemkode (identifier, description) VALUES ('99', 'Annet');
INSERT INTO referansesystemkode (identifier, description) VALUES ('50', 'ED 50 Geografisk, ingen projeksjon');
INSERT INTO referansesystemkode (identifier, description) VALUES ('87', 'ED 87 Geografisk');
INSERT INTO referansesystemkode (identifier, description) VALUES ('73', 'ETRS89-LAEA');
INSERT INTO referansesystemkode (identifier, description) VALUES ('74', 'ETRS89-LCC');
INSERT INTO referansesystemkode (identifier, description) VALUES ('84', 'EUREF89 Geografisk');
INSERT INTO referansesystemkode (identifier, description) VALUES ('210', 'EUREF89 NTM Sone 10');
INSERT INTO referansesystemkode (identifier, description) VALUES ('211', 'EUREF89 NTM Sone 11');
INSERT INTO referansesystemkode (identifier, description) VALUES ('212', 'EUREF89 NTM Sone 12');
INSERT INTO referansesystemkode (identifier, description) VALUES ('214', 'EUREF89 NTM Sone 14');
INSERT INTO referansesystemkode (identifier, description) VALUES ('215', 'EUREF89 NTM Sone 15');
INSERT INTO referansesystemkode (identifier, description) VALUES ('216', 'EUREF89 NTM Sone 16');
INSERT INTO referansesystemkode (identifier, description) VALUES ('217', 'EUREF89 NTM Sone 17');
INSERT INTO referansesystemkode (identifier, description) VALUES ('218', 'EUREF89 NTM Sone 18');
INSERT INTO referansesystemkode (identifier, description) VALUES ('219', 'EUREF89 NTM Sone 19');
INSERT INTO referansesystemkode (identifier, description) VALUES ('220', 'EUREF89 NTM Sone 20');
INSERT INTO referansesystemkode (identifier, description) VALUES ('221', 'EUREF89 NTM Sone 21');
INSERT INTO referansesystemkode (identifier, description) VALUES ('222', 'EUREF89 NTM Sone 22');
INSERT INTO referansesystemkode (identifier, description) VALUES ('223', 'EUREF89 NTM Sone 23');
INSERT INTO referansesystemkode (identifier, description) VALUES ('224', 'EUREF89 NTM Sone 24');
INSERT INTO referansesystemkode (identifier, description) VALUES ('225', 'EUREF89 NTM Sone 25');
INSERT INTO referansesystemkode (identifier, description) VALUES ('226', 'EUREF89 NTM Sone 26');
INSERT INTO referansesystemkode (identifier, description) VALUES ('227', 'EUREF89 NTM Sone 27');
INSERT INTO referansesystemkode (identifier, description) VALUES ('228', 'EUREF89 NTM Sone 28');
INSERT INTO referansesystemkode (identifier, description) VALUES ('229', 'EUREF89 NTM Sone 29');
INSERT INTO referansesystemkode (identifier, description) VALUES ('230', 'EUREF89 NTM Sone 30');
INSERT INTO referansesystemkode (identifier, description) VALUES ('205', 'EUREF89 NTM Sone 5');
INSERT INTO referansesystemkode (identifier, description) VALUES ('206', 'EUREF89 NTM Sone 6');
INSERT INTO referansesystemkode (identifier, description) VALUES ('207', 'EUREF89 NTM Sone 7');
INSERT INTO referansesystemkode (identifier, description) VALUES ('208', 'EUREF89 NTM Sone 8');
INSERT INTO referansesystemkode (identifier, description) VALUES ('209', 'EUREF89 NTM Sone 9');
INSERT INTO referansesystemkode (identifier, description) VALUES ('19', 'EUREF89 UTM Sone 29 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('20', 'EUREF89 UTM Sone 30 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('213', 'EYREF89 NTM Sone 13');
INSERT INTO referansesystemkode (identifier, description) VALUES ('75', 'ITRS');
INSERT INTO referansesystemkode (identifier, description) VALUES ('110', 'Lokalt nett, Ålesund');
INSERT INTO referansesystemkode (identifier, description) VALUES ('103', 'Lokalt nett, Asker');
INSERT INTO referansesystemkode (identifier, description) VALUES ('102', 'Lokalt nett, Bærum');
INSERT INTO referansesystemkode (identifier, description) VALUES ('106', 'Lokalt nett, Bergen/Askøy');
INSERT INTO referansesystemkode (identifier, description) VALUES ('108', 'Lokalt nett, Bodø');
INSERT INTO referansesystemkode (identifier, description) VALUES ('105', 'Lokalt nett, Drammen');
INSERT INTO referansesystemkode (identifier, description) VALUES ('109', 'Lokalt nett, Kristiansund');
INSERT INTO referansesystemkode (identifier, description) VALUES ('104', 'Lokalt nett, Lillehammer');
INSERT INTO referansesystemkode (identifier, description) VALUES ('101', 'Lokalt nett, Oslo');
INSERT INTO referansesystemkode (identifier, description) VALUES ('107', 'Lokalt nett, Trondheim');
INSERT INTO referansesystemkode (identifier, description) VALUES ('41', 'Lokalt nett, uspesifisert');
INSERT INTO referansesystemkode (identifier, description) VALUES ('9', 'NGO1948 , datum NGO1948, Geografisk');
INSERT INTO referansesystemkode (identifier, description) VALUES ('51', 'NGO-56A basert på NGO1948');
INSERT INTO referansesystemkode (identifier, description) VALUES ('52', 'NGO-56B basert på NGO1948');
INSERT INTO referansesystemkode (identifier, description) VALUES ('53', 'NGO-64A basert på NGO1948');
INSERT INTO referansesystemkode (identifier, description) VALUES ('54', 'NGO-64B basert på NGO1948');
INSERT INTO referansesystemkode (identifier, description) VALUES ('1', 'NGO-akse I, NGO1948,  Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('2', 'NGO-akse II,  NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('3', 'NGO-akse III, NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('4', 'NGO-akse IV, NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('5', 'NGO-akse V, NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('6', 'NGO-akse VI, NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('7', 'NGO-akse VII, NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('8', 'NGO-akse VIII, NGO1948, Gauss-Krüger');
INSERT INTO referansesystemkode (identifier, description) VALUES ('31', 'UTM sone 31 basert på ED50');
INSERT INTO referansesystemkode (identifier, description) VALUES ('21', 'UTM sone 31,basert på  EUREF89');
INSERT INTO referansesystemkode (identifier, description) VALUES ('32', 'UTM sone 32 basert på ED50');
INSERT INTO referansesystemkode (identifier, description) VALUES ('22', 'UTM sone 32 basert på EUREF89');
INSERT INTO referansesystemkode (identifier, description) VALUES ('33', 'UTM sone 33 basert på ED50');
INSERT INTO referansesystemkode (identifier, description) VALUES ('23', 'UTM sone 33 basert på EUREF89');
INSERT INTO referansesystemkode (identifier, description) VALUES ('34', 'UTM sone 34 basert på ED50');
INSERT INTO referansesystemkode (identifier, description) VALUES ('24', 'UTM sone 34 basert på EUREF89');
INSERT INTO referansesystemkode (identifier, description) VALUES ('35', 'UTM sone 35 basert på ED50');
INSERT INTO referansesystemkode (identifier, description) VALUES ('25', 'UTM sone 35 basert på EUREF89');
INSERT INTO referansesystemkode (identifier, description) VALUES ('36', 'UTM sone 36 basert på ED50');
INSERT INTO referansesystemkode (identifier, description) VALUES ('26', 'UTM sone 36 basert på EUREF89');
INSERT INTO referansesystemkode (identifier, description) VALUES ('72', 'WGS72 Geografisk, ingen projeksjon');
INSERT INTO referansesystemkode (identifier, description) VALUES ('184', 'WGS84 Geografisk 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('59', 'WGS84 UTM 29 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('60', 'WGS84 UTM 30 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('61', 'WGS84 UTM 31 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('62', 'WGS84 UTM 32 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('63', 'WGS84 UTM 33 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('64', 'WGS84 UTM 34 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('65', 'WGS84 UTM 35 2d');
INSERT INTO referansesystemkode (identifier, description) VALUES ('66', 'WGS84 UTM 36 2d');

CREATE TABLE sikringspunkttype (
                                   identifier text NOT NULL,
                                   description text)
    WITH (OIDS=FALSE);

INSERT INTO sikringspunkttype (identifier, description) VALUES ('Sikringsbolt', 'Sikringsbolt');
INSERT INTO sikringspunkttype (identifier, description) VALUES ('TrigonometriskPunkt', 'TrigonometriskPunkt');
INSERT INTO sikringspunkttype (identifier, description) VALUES ('Borehull', 'Borehull');
INSERT INTO sikringspunkttype (identifier, description) VALUES ('Rør', 'Rør');
INSERT INTO sikringspunkttype (identifier, description) VALUES ('OffentligGodkjentGrensemerke', 'OffentligGodkjentGrensemerke');
INSERT INTO sikringspunkttype (identifier, description) VALUES ('Annet', 'Annet');

CREATE TABLE skilttype (
                           identifier text NOT NULL,
                           description text)
    WITH (OIDS=FALSE);

INSERT INTO skilttype (identifier, description) VALUES ('TypeA', 'TypeA');
INSERT INTO skilttype (identifier, description) VALUES ('TypeB', 'TypeB');
INSERT INTO skilttype (identifier, description) VALUES ('TypeC', 'TypeC');
INSERT INTO skilttype (identifier, description) VALUES ('TypeD', 'TypeD');
INSERT INTO skilttype (identifier, description) VALUES ('AndreOpplysningsskilt', 'AndreOpplysningsskilt');

CREATE TABLE underlag (
                          identifier text NOT NULL,
                          description text)
    WITH (OIDS=FALSE);

INSERT INTO underlag (identifier, description) VALUES ('Fjell', 'Fjell');
INSERT INTO underlag (identifier, description) VALUES ('Stein', 'Stein');
INSERT INTO underlag (identifier, description) VALUES ('Jord', 'Jord');
INSERT INTO underlag (identifier, description) VALUES ('Betong', 'Betong');
INSERT INTO underlag (identifier, description) VALUES ('Myr', 'Myr');
INSERT INTO underlag (identifier, description) VALUES ('Morene', 'Morene');
INSERT INTO underlag (identifier, description) VALUES ('Annet', 'Annet');

CREATE TABLE delomraade (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Delområde',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            opphav text,
                            produkt text,
                            versjon text,
                            omraadeid int,
                            originaldatavert text,
                            kopidato timestamp with time zone,
                            posisjon geometry(Point, 25833),
                            delomraadenummer text NOT NULL,
                            delomraadenavn text,
                            kommunenummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE delomraade_delomraadegrens (
                                            delomraade_fk int,
                                            delomraadegrens_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE delomraadegrense (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'Delområdegrense',
                                  lokalid text NOT NULL,
                                  navnerom text NOT NULL,
                                  versjonid text,
                                  datafangstdato timestamp with time zone,
                                  oppdateringsdato timestamp with time zone,
                                  sluttdato timestamp with time zone,
                                  gyldigfra timestamp with time zone,
                                  gyldigtil timestamp with time zone,
                                  datauttaksdato timestamp with time zone,
                                  typeendring text,
                                  tidspunktendring timestamp with time zone,
                                  maalemetode text,
                                  noeyaktighet int,
                                  opphav text,
                                  produkt text,
                                  versjon text,
                                  omraadeid int,
                                  originaldatavert text,
                                  kopidato timestamp with time zone,
                                  grense geometry(Linestring, 25833) NOT NULL,
                                  foelgerterrengdetalj text,
                                  foelgerteiggrense boolean,
                                  noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets_delomraadegrense (
                                             grunnkrets_fk int,
                                             delomraadegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Grunnkrets',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            opphav text,
                            produkt text,
                            versjon text,
                            omraadeid int,
                            originaldatavert text,
                            kopidato timestamp with time zone,
                            posisjon geometry(Point, 25833),
                            grunnkretsnummer text NOT NULL,
                            grunnkretsnavn text,
                            kommunenummer text NOT NULL,
                            delomraadeinndeling_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets_grunnkretsgrense (
                                             grunnkrets_fk int,
                                             grunnkretsgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkretsgrense (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'Grunnkretsgrense',
                                  lokalid text NOT NULL,
                                  navnerom text NOT NULL,
                                  versjonid text,
                                  datafangstdato timestamp with time zone,
                                  oppdateringsdato timestamp with time zone,
                                  sluttdato timestamp with time zone,
                                  gyldigfra timestamp with time zone,
                                  gyldigtil timestamp with time zone,
                                  datauttaksdato timestamp with time zone,
                                  typeendring text,
                                  tidspunktendring timestamp with time zone,
                                  maalemetode text,
                                  noeyaktighet int,
                                  opphav text,
                                  produkt text,
                                  versjon text,
                                  omraadeid int,
                                  originaldatavert text,
                                  kopidato timestamp with time zone,
                                  grense geometry(Linestring, 25833) NOT NULL,
                                  foelgerterrengdetalj text,
                                  foelgerteiggrense boolean,
                                  noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE valgdistriktsgrense (
                                     objid serial NOT NULL,
                                     objtype text DEFAULT 'Valgdistriktsgrense',
                                     identifikasjon_lokalid text NOT NULL,
                                     identifikasjon_navnerom text NOT NULL,
                                     identifikasjon_versjonid text,
                                     datafangstdato timestamp with time zone,
                                     oppdateringsdato timestamp with time zone,
                                     sluttdato timestamp with time zone,
                                     gyldigfra timestamp with time zone,
                                     gyldigtil timestamp with time zone,
                                     datauttaksdato timestamp with time zone,
                                     typeendring text,
                                     tidspunktendring timestamp with time zone,
                                     maalemetode text,
                                     noeyaktighet int,
                                     opphav text,
                                     produkt text,
                                     versjon text,
                                     omraadeid int,
                                     originaldatavert text,
                                     kopidato timestamp with time zone,
                                     grense geometry(Linestring, 25833) NOT NULL,
                                     foelgergrunnkretsgrense_lokalid text,
                                     foelgergrunnkretsgrense_navnerom text,
                                     foelgergrunnkretsgrense_versjonid text,
                                     foelgerterrengdetalj text,
                                     noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE stemmekrets_valgdistriktsgrense (
                                                 stemmekrets_fk int,
                                                 valgdistriktsgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE skolekrets (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Skolekrets',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            opphav text,
                            produkt text,
                            versjon text,
                            omraadeid int,
                            originaldatavert text,
                            kopidato timestamp with time zone,
                            informasjon text,
                            posisjon geometry(Point, 25833),
                            skolekretsnummer text NOT NULL,
                            skolekretsnavn text,
                            kommunenummer text NOT NULL,
                            barnetrinn boolean,
                            ungdomstrinn boolean)
    WITH (OIDS=FALSE);

CREATE TABLE skolekrets_skolekretsgrense (
                                             skolekrets_fk int,
                                             skolekretsgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE skolekretsgrense (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'Skolekretsgrense',
                                  identifikasjon_lokalid text NOT NULL,
                                  identifikasjon_navnerom text NOT NULL,
                                  identifikasjon_versjonid text,
                                  datafangstdato timestamp with time zone,
                                  oppdateringsdato timestamp with time zone,
                                  sluttdato timestamp with time zone,
                                  gyldigfra timestamp with time zone,
                                  gyldigtil timestamp with time zone,
                                  datauttaksdato timestamp with time zone,
                                  typeendring text,
                                  tidspunktendring timestamp with time zone,
                                  maalemetode text,
                                  noeyaktighet int,
                                  opphav text,
                                  produkt text,
                                  versjon text,
                                  omraadeid int,
                                  originaldatavert text,
                                  kopidato timestamp with time zone,
                                  grense geometry(Linestring, 25833) NOT NULL,
                                  foelgergrunnkretsgrense_lokalid text,
                                  foelgergrunnkretsgrense_navnerom text,
                                  foelgergrunnkretsgrense_versjonid text,
                                  foelgerterrengdetalj text,
                                  noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE stemmekrets (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Stemmekrets',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             opphav text,
                             produkt text,
                             versjon text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             informasjon text,
                             posisjon geometry(Point, 25833),
                             stemmekretsnummer text NOT NULL,
                             stemmekretsnavn text NOT NULL,
                             tellekretsnummer text,
                             tellekretsnavn text,
                             valgdistriktsnummer text,
                             fylkesnummer text,
                             kommunenummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE stemmekrets_stemmekretsgrense (
                                               stemmekrets_fk int,
                                               stemmekretsgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE stemmekretsgrense (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'Stemmekretsgrense',
                                   identifikasjon_lokalid text NOT NULL,
                                   identifikasjon_navnerom text NOT NULL,
                                   identifikasjon_versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   maalemetode text,
                                   noeyaktighet int,
                                   opphav text,
                                   produkt text,
                                   versjon text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   grense geometry(Linestring, 25833) NOT NULL,
                                   foelgergrunnkretsgrense_lokalid text,
                                   foelgergrunnkretsgrense_navnerom text,
                                   foelgergrunnkretsgrense_versjonid text,
                                   foelgerterrengdetalj text,
                                   noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorgrense (
                              objid serial NOT NULL,
                              objtype text DEFAULT 'Sektorgrense',
                              lokalid text NOT NULL,
                              navnerom text NOT NULL,
                              versjonid text,
                              datafangstdato timestamp with time zone,
                              oppdateringsdato timestamp with time zone,
                              sluttdato timestamp with time zone,
                              gyldigfra timestamp with time zone,
                              gyldigtil timestamp with time zone,
                              datauttaksdato timestamp with time zone,
                              typeendring text,
                              tidspunktendring timestamp with time zone,
                              maalemetode text NOT NULL,
                              noeyaktighet int,
                              opphav text,
                              produkt text,
                              versjon text,
                              omraadeid int,
                              originaldatavert text,
                              kopidato timestamp with time zone,
                              grense geometry(Linestring, 25833) NOT NULL,
                              foelgeradministrativgrense text)
    WITH (OIDS=FALSE);

CREATE TABLE sektoromraade_sektorgrense (
                                            sektoromraade_fk int,
                                            sektorgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlokalitet (
                                 objid serial NOT NULL,
                                 objtype text DEFAULT 'Sektorlokalitet',
                                 lokalid text NOT NULL,
                                 navnerom text NOT NULL,
                                 versjonid text,
                                 datafangstdato timestamp with time zone,
                                 oppdateringsdato timestamp with time zone,
                                 sluttdato timestamp with time zone,
                                 gyldigfra timestamp with time zone,
                                 gyldigtil timestamp with time zone,
                                 datauttaksdato timestamp with time zone,
                                 typeendring text,
                                 tidspunktendring timestamp with time zone,
                                 maalemetode text NOT NULL,
                                 noeyaktighet int,
                                 opphav text,
                                 produkt text,
                                 versjon text,
                                 omraadeid int,
                                 originaldatavert text,
                                 kopidato timestamp with time zone,
                                 posisjon geometry(Point, 25833) NOT NULL,
                                 navn text,
                                 funksjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sektoromraade_sektorlokalitet (
                                               sektoromraade_fk int,
                                               sektorlokalitet_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE sektoromraade (
                               objid serial NOT NULL,
                               objtype text DEFAULT 'Sektorområde',
                               lokalid text NOT NULL,
                               navnerom text NOT NULL,
                               versjonid text,
                               datafangstdato timestamp with time zone,
                               oppdateringsdato timestamp with time zone,
                               sluttdato timestamp with time zone,
                               gyldigfra timestamp with time zone,
                               gyldigtil timestamp with time zone,
                               datauttaksdato timestamp with time zone,
                               typeendring text,
                               tidspunktendring timestamp with time zone,
                               opphav text,
                               produkt text,
                               versjon text,
                               omraadeid int,
                               originaldatavert text,
                               kopidato timestamp with time zone,
                               nivaanummer text,
                               nivaanavn text,
                               sektoreier text,
                               sektornavn text,
                               sektornummer text,
                               alternativtsektornummer text)
    WITH (OIDS=FALSE);

CREATE TABLE bispedoemme (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Bispedømme',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             opphav text,
                             produkt text,
                             versjon text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             posisjon geometry(Point, 25833),
                             organisasjonsnummer int,
                             bispedoemmenummer text,
                             bispedoemmenavn text)
    WITH (OIDS=FALSE);

CREATE TABLE bispedoemme_avgrensning (
                                         bispedoemme_fk int,
                                         avgrensning_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE bispedoemmegrense (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'Bispedømmegrense',
                                   identifikasjon_lokalid text NOT NULL,
                                   identifikasjon_navnerom text NOT NULL,
                                   identifikasjon_versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   maalemetode text,
                                   noeyaktighet int,
                                   opphav text,
                                   produkt text,
                                   versjon text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   grense geometry(Linestring, 25833) NOT NULL,
                                   foelgergrunnkretsgrense_lokalid text,
                                   foelgergrunnkretsgrense_navnerom text,
                                   foelgergrunnkretsgrense_versjonid text,
                                   foelgeradministrativgrense text,
                                   noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE prosti_avgrensning (
                                    prosti_fk int,
                                    avgrensning_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE sokn_avgrensning (
                                  sokn_fk int,
                                  avgrensning_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE prosti (
                        objid serial NOT NULL,
                        objtype text DEFAULT 'Prosti',
                        lokalid text NOT NULL,
                        navnerom text NOT NULL,
                        versjonid text,
                        datafangstdato timestamp with time zone,
                        oppdateringsdato timestamp with time zone,
                        sluttdato timestamp with time zone,
                        gyldigfra timestamp with time zone,
                        gyldigtil timestamp with time zone,
                        datauttaksdato timestamp with time zone,
                        typeendring text,
                        tidspunktendring timestamp with time zone,
                        opphav text,
                        produkt text,
                        versjon text,
                        omraadeid int,
                        originaldatavert text,
                        kopidato timestamp with time zone,
                        posisjon geometry(Point, 25833),
                        organisasjonsnummer int,
                        prostinummer text,
                        prostinavn text,
                        bispedoemme_fk int)
    WITH (OIDS=FALSE);

--CREATE TABLE prosti_avgrensning (
--prosti_fk int,
--avgrensning_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE prostigrense (
                              objid serial NOT NULL,
                              objtype text DEFAULT 'Prostigrense',
                              identifikasjon_lokalid text NOT NULL,
                              identifikasjon_navnerom text NOT NULL,
                              identifikasjon_versjonid text,
                              datafangstdato timestamp with time zone,
                              oppdateringsdato timestamp with time zone,
                              sluttdato timestamp with time zone,
                              gyldigfra timestamp with time zone,
                              gyldigtil timestamp with time zone,
                              datauttaksdato timestamp with time zone,
                              typeendring text,
                              tidspunktendring timestamp with time zone,
                              maalemetode text,
                              noeyaktighet int,
                              opphav text,
                              produkt text,
                              versjon text,
                              omraadeid int,
                              originaldatavert text,
                              kopidato timestamp with time zone,
                              grense geometry(Linestring, 25833) NOT NULL,
                              foelgergrunnkretsgrense_lokalid text,
                              foelgergrunnkretsgrense_navnerom text,
                              foelgergrunnkretsgrense_versjonid text,
                              foelgeradministrativgrense text,
                              noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

--CREATE TABLE sokn_avgrensning (
--sokn_fk int,
--avgrensning_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE sokn (
                      objid serial NOT NULL,
                      objtype text DEFAULT 'Sokn',
                      lokalid text NOT NULL,
                      navnerom text NOT NULL,
                      versjonid text,
                      datafangstdato timestamp with time zone,
                      oppdateringsdato timestamp with time zone,
                      sluttdato timestamp with time zone,
                      gyldigfra timestamp with time zone,
                      gyldigtil timestamp with time zone,
                      datauttaksdato timestamp with time zone,
                      typeendring text,
                      tidspunktendring timestamp with time zone,
                      opphav text,
                      produkt text,
                      versjon text,
                      omraadeid int,
                      originaldatavert text,
                      kopidato timestamp with time zone,
                      posisjon geometry(Point, 25833),
                      organisasjonsnummer int NOT NULL,
                      soknenummer text,
                      soknenavn text,
                      prosti_fk int)
    WITH (OIDS=FALSE);

--CREATE TABLE sokn_avgrensning (
--sokn_fk int,
--avgrensning_fk int)
--WITH (OIDS=FALSE);

CREATE TABLE soknegrense (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Soknegrense',
                             identifikasjon_lokalid text NOT NULL,
                             identifikasjon_navnerom text NOT NULL,
                             identifikasjon_versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             maalemetode text,
                             noeyaktighet int,
                             opphav text,
                             produkt text,
                             versjon text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             grense geometry(Linestring, 25833) NOT NULL,
                             foelgergrunnkretsgrense_lokalid text,
                             foelgergrunnkretsgrense_navnerom text,
                             foelgergrunnkretsgrense_versjonid text,
                             foelgeradministrativgrense text,
                             noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE bispedoemmenummer (
                                   identifier text NOT NULL,
                                   description text)
    WITH (OIDS=FALSE);

INSERT INTO bispedoemmenummer (identifier, description) VALUES ('01', '01');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('02', '02');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('03', '03');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('04', '04');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('05', '05');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('06', '06');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('07', '07');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('08', '08');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('09', '09');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('10', '10');
INSERT INTO bispedoemmenummer (identifier, description) VALUES ('11', '11');

CREATE TABLE prostinummer (
                              identifier text NOT NULL,
                              description text)
    WITH (OIDS=FALSE);

INSERT INTO prostinummer (identifier, description) VALUES ('0101', '0101');
INSERT INTO prostinummer (identifier, description) VALUES ('0102', '0102');
INSERT INTO prostinummer (identifier, description) VALUES ('0103', '0103');
INSERT INTO prostinummer (identifier, description) VALUES ('0104', '0104');
INSERT INTO prostinummer (identifier, description) VALUES ('0105', '0105');
INSERT INTO prostinummer (identifier, description) VALUES ('0106', '0106');
INSERT INTO prostinummer (identifier, description) VALUES ('0107', '0107');
INSERT INTO prostinummer (identifier, description) VALUES ('0108', '0108');
INSERT INTO prostinummer (identifier, description) VALUES ('0201', '0201');
INSERT INTO prostinummer (identifier, description) VALUES ('0202', '0202');
INSERT INTO prostinummer (identifier, description) VALUES ('0203', '0203');
INSERT INTO prostinummer (identifier, description) VALUES ('0204', '0204');
INSERT INTO prostinummer (identifier, description) VALUES ('0206', '0206');
INSERT INTO prostinummer (identifier, description) VALUES ('0207', '0207');
INSERT INTO prostinummer (identifier, description) VALUES ('0209', '0209');
INSERT INTO prostinummer (identifier, description) VALUES ('0210', '0210');
INSERT INTO prostinummer (identifier, description) VALUES ('0301', '0301');
INSERT INTO prostinummer (identifier, description) VALUES ('0304', '0304');
INSERT INTO prostinummer (identifier, description) VALUES ('0305', '0305');
INSERT INTO prostinummer (identifier, description) VALUES ('0306', '0306');
INSERT INTO prostinummer (identifier, description) VALUES ('0307', '0307');
INSERT INTO prostinummer (identifier, description) VALUES ('0308', '0308');
INSERT INTO prostinummer (identifier, description) VALUES ('0309', '0309');
INSERT INTO prostinummer (identifier, description) VALUES ('0310', '0310');
INSERT INTO prostinummer (identifier, description) VALUES ('0311', '0311');
INSERT INTO prostinummer (identifier, description) VALUES ('0312', '0312');
INSERT INTO prostinummer (identifier, description) VALUES ('0401', '0401');
INSERT INTO prostinummer (identifier, description) VALUES ('0402', '0402');
INSERT INTO prostinummer (identifier, description) VALUES ('0403', '0403');
INSERT INTO prostinummer (identifier, description) VALUES ('0404', '0404');
INSERT INTO prostinummer (identifier, description) VALUES ('0405', '0405');
INSERT INTO prostinummer (identifier, description) VALUES ('0406', '0406');
INSERT INTO prostinummer (identifier, description) VALUES ('0407', '0407');
INSERT INTO prostinummer (identifier, description) VALUES ('0408', '0408');
INSERT INTO prostinummer (identifier, description) VALUES ('0409', '0409');
INSERT INTO prostinummer (identifier, description) VALUES ('0410', '0410');
INSERT INTO prostinummer (identifier, description) VALUES ('0501', '0501');
INSERT INTO prostinummer (identifier, description) VALUES ('0505', '0505');
INSERT INTO prostinummer (identifier, description) VALUES ('0506', '0506');
INSERT INTO prostinummer (identifier, description) VALUES ('0507', '0507');
INSERT INTO prostinummer (identifier, description) VALUES ('0508', '0508');
INSERT INTO prostinummer (identifier, description) VALUES ('0509', '0509');
INSERT INTO prostinummer (identifier, description) VALUES ('0510', '0510');
INSERT INTO prostinummer (identifier, description) VALUES ('0514', '0514');
INSERT INTO prostinummer (identifier, description) VALUES ('0515', '0515');
INSERT INTO prostinummer (identifier, description) VALUES ('0601', '0601');
INSERT INTO prostinummer (identifier, description) VALUES ('0602', '0602');
INSERT INTO prostinummer (identifier, description) VALUES ('0603', '0603');
INSERT INTO prostinummer (identifier, description) VALUES ('0605', '0605');
INSERT INTO prostinummer (identifier, description) VALUES ('0607', '0607');
INSERT INTO prostinummer (identifier, description) VALUES ('0608', '0608');
INSERT INTO prostinummer (identifier, description) VALUES ('0609', '0609');
INSERT INTO prostinummer (identifier, description) VALUES ('0610', '0610');
INSERT INTO prostinummer (identifier, description) VALUES ('0611', '0611');
INSERT INTO prostinummer (identifier, description) VALUES ('0701', '0701');
INSERT INTO prostinummer (identifier, description) VALUES ('0702', '0702');
INSERT INTO prostinummer (identifier, description) VALUES ('0704', '0704');
INSERT INTO prostinummer (identifier, description) VALUES ('0705', '0705');
INSERT INTO prostinummer (identifier, description) VALUES ('0707', '0707');
INSERT INTO prostinummer (identifier, description) VALUES ('0708', '0708');
INSERT INTO prostinummer (identifier, description) VALUES ('0709', '0709');
INSERT INTO prostinummer (identifier, description) VALUES ('0711', '0711');
INSERT INTO prostinummer (identifier, description) VALUES ('0712', '0712');
INSERT INTO prostinummer (identifier, description) VALUES ('0713', '0713');
INSERT INTO prostinummer (identifier, description) VALUES ('0714', '0714');
INSERT INTO prostinummer (identifier, description) VALUES ('0801', '0801');
INSERT INTO prostinummer (identifier, description) VALUES ('0802', '0802');
INSERT INTO prostinummer (identifier, description) VALUES ('0804', '0804');
INSERT INTO prostinummer (identifier, description) VALUES ('0805', '0805');
INSERT INTO prostinummer (identifier, description) VALUES ('0806', '0806');
INSERT INTO prostinummer (identifier, description) VALUES ('0807', '0807');
INSERT INTO prostinummer (identifier, description) VALUES ('0901', '0901');
INSERT INTO prostinummer (identifier, description) VALUES ('0902', '0902');
INSERT INTO prostinummer (identifier, description) VALUES ('0903', '0903');
INSERT INTO prostinummer (identifier, description) VALUES ('0905', '0905');
INSERT INTO prostinummer (identifier, description) VALUES ('0906', '0906');
INSERT INTO prostinummer (identifier, description) VALUES ('0907', '0907');
INSERT INTO prostinummer (identifier, description) VALUES ('0908', '0908');
INSERT INTO prostinummer (identifier, description) VALUES ('0911', '0911');
INSERT INTO prostinummer (identifier, description) VALUES ('0915', '0915');
INSERT INTO prostinummer (identifier, description) VALUES ('1001', '1001');
INSERT INTO prostinummer (identifier, description) VALUES ('1002', '1002');
INSERT INTO prostinummer (identifier, description) VALUES ('1003', '1003');
INSERT INTO prostinummer (identifier, description) VALUES ('1004', '1004');
INSERT INTO prostinummer (identifier, description) VALUES ('1005', '1005');
INSERT INTO prostinummer (identifier, description) VALUES ('1006', '1006');
INSERT INTO prostinummer (identifier, description) VALUES ('1007', '1007');
INSERT INTO prostinummer (identifier, description) VALUES ('1008', '1008');
INSERT INTO prostinummer (identifier, description) VALUES ('1101', '1101');
INSERT INTO prostinummer (identifier, description) VALUES ('1102', '1102');
INSERT INTO prostinummer (identifier, description) VALUES ('1103', '1103');
INSERT INTO prostinummer (identifier, description) VALUES ('1104', '1104');
INSERT INTO prostinummer (identifier, description) VALUES ('1105', '1105');
INSERT INTO prostinummer (identifier, description) VALUES ('1106', '1106');
INSERT INTO prostinummer (identifier, description) VALUES ('1107', '1107');
INSERT INTO prostinummer (identifier, description) VALUES ('1108', '1108');
INSERT INTO prostinummer (identifier, description) VALUES ('1109', '1109');

CREATE TABLE soknenummer (
                             identifier text NOT NULL,
                             description text)
    WITH (OIDS=FALSE);

INSERT INTO soknenummer (identifier, description) VALUES ('05140903', '05140903');
INSERT INTO soknenummer (identifier, description) VALUES ('05070703', '05070703');
INSERT INTO soknenummer (identifier, description) VALUES ('05050204', '05050204');
INSERT INTO soknenummer (identifier, description) VALUES ('07110304', '07110304');
INSERT INTO soknenummer (identifier, description) VALUES ('07110105', '07110105');
INSERT INTO soknenummer (identifier, description) VALUES ('09060805', '09060805');
INSERT INTO soknenummer (identifier, description) VALUES ('10040603', '10040603');
INSERT INTO soknenummer (identifier, description) VALUES ('10080104', '10080104');
INSERT INTO soknenummer (identifier, description) VALUES ('01010301', '01010301');
INSERT INTO soknenummer (identifier, description) VALUES ('01010302', '01010302');
INSERT INTO soknenummer (identifier, description) VALUES ('01010401', '01010401');
INSERT INTO soknenummer (identifier, description) VALUES ('01010501', '01010501');
INSERT INTO soknenummer (identifier, description) VALUES ('01010901', '01010901');
INSERT INTO soknenummer (identifier, description) VALUES ('01011001', '01011001');
INSERT INTO soknenummer (identifier, description) VALUES ('01020101', '01020101');
INSERT INTO soknenummer (identifier, description) VALUES ('01020102', '01020102');
INSERT INTO soknenummer (identifier, description) VALUES ('01020201', '01020201');
INSERT INTO soknenummer (identifier, description) VALUES ('01020202', '01020202');
INSERT INTO soknenummer (identifier, description) VALUES ('01020301', '01020301');
INSERT INTO soknenummer (identifier, description) VALUES ('01020401', '01020401');
INSERT INTO soknenummer (identifier, description) VALUES ('01020601', '01020601');
INSERT INTO soknenummer (identifier, description) VALUES ('01020801', '01020801');
INSERT INTO soknenummer (identifier, description) VALUES ('01041001', '01041001');
INSERT INTO soknenummer (identifier, description) VALUES ('01041002', '01041002');
INSERT INTO soknenummer (identifier, description) VALUES ('01031201', '01031201');
INSERT INTO soknenummer (identifier, description) VALUES ('01050901', '01050901');
INSERT INTO soknenummer (identifier, description) VALUES ('01050301', '01050301');
INSERT INTO soknenummer (identifier, description) VALUES ('01050401', '01050401');
INSERT INTO soknenummer (identifier, description) VALUES ('01050501', '01050501');
INSERT INTO soknenummer (identifier, description) VALUES ('01050502', '01050502');
INSERT INTO soknenummer (identifier, description) VALUES ('01050601', '01050601');
INSERT INTO soknenummer (identifier, description) VALUES ('01050701', '01050701');
INSERT INTO soknenummer (identifier, description) VALUES ('01050702', '01050702');
INSERT INTO soknenummer (identifier, description) VALUES ('01050801', '01050801');
INSERT INTO soknenummer (identifier, description) VALUES ('01060101', '01060101');
INSERT INTO soknenummer (identifier, description) VALUES ('01060102', '01060102');
INSERT INTO soknenummer (identifier, description) VALUES ('01060201', '01060201');
INSERT INTO soknenummer (identifier, description) VALUES ('01060301', '01060301');
INSERT INTO soknenummer (identifier, description) VALUES ('01060302', '01060302');
INSERT INTO soknenummer (identifier, description) VALUES ('01060401', '01060401');
INSERT INTO soknenummer (identifier, description) VALUES ('01060501', '01060501');
INSERT INTO soknenummer (identifier, description) VALUES ('01060601', '01060601');
INSERT INTO soknenummer (identifier, description) VALUES ('01060701', '01060701');
INSERT INTO soknenummer (identifier, description) VALUES ('01060801', '01060801');
INSERT INTO soknenummer (identifier, description) VALUES ('01070101', '01070101');
INSERT INTO soknenummer (identifier, description) VALUES ('01070102', '01070102');
INSERT INTO soknenummer (identifier, description) VALUES ('01070201', '01070201');
INSERT INTO soknenummer (identifier, description) VALUES ('01070301', '01070301');
INSERT INTO soknenummer (identifier, description) VALUES ('01070302', '01070302');
INSERT INTO soknenummer (identifier, description) VALUES ('02010101', '02010101');
INSERT INTO soknenummer (identifier, description) VALUES ('02010201', '02010201');
INSERT INTO soknenummer (identifier, description) VALUES ('02010301', '02010301');
INSERT INTO soknenummer (identifier, description) VALUES ('02010302', '02010302');
INSERT INTO soknenummer (identifier, description) VALUES ('02010401', '02010401');
INSERT INTO soknenummer (identifier, description) VALUES ('02010501', '02010501');
INSERT INTO soknenummer (identifier, description) VALUES ('02010601', '02010601');
INSERT INTO soknenummer (identifier, description) VALUES ('02010602', '02010602');
INSERT INTO soknenummer (identifier, description) VALUES ('02010701', '02010701');
INSERT INTO soknenummer (identifier, description) VALUES ('02010702', '02010702');
INSERT INTO soknenummer (identifier, description) VALUES ('02010801', '02010801');
INSERT INTO soknenummer (identifier, description) VALUES ('02020101', '02020101');
INSERT INTO soknenummer (identifier, description) VALUES ('02020204', '02020204');
INSERT INTO soknenummer (identifier, description) VALUES ('02020205', '02020205');
INSERT INTO soknenummer (identifier, description) VALUES ('02020301', '02020301');
INSERT INTO soknenummer (identifier, description) VALUES ('02020401', '02020401');
INSERT INTO soknenummer (identifier, description) VALUES ('02020402', '02020402');
INSERT INTO soknenummer (identifier, description) VALUES ('02020501', '02020501');
INSERT INTO soknenummer (identifier, description) VALUES ('02020502', '02020502');
INSERT INTO soknenummer (identifier, description) VALUES ('02020601', '02020601');
INSERT INTO soknenummer (identifier, description) VALUES ('02020602', '02020602');
INSERT INTO soknenummer (identifier, description) VALUES ('02020603', '02020603');
INSERT INTO soknenummer (identifier, description) VALUES ('02020604', '02020604');
INSERT INTO soknenummer (identifier, description) VALUES ('02020701', '02020701');
INSERT INTO soknenummer (identifier, description) VALUES ('02020702', '02020702');
INSERT INTO soknenummer (identifier, description) VALUES ('02020703', '02020703');
INSERT INTO soknenummer (identifier, description) VALUES ('02030101', '02030101');
INSERT INTO soknenummer (identifier, description) VALUES ('02030102', '02030102');
INSERT INTO soknenummer (identifier, description) VALUES ('02030103', '02030103');
INSERT INTO soknenummer (identifier, description) VALUES ('02030104', '02030104');
INSERT INTO soknenummer (identifier, description) VALUES ('02030201', '02030201');
INSERT INTO soknenummer (identifier, description) VALUES ('02030202', '02030202');
INSERT INTO soknenummer (identifier, description) VALUES ('02030203', '02030203');
INSERT INTO soknenummer (identifier, description) VALUES ('02030204', '02030204');
INSERT INTO soknenummer (identifier, description) VALUES ('02030205', '02030205');
INSERT INTO soknenummer (identifier, description) VALUES ('02030206', '02030206');
INSERT INTO soknenummer (identifier, description) VALUES ('02030401', '02030401');
INSERT INTO soknenummer (identifier, description) VALUES ('02040101', '02040101');
INSERT INTO soknenummer (identifier, description) VALUES ('02040201', '02040201');
INSERT INTO soknenummer (identifier, description) VALUES ('02040301', '02040301');
INSERT INTO soknenummer (identifier, description) VALUES ('02040401', '02040401');
INSERT INTO soknenummer (identifier, description) VALUES ('02040402', '02040402');
INSERT INTO soknenummer (identifier, description) VALUES ('02040501', '02040501');
INSERT INTO soknenummer (identifier, description) VALUES ('02040502', '02040502');
INSERT INTO soknenummer (identifier, description) VALUES ('02030601', '02030601');
INSERT INTO soknenummer (identifier, description) VALUES ('02030801', '02030801');
INSERT INTO soknenummer (identifier, description) VALUES ('02030802', '02030802');
INSERT INTO soknenummer (identifier, description) VALUES ('02090101', '02090101');
INSERT INTO soknenummer (identifier, description) VALUES ('02090102', '02090102');
INSERT INTO soknenummer (identifier, description) VALUES ('02090103', '02090103');
INSERT INTO soknenummer (identifier, description) VALUES ('02090201', '02090201');
INSERT INTO soknenummer (identifier, description) VALUES ('02090202', '02090202');
INSERT INTO soknenummer (identifier, description) VALUES ('02090203', '02090203');
INSERT INTO soknenummer (identifier, description) VALUES ('02090204', '02090204');
INSERT INTO soknenummer (identifier, description) VALUES ('02090401', '02090401');
INSERT INTO soknenummer (identifier, description) VALUES ('02090402', '02090402');
INSERT INTO soknenummer (identifier, description) VALUES ('02090403', '02090403');
INSERT INTO soknenummer (identifier, description) VALUES ('02100101', '02100101');
INSERT INTO soknenummer (identifier, description) VALUES ('02100102', '02100102');
INSERT INTO soknenummer (identifier, description) VALUES ('02100103', '02100103');
INSERT INTO soknenummer (identifier, description) VALUES ('02100201', '02100201');
INSERT INTO soknenummer (identifier, description) VALUES ('02100202', '02100202');
INSERT INTO soknenummer (identifier, description) VALUES ('02100301', '02100301');
INSERT INTO soknenummer (identifier, description) VALUES ('02100302', '02100302');
INSERT INTO soknenummer (identifier, description) VALUES ('02060101', '02060101');
INSERT INTO soknenummer (identifier, description) VALUES ('02060201', '02060201');
INSERT INTO soknenummer (identifier, description) VALUES ('02060301', '02060301');
INSERT INTO soknenummer (identifier, description) VALUES ('02060801', '02060801');
INSERT INTO soknenummer (identifier, description) VALUES ('02060802', '02060802');
INSERT INTO soknenummer (identifier, description) VALUES ('02060901', '02060901');
INSERT INTO soknenummer (identifier, description) VALUES ('02061001', '02061001');
INSERT INTO soknenummer (identifier, description) VALUES ('02061101', '02061101');
INSERT INTO soknenummer (identifier, description) VALUES ('02061102', '02061102');
INSERT INTO soknenummer (identifier, description) VALUES ('02070101', '02070101');
INSERT INTO soknenummer (identifier, description) VALUES ('02070102', '02070102');
INSERT INTO soknenummer (identifier, description) VALUES ('02070301', '02070301');
INSERT INTO soknenummer (identifier, description) VALUES ('02070302', '02070302');
INSERT INTO soknenummer (identifier, description) VALUES ('02070303', '02070303');
INSERT INTO soknenummer (identifier, description) VALUES ('02070304', '02070304');
INSERT INTO soknenummer (identifier, description) VALUES ('02070401', '02070401');
INSERT INTO soknenummer (identifier, description) VALUES ('02070501', '02070501');
INSERT INTO soknenummer (identifier, description) VALUES ('02070502', '02070502');
INSERT INTO soknenummer (identifier, description) VALUES ('02070503', '02070503');
INSERT INTO soknenummer (identifier, description) VALUES ('02070504', '02070504');
INSERT INTO soknenummer (identifier, description) VALUES ('02070601', '02070601');
INSERT INTO soknenummer (identifier, description) VALUES ('02070701', '02070701');
INSERT INTO soknenummer (identifier, description) VALUES ('02070702', '02070702');
INSERT INTO soknenummer (identifier, description) VALUES ('02070801', '02070801');
INSERT INTO soknenummer (identifier, description) VALUES ('02070802', '02070802');
INSERT INTO soknenummer (identifier, description) VALUES ('02070803', '02070803');
INSERT INTO soknenummer (identifier, description) VALUES ('02070804', '02070804');
INSERT INTO soknenummer (identifier, description) VALUES ('02070805', '02070805');
INSERT INTO soknenummer (identifier, description) VALUES ('02070806', '02070806');
INSERT INTO soknenummer (identifier, description) VALUES ('02030901', '02030901');
INSERT INTO soknenummer (identifier, description) VALUES ('02030902', '02030902');
INSERT INTO soknenummer (identifier, description) VALUES ('02030903', '02030903');
INSERT INTO soknenummer (identifier, description) VALUES ('02030904', '02030904');
INSERT INTO soknenummer (identifier, description) VALUES ('02030905', '02030905');
INSERT INTO soknenummer (identifier, description) VALUES ('02061201', '02061201');
INSERT INTO soknenummer (identifier, description) VALUES ('02061202', '02061202');
INSERT INTO soknenummer (identifier, description) VALUES ('02061301', '02061301');
INSERT INTO soknenummer (identifier, description) VALUES ('02061302', '02061302');
INSERT INTO soknenummer (identifier, description) VALUES ('02061303', '02061303');
INSERT INTO soknenummer (identifier, description) VALUES ('03010101', '03010101');
INSERT INTO soknenummer (identifier, description) VALUES ('03010201', '03010201');
INSERT INTO soknenummer (identifier, description) VALUES ('03010301', '03010301');
INSERT INTO soknenummer (identifier, description) VALUES ('03010302', '03010302');
INSERT INTO soknenummer (identifier, description) VALUES ('03010401', '03010401');
INSERT INTO soknenummer (identifier, description) VALUES ('03010501', '03010501');
INSERT INTO soknenummer (identifier, description) VALUES ('03010502', '03010502');
INSERT INTO soknenummer (identifier, description) VALUES ('03010601', '03010601');
INSERT INTO soknenummer (identifier, description) VALUES ('03120101', '03120101');
INSERT INTO soknenummer (identifier, description) VALUES ('03120102', '03120102');
INSERT INTO soknenummer (identifier, description) VALUES ('03120201', '03120201');
INSERT INTO soknenummer (identifier, description) VALUES ('03120301', '03120301');
INSERT INTO soknenummer (identifier, description) VALUES ('03120302', '03120302');
INSERT INTO soknenummer (identifier, description) VALUES ('03120401', '03120401');
INSERT INTO soknenummer (identifier, description) VALUES ('03120402', '03120402');
INSERT INTO soknenummer (identifier, description) VALUES ('03120403', '03120403');
INSERT INTO soknenummer (identifier, description) VALUES ('03120501', '03120501');
INSERT INTO soknenummer (identifier, description) VALUES ('03120502', '03120502');
INSERT INTO soknenummer (identifier, description) VALUES ('03120601', '03120601');
INSERT INTO soknenummer (identifier, description) VALUES ('03120602', '03120602');
INSERT INTO soknenummer (identifier, description) VALUES ('03120603', '03120603');
INSERT INTO soknenummer (identifier, description) VALUES ('03120701', '03120701');
INSERT INTO soknenummer (identifier, description) VALUES ('03120702', '03120702');
INSERT INTO soknenummer (identifier, description) VALUES ('03120703', '03120703');
INSERT INTO soknenummer (identifier, description) VALUES ('03120801', '03120801');
INSERT INTO soknenummer (identifier, description) VALUES ('03120802', '03120802');
INSERT INTO soknenummer (identifier, description) VALUES ('03120901', '03120901');
INSERT INTO soknenummer (identifier, description) VALUES ('03120902', '03120902');
INSERT INTO soknenummer (identifier, description) VALUES ('03040101', '03040101');
INSERT INTO soknenummer (identifier, description) VALUES ('03040102', '03040102');
INSERT INTO soknenummer (identifier, description) VALUES ('03040103', '03040103');
INSERT INTO soknenummer (identifier, description) VALUES ('03040104', '03040104');
INSERT INTO soknenummer (identifier, description) VALUES ('03040105', '03040105');
INSERT INTO soknenummer (identifier, description) VALUES ('03040201', '03040201');
INSERT INTO soknenummer (identifier, description) VALUES ('03040202', '03040202');
INSERT INTO soknenummer (identifier, description) VALUES ('03040203', '03040203');
INSERT INTO soknenummer (identifier, description) VALUES ('03040204', '03040204');
INSERT INTO soknenummer (identifier, description) VALUES ('03040205', '03040205');
INSERT INTO soknenummer (identifier, description) VALUES ('03040206', '03040206');
INSERT INTO soknenummer (identifier, description) VALUES ('03040207', '03040207');
INSERT INTO soknenummer (identifier, description) VALUES ('03040301', '03040301');
INSERT INTO soknenummer (identifier, description) VALUES ('03040302', '03040302');
INSERT INTO soknenummer (identifier, description) VALUES ('03040303', '03040303');
INSERT INTO soknenummer (identifier, description) VALUES ('03040304', '03040304');
INSERT INTO soknenummer (identifier, description) VALUES ('03040305', '03040305');
INSERT INTO soknenummer (identifier, description) VALUES ('03040401', '03040401');
INSERT INTO soknenummer (identifier, description) VALUES ('03040402', '03040402');
INSERT INTO soknenummer (identifier, description) VALUES ('03040403', '03040403');
INSERT INTO soknenummer (identifier, description) VALUES ('03040506', '03040506');
INSERT INTO soknenummer (identifier, description) VALUES ('03040502', '03040502');
INSERT INTO soknenummer (identifier, description) VALUES ('03040503', '03040503');
INSERT INTO soknenummer (identifier, description) VALUES ('03050101', '03050101');
INSERT INTO soknenummer (identifier, description) VALUES ('03050102', '03050102');
INSERT INTO soknenummer (identifier, description) VALUES ('03050103', '03050103');
INSERT INTO soknenummer (identifier, description) VALUES ('03050104', '03050104');
INSERT INTO soknenummer (identifier, description) VALUES ('03050201', '03050201');
INSERT INTO soknenummer (identifier, description) VALUES ('03050202', '03050202');
INSERT INTO soknenummer (identifier, description) VALUES ('03050203', '03050203');
INSERT INTO soknenummer (identifier, description) VALUES ('03050204', '03050204');
INSERT INTO soknenummer (identifier, description) VALUES ('03050301', '03050301');
INSERT INTO soknenummer (identifier, description) VALUES ('03050302', '03050302');
INSERT INTO soknenummer (identifier, description) VALUES ('03050303', '03050303');
INSERT INTO soknenummer (identifier, description) VALUES ('03050304', '03050304');
INSERT INTO soknenummer (identifier, description) VALUES ('03050401', '03050401');
INSERT INTO soknenummer (identifier, description) VALUES ('03050402', '03050402');
INSERT INTO soknenummer (identifier, description) VALUES ('03050403', '03050403');
INSERT INTO soknenummer (identifier, description) VALUES ('03050501', '03050501');
INSERT INTO soknenummer (identifier, description) VALUES ('03050601', '03050601');
INSERT INTO soknenummer (identifier, description) VALUES ('03050602', '03050602');
INSERT INTO soknenummer (identifier, description) VALUES ('03060101', '03060101');
INSERT INTO soknenummer (identifier, description) VALUES ('03060102', '03060102');
INSERT INTO soknenummer (identifier, description) VALUES ('03060103', '03060103');
INSERT INTO soknenummer (identifier, description) VALUES ('03060201', '03060201');
INSERT INTO soknenummer (identifier, description) VALUES ('03060202', '03060202');
INSERT INTO soknenummer (identifier, description) VALUES ('03060203', '03060203');
INSERT INTO soknenummer (identifier, description) VALUES ('03060204', '03060204');
INSERT INTO soknenummer (identifier, description) VALUES ('03060301', '03060301');
INSERT INTO soknenummer (identifier, description) VALUES ('03060401', '03060401');
INSERT INTO soknenummer (identifier, description) VALUES ('03060402', '03060402');
INSERT INTO soknenummer (identifier, description) VALUES ('03060501', '03060501');
INSERT INTO soknenummer (identifier, description) VALUES ('03060502', '03060502');
INSERT INTO soknenummer (identifier, description) VALUES ('03060503', '03060503');
INSERT INTO soknenummer (identifier, description) VALUES ('03060701', '03060701');
INSERT INTO soknenummer (identifier, description) VALUES ('03060702', '03060702');
INSERT INTO soknenummer (identifier, description) VALUES ('03070101', '03070101');
INSERT INTO soknenummer (identifier, description) VALUES ('06011301', '06011301');
INSERT INTO soknenummer (identifier, description) VALUES ('06011401', '06011401');
INSERT INTO soknenummer (identifier, description) VALUES ('06011501', '06011501');
INSERT INTO soknenummer (identifier, description) VALUES ('06020101', '06020101');
INSERT INTO soknenummer (identifier, description) VALUES ('06020102', '06020102');
INSERT INTO soknenummer (identifier, description) VALUES ('06020103', '06020103');
INSERT INTO soknenummer (identifier, description) VALUES ('06020201', '06020201');
INSERT INTO soknenummer (identifier, description) VALUES ('06020301', '06020301');
INSERT INTO soknenummer (identifier, description) VALUES ('06020302', '06020302');
INSERT INTO soknenummer (identifier, description) VALUES ('06020401', '06020401');
INSERT INTO soknenummer (identifier, description) VALUES ('06030501', '06030501');
INSERT INTO soknenummer (identifier, description) VALUES ('06030502', '06030502');
INSERT INTO soknenummer (identifier, description) VALUES ('06030601', '06030601');
INSERT INTO soknenummer (identifier, description) VALUES ('06030602', '06030602');
INSERT INTO soknenummer (identifier, description) VALUES ('06030603', '06030603');
INSERT INTO soknenummer (identifier, description) VALUES ('06030701', '06030701');
INSERT INTO soknenummer (identifier, description) VALUES ('06030702', '06030702');
INSERT INTO soknenummer (identifier, description) VALUES ('06030703', '06030703');
INSERT INTO soknenummer (identifier, description) VALUES ('06030801', '06030801');
INSERT INTO soknenummer (identifier, description) VALUES ('06030802', '06030802');
INSERT INTO soknenummer (identifier, description) VALUES ('06030803', '06030803');
INSERT INTO soknenummer (identifier, description) VALUES ('06030804', '06030804');
INSERT INTO soknenummer (identifier, description) VALUES ('06110101', '06110101');
INSERT INTO soknenummer (identifier, description) VALUES ('06110102', '06110102');
INSERT INTO soknenummer (identifier, description) VALUES ('06080901', '06080901');
INSERT INTO soknenummer (identifier, description) VALUES ('06110201', '06110201');
INSERT INTO soknenummer (identifier, description) VALUES ('06110202', '06110202');
INSERT INTO soknenummer (identifier, description) VALUES ('06110203', '06110203');
INSERT INTO soknenummer (identifier, description) VALUES ('06050101', '06050101');
INSERT INTO soknenummer (identifier, description) VALUES ('06050201', '06050201');
INSERT INTO soknenummer (identifier, description) VALUES ('06050301', '06050301');
INSERT INTO soknenummer (identifier, description) VALUES ('06050502', '06050502');
INSERT INTO soknenummer (identifier, description) VALUES ('06050901', '06050901');
INSERT INTO soknenummer (identifier, description) VALUES ('06050902', '06050902');
INSERT INTO soknenummer (identifier, description) VALUES ('06050903', '06050903');
INSERT INTO soknenummer (identifier, description) VALUES ('06050904', '06050904');
INSERT INTO soknenummer (identifier, description) VALUES ('06110301', '06110301');
INSERT INTO soknenummer (identifier, description) VALUES ('06110401', '06110401');
INSERT INTO soknenummer (identifier, description) VALUES ('06110402', '06110402');
INSERT INTO soknenummer (identifier, description) VALUES ('06110403', '06110403');
INSERT INTO soknenummer (identifier, description) VALUES ('06110404', '06110404');
INSERT INTO soknenummer (identifier, description) VALUES ('06051001', '06051001');
INSERT INTO soknenummer (identifier, description) VALUES ('06051002', '06051002');
INSERT INTO soknenummer (identifier, description) VALUES ('06051003', '06051003');
INSERT INTO soknenummer (identifier, description) VALUES ('06051101', '06051101');
INSERT INTO soknenummer (identifier, description) VALUES ('09150205', '09150205');
INSERT INTO soknenummer (identifier, description) VALUES ('09150206', '09150206');
INSERT INTO soknenummer (identifier, description) VALUES ('09150301', '09150301');
INSERT INTO soknenummer (identifier, description) VALUES ('09150207', '09150207');
INSERT INTO soknenummer (identifier, description) VALUES ('09150401', '09150401');
INSERT INTO soknenummer (identifier, description) VALUES ('09150402', '09150402');
INSERT INTO soknenummer (identifier, description) VALUES ('09150403', '09150403');
INSERT INTO soknenummer (identifier, description) VALUES ('09150404', '09150404');
INSERT INTO soknenummer (identifier, description) VALUES ('09150405', '09150405');
INSERT INTO soknenummer (identifier, description) VALUES ('09150406', '09150406');
INSERT INTO soknenummer (identifier, description) VALUES ('09150407', '09150407');
INSERT INTO soknenummer (identifier, description) VALUES ('09150408', '09150408');
INSERT INTO soknenummer (identifier, description) VALUES ('09150409', '09150409');
INSERT INTO soknenummer (identifier, description) VALUES ('09110901', '09110901');
INSERT INTO soknenummer (identifier, description) VALUES ('09150601', '09150601');
INSERT INTO soknenummer (identifier, description) VALUES ('09150701', '09150701');
INSERT INTO soknenummer (identifier, description) VALUES ('09150702', '09150702');
INSERT INTO soknenummer (identifier, description) VALUES ('09150703', '09150703');
INSERT INTO soknenummer (identifier, description) VALUES ('09150704', '09150704');
INSERT INTO soknenummer (identifier, description) VALUES ('09110101', '09110101');
INSERT INTO soknenummer (identifier, description) VALUES ('09110102', '09110102');
INSERT INTO soknenummer (identifier, description) VALUES ('09110103', '09110103');
INSERT INTO soknenummer (identifier, description) VALUES ('09110201', '09110201');
INSERT INTO soknenummer (identifier, description) VALUES ('09110202', '09110202');
INSERT INTO soknenummer (identifier, description) VALUES ('09110301', '09110301');
INSERT INTO soknenummer (identifier, description) VALUES ('09110302', '09110302');
INSERT INTO soknenummer (identifier, description) VALUES ('09110401', '09110401');
INSERT INTO soknenummer (identifier, description) VALUES ('09110402', '09110402');
INSERT INTO soknenummer (identifier, description) VALUES ('03070102', '03070102');
INSERT INTO soknenummer (identifier, description) VALUES ('03070103', '03070103');
INSERT INTO soknenummer (identifier, description) VALUES ('03070201', '03070201');
INSERT INTO soknenummer (identifier, description) VALUES ('03070202', '03070202');
INSERT INTO soknenummer (identifier, description) VALUES ('03070203', '03070203');
INSERT INTO soknenummer (identifier, description) VALUES ('03070401', '03070401');
INSERT INTO soknenummer (identifier, description) VALUES ('03070402', '03070402');
INSERT INTO soknenummer (identifier, description) VALUES ('03070403', '03070403');
INSERT INTO soknenummer (identifier, description) VALUES ('03070501', '03070501');
INSERT INTO soknenummer (identifier, description) VALUES ('03070601', '03070601');
INSERT INTO soknenummer (identifier, description) VALUES ('03070602', '03070602');
INSERT INTO soknenummer (identifier, description) VALUES ('03070701', '03070701');
INSERT INTO soknenummer (identifier, description) VALUES ('03070702', '03070702');
INSERT INTO soknenummer (identifier, description) VALUES ('03070703', '03070703');
INSERT INTO soknenummer (identifier, description) VALUES ('03080101', '03080101');
INSERT INTO soknenummer (identifier, description) VALUES ('03080102', '03080102');
INSERT INTO soknenummer (identifier, description) VALUES ('03080201', '03080201');
INSERT INTO soknenummer (identifier, description) VALUES ('03080202', '03080202');
INSERT INTO soknenummer (identifier, description) VALUES ('03080203', '03080203');
INSERT INTO soknenummer (identifier, description) VALUES ('03080301', '03080301');
INSERT INTO soknenummer (identifier, description) VALUES ('03080302', '03080302');
INSERT INTO soknenummer (identifier, description) VALUES ('03080401', '03080401');
INSERT INTO soknenummer (identifier, description) VALUES ('03080501', '03080501');
INSERT INTO soknenummer (identifier, description) VALUES ('03080502', '03080502');
INSERT INTO soknenummer (identifier, description) VALUES ('03080503', '03080503');
INSERT INTO soknenummer (identifier, description) VALUES ('03080601', '03080601');
INSERT INTO soknenummer (identifier, description) VALUES ('03080602', '03080602');
INSERT INTO soknenummer (identifier, description) VALUES ('03080701', '03080701');
INSERT INTO soknenummer (identifier, description) VALUES ('03080801', '03080801');
INSERT INTO soknenummer (identifier, description) VALUES ('03090102', '03090102');
INSERT INTO soknenummer (identifier, description) VALUES ('03090201', '03090201');
INSERT INTO soknenummer (identifier, description) VALUES ('03090204', '03090204');
INSERT INTO soknenummer (identifier, description) VALUES ('03090203', '03090203');
INSERT INTO soknenummer (identifier, description) VALUES ('03090301', '03090301');
INSERT INTO soknenummer (identifier, description) VALUES ('03090302', '03090302');
INSERT INTO soknenummer (identifier, description) VALUES ('03090401', '03090401');
INSERT INTO soknenummer (identifier, description) VALUES ('03090501', '03090501');
INSERT INTO soknenummer (identifier, description) VALUES ('03090502', '03090502');
INSERT INTO soknenummer (identifier, description) VALUES ('03090503', '03090503');
INSERT INTO soknenummer (identifier, description) VALUES ('03090601', '03090601');
INSERT INTO soknenummer (identifier, description) VALUES ('03090602', '03090602');
INSERT INTO soknenummer (identifier, description) VALUES ('03090603', '03090603');
INSERT INTO soknenummer (identifier, description) VALUES ('03090604', '03090604');
INSERT INTO soknenummer (identifier, description) VALUES ('03100101', '03100101');
INSERT INTO soknenummer (identifier, description) VALUES ('03100102', '03100102');
INSERT INTO soknenummer (identifier, description) VALUES ('03100103', '03100103');
INSERT INTO soknenummer (identifier, description) VALUES ('03100104', '03100104');
INSERT INTO soknenummer (identifier, description) VALUES ('03100105', '03100105');
INSERT INTO soknenummer (identifier, description) VALUES ('03100106', '03100106');
INSERT INTO soknenummer (identifier, description) VALUES ('03100201', '03100201');
INSERT INTO soknenummer (identifier, description) VALUES ('03100202', '03100202');
INSERT INTO soknenummer (identifier, description) VALUES ('03100203', '03100203');
INSERT INTO soknenummer (identifier, description) VALUES ('03100204', '03100204');
INSERT INTO soknenummer (identifier, description) VALUES ('03100205', '03100205');
INSERT INTO soknenummer (identifier, description) VALUES ('03100301', '03100301');
INSERT INTO soknenummer (identifier, description) VALUES ('03100302', '03100302');
INSERT INTO soknenummer (identifier, description) VALUES ('03100401', '03100401');
INSERT INTO soknenummer (identifier, description) VALUES ('03100402', '03100402');
INSERT INTO soknenummer (identifier, description) VALUES ('03100403', '03100403');
INSERT INTO soknenummer (identifier, description) VALUES ('03100501', '03100501');
INSERT INTO soknenummer (identifier, description) VALUES ('03100502', '03100502');
INSERT INTO soknenummer (identifier, description) VALUES ('03100503', '03100503');
INSERT INTO soknenummer (identifier, description) VALUES ('03100504', '03100504');
INSERT INTO soknenummer (identifier, description) VALUES ('03100601', '03100601');
INSERT INTO soknenummer (identifier, description) VALUES ('03100602', '03100602');
INSERT INTO soknenummer (identifier, description) VALUES ('03100603', '03100603');
INSERT INTO soknenummer (identifier, description) VALUES ('03100604', '03100604');
INSERT INTO soknenummer (identifier, description) VALUES ('03110101', '03110101');
INSERT INTO soknenummer (identifier, description) VALUES ('03110102', '03110102');
INSERT INTO soknenummer (identifier, description) VALUES ('03110103', '03110103');
INSERT INTO soknenummer (identifier, description) VALUES ('03110104', '03110104');
INSERT INTO soknenummer (identifier, description) VALUES ('03110105', '03110105');
INSERT INTO soknenummer (identifier, description) VALUES ('03110106', '03110106');
INSERT INTO soknenummer (identifier, description) VALUES ('04010101', '04010101');
INSERT INTO soknenummer (identifier, description) VALUES ('04010201', '04010201');
INSERT INTO soknenummer (identifier, description) VALUES ('04010301', '04010301');
INSERT INTO soknenummer (identifier, description) VALUES ('04010302', '04010302');
INSERT INTO soknenummer (identifier, description) VALUES ('04010401', '04010401');
INSERT INTO soknenummer (identifier, description) VALUES ('04010402', '04010402');
INSERT INTO soknenummer (identifier, description) VALUES ('04010501', '04010501');
INSERT INTO soknenummer (identifier, description) VALUES ('04010502', '04010502');
INSERT INTO soknenummer (identifier, description) VALUES ('04010503', '04010503');
INSERT INTO soknenummer (identifier, description) VALUES ('04090501', '04090501');
INSERT INTO soknenummer (identifier, description) VALUES ('04090502', '04090502');
INSERT INTO soknenummer (identifier, description) VALUES ('04090503', '04090503');
INSERT INTO soknenummer (identifier, description) VALUES ('04020101', '04020101');
INSERT INTO soknenummer (identifier, description) VALUES ('04020103', '04020103');
INSERT INTO soknenummer (identifier, description) VALUES ('04020201', '04020201');
INSERT INTO soknenummer (identifier, description) VALUES ('04020202', '04020202');
INSERT INTO soknenummer (identifier, description) VALUES ('04020301', '04020301');
INSERT INTO soknenummer (identifier, description) VALUES ('04020303', '04020303');
INSERT INTO soknenummer (identifier, description) VALUES ('04020304', '04020304');
INSERT INTO soknenummer (identifier, description) VALUES ('04020401', '04020401');
INSERT INTO soknenummer (identifier, description) VALUES ('04020501', '04020501');
INSERT INTO soknenummer (identifier, description) VALUES ('04020502', '04020502');
INSERT INTO soknenummer (identifier, description) VALUES ('04020503', '04020503');
INSERT INTO soknenummer (identifier, description) VALUES ('04030101', '04030101');
INSERT INTO soknenummer (identifier, description) VALUES ('04030201', '04030201');
INSERT INTO soknenummer (identifier, description) VALUES ('04030202', '04030202');
INSERT INTO soknenummer (identifier, description) VALUES ('04030203', '04030203');
INSERT INTO soknenummer (identifier, description) VALUES ('04010901', '04010901');
INSERT INTO soknenummer (identifier, description) VALUES ('04010902', '04010902');
INSERT INTO soknenummer (identifier, description) VALUES ('04010903', '04010903');
INSERT INTO soknenummer (identifier, description) VALUES ('04010904', '04010904');
INSERT INTO soknenummer (identifier, description) VALUES ('04010905', '04010905');
INSERT INTO soknenummer (identifier, description) VALUES ('04041201', '04041201');
INSERT INTO soknenummer (identifier, description) VALUES ('04030601', '04030601');
INSERT INTO soknenummer (identifier, description) VALUES ('04040101', '04040101');
INSERT INTO soknenummer (identifier, description) VALUES ('04040201', '04040201');
INSERT INTO soknenummer (identifier, description) VALUES ('04040301', '04040301');
INSERT INTO soknenummer (identifier, description) VALUES ('04040302', '04040302');
INSERT INTO soknenummer (identifier, description) VALUES ('04040401', '04040401');
INSERT INTO soknenummer (identifier, description) VALUES ('04040501', '04040501');
INSERT INTO soknenummer (identifier, description) VALUES ('04040601', '04040601');
INSERT INTO soknenummer (identifier, description) VALUES ('04040602', '04040602');
INSERT INTO soknenummer (identifier, description) VALUES ('04050101', '04050101');
INSERT INTO soknenummer (identifier, description) VALUES ('04050102', '04050102');
INSERT INTO soknenummer (identifier, description) VALUES ('04050103', '04050103');
INSERT INTO soknenummer (identifier, description) VALUES ('04050104', '04050104');
INSERT INTO soknenummer (identifier, description) VALUES ('04041101', '04041101');
INSERT INTO soknenummer (identifier, description) VALUES ('04050301', '04050301');
INSERT INTO soknenummer (identifier, description) VALUES ('04050401', '04050401');
INSERT INTO soknenummer (identifier, description) VALUES ('04050402', '04050402');
INSERT INTO soknenummer (identifier, description) VALUES ('04050501', '04050501');
INSERT INTO soknenummer (identifier, description) VALUES ('04041102', '04041102');
INSERT INTO soknenummer (identifier, description) VALUES ('04060201', '04060201');
INSERT INTO soknenummer (identifier, description) VALUES ('04060202', '04060202');
INSERT INTO soknenummer (identifier, description) VALUES ('04060203', '04060203');
INSERT INTO soknenummer (identifier, description) VALUES ('04060204', '04060204');
INSERT INTO soknenummer (identifier, description) VALUES ('04060301', '04060301');
INSERT INTO soknenummer (identifier, description) VALUES ('04060302', '04060302');
INSERT INTO soknenummer (identifier, description) VALUES ('04060303', '04060303');
INSERT INTO soknenummer (identifier, description) VALUES ('04060401', '04060401');
INSERT INTO soknenummer (identifier, description) VALUES ('04060402', '04060402');
INSERT INTO soknenummer (identifier, description) VALUES ('04060501', '04060501');
INSERT INTO soknenummer (identifier, description) VALUES ('04060502', '04060502');
INSERT INTO soknenummer (identifier, description) VALUES ('04060503', '04060503');
INSERT INTO soknenummer (identifier, description) VALUES ('04070101', '04070101');
INSERT INTO soknenummer (identifier, description) VALUES ('04070201', '04070201');
INSERT INTO soknenummer (identifier, description) VALUES ('04070202', '04070202');
INSERT INTO soknenummer (identifier, description) VALUES ('04070203', '04070203');
INSERT INTO soknenummer (identifier, description) VALUES ('04070204', '04070204');
INSERT INTO soknenummer (identifier, description) VALUES ('04070301', '04070301');
INSERT INTO soknenummer (identifier, description) VALUES ('04070302', '04070302');
INSERT INTO soknenummer (identifier, description) VALUES ('04070401', '04070401');
INSERT INTO soknenummer (identifier, description) VALUES ('04070402', '04070402');
INSERT INTO soknenummer (identifier, description) VALUES ('04070403', '04070403');
INSERT INTO soknenummer (identifier, description) VALUES ('04070501', '04070501');
INSERT INTO soknenummer (identifier, description) VALUES ('04080101', '04080101');
INSERT INTO soknenummer (identifier, description) VALUES ('04080102', '04080102');
INSERT INTO soknenummer (identifier, description) VALUES ('04080103', '04080103');
INSERT INTO soknenummer (identifier, description) VALUES ('04080201', '04080201');
INSERT INTO soknenummer (identifier, description) VALUES ('04080202', '04080202');
INSERT INTO soknenummer (identifier, description) VALUES ('04080302', '04080302');
INSERT INTO soknenummer (identifier, description) VALUES ('04080401', '04080401');
INSERT INTO soknenummer (identifier, description) VALUES ('04080402', '04080402');
INSERT INTO soknenummer (identifier, description) VALUES ('04080403', '04080403');
INSERT INTO soknenummer (identifier, description) VALUES ('04090101', '04090101');
INSERT INTO soknenummer (identifier, description) VALUES ('04090102', '04090102');
INSERT INTO soknenummer (identifier, description) VALUES ('04090201', '04090201');
INSERT INTO soknenummer (identifier, description) VALUES ('04090301', '04090301');
INSERT INTO soknenummer (identifier, description) VALUES ('04090401', '04090401');
INSERT INTO soknenummer (identifier, description) VALUES ('04090402', '04090402');
INSERT INTO soknenummer (identifier, description) VALUES ('04090403', '04090403');
INSERT INTO soknenummer (identifier, description) VALUES ('04041301', '04041301');
INSERT INTO soknenummer (identifier, description) VALUES ('04041302', '04041302');
INSERT INTO soknenummer (identifier, description) VALUES ('01070501', '01070501');
INSERT INTO soknenummer (identifier, description) VALUES ('01070502', '01070502');
INSERT INTO soknenummer (identifier, description) VALUES ('01070503', '01070503');
INSERT INTO soknenummer (identifier, description) VALUES ('04041303', '04041303');
INSERT INTO soknenummer (identifier, description) VALUES ('04041304', '04041304');
INSERT INTO soknenummer (identifier, description) VALUES ('05010101', '05010101');
INSERT INTO soknenummer (identifier, description) VALUES ('05010201', '05010201');
INSERT INTO soknenummer (identifier, description) VALUES ('05010301', '05010301');
INSERT INTO soknenummer (identifier, description) VALUES ('05010302', '05010302');
INSERT INTO soknenummer (identifier, description) VALUES ('05010401', '05010401');
INSERT INTO soknenummer (identifier, description) VALUES ('05010501', '05010501');
INSERT INTO soknenummer (identifier, description) VALUES ('05010502', '05010502');
INSERT INTO soknenummer (identifier, description) VALUES ('05010601', '05010601');
INSERT INTO soknenummer (identifier, description) VALUES ('05010701', '05010701');
INSERT INTO soknenummer (identifier, description) VALUES ('05010702', '05010702');
INSERT INTO soknenummer (identifier, description) VALUES ('05010801', '05010801');
INSERT INTO soknenummer (identifier, description) VALUES ('05010802', '05010802');
INSERT INTO soknenummer (identifier, description) VALUES ('05150202', '05150202');
INSERT INTO soknenummer (identifier, description) VALUES ('05150201', '05150201');
INSERT INTO soknenummer (identifier, description) VALUES ('05011001', '05011001');
INSERT INTO soknenummer (identifier, description) VALUES ('05010901', '05010901');
INSERT INTO soknenummer (identifier, description) VALUES ('05010902', '05010902');
INSERT INTO soknenummer (identifier, description) VALUES ('05150102', '05150102');
INSERT INTO soknenummer (identifier, description) VALUES ('05150103', '05150103');
INSERT INTO soknenummer (identifier, description) VALUES ('05050501', '05050501');
INSERT INTO soknenummer (identifier, description) VALUES ('05150301', '05150301');
INSERT INTO soknenummer (identifier, description) VALUES ('05150302', '05150302');
INSERT INTO soknenummer (identifier, description) VALUES ('05150401', '05150401');
INSERT INTO soknenummer (identifier, description) VALUES ('05150501', '05150501');
INSERT INTO soknenummer (identifier, description) VALUES ('05150502', '05150502');
INSERT INTO soknenummer (identifier, description) VALUES ('05150503', '05150503');
INSERT INTO soknenummer (identifier, description) VALUES ('05150504', '05150504');
INSERT INTO soknenummer (identifier, description) VALUES ('05150601', '05150601');
INSERT INTO soknenummer (identifier, description) VALUES ('05150602', '05150602');
INSERT INTO soknenummer (identifier, description) VALUES ('05150603', '05150603');
INSERT INTO soknenummer (identifier, description) VALUES ('05150701', '05150701');
INSERT INTO soknenummer (identifier, description) VALUES ('05050101', '05050101');
INSERT INTO soknenummer (identifier, description) VALUES ('05050102', '05050102');
INSERT INTO soknenummer (identifier, description) VALUES ('05050103', '05050103');
INSERT INTO soknenummer (identifier, description) VALUES ('05050203', '05050203');
INSERT INTO soknenummer (identifier, description) VALUES ('05050401', '05050401');
INSERT INTO soknenummer (identifier, description) VALUES ('05050403', '05050403');
INSERT INTO soknenummer (identifier, description) VALUES ('05060101', '05060101');
INSERT INTO soknenummer (identifier, description) VALUES ('05060102', '05060102');
INSERT INTO soknenummer (identifier, description) VALUES ('05060201', '05060201');
INSERT INTO soknenummer (identifier, description) VALUES ('05060202', '05060202');
INSERT INTO soknenummer (identifier, description) VALUES ('05060301', '05060301');
INSERT INTO soknenummer (identifier, description) VALUES ('05060302', '05060302');
INSERT INTO soknenummer (identifier, description) VALUES ('05060401', '05060401');
INSERT INTO soknenummer (identifier, description) VALUES ('05060402', '05060402');
INSERT INTO soknenummer (identifier, description) VALUES ('05060403', '05060403');
INSERT INTO soknenummer (identifier, description) VALUES ('05070101', '05070101');
INSERT INTO soknenummer (identifier, description) VALUES ('05070201', '05070201');
INSERT INTO soknenummer (identifier, description) VALUES ('05070301', '05070301');
INSERT INTO soknenummer (identifier, description) VALUES ('05070501', '05070501');
INSERT INTO soknenummer (identifier, description) VALUES ('05070601', '05070601');
INSERT INTO soknenummer (identifier, description) VALUES ('05080101', '05080101');
INSERT INTO soknenummer (identifier, description) VALUES ('05080102', '05080102');
INSERT INTO soknenummer (identifier, description) VALUES ('05080103', '05080103');
INSERT INTO soknenummer (identifier, description) VALUES ('05080201', '05080201');
INSERT INTO soknenummer (identifier, description) VALUES ('05080203', '05080203');
INSERT INTO soknenummer (identifier, description) VALUES ('05080301', '05080301');
INSERT INTO soknenummer (identifier, description) VALUES ('05080302', '05080302');
INSERT INTO soknenummer (identifier, description) VALUES ('05090101', '05090101');
INSERT INTO soknenummer (identifier, description) VALUES ('05090102', '05090102');
INSERT INTO soknenummer (identifier, description) VALUES ('05090103', '05090103');
INSERT INTO soknenummer (identifier, description) VALUES ('05090203', '05090203');
INSERT INTO soknenummer (identifier, description) VALUES ('05090301', '05090301');
INSERT INTO soknenummer (identifier, description) VALUES ('05090302', '05090302');
INSERT INTO soknenummer (identifier, description) VALUES ('05090303', '05090303');
INSERT INTO soknenummer (identifier, description) VALUES ('05090401', '05090401');
INSERT INTO soknenummer (identifier, description) VALUES ('05090501', '05090501');
INSERT INTO soknenummer (identifier, description) VALUES ('05090502', '05090502');
INSERT INTO soknenummer (identifier, description) VALUES ('05090503', '05090503');
INSERT INTO soknenummer (identifier, description) VALUES ('05100101', '05100101');
INSERT INTO soknenummer (identifier, description) VALUES ('05100202', '05100202');
INSERT INTO soknenummer (identifier, description) VALUES ('05100404', '05100404');
INSERT INTO soknenummer (identifier, description) VALUES ('05100601', '05100601');
INSERT INTO soknenummer (identifier, description) VALUES ('05100602', '05100602');
INSERT INTO soknenummer (identifier, description) VALUES ('05100901', '05100901');
INSERT INTO soknenummer (identifier, description) VALUES ('05101001', '05101001');
INSERT INTO soknenummer (identifier, description) VALUES ('05140101', '05140101');
INSERT INTO soknenummer (identifier, description) VALUES ('05140201', '05140201');
INSERT INTO soknenummer (identifier, description) VALUES ('05140202', '05140202');
INSERT INTO soknenummer (identifier, description) VALUES ('05140203', '05140203');
INSERT INTO soknenummer (identifier, description) VALUES ('05140401', '05140401');
INSERT INTO soknenummer (identifier, description) VALUES ('05140601', '05140601');
INSERT INTO soknenummer (identifier, description) VALUES ('05140703', '05140703');
INSERT INTO soknenummer (identifier, description) VALUES ('05140801', '05140801');
INSERT INTO soknenummer (identifier, description) VALUES ('05141003', '05141003');
INSERT INTO soknenummer (identifier, description) VALUES ('05141004', '05141004');
INSERT INTO soknenummer (identifier, description) VALUES ('05141103', '05141103');
INSERT INTO soknenummer (identifier, description) VALUES ('05141201', '05141201');
INSERT INTO soknenummer (identifier, description) VALUES ('05141402', '05141402');
INSERT INTO soknenummer (identifier, description) VALUES ('05141501', '05141501');
INSERT INTO soknenummer (identifier, description) VALUES ('05141503', '05141503');
INSERT INTO soknenummer (identifier, description) VALUES ('06010301', '06010301');
INSERT INTO soknenummer (identifier, description) VALUES ('06011101', '06011101');
INSERT INTO soknenummer (identifier, description) VALUES ('06011102', '06011102');
INSERT INTO soknenummer (identifier, description) VALUES ('06011201', '06011201');
INSERT INTO soknenummer (identifier, description) VALUES ('06051102', '06051102');
INSERT INTO soknenummer (identifier, description) VALUES ('06051202', '06051202');
INSERT INTO soknenummer (identifier, description) VALUES ('06070101', '06070101');
INSERT INTO soknenummer (identifier, description) VALUES ('06070102', '06070102');
INSERT INTO soknenummer (identifier, description) VALUES ('06070103', '06070103');
INSERT INTO soknenummer (identifier, description) VALUES ('06070501', '06070501');
INSERT INTO soknenummer (identifier, description) VALUES ('06070502', '06070502');
INSERT INTO soknenummer (identifier, description) VALUES ('06070503', '06070503');
INSERT INTO soknenummer (identifier, description) VALUES ('06070601', '06070601');
INSERT INTO soknenummer (identifier, description) VALUES ('06070602', '06070602');
INSERT INTO soknenummer (identifier, description) VALUES ('06070701', '06070701');
INSERT INTO soknenummer (identifier, description) VALUES ('06070702', '06070702');
INSERT INTO soknenummer (identifier, description) VALUES ('06070703', '06070703');
INSERT INTO soknenummer (identifier, description) VALUES ('06070704', '06070704');
INSERT INTO soknenummer (identifier, description) VALUES ('06080101', '06080101');
INSERT INTO soknenummer (identifier, description) VALUES ('06080102', '06080102');
INSERT INTO soknenummer (identifier, description) VALUES ('06080201', '06080201');
INSERT INTO soknenummer (identifier, description) VALUES ('06080202', '06080202');
INSERT INTO soknenummer (identifier, description) VALUES ('06080301', '06080301');
INSERT INTO soknenummer (identifier, description) VALUES ('06080401', '06080401');
INSERT INTO soknenummer (identifier, description) VALUES ('06080601', '06080601');
INSERT INTO soknenummer (identifier, description) VALUES ('06080701', '06080701');
INSERT INTO soknenummer (identifier, description) VALUES ('06090101', '06090101');
INSERT INTO soknenummer (identifier, description) VALUES ('06090201', '06090201');
INSERT INTO soknenummer (identifier, description) VALUES ('06090202', '06090202');
INSERT INTO soknenummer (identifier, description) VALUES ('06090301', '06090301');
INSERT INTO soknenummer (identifier, description) VALUES ('06090302', '06090302');
INSERT INTO soknenummer (identifier, description) VALUES ('06090401', '06090401');
INSERT INTO soknenummer (identifier, description) VALUES ('06090402', '06090402');
INSERT INTO soknenummer (identifier, description) VALUES ('06090501', '06090501');
INSERT INTO soknenummer (identifier, description) VALUES ('06100101', '06100101');
INSERT INTO soknenummer (identifier, description) VALUES ('06100201', '06100201');
INSERT INTO soknenummer (identifier, description) VALUES ('06100301', '06100301');
INSERT INTO soknenummer (identifier, description) VALUES ('06100401', '06100401');
INSERT INTO soknenummer (identifier, description) VALUES ('06100402', '06100402');
INSERT INTO soknenummer (identifier, description) VALUES ('06100501', '06100501');
INSERT INTO soknenummer (identifier, description) VALUES ('06100502', '06100502');
INSERT INTO soknenummer (identifier, description) VALUES ('07010101', '07010101');
INSERT INTO soknenummer (identifier, description) VALUES ('07140101', '07140101');
INSERT INTO soknenummer (identifier, description) VALUES ('07140102', '07140102');
INSERT INTO soknenummer (identifier, description) VALUES ('07140103', '07140103');
INSERT INTO soknenummer (identifier, description) VALUES ('07140104', '07140104');
INSERT INTO soknenummer (identifier, description) VALUES ('07011101', '07011101');
INSERT INTO soknenummer (identifier, description) VALUES ('07020101', '07020101');
INSERT INTO soknenummer (identifier, description) VALUES ('07020201', '07020201');
INSERT INTO soknenummer (identifier, description) VALUES ('07020301', '07020301');
INSERT INTO soknenummer (identifier, description) VALUES ('07140105', '07140105');
INSERT INTO soknenummer (identifier, description) VALUES ('07140106', '07140106');
INSERT INTO soknenummer (identifier, description) VALUES ('07140107', '07140107');
INSERT INTO soknenummer (identifier, description) VALUES ('07020601', '07020601');
INSERT INTO soknenummer (identifier, description) VALUES ('07011201', '07011201');
INSERT INTO soknenummer (identifier, description) VALUES ('07011202', '07011202');
INSERT INTO soknenummer (identifier, description) VALUES ('07141108', '07141108');
INSERT INTO soknenummer (identifier, description) VALUES ('07141109', '07141109');
INSERT INTO soknenummer (identifier, description) VALUES ('07011301', '07011301');
INSERT INTO soknenummer (identifier, description) VALUES ('07011401', '07011401');
INSERT INTO soknenummer (identifier, description) VALUES ('07040101', '07040101');
INSERT INTO soknenummer (identifier, description) VALUES ('07040201', '07040201');
INSERT INTO soknenummer (identifier, description) VALUES ('07040202', '07040202');
INSERT INTO soknenummer (identifier, description) VALUES ('07040301', '07040301');
INSERT INTO soknenummer (identifier, description) VALUES ('07040401', '07040401');
INSERT INTO soknenummer (identifier, description) VALUES ('07040501', '07040501');
INSERT INTO soknenummer (identifier, description) VALUES ('07050101', '07050101');
INSERT INTO soknenummer (identifier, description) VALUES ('07050102', '07050102');
INSERT INTO soknenummer (identifier, description) VALUES ('07050204', '07050204');
INSERT INTO soknenummer (identifier, description) VALUES ('07050206', '07050206');
INSERT INTO soknenummer (identifier, description) VALUES ('07050301', '07050301');
INSERT INTO soknenummer (identifier, description) VALUES ('07050401', '07050401');
INSERT INTO soknenummer (identifier, description) VALUES ('07050501', '07050501');
INSERT INTO soknenummer (identifier, description) VALUES ('07050502', '07050502');
INSERT INTO soknenummer (identifier, description) VALUES ('07050503', '07050503');
INSERT INTO soknenummer (identifier, description) VALUES ('07050504', '07050504');
INSERT INTO soknenummer (identifier, description) VALUES ('07050505', '07050505');
INSERT INTO soknenummer (identifier, description) VALUES ('07050506', '07050506');
INSERT INTO soknenummer (identifier, description) VALUES ('07050507', '07050507');
INSERT INTO soknenummer (identifier, description) VALUES ('07050701', '07050701');
INSERT INTO soknenummer (identifier, description) VALUES ('07050704', '07050704');
INSERT INTO soknenummer (identifier, description) VALUES ('07050801', '07050801');
INSERT INTO soknenummer (identifier, description) VALUES ('07050802', '07050802');
INSERT INTO soknenummer (identifier, description) VALUES ('07050803', '07050803');
INSERT INTO soknenummer (identifier, description) VALUES ('07050804', '07050804');
INSERT INTO soknenummer (identifier, description) VALUES ('07050901', '07050901');
INSERT INTO soknenummer (identifier, description) VALUES ('07051001', '07051001');
INSERT INTO soknenummer (identifier, description) VALUES ('07051002', '07051002');
INSERT INTO soknenummer (identifier, description) VALUES ('07051005', '07051005');
INSERT INTO soknenummer (identifier, description) VALUES ('07021401', '07021401');
INSERT INTO soknenummer (identifier, description) VALUES ('07021001', '07021001');
INSERT INTO soknenummer (identifier, description) VALUES ('07081101', '07081101');
INSERT INTO soknenummer (identifier, description) VALUES ('07021501', '07021501');
INSERT INTO soknenummer (identifier, description) VALUES ('07070202', '07070202');
INSERT INTO soknenummer (identifier, description) VALUES ('07070303', '07070303');
INSERT INTO soknenummer (identifier, description) VALUES ('07070304', '07070304');
INSERT INTO soknenummer (identifier, description) VALUES ('07040601', '07040601');
INSERT INTO soknenummer (identifier, description) VALUES ('07040602', '07040602');
INSERT INTO soknenummer (identifier, description) VALUES ('07040603', '07040603');
INSERT INTO soknenummer (identifier, description) VALUES ('07040604', '07040604');
INSERT INTO soknenummer (identifier, description) VALUES ('07040605', '07040605');
INSERT INTO soknenummer (identifier, description) VALUES ('07070601', '07070601');
INSERT INTO soknenummer (identifier, description) VALUES ('07070701', '07070701');
INSERT INTO soknenummer (identifier, description) VALUES ('07080101', '07080101');
INSERT INTO soknenummer (identifier, description) VALUES ('07080102', '07080102');
INSERT INTO soknenummer (identifier, description) VALUES ('07080103', '07080103');
INSERT INTO soknenummer (identifier, description) VALUES ('07080201', '07080201');
INSERT INTO soknenummer (identifier, description) VALUES ('07080202', '07080202');
INSERT INTO soknenummer (identifier, description) VALUES ('07080401', '07080401');
INSERT INTO soknenummer (identifier, description) VALUES ('07080402', '07080402');
INSERT INTO soknenummer (identifier, description) VALUES ('07080403', '07080403');
INSERT INTO soknenummer (identifier, description) VALUES ('07080404', '07080404');
INSERT INTO soknenummer (identifier, description) VALUES ('07080501', '07080501');
INSERT INTO soknenummer (identifier, description) VALUES ('07080502', '07080502');
INSERT INTO soknenummer (identifier, description) VALUES ('07080503', '07080503');
INSERT INTO soknenummer (identifier, description) VALUES ('07080504', '07080504');
INSERT INTO soknenummer (identifier, description) VALUES ('07080505', '07080505');
INSERT INTO soknenummer (identifier, description) VALUES ('07080601', '07080601');
INSERT INTO soknenummer (identifier, description) VALUES ('07080602', '07080602');
INSERT INTO soknenummer (identifier, description) VALUES ('07080801', '07080801');
INSERT INTO soknenummer (identifier, description) VALUES ('07080802', '07080802');
INSERT INTO soknenummer (identifier, description) VALUES ('07080803', '07080803');
INSERT INTO soknenummer (identifier, description) VALUES ('07080804', '07080804');
INSERT INTO soknenummer (identifier, description) VALUES ('07080901', '07080901');
INSERT INTO soknenummer (identifier, description) VALUES ('07080902', '07080902');
INSERT INTO soknenummer (identifier, description) VALUES ('07080903', '07080903');
INSERT INTO soknenummer (identifier, description) VALUES ('07080904', '07080904');
INSERT INTO soknenummer (identifier, description) VALUES ('07080905', '07080905');
INSERT INTO soknenummer (identifier, description) VALUES ('07080906', '07080906');
INSERT INTO soknenummer (identifier, description) VALUES ('07090101', '07090101');
INSERT INTO soknenummer (identifier, description) VALUES ('07090102', '07090102');
INSERT INTO soknenummer (identifier, description) VALUES ('07090103', '07090103');
INSERT INTO soknenummer (identifier, description) VALUES ('07090104', '07090104');
INSERT INTO soknenummer (identifier, description) VALUES ('07090201', '07090201');
INSERT INTO soknenummer (identifier, description) VALUES ('07090202', '07090202');
INSERT INTO soknenummer (identifier, description) VALUES ('07090203', '07090203');
INSERT INTO soknenummer (identifier, description) VALUES ('07090204', '07090204');
INSERT INTO soknenummer (identifier, description) VALUES ('07090401', '07090401');
INSERT INTO soknenummer (identifier, description) VALUES ('07090402', '07090402');
INSERT INTO soknenummer (identifier, description) VALUES ('07090501', '07090501');
INSERT INTO soknenummer (identifier, description) VALUES ('07090502', '07090502');
INSERT INTO soknenummer (identifier, description) VALUES ('07090503', '07090503');
INSERT INTO soknenummer (identifier, description) VALUES ('07090601', '07090601');
INSERT INTO soknenummer (identifier, description) VALUES ('07090602', '07090602');
INSERT INTO soknenummer (identifier, description) VALUES ('07090604', '07090604');
INSERT INTO soknenummer (identifier, description) VALUES ('07090701', '07090701');
INSERT INTO soknenummer (identifier, description) VALUES ('07090702', '07090702');
INSERT INTO soknenummer (identifier, description) VALUES ('07090801', '07090801');
INSERT INTO soknenummer (identifier, description) VALUES ('07110801', '07110801');
INSERT INTO soknenummer (identifier, description) VALUES ('07110802', '07110802');
INSERT INTO soknenummer (identifier, description) VALUES ('07110803', '07110803');
INSERT INTO soknenummer (identifier, description) VALUES ('07110804', '07110804');
INSERT INTO soknenummer (identifier, description) VALUES ('07090901', '07090901');
INSERT INTO soknenummer (identifier, description) VALUES ('07090902', '07090902');
INSERT INTO soknenummer (identifier, description) VALUES ('07090903', '07090903');
INSERT INTO soknenummer (identifier, description) VALUES ('07090904', '07090904');
INSERT INTO soknenummer (identifier, description) VALUES ('07090905', '07090905');
INSERT INTO soknenummer (identifier, description) VALUES ('07091001', '07091001');
INSERT INTO soknenummer (identifier, description) VALUES ('07070801', '07070801');
INSERT INTO soknenummer (identifier, description) VALUES ('07070802', '07070802');
INSERT INTO soknenummer (identifier, description) VALUES ('07070803', '07070803');
INSERT INTO soknenummer (identifier, description) VALUES ('07110901', '07110901');
INSERT INTO soknenummer (identifier, description) VALUES ('07070901', '07070901');
INSERT INTO soknenummer (identifier, description) VALUES ('07110101', '07110101');
INSERT INTO soknenummer (identifier, description) VALUES ('07110102', '07110102');
INSERT INTO soknenummer (identifier, description) VALUES ('07110201', '07110201');
INSERT INTO soknenummer (identifier, description) VALUES ('07110202', '07110202');
INSERT INTO soknenummer (identifier, description) VALUES ('07110401', '07110401');
INSERT INTO soknenummer (identifier, description) VALUES ('07110501', '07110501');
INSERT INTO soknenummer (identifier, description) VALUES ('07110601', '07110601');
INSERT INTO soknenummer (identifier, description) VALUES ('07110602', '07110602');
INSERT INTO soknenummer (identifier, description) VALUES ('07110603', '07110603');
INSERT INTO soknenummer (identifier, description) VALUES ('07110604', '07110604');
INSERT INTO soknenummer (identifier, description) VALUES ('07120101', '07120101');
INSERT INTO soknenummer (identifier, description) VALUES ('07120102', '07120102');
INSERT INTO soknenummer (identifier, description) VALUES ('07120103', '07120103');
INSERT INTO soknenummer (identifier, description) VALUES ('07120201', '07120201');
INSERT INTO soknenummer (identifier, description) VALUES ('07120202', '07120202');
INSERT INTO soknenummer (identifier, description) VALUES ('07120203', '07120203');
INSERT INTO soknenummer (identifier, description) VALUES ('07120301', '07120301');
INSERT INTO soknenummer (identifier, description) VALUES ('07120302', '07120302');
INSERT INTO soknenummer (identifier, description) VALUES ('07120303', '07120303');
INSERT INTO soknenummer (identifier, description) VALUES ('08020801', '08020801');
INSERT INTO soknenummer (identifier, description) VALUES ('07120402', '07120402');
INSERT INTO soknenummer (identifier, description) VALUES ('07120403', '07120403');
INSERT INTO soknenummer (identifier, description) VALUES ('07120502', '07120502');
INSERT INTO soknenummer (identifier, description) VALUES ('07120504', '07120504');
INSERT INTO soknenummer (identifier, description) VALUES ('07120601', '07120601');
INSERT INTO soknenummer (identifier, description) VALUES ('07120602', '07120602');
INSERT INTO soknenummer (identifier, description) VALUES ('07120603', '07120603');
INSERT INTO soknenummer (identifier, description) VALUES ('07120604', '07120604');
INSERT INTO soknenummer (identifier, description) VALUES ('07120801', '07120801');
INSERT INTO soknenummer (identifier, description) VALUES ('07120802', '07120802');
INSERT INTO soknenummer (identifier, description) VALUES ('07120803', '07120803');
INSERT INTO soknenummer (identifier, description) VALUES ('07120901', '07120901');
INSERT INTO soknenummer (identifier, description) VALUES ('07120902', '07120902');
INSERT INTO soknenummer (identifier, description) VALUES ('07120903', '07120903');
INSERT INTO soknenummer (identifier, description) VALUES ('07130201', '07130201');
INSERT INTO soknenummer (identifier, description) VALUES ('07130301', '07130301');
INSERT INTO soknenummer (identifier, description) VALUES ('07130302', '07130302');
INSERT INTO soknenummer (identifier, description) VALUES ('07130303', '07130303');
INSERT INTO soknenummer (identifier, description) VALUES ('07130304', '07130304');
INSERT INTO soknenummer (identifier, description) VALUES ('07130305', '07130305');
INSERT INTO soknenummer (identifier, description) VALUES ('08010101', '08010101');
INSERT INTO soknenummer (identifier, description) VALUES ('08010201', '08010201');
INSERT INTO soknenummer (identifier, description) VALUES ('08010204', '08010204');
INSERT INTO soknenummer (identifier, description) VALUES ('08010205', '08010205');
INSERT INTO soknenummer (identifier, description) VALUES ('08010305', '08010305');
INSERT INTO soknenummer (identifier, description) VALUES ('08010303', '08010303');
INSERT INTO soknenummer (identifier, description) VALUES ('08010304', '08010304');
INSERT INTO soknenummer (identifier, description) VALUES ('08010501', '08010501');
INSERT INTO soknenummer (identifier, description) VALUES ('08010502', '08010502');
INSERT INTO soknenummer (identifier, description) VALUES ('08010503', '08010503');
INSERT INTO soknenummer (identifier, description) VALUES ('08020101', '08020101');
INSERT INTO soknenummer (identifier, description) VALUES ('08020102', '08020102');
INSERT INTO soknenummer (identifier, description) VALUES ('08020103', '08020103');
INSERT INTO soknenummer (identifier, description) VALUES ('08020104', '08020104');
INSERT INTO soknenummer (identifier, description) VALUES ('08020201', '08020201');
INSERT INTO soknenummer (identifier, description) VALUES ('08020202', '08020202');
INSERT INTO soknenummer (identifier, description) VALUES ('08020301', '08020301');
INSERT INTO soknenummer (identifier, description) VALUES ('08020302', '08020302');
INSERT INTO soknenummer (identifier, description) VALUES ('08020401', '08020401');
INSERT INTO soknenummer (identifier, description) VALUES ('08020402', '08020402');
INSERT INTO soknenummer (identifier, description) VALUES ('08020403', '08020403');
INSERT INTO soknenummer (identifier, description) VALUES ('08020404', '08020404');
INSERT INTO soknenummer (identifier, description) VALUES ('08020501', '08020501');
INSERT INTO soknenummer (identifier, description) VALUES ('08020503', '08020503');
INSERT INTO soknenummer (identifier, description) VALUES ('08020601', '08020601');
INSERT INTO soknenummer (identifier, description) VALUES ('08020602', '08020602');
INSERT INTO soknenummer (identifier, description) VALUES ('08020603', '08020603');
INSERT INTO soknenummer (identifier, description) VALUES ('08020701', '08020701');
INSERT INTO soknenummer (identifier, description) VALUES ('08020702', '08020702');
INSERT INTO soknenummer (identifier, description) VALUES ('08040901', '08040901');
INSERT INTO soknenummer (identifier, description) VALUES ('08041101', '08041101');
INSERT INTO soknenummer (identifier, description) VALUES ('08040902', '08040902');
INSERT INTO soknenummer (identifier, description) VALUES ('08041102', '08041102');
INSERT INTO soknenummer (identifier, description) VALUES ('08041103', '08041103');
INSERT INTO soknenummer (identifier, description) VALUES ('08041104', '08041104');
INSERT INTO soknenummer (identifier, description) VALUES ('08041105', '08041105');
INSERT INTO soknenummer (identifier, description) VALUES ('08041106', '08041106');
INSERT INTO soknenummer (identifier, description) VALUES ('08041001', '08041001');
INSERT INTO soknenummer (identifier, description) VALUES ('08041002', '08041002');
INSERT INTO soknenummer (identifier, description) VALUES ('08040201', '08040201');
INSERT INTO soknenummer (identifier, description) VALUES ('08040401', '08040401');
INSERT INTO soknenummer (identifier, description) VALUES ('08040402', '08040402');
INSERT INTO soknenummer (identifier, description) VALUES ('08040501', '08040501');
INSERT INTO soknenummer (identifier, description) VALUES ('08040601', '08040601');
INSERT INTO soknenummer (identifier, description) VALUES ('08040602', '08040602');
INSERT INTO soknenummer (identifier, description) VALUES ('08040603', '08040603');
INSERT INTO soknenummer (identifier, description) VALUES ('08040701', '08040701');
INSERT INTO soknenummer (identifier, description) VALUES ('08040702', '08040702');
INSERT INTO soknenummer (identifier, description) VALUES ('08040801', '08040801');
INSERT INTO soknenummer (identifier, description) VALUES ('08040802', '08040802');
INSERT INTO soknenummer (identifier, description) VALUES ('08040803', '08040803');
INSERT INTO soknenummer (identifier, description) VALUES ('08050101', '08050101');
INSERT INTO soknenummer (identifier, description) VALUES ('08050102', '08050102');
INSERT INTO soknenummer (identifier, description) VALUES ('08050103', '08050103');
INSERT INTO soknenummer (identifier, description) VALUES ('08050105', '08050105');
INSERT INTO soknenummer (identifier, description) VALUES ('08050106', '08050106');
INSERT INTO soknenummer (identifier, description) VALUES ('08050107', '08050107');
INSERT INTO soknenummer (identifier, description) VALUES ('08050108', '08050108');
INSERT INTO soknenummer (identifier, description) VALUES ('08010701', '08010701');
INSERT INTO soknenummer (identifier, description) VALUES ('08010702', '08010702');
INSERT INTO soknenummer (identifier, description) VALUES ('08010703', '08010703');
INSERT INTO soknenummer (identifier, description) VALUES ('08010704', '08010704');
INSERT INTO soknenummer (identifier, description) VALUES ('08050301', '08050301');
INSERT INTO soknenummer (identifier, description) VALUES ('08050302', '08050302');
INSERT INTO soknenummer (identifier, description) VALUES ('08050303', '08050303');
INSERT INTO soknenummer (identifier, description) VALUES ('08050304', '08050304');
INSERT INTO soknenummer (identifier, description) VALUES ('08010601', '08010601');
INSERT INTO soknenummer (identifier, description) VALUES ('08060401', '08060401');
INSERT INTO soknenummer (identifier, description) VALUES ('08060402', '08060402');
INSERT INTO soknenummer (identifier, description) VALUES ('08060403', '08060403');
INSERT INTO soknenummer (identifier, description) VALUES ('08060701', '08060701');
INSERT INTO soknenummer (identifier, description) VALUES ('08060702', '08060702');
INSERT INTO soknenummer (identifier, description) VALUES ('08060703', '08060703');
INSERT INTO soknenummer (identifier, description) VALUES ('08060801', '08060801');
INSERT INTO soknenummer (identifier, description) VALUES ('08060802', '08060802');
INSERT INTO soknenummer (identifier, description) VALUES ('08060803', '08060803');
INSERT INTO soknenummer (identifier, description) VALUES ('08060901', '08060901');
INSERT INTO soknenummer (identifier, description) VALUES ('08060902', '08060902');
INSERT INTO soknenummer (identifier, description) VALUES ('08060903', '08060903');
INSERT INTO soknenummer (identifier, description) VALUES ('08070101', '08070101');
INSERT INTO soknenummer (identifier, description) VALUES ('08070102', '08070102');
INSERT INTO soknenummer (identifier, description) VALUES ('08070201', '08070201');
INSERT INTO soknenummer (identifier, description) VALUES ('08070202', '08070202');
INSERT INTO soknenummer (identifier, description) VALUES ('08070203', '08070203');
INSERT INTO soknenummer (identifier, description) VALUES ('08070204', '08070204');
INSERT INTO soknenummer (identifier, description) VALUES ('09061001', '09061001');
INSERT INTO soknenummer (identifier, description) VALUES ('08070404', '08070404');
INSERT INTO soknenummer (identifier, description) VALUES ('08070402', '08070402');
INSERT INTO soknenummer (identifier, description) VALUES ('08070501', '08070501');
INSERT INTO soknenummer (identifier, description) VALUES ('08070502', '08070502');
INSERT INTO soknenummer (identifier, description) VALUES ('08070503', '08070503');
INSERT INTO soknenummer (identifier, description) VALUES ('08070601', '08070601');
INSERT INTO soknenummer (identifier, description) VALUES ('08070602', '08070602');
INSERT INTO soknenummer (identifier, description) VALUES ('09010101', '09010101');
INSERT INTO soknenummer (identifier, description) VALUES ('09010302', '09010302');
INSERT INTO soknenummer (identifier, description) VALUES ('09020101', '09020101');
INSERT INTO soknenummer (identifier, description) VALUES ('09020102', '09020102');
INSERT INTO soknenummer (identifier, description) VALUES ('09020401', '09020401');
INSERT INTO soknenummer (identifier, description) VALUES ('09030101', '09030101');
INSERT INTO soknenummer (identifier, description) VALUES ('09030201', '09030201');
INSERT INTO soknenummer (identifier, description) VALUES ('09030601', '09030601');
INSERT INTO soknenummer (identifier, description) VALUES ('09030701', '09030701');
INSERT INTO soknenummer (identifier, description) VALUES ('09050101', '09050101');
INSERT INTO soknenummer (identifier, description) VALUES ('09050102', '09050102');
INSERT INTO soknenummer (identifier, description) VALUES ('09050103', '09050103');
INSERT INTO soknenummer (identifier, description) VALUES ('09050104', '09050104');
INSERT INTO soknenummer (identifier, description) VALUES ('09050201', '09050201');
INSERT INTO soknenummer (identifier, description) VALUES ('09050501', '09050501');
INSERT INTO soknenummer (identifier, description) VALUES ('09050502', '09050502');
INSERT INTO soknenummer (identifier, description) VALUES ('09050601', '09050601');
INSERT INTO soknenummer (identifier, description) VALUES ('09050602', '09050602');
INSERT INTO soknenummer (identifier, description) VALUES ('09060101', '09060101');
INSERT INTO soknenummer (identifier, description) VALUES ('09060102', '09060102');
INSERT INTO soknenummer (identifier, description) VALUES ('09060103', '09060103');
INSERT INTO soknenummer (identifier, description) VALUES ('09060104', '09060104');
INSERT INTO soknenummer (identifier, description) VALUES ('09060401', '09060401');
INSERT INTO soknenummer (identifier, description) VALUES ('09060402', '09060402');
INSERT INTO soknenummer (identifier, description) VALUES ('09060501', '09060501');
INSERT INTO soknenummer (identifier, description) VALUES ('09060502', '09060502');
INSERT INTO soknenummer (identifier, description) VALUES ('09060503', '09060503');
INSERT INTO soknenummer (identifier, description) VALUES ('09060601', '09060601');
INSERT INTO soknenummer (identifier, description) VALUES ('09060801', '09060801');
INSERT INTO soknenummer (identifier, description) VALUES ('09060802', '09060802');
INSERT INTO soknenummer (identifier, description) VALUES ('09060803', '09060803');
INSERT INTO soknenummer (identifier, description) VALUES ('09060804', '09060804');
INSERT INTO soknenummer (identifier, description) VALUES ('09060901', '09060901');
INSERT INTO soknenummer (identifier, description) VALUES ('09070101', '09070101');
INSERT INTO soknenummer (identifier, description) VALUES ('09070102', '09070102');
INSERT INTO soknenummer (identifier, description) VALUES ('09070103', '09070103');
INSERT INTO soknenummer (identifier, description) VALUES ('09070104', '09070104');
INSERT INTO soknenummer (identifier, description) VALUES ('09070201', '09070201');
INSERT INTO soknenummer (identifier, description) VALUES ('09070202', '09070202');
INSERT INTO soknenummer (identifier, description) VALUES ('09070203', '09070203');
INSERT INTO soknenummer (identifier, description) VALUES ('09070204', '09070204');
INSERT INTO soknenummer (identifier, description) VALUES ('09070301', '09070301');
INSERT INTO soknenummer (identifier, description) VALUES ('09070302', '09070302');
INSERT INTO soknenummer (identifier, description) VALUES ('09070303', '09070303');
INSERT INTO soknenummer (identifier, description) VALUES ('09070401', '09070401');
INSERT INTO soknenummer (identifier, description) VALUES ('09070402', '09070402');
INSERT INTO soknenummer (identifier, description) VALUES ('09070403', '09070403');
INSERT INTO soknenummer (identifier, description) VALUES ('09070404', '09070404');
INSERT INTO soknenummer (identifier, description) VALUES ('09070501', '09070501');
INSERT INTO soknenummer (identifier, description) VALUES ('09070503', '09070503');
INSERT INTO soknenummer (identifier, description) VALUES ('09070601', '09070601');
INSERT INTO soknenummer (identifier, description) VALUES ('09070602', '09070602');
INSERT INTO soknenummer (identifier, description) VALUES ('09070603', '09070603');
INSERT INTO soknenummer (identifier, description) VALUES ('09080101', '09080101');
INSERT INTO soknenummer (identifier, description) VALUES ('09080102', '09080102');
INSERT INTO soknenummer (identifier, description) VALUES ('09080103', '09080103');
INSERT INTO soknenummer (identifier, description) VALUES ('09080201', '09080201');
INSERT INTO soknenummer (identifier, description) VALUES ('09080202', '09080202');
INSERT INTO soknenummer (identifier, description) VALUES ('09080301', '09080301');
INSERT INTO soknenummer (identifier, description) VALUES ('09080302', '09080302');
INSERT INTO soknenummer (identifier, description) VALUES ('09080501', '09080501');
INSERT INTO soknenummer (identifier, description) VALUES ('09080502', '09080502');
INSERT INTO soknenummer (identifier, description) VALUES ('09150101', '09150101');
INSERT INTO soknenummer (identifier, description) VALUES ('09150103', '09150103');
INSERT INTO soknenummer (identifier, description) VALUES ('09150201', '09150201');
INSERT INTO soknenummer (identifier, description) VALUES ('09150202', '09150202');
INSERT INTO soknenummer (identifier, description) VALUES ('09150203', '09150203');
INSERT INTO soknenummer (identifier, description) VALUES ('09150204', '09150204');
INSERT INTO soknenummer (identifier, description) VALUES ('09110603', '09110603');
INSERT INTO soknenummer (identifier, description) VALUES ('09110701', '09110701');
INSERT INTO soknenummer (identifier, description) VALUES ('09110902', '09110902');
INSERT INTO soknenummer (identifier, description) VALUES ('09110804', '09110804');
INSERT INTO soknenummer (identifier, description) VALUES ('09110806', '09110806');
INSERT INTO soknenummer (identifier, description) VALUES ('09030801', '09030801');
INSERT INTO soknenummer (identifier, description) VALUES ('09030802', '09030802');
INSERT INTO soknenummer (identifier, description) VALUES ('09030803', '09030803');
INSERT INTO soknenummer (identifier, description) VALUES ('10010101', '10010101');
INSERT INTO soknenummer (identifier, description) VALUES ('10010202', '10010202');
INSERT INTO soknenummer (identifier, description) VALUES ('10010301', '10010301');
INSERT INTO soknenummer (identifier, description) VALUES ('10010302', '10010302');
INSERT INTO soknenummer (identifier, description) VALUES ('10010401', '10010401');
INSERT INTO soknenummer (identifier, description) VALUES ('10010501', '10010501');
INSERT INTO soknenummer (identifier, description) VALUES ('10010502', '10010502');
INSERT INTO soknenummer (identifier, description) VALUES ('10010503', '10010503');
INSERT INTO soknenummer (identifier, description) VALUES ('10010601', '10010601');
INSERT INTO soknenummer (identifier, description) VALUES ('10010701', '10010701');
INSERT INTO soknenummer (identifier, description) VALUES ('10010702', '10010702');
INSERT INTO soknenummer (identifier, description) VALUES ('10020101', '10020101');
INSERT INTO soknenummer (identifier, description) VALUES ('10020102', '10020102');
INSERT INTO soknenummer (identifier, description) VALUES ('10020201', '10020201');
INSERT INTO soknenummer (identifier, description) VALUES ('10020401', '10020401');
INSERT INTO soknenummer (identifier, description) VALUES ('10020601', '10020601');
INSERT INTO soknenummer (identifier, description) VALUES ('10030101', '10030101');
INSERT INTO soknenummer (identifier, description) VALUES ('10030201', '10030201');
INSERT INTO soknenummer (identifier, description) VALUES ('10030301', '10030301');
INSERT INTO soknenummer (identifier, description) VALUES ('10030302', '10030302');
INSERT INTO soknenummer (identifier, description) VALUES ('10030303', '10030303');
INSERT INTO soknenummer (identifier, description) VALUES ('10030304', '10030304');
INSERT INTO soknenummer (identifier, description) VALUES ('10030401', '10030401');
INSERT INTO soknenummer (identifier, description) VALUES ('10030501', '10030501');
INSERT INTO soknenummer (identifier, description) VALUES ('10030502', '10030502');
INSERT INTO soknenummer (identifier, description) VALUES ('10030503', '10030503');
INSERT INTO soknenummer (identifier, description) VALUES ('10030601', '10030601');
INSERT INTO soknenummer (identifier, description) VALUES ('10030701', '10030701');
INSERT INTO soknenummer (identifier, description) VALUES ('10030702', '10030702');
INSERT INTO soknenummer (identifier, description) VALUES ('10030703', '10030703');
INSERT INTO soknenummer (identifier, description) VALUES ('10040101', '10040101');
INSERT INTO soknenummer (identifier, description) VALUES ('10040102', '10040102');
INSERT INTO soknenummer (identifier, description) VALUES ('10040103', '10040103');
INSERT INTO soknenummer (identifier, description) VALUES ('10040104', '10040104');
INSERT INTO soknenummer (identifier, description) VALUES ('10040201', '10040201');
INSERT INTO soknenummer (identifier, description) VALUES ('10040301', '10040301');
INSERT INTO soknenummer (identifier, description) VALUES ('10040401', '10040401');
INSERT INTO soknenummer (identifier, description) VALUES ('10040501', '10040501');
INSERT INTO soknenummer (identifier, description) VALUES ('10040502', '10040502');
INSERT INTO soknenummer (identifier, description) VALUES ('10040503', '10040503');
INSERT INTO soknenummer (identifier, description) VALUES ('10050101', '10050101');
INSERT INTO soknenummer (identifier, description) VALUES ('10050102', '10050102');
INSERT INTO soknenummer (identifier, description) VALUES ('10050103', '10050103');
INSERT INTO soknenummer (identifier, description) VALUES ('10050201', '10050201');
INSERT INTO soknenummer (identifier, description) VALUES ('10050202', '10050202');
INSERT INTO soknenummer (identifier, description) VALUES ('10050301', '10050301');
INSERT INTO soknenummer (identifier, description) VALUES ('10050501', '10050501');
INSERT INTO soknenummer (identifier, description) VALUES ('10050601', '10050601');
INSERT INTO soknenummer (identifier, description) VALUES ('10050602', '10050602');
INSERT INTO soknenummer (identifier, description) VALUES ('10050603', '10050603');
INSERT INTO soknenummer (identifier, description) VALUES ('10060101', '10060101');
INSERT INTO soknenummer (identifier, description) VALUES ('10060201', '10060201');
INSERT INTO soknenummer (identifier, description) VALUES ('10060202', '10060202');
INSERT INTO soknenummer (identifier, description) VALUES ('10060203', '10060203');
INSERT INTO soknenummer (identifier, description) VALUES ('10060301', '10060301');
INSERT INTO soknenummer (identifier, description) VALUES ('10060302', '10060302');
INSERT INTO soknenummer (identifier, description) VALUES ('10060401', '10060401');
INSERT INTO soknenummer (identifier, description) VALUES ('10060402', '10060402');
INSERT INTO soknenummer (identifier, description) VALUES ('10060403', '10060403');
INSERT INTO soknenummer (identifier, description) VALUES ('10060501', '10060501');
INSERT INTO soknenummer (identifier, description) VALUES ('10060601', '10060601');
INSERT INTO soknenummer (identifier, description) VALUES ('11020604', '11020604');
INSERT INTO soknenummer (identifier, description) VALUES ('10080701', '10080701');
INSERT INTO soknenummer (identifier, description) VALUES ('10070101', '10070101');
INSERT INTO soknenummer (identifier, description) VALUES ('10070102', '10070102');
INSERT INTO soknenummer (identifier, description) VALUES ('10070201', '10070201');
INSERT INTO soknenummer (identifier, description) VALUES ('10070202', '10070202');
INSERT INTO soknenummer (identifier, description) VALUES ('10070203', '10070203');
INSERT INTO soknenummer (identifier, description) VALUES ('10070302', '10070302');
INSERT INTO soknenummer (identifier, description) VALUES ('10070303', '10070303');
INSERT INTO soknenummer (identifier, description) VALUES ('10070401', '10070401');
INSERT INTO soknenummer (identifier, description) VALUES ('10070501', '10070501');
INSERT INTO soknenummer (identifier, description) VALUES ('10070502', '10070502');
INSERT INTO soknenummer (identifier, description) VALUES ('10070601', '10070601');
INSERT INTO soknenummer (identifier, description) VALUES ('10070602', '10070602');
INSERT INTO soknenummer (identifier, description) VALUES ('10080201', '10080201');
INSERT INTO soknenummer (identifier, description) VALUES ('10080503', '10080503');
INSERT INTO soknenummer (identifier, description) VALUES ('10080601', '10080601');
INSERT INTO soknenummer (identifier, description) VALUES ('11010101', '11010101');
INSERT INTO soknenummer (identifier, description) VALUES ('11010201', '11010201');
INSERT INTO soknenummer (identifier, description) VALUES ('11010202', '11010202');
INSERT INTO soknenummer (identifier, description) VALUES ('11010301', '11010301');
INSERT INTO soknenummer (identifier, description) VALUES ('11010401', '11010401');
INSERT INTO soknenummer (identifier, description) VALUES ('11010501', '11010501');
INSERT INTO soknenummer (identifier, description) VALUES ('11010601', '11010601');
INSERT INTO soknenummer (identifier, description) VALUES ('11010602', '11010602');
INSERT INTO soknenummer (identifier, description) VALUES ('11010603', '11010603');
INSERT INTO soknenummer (identifier, description) VALUES ('11010701', '11010701');
INSERT INTO soknenummer (identifier, description) VALUES ('11020101', '11020101');
INSERT INTO soknenummer (identifier, description) VALUES ('11020201', '11020201');
INSERT INTO soknenummer (identifier, description) VALUES ('11020301', '11020301');
INSERT INTO soknenummer (identifier, description) VALUES ('11020302', '11020302');
INSERT INTO soknenummer (identifier, description) VALUES ('11020501', '11020501');
INSERT INTO soknenummer (identifier, description) VALUES ('11020502', '11020502');
INSERT INTO soknenummer (identifier, description) VALUES ('11020601', '11020601');
INSERT INTO soknenummer (identifier, description) VALUES ('11020602', '11020602');
INSERT INTO soknenummer (identifier, description) VALUES ('11020603', '11020603');
INSERT INTO soknenummer (identifier, description) VALUES ('11020701', '11020701');
INSERT INTO soknenummer (identifier, description) VALUES ('11020801', '11020801');
INSERT INTO soknenummer (identifier, description) VALUES ('11030101', '11030101');
INSERT INTO soknenummer (identifier, description) VALUES ('11030401', '11030401');
INSERT INTO soknenummer (identifier, description) VALUES ('11030402', '11030402');
INSERT INTO soknenummer (identifier, description) VALUES ('11030501', '11030501');
INSERT INTO soknenummer (identifier, description) VALUES ('11030601', '11030601');
INSERT INTO soknenummer (identifier, description) VALUES ('11030602', '11030602');
INSERT INTO soknenummer (identifier, description) VALUES ('11040101', '11040101');
INSERT INTO soknenummer (identifier, description) VALUES ('11040102', '11040102');
INSERT INTO soknenummer (identifier, description) VALUES ('11040301', '11040301');
INSERT INTO soknenummer (identifier, description) VALUES ('11040401', '11040401');
INSERT INTO soknenummer (identifier, description) VALUES ('11040501', '11040501');
INSERT INTO soknenummer (identifier, description) VALUES ('11040601', '11040601');
INSERT INTO soknenummer (identifier, description) VALUES ('11050101', '11050101');
INSERT INTO soknenummer (identifier, description) VALUES ('11050102', '11050102');
INSERT INTO soknenummer (identifier, description) VALUES ('11050301', '11050301');
INSERT INTO soknenummer (identifier, description) VALUES ('11050401', '11050401');
INSERT INTO soknenummer (identifier, description) VALUES ('11060101', '11060101');
INSERT INTO soknenummer (identifier, description) VALUES ('11060401', '11060401');
INSERT INTO soknenummer (identifier, description) VALUES ('11060501', '11060501');
INSERT INTO soknenummer (identifier, description) VALUES ('11060502', '11060502');
INSERT INTO soknenummer (identifier, description) VALUES ('11060601', '11060601');
INSERT INTO soknenummer (identifier, description) VALUES ('11060701', '11060701');
INSERT INTO soknenummer (identifier, description) VALUES ('11070101', '11070101');
INSERT INTO soknenummer (identifier, description) VALUES ('11070201', '11070201');
INSERT INTO soknenummer (identifier, description) VALUES ('11070402', '11070402');
INSERT INTO soknenummer (identifier, description) VALUES ('11070501', '11070501');
INSERT INTO soknenummer (identifier, description) VALUES ('11080101', '11080101');
INSERT INTO soknenummer (identifier, description) VALUES ('11080201', '11080201');
INSERT INTO soknenummer (identifier, description) VALUES ('11080301', '11080301');
INSERT INTO soknenummer (identifier, description) VALUES ('11080401', '11080401');
INSERT INTO soknenummer (identifier, description) VALUES ('11080501', '11080501');
INSERT INTO soknenummer (identifier, description) VALUES ('11030701', '11030701');
INSERT INTO soknenummer (identifier, description) VALUES ('11030702', '11030702');
INSERT INTO soknenummer (identifier, description) VALUES ('11030801', '11030801');
INSERT INTO soknenummer (identifier, description) VALUES ('11030902', '11030902');
INSERT INTO soknenummer (identifier, description) VALUES ('11031001', '11031001');
INSERT INTO soknenummer (identifier, description) VALUES ('05090204', '05090204');
INSERT INTO soknenummer (identifier, description) VALUES ('03040504', '03040504');
INSERT INTO soknenummer (identifier, description) VALUES ('08010206', '08010206');
INSERT INTO soknenummer (identifier, description) VALUES ('06100403', '06100403');
INSERT INTO soknenummer (identifier, description) VALUES ('02030304', '02030304');
INSERT INTO soknenummer (identifier, description) VALUES ('02020206', '02020206');
INSERT INTO soknenummer (identifier, description) VALUES ('07070204', '07070204');
INSERT INTO soknenummer (identifier, description) VALUES ('02090303', '02090303');
INSERT INTO soknenummer (identifier, description) VALUES ('11070404', '11070404');
INSERT INTO soknenummer (identifier, description) VALUES ('11060203', '11060203');
INSERT INTO soknenummer (identifier, description) VALUES ('11031103', '11031103');
INSERT INTO soknenummer (identifier, description) VALUES ('07120505', '07120505');
INSERT INTO soknenummer (identifier, description) VALUES ('09110503', '09110503');
INSERT INTO soknenummer (identifier, description) VALUES ('09110604', '09110604');
INSERT INTO soknenummer (identifier, description) VALUES ('05150402', '05150402');
INSERT INTO soknenummer (identifier, description) VALUES ('05150204', '05150204');
INSERT INTO soknenummer (identifier, description) VALUES ('05150203', '05150203');
INSERT INTO soknenummer (identifier, description) VALUES ('05100703', '05100703');
INSERT INTO soknenummer (identifier, description) VALUES ('05100502', '05100502');
INSERT INTO soknenummer (identifier, description) VALUES ('05140504', '05140504');
INSERT INTO soknenummer (identifier, description) VALUES ('05141404', '05141404');
INSERT INTO soknenummer (identifier, description) VALUES ('04060103', '04060103');
INSERT INTO soknenummer (identifier, description) VALUES ('01020503', '01020503');
INSERT INTO soknenummer (identifier, description) VALUES ('01020703', '01020703');
INSERT INTO soknenummer (identifier, description) VALUES ('09150801', '09150801');
INSERT INTO soknenummer (identifier, description) VALUES ('05141305', '05141305');
INSERT INTO soknenummer (identifier, description) VALUES ('05140306', '05140306');
INSERT INTO soknenummer (identifier, description) VALUES ('03070301', '03070301');
INSERT INTO soknenummer (identifier, description) VALUES ('03070302', '03070302');
INSERT INTO soknenummer (identifier, description) VALUES ('10020303', '10020303');
INSERT INTO soknenummer (identifier, description) VALUES ('09050304', '09050304');
INSERT INTO soknenummer (identifier, description) VALUES ('04030703', '04030703');
INSERT INTO soknenummer (identifier, description) VALUES ('01010802', '01010802');
INSERT INTO soknenummer (identifier, description) VALUES ('01030902', '01030902');
INSERT INTO soknenummer (identifier, description) VALUES ('01030702', '01030702');
INSERT INTO soknenummer (identifier, description) VALUES ('01030403', '01030403');
INSERT INTO soknenummer (identifier, description) VALUES ('01030102', '01030102');
INSERT INTO soknenummer (identifier, description) VALUES ('01040102', '01040102');
INSERT INTO soknenummer (identifier, description) VALUES ('01040502', '01040502');
INSERT INTO soknenummer (identifier, description) VALUES ('01040803', '01040803');
INSERT INTO soknenummer (identifier, description) VALUES ('01050103', '01050103');
INSERT INTO soknenummer (identifier, description) VALUES ('09050403', '09050403');
INSERT INTO soknenummer (identifier, description) VALUES ('10010803', '10010803');
INSERT INTO soknenummer (identifier, description) VALUES ('03060603', '03060603');
INSERT INTO soknenummer (identifier, description) VALUES ('07090304', '07090304');
INSERT INTO soknenummer (identifier, description) VALUES ('10010203', '10010203');
INSERT INTO soknenummer (identifier, description) VALUES ('07130102', '07130102');
INSERT INTO soknenummer (identifier, description) VALUES ('07130103', '07130103');
INSERT INTO soknenummer (identifier, description) VALUES ('07130104', '07130104');
INSERT INTO soknenummer (identifier, description) VALUES ('04030404', '04030404');
INSERT INTO soknenummer (identifier, description) VALUES ('05070404', '05070404');
INSERT INTO soknenummer (identifier, description) VALUES ('05100405', '05100405');
INSERT INTO soknenummer (identifier, description) VALUES ('08040102', '08040102');
INSERT INTO soknenummer (identifier, description) VALUES ('06010102', '06010102');
INSERT INTO soknenummer (identifier, description) VALUES ('02030704', '02030704');
INSERT INTO soknenummer (identifier, description) VALUES ('02030503', '02030503');
INSERT INTO soknenummer (identifier, description) VALUES ('11020104', '11020104');
INSERT INTO soknenummer (identifier, description) VALUES ('02100403', '02100403');
INSERT INTO soknenummer (identifier, description) VALUES ('05150101', '05150101');
INSERT INTO soknenummer (identifier, description) VALUES ('05080404', '05080404');
INSERT INTO soknenummer (identifier, description) VALUES ('09010202', '09010202');
INSERT INTO soknenummer (identifier, description) VALUES ('09020202', '09020202');
INSERT INTO soknenummer (identifier, description) VALUES ('09020603', '09020603');
INSERT INTO soknenummer (identifier, description) VALUES ('09110807', '09110807');
INSERT INTO soknenummer (identifier, description) VALUES ('01070402', '01070402');
INSERT INTO soknenummer (identifier, description) VALUES ('01070401', '01070401');
INSERT INTO soknenummer (identifier, description) VALUES ('07120704', '07120704');
INSERT INTO soknenummer (identifier, description) VALUES ('07120706', '07120706');
INSERT INTO soknenummer (identifier, description) VALUES ('07130403', '07130403');
INSERT INTO soknenummer (identifier, description) VALUES ('07120705', '07120705');
INSERT INTO soknenummer (identifier, description) VALUES ('07070104', '07070104');
INSERT INTO soknenummer (identifier, description) VALUES ('07070306', '07070306');
INSERT INTO soknenummer (identifier, description) VALUES ('07070504', '07070504');
INSERT INTO soknenummer (identifier, description) VALUES ('01010105', '01010105');
INSERT INTO soknenummer (identifier, description) VALUES ('01010104', '01010104');
INSERT INTO soknenummer (identifier, description) VALUES ('04080304', '04080304');
INSERT INTO soknenummer (identifier, description) VALUES ('04020904', '04020904');
INSERT INTO soknenummer (identifier, description) VALUES ('05141604', '05141604');
INSERT INTO soknenummer (identifier, description) VALUES ('05050305', '05050305');
INSERT INTO soknenummer (identifier, description) VALUES ('06051204', '06051204');
INSERT INTO soknenummer (identifier, description) VALUES ('09030503', '09030503');
INSERT INTO soknenummer (identifier, description) VALUES ('09150105', '09150105');
INSERT INTO soknenummer (identifier, description) VALUES ('10080303', '10080303');
INSERT INTO soknenummer (identifier, description) VALUES ('09060705', '09060705');
INSERT INTO soknenummer (identifier, description) VALUES ('09060706', '09060706');

CREATE TABLE postnummeromraade (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'Postnummerområde',
                                   lokalid text NOT NULL,
                                   navnerom text NOT NULL,
                                   versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   opphav text,
                                   produkt text,
                                   versjon text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   postnummer text NOT NULL,
                                   poststed text,
                                   postomraade_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraade_postsonegrense (
                                                  postnummeromraade_fk int,
                                                  postsonegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraade_postnummeromraadegrense (
                                                           postnummeromraade_fk int,
                                                           postnummeromraadegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraade_postomraadegrense (
                                                     postnummeromraade_fk int,
                                                     postomraadegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraade_postregiongrense (
                                                    postnummeromraade_fk int,
                                                    postregiongrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraadegrense (
                                         objid serial NOT NULL,
                                         objtype text DEFAULT 'Postnummerområdegrense',
                                         identifikasjon_lokalid text NOT NULL,
                                         identifikasjon_navnerom text NOT NULL,
                                         identifikasjon_versjonid text,
                                         datafangstdato timestamp with time zone,
                                         oppdateringsdato timestamp with time zone,
                                         sluttdato timestamp with time zone,
                                         gyldigfra timestamp with time zone,
                                         gyldigtil timestamp with time zone,
                                         datauttaksdato timestamp with time zone,
                                         typeendring text,
                                         tidspunktendring timestamp with time zone,
                                         maalemetode text,
                                         noeyaktighet int,
                                         opphav text,
                                         produkt text,
                                         versjon text,
                                         omraadeid int,
                                         originaldatavert text,
                                         kopidato timestamp with time zone,
                                         grense geometry(Linestring, 25833) NOT NULL,
                                         foelgergrunnkretsgrense_lokalid text,
                                         foelgergrunnkretsgrense_navnerom text,
                                         foelgergrunnkretsgrense_versjonid text,
                                         foelgeradministrativgrense text,
                                         noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE postomraade (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Postområde',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             opphav text,
                             produkt text,
                             versjon text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             postomraadenummer text NOT NULL,
                             postregion_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postomraade_postsonegrense (
                                            postomraade_fk int,
                                            postsonegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postomraade_postomraadegrense (
                                               postomraade_fk int,
                                               postomraadegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postomraade_postregiongrense (
                                              postomraade_fk int,
                                              postregiongrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postomraadegrense (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'Postområdegrense',
                                   identifikasjon_lokalid text NOT NULL,
                                   identifikasjon_navnerom text NOT NULL,
                                   identifikasjon_versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   maalemetode text,
                                   noeyaktighet int,
                                   opphav text,
                                   produkt text,
                                   versjon text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   grense geometry(Linestring, 25833) NOT NULL,
                                   foelgergrunnkretsgrense_lokalid text,
                                   foelgergrunnkretsgrense_navnerom text,
                                   foelgergrunnkretsgrense_versjonid text,
                                   foelgeradministrativgrense text,
                                   noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE postregion (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Postregion',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            opphav text,
                            produkt text,
                            versjon text,
                            omraadeid int,
                            originaldatavert text,
                            kopidato timestamp with time zone,
                            postregionnummer text NOT NULL,
                            postsone_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postregion_postsonegrense (
                                           postregion_fk int,
                                           postsonegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postregion_postregiongrense (
                                             postregion_fk int,
                                             postregiongrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postregiongrense (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'Postregiongrense',
                                  identifikasjon_lokalid text NOT NULL,
                                  identifikasjon_navnerom text NOT NULL,
                                  identifikasjon_versjonid text,
                                  datafangstdato timestamp with time zone,
                                  oppdateringsdato timestamp with time zone,
                                  sluttdato timestamp with time zone,
                                  gyldigfra timestamp with time zone,
                                  gyldigtil timestamp with time zone,
                                  datauttaksdato timestamp with time zone,
                                  typeendring text,
                                  tidspunktendring timestamp with time zone,
                                  maalemetode text,
                                  noeyaktighet int,
                                  opphav text,
                                  produkt text,
                                  versjon text,
                                  omraadeid int,
                                  originaldatavert text,
                                  kopidato timestamp with time zone,
                                  grense geometry(Linestring, 25833) NOT NULL,
                                  foelgergrunnkretsgrense_lokalid text,
                                  foelgergrunnkretsgrense_navnerom text,
                                  foelgergrunnkretsgrense_versjonid text,
                                  foelgeradministrativgrense text,
                                  noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE postsone (
                          objid serial NOT NULL,
                          objtype text DEFAULT 'Postsone',
                          lokalid text NOT NULL,
                          navnerom text NOT NULL,
                          versjonid text,
                          datafangstdato timestamp with time zone,
                          oppdateringsdato timestamp with time zone,
                          sluttdato timestamp with time zone,
                          gyldigfra timestamp with time zone,
                          gyldigtil timestamp with time zone,
                          datauttaksdato timestamp with time zone,
                          typeendring text,
                          tidspunktendring timestamp with time zone,
                          opphav text,
                          produkt text,
                          versjon text,
                          omraadeid int,
                          originaldatavert text,
                          kopidato timestamp with time zone,
                          postsonenummer text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE postsone_postsonegrense (
                                         postsone_fk int,
                                         postsonegrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE postsonegrense (
                                objid serial NOT NULL,
                                objtype text DEFAULT 'Postsonegrense',
                                identifikasjon_lokalid text NOT NULL,
                                identifikasjon_navnerom text NOT NULL,
                                identifikasjon_versjonid text,
                                datafangstdato timestamp with time zone,
                                oppdateringsdato timestamp with time zone,
                                sluttdato timestamp with time zone,
                                gyldigfra timestamp with time zone,
                                gyldigtil timestamp with time zone,
                                datauttaksdato timestamp with time zone,
                                typeendring text,
                                tidspunktendring timestamp with time zone,
                                maalemetode text,
                                noeyaktighet int,
                                opphav text,
                                produkt text,
                                versjon text,
                                omraadeid int,
                                originaldatavert text,
                                kopidato timestamp with time zone,
                                grense geometry(Linestring, 25833) NOT NULL,
                                foelgergrunnkretsgrense_lokalid text,
                                foelgergrunnkretsgrense_navnerom text,
                                foelgergrunnkretsgrense_versjonid text,
                                foelgeradministrativgrense text,
                                noeyaktighetsklasse text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraade (
                                 objid serial NOT NULL,
                                 objtype text DEFAULT 'LovVirkeområde',
                                 lokalid text NOT NULL,
                                 navnerom text NOT NULL,
                                 versjonid text,
                                 datafangstdato timestamp with time zone,
                                 oppdateringsdato timestamp with time zone,
                                 sluttdato timestamp with time zone,
                                 gyldigfra timestamp with time zone,
                                 gyldigtil timestamp with time zone,
                                 datauttaksdato timestamp with time zone,
                                 typeendring text,
                                 tidspunktendring timestamp with time zone,
                                 maalemetode text NOT NULL,
                                 noeyaktighet int,
                                 opphav text,
                                 produkt text,
                                 versjon text,
                                 omraadeid int,
                                 originaldatavert text,
                                 kopidato timestamp with time zone,
                                 gjelderomraade text,
                                 virkeomraadenavn text,
                                 spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraadegrense (
                                       objid serial NOT NULL,
                                       objtype text DEFAULT 'LovVirkeområdeGrense',
                                       lokalid text NOT NULL,
                                       navnerom text NOT NULL,
                                       versjonid text,
                                       datafangstdato timestamp with time zone,
                                       oppdateringsdato timestamp with time zone,
                                       sluttdato timestamp with time zone,
                                       gyldigfra timestamp with time zone,
                                       gyldigtil timestamp with time zone,
                                       datauttaksdato timestamp with time zone,
                                       typeendring text,
                                       tidspunktendring timestamp with time zone,
                                       maalemetode text NOT NULL,
                                       noeyaktighet int,
                                       opphav text,
                                       produkt text,
                                       versjon text,
                                       omraadeid int,
                                       originaldatavert text,
                                       kopidato timestamp with time zone,
                                       gjelderomraade text,
                                       virkeomraadenavn text,
                                       grense geometry(Linestring, 25833) NOT NULL,
                                       grensestatus text,
                                       fastsettingstype text,
                                       sonenautisk int,
                                       foelgeradministrativgrense text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraadegrense_lovvirkeomraadegrensepunkt (
                                                                  lovvirkeomraadegrense_fk int,
                                                                  lovvirkeomraadegrensepunkt_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraadegrensepunkt (
                                            objid serial NOT NULL,
                                            objtype text DEFAULT 'LovVirkeområdeGrensePunkt',
                                            lokalid text NOT NULL,
                                            navnerom text NOT NULL,
                                            versjonid text,
                                            datafangstdato timestamp with time zone,
                                            oppdateringsdato timestamp with time zone,
                                            sluttdato timestamp with time zone,
                                            gyldigfra timestamp with time zone,
                                            gyldigtil timestamp with time zone,
                                            datauttaksdato timestamp with time zone,
                                            typeendring text,
                                            tidspunktendring timestamp with time zone,
                                            maalemetode text NOT NULL,
                                            noeyaktighet int,
                                            opphav text,
                                            produkt text,
                                            versjon text,
                                            omraadeid int,
                                            originaldatavert text,
                                            kopidato timestamp with time zone,
                                            posisjon geometry(Point, 25833) NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE dokumentasjonsreferanse (
                                         objid serial NOT NULL,
                                         objtype text DEFAULT 'Dokumentasjonsreferanse',
                                         rettskildetittel text NOT NULL,
                                         rettskildeid text,
                                         hjemmel text,
                                         fastsettingsmyndighet text,
                                         fastsettingsdato date NOT NULL,
                                         fylkesgrense_fk int,
                                         kommunegrense_fk int,
                                         riksgrense_fk int,
                                         lovvirkeomraade_fk int,
                                         lovvirkeomraadegrense_fk int,
                                         avtaltavgrensningslinje_fk int,
                                         detsaerskilteomraadet_fk int,
                                         fiskerisone_fk int,
                                         fiskevernsone_fk int,
                                         grense200nautiskemil_fk int,
                                         grunnlinje_fk int,
                                         graasonegrense_fk int,
                                         graasonen_fk int,
                                         indrefarvann_fk int,
                                         kontinentalsokkel_fk int,
                                         norgesoekonomiskesone_fk int,
                                         sjoeterritorium_fk int,
                                         territorialfarvann_fk int,
                                         territorialgrense_fk int,
                                         tilstoetendesone_fk int,
                                         yttergrensesokkel_fk int,
                                         yttergrensetilstoetendesone_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE punktreferanse (
                                objid serial NOT NULL,
                                objtype text DEFAULT 'Punktreferanse',
                                punktnummer text,
                                punktnavn text,
                                grensemerkeland_fk int,
                                lovvirkeomraadegrensepunkt_fk int,
                                grenseboeye_fk int,
                                grensepunktsjoe_fk int,
                                sjoeroeys_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE administrativgrensekode (
                                         identifier text NOT NULL,
                                         description text)
    WITH (OIDS=FALSE);

INSERT INTO administrativgrensekode (identifier, description) VALUES ('Kommunegrense', 'Kommunegrense');
INSERT INTO administrativgrensekode (identifier, description) VALUES ('Fylkesgrense', 'Fylkesgrense');
INSERT INTO administrativgrensekode (identifier, description) VALUES ('Riksgrense', 'Riksgrense');
INSERT INTO administrativgrensekode (identifier, description) VALUES ('Territorialgrense', 'Territorialgrense');

CREATE TABLE fastsettingstype (
                                  identifier text NOT NULL,
                                  description text)
    WITH (OIDS=FALSE);

INSERT INTO fastsettingstype (identifier, description) VALUES ('Beregnet', 'Beregnet');
INSERT INTO fastsettingstype (identifier, description) VALUES ('BestemteKoordinater', 'BestemteKoordinater');
INSERT INTO fastsettingstype (identifier, description) VALUES ('Grensebeskrivelse', 'Grensebeskrivelse');
INSERT INTO fastsettingstype (identifier, description) VALUES ('Oppmålt', 'Oppmålt');

CREATE TABLE grensestatus (
                              identifier text NOT NULL,
                              description text)
    WITH (OIDS=FALSE);

INSERT INTO grensestatus (identifier, description) VALUES ('Traktat', 'Traktat');
INSERT INTO grensestatus (identifier, description) VALUES ('Lovfestet', 'Lovfestet');
INSERT INTO grensestatus (identifier, description) VALUES ('KongeligResolusjon', 'KongeligResolusjon');
INSERT INTO grensestatus (identifier, description) VALUES ('RettskraftigDom', 'RettskraftigDom');
INSERT INTO grensestatus (identifier, description) VALUES ('Godkjent', 'Godkjent');
INSERT INTO grensestatus (identifier, description) VALUES ('Foreslått', 'Foreslått');
INSERT INTO grensestatus (identifier, description) VALUES ('Historisk', 'Historisk');

CREATE TABLE maritimtomraadesonetype (
                                         identifier text NOT NULL,
                                         description text)
    WITH (OIDS=FALSE);

INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('SuvereneRettigheterHavbunnOgVannmasse', 'SuvereneRettigheterHavbunnOgVannmasse');
INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('SuvereneRettigheterHavbunn', 'SuvereneRettigheterHavbunn');
INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('SuvereneRettigheterVannmasse', 'SuvereneRettigheterVannmasse');
INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('ReguleringAvRestriksjonFiskeOgFangst', 'ReguleringAvRestriksjonFiskeOgFangst');
INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('UtvidetKontroll', 'UtvidetKontroll');
INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('SuverenitetHavområde', 'SuverenitetHavområde');
INSERT INTO maritimtomraadesonetype (identifier, description) VALUES ('SuverenitetLandområdeOgHavområde', 'SuverenitetLandområdeOgHavområde');

CREATE TABLE miljoeomraade (
                               identifier text NOT NULL,
                               description text)
    WITH (OIDS=FALSE);

INSERT INTO miljoeomraade (identifier, description) VALUES ('Jord', 'Jord');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Lyd', 'Lyd');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Naturressurser', 'Naturressurser');
INSERT INTO miljoeomraade (identifier, description) VALUES ('KlimaOgKlimaendringer', 'KlimaOgKlimaendringer');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Helsebeskyttelse', 'Helsebeskyttelse');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Luft', 'Luft');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Vann', 'Vann');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Avfall', 'Avfall');
INSERT INTO miljoeomraade (identifier, description) VALUES ('NaturOgBiologiskMangfold', 'NaturOgBiologiskMangfold');
INSERT INTO miljoeomraade (identifier, description) VALUES ('BærekraftigUtvikling', 'BærekraftigUtvikling');
INSERT INTO miljoeomraade (identifier, description) VALUES ('Arealbruk', 'Arealbruk');

CREATE TABLE sonetype (
                          identifier text NOT NULL,
                          description text)
    WITH (OIDS=FALSE);

INSERT INTO sonetype (identifier, description) VALUES ('LuftkvalitetAdministrativSone', 'LuftkvalitetAdministrativSone');
INSERT INTO sonetype (identifier, description) VALUES ('Lydrestriksjonssone', 'Lydrestriksjonssone');
INSERT INTO sonetype (identifier, description) VALUES ('RestriksjonssoneForDyresykdommer', 'RestriksjonssoneForDyresykdommer');
INSERT INTO sonetype (identifier, description) VALUES ('TillatelsesområdeForProspekteringOgGruvedrift', 'TillatelsesområdeForProspekteringOgGruvedrift');
INSERT INTO sonetype (identifier, description) VALUES ('RegulertFarled', 'RegulertFarled');
INSERT INTO sonetype (identifier, description) VALUES ('RestriksjonssoneRundtForurensetGrunn', 'RestriksjonssoneRundtForurensetGrunn');
INSERT INTO sonetype (identifier, description) VALUES ('OmrådeForDeponeringAvAvfall', 'OmrådeForDeponeringAvAvfall');
INSERT INTO sonetype (identifier, description) VALUES ('Kystsoneforvaltning', 'Kystsoneforvaltning');
INSERT INTO sonetype (identifier, description) VALUES ('VerneområdeForDrikkevann', 'VerneområdeForDrikkevann');
INSERT INTO sonetype (identifier, description) VALUES ('SårbartOmråde', 'SårbartOmråde');
INSERT INTO sonetype (identifier, description) VALUES ('MaritimtOmråde', 'MaritimtOmråde');
INSERT INTO sonetype (identifier, description) VALUES ('Nedbørsfelt', 'Nedbørsfelt');
INSERT INTO sonetype (identifier, description) VALUES ('Badevann', 'Badevann');
INSERT INTO sonetype (identifier, description) VALUES ('FlomAdministrasjonsenhet', 'FlomAdministrasjonsenhet');
INSERT INTO sonetype (identifier, description) VALUES ('VannmasseUnderVanndirektivet', 'VannmasseUnderVanndirektivet');
INSERT INTO sonetype (identifier, description) VALUES ('SensitivtVannområde', 'SensitivtVannområde');
INSERT INTO sonetype (identifier, description) VALUES ('AngittFarvann', 'AngittFarvann');
INSERT INTO sonetype (identifier, description) VALUES ('VernesoneForPlanteliv', 'VernesoneForPlanteliv');
INSERT INTO sonetype (identifier, description) VALUES ('Skogforvaltningsområde', 'Skogforvaltningsområde');

CREATE TABLE avtaltavgrensningslinje (
                                         objid serial NOT NULL,
                                         objtype text DEFAULT 'AvtaltAvgrensningslinje',
                                         lokalid text NOT NULL,
                                         navnerom text NOT NULL,
                                         versjonid text,
                                         datafangstdato timestamp with time zone,
                                         oppdateringsdato timestamp with time zone,
                                         sluttdato timestamp with time zone,
                                         gyldigfra timestamp with time zone,
                                         gyldigtil timestamp with time zone,
                                         datauttaksdato timestamp with time zone,
                                         typeendring text,
                                         tidspunktendring timestamp with time zone,
                                         opphav text,
                                         omraadeid int,
                                         originaldatavert text,
                                         kopidato timestamp with time zone,
                                         produkt text,
                                         versjon text,
                                         maalemetode text NOT NULL,
                                         noeyaktighet int,
                                         grense geometry(Linestring, 25833) NOT NULL,
                                         gjelderomraade text,
                                         grensestatus text,
                                         virkeomraadenavn text,
                                         transformasjonsdato date,
                                         fastsettingstype text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE avtaltavgrensningslinje_grenseboeye (
                                                     avtaltavgrensningslinje_fk int,
                                                     grenseboeye_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeroeys_avtaltavgrensningslinje (
                                                   sjoeroeys_fk int,
                                                   avtaltavgrensningslinje_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE avtaltavgrensningslinje_grensepunktsjoe (
                                                         avtaltavgrensningslinje_fk int,
                                                         grensepunktsjoe_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE beregnetmidtlinje (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'BeregnetMidtlinje',
                                   lokalid text NOT NULL,
                                   navnerom text NOT NULL,
                                   versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   opphav text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   produkt text,
                                   versjon text,
                                   maalemetode text NOT NULL,
                                   noeyaktighet int,
                                   grense geometry(Linestring, 25833) NOT NULL,
                                   gjelderomraade text,
                                   grensestatus text,
                                   virkeomraadenavn text,
                                   transformasjonsdato date)
    WITH (OIDS=FALSE);

CREATE TABLE grensepunktsjoe_beregnetmidtlinje (
                                                   grensepunktsjoe_fk int,
                                                   beregnetmidtlinje_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE detsaerskilteomraadet (
                                       objid serial NOT NULL,
                                       objtype text DEFAULT 'DetSærskilteOmrådet',
                                       lokalid text NOT NULL,
                                       navnerom text NOT NULL,
                                       versjonid text,
                                       datafangstdato timestamp with time zone,
                                       oppdateringsdato timestamp with time zone,
                                       sluttdato timestamp with time zone,
                                       gyldigfra timestamp with time zone,
                                       gyldigtil timestamp with time zone,
                                       datauttaksdato timestamp with time zone,
                                       typeendring text,
                                       tidspunktendring timestamp with time zone,
                                       opphav text,
                                       omraadeid int,
                                       originaldatavert text,
                                       kopidato timestamp with time zone,
                                       produkt text,
                                       versjon text,
                                       gjelderomraade text,
                                       virkeomraadenavn text,
                                       spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskerisone (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Fiskerisone',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             opphav text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             produkt text,
                             versjon text,
                             gjelderomraade text,
                             virkeomraadenavn text,
                             spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskevernsone (
                               objid serial NOT NULL,
                               objtype text DEFAULT 'Fiskevernsone',
                               lokalid text NOT NULL,
                               navnerom text NOT NULL,
                               versjonid text,
                               datafangstdato timestamp with time zone,
                               oppdateringsdato timestamp with time zone,
                               sluttdato timestamp with time zone,
                               gyldigfra timestamp with time zone,
                               gyldigtil timestamp with time zone,
                               datauttaksdato timestamp with time zone,
                               typeendring text,
                               tidspunktendring timestamp with time zone,
                               opphav text,
                               omraadeid int,
                               originaldatavert text,
                               kopidato timestamp with time zone,
                               produkt text,
                               versjon text,
                               gjelderomraade text,
                               virkeomraadenavn text,
                               spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE grense200nautiskemil (
                                      objid serial NOT NULL,
                                      objtype text DEFAULT 'Grense200NautiskeMil',
                                      lokalid text NOT NULL,
                                      navnerom text NOT NULL,
                                      versjonid text,
                                      datafangstdato timestamp with time zone,
                                      oppdateringsdato timestamp with time zone,
                                      sluttdato timestamp with time zone,
                                      gyldigfra timestamp with time zone,
                                      gyldigtil timestamp with time zone,
                                      datauttaksdato timestamp with time zone,
                                      typeendring text,
                                      tidspunktendring timestamp with time zone,
                                      opphav text,
                                      omraadeid int,
                                      originaldatavert text,
                                      kopidato timestamp with time zone,
                                      produkt text,
                                      versjon text,
                                      maalemetode text NOT NULL,
                                      noeyaktighet int,
                                      grense geometry(Linestring, 25833) NOT NULL,
                                      gjelderomraade text,
                                      grensestatus text,
                                      virkeomraadenavn text,
                                      transformasjonsdato date,
                                      land text,
                                      fastsettingstype text NOT NULL,
                                      beregningsdato date)
    WITH (OIDS=FALSE);

CREATE TABLE grenseboeye (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Grensebøye',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             opphav text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             produkt text,
                             versjon text,
                             maalemetode text NOT NULL,
                             noeyaktighet int,
                             posisjon geometry(Point, 25833) NOT NULL,
                             beskrivelse text,
                             etableringsdato date,
                             egenhoeyde double precision,
                             arbeidutfoertdato date,
                             arbeidutfoertbeskrivelse text,
                             grunnlinje_fk int,
                             riksgrense_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grensepunktsjoe (
                                 objid serial NOT NULL,
                                 objtype text DEFAULT 'GrensepunktSjø',
                                 lokalid text NOT NULL,
                                 navnerom text NOT NULL,
                                 versjonid text,
                                 datafangstdato timestamp with time zone,
                                 oppdateringsdato timestamp with time zone,
                                 sluttdato timestamp with time zone,
                                 gyldigfra timestamp with time zone,
                                 gyldigtil timestamp with time zone,
                                 datauttaksdato timestamp with time zone,
                                 typeendring text,
                                 tidspunktendring timestamp with time zone,
                                 opphav text,
                                 omraadeid int,
                                 originaldatavert text,
                                 kopidato timestamp with time zone,
                                 produkt text,
                                 versjon text,
                                 maalemetode text NOT NULL,
                                 noeyaktighet int,
                                 posisjon geometry(Point, 25833) NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlinje_grensepunktsjoe (
                                             sektorlinje_fk int,
                                             grensepunktsjoe_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnlinje_grensepunktsjoe (
                                            grunnlinje_fk int,
                                            grensepunktsjoe_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE graasonegrense_grensepunktsjoe (
                                                graasonegrense_fk int,
                                                grensepunktsjoe_fk int)
    WITH (OIDS=FALSE);

CREATE TABLE grunnlinje (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Grunnlinje',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            opphav text,
                            omraadeid int,
                            originaldatavert text,
                            kopidato timestamp with time zone,
                            produkt text,
                            versjon text,
                            maalemetode text NOT NULL,
                            noeyaktighet int,
                            grense geometry(Linestring, 25833) NOT NULL,
                            gjelderomraade text,
                            grensestatus text,
                            virkeomraadenavn text,
                            transformasjonsdato date,
                            land text,
                            fastsettingstype text NOT NULL,
                            kommunenummer text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonegrense (
                                objid serial NOT NULL,
                                objtype text DEFAULT 'Gråsonegrense',
                                lokalid text NOT NULL,
                                navnerom text NOT NULL,
                                versjonid text,
                                datafangstdato timestamp with time zone,
                                oppdateringsdato timestamp with time zone,
                                sluttdato timestamp with time zone,
                                gyldigfra timestamp with time zone,
                                gyldigtil timestamp with time zone,
                                datauttaksdato timestamp with time zone,
                                typeendring text,
                                tidspunktendring timestamp with time zone,
                                opphav text,
                                omraadeid int,
                                originaldatavert text,
                                kopidato timestamp with time zone,
                                produkt text,
                                versjon text,
                                maalemetode text NOT NULL,
                                noeyaktighet int,
                                grense geometry(Linestring, 25833) NOT NULL,
                                gjelderomraade text,
                                grensestatus text,
                                virkeomraadenavn text,
                                transformasjonsdato date,
                                fastsettingstype text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE graasonen (
                           objid serial NOT NULL,
                           objtype text DEFAULT 'Gråsonen',
                           lokalid text NOT NULL,
                           navnerom text NOT NULL,
                           versjonid text,
                           datafangstdato timestamp with time zone,
                           oppdateringsdato timestamp with time zone,
                           sluttdato timestamp with time zone,
                           gyldigfra timestamp with time zone,
                           gyldigtil timestamp with time zone,
                           datauttaksdato timestamp with time zone,
                           typeendring text,
                           tidspunktendring timestamp with time zone,
                           opphav text,
                           omraadeid int,
                           originaldatavert text,
                           kopidato timestamp with time zone,
                           produkt text,
                           versjon text,
                           gjelderomraade text,
                           virkeomraadenavn text,
                           spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE indrefarvann (
                              objid serial NOT NULL,
                              objtype text DEFAULT 'IndreFarvann',
                              lokalid text NOT NULL,
                              navnerom text NOT NULL,
                              versjonid text,
                              datafangstdato timestamp with time zone,
                              oppdateringsdato timestamp with time zone,
                              sluttdato timestamp with time zone,
                              gyldigfra timestamp with time zone,
                              gyldigtil timestamp with time zone,
                              datauttaksdato timestamp with time zone,
                              typeendring text,
                              tidspunktendring timestamp with time zone,
                              opphav text,
                              omraadeid int,
                              originaldatavert text,
                              kopidato timestamp with time zone,
                              produkt text,
                              versjon text,
                              gjelderomraade text,
                              virkeomraadenavn text,
                              spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE internasjonaltfarvann (
                                       objid serial NOT NULL,
                                       objtype text DEFAULT 'InternasjonaltFarvann',
                                       lokalid text NOT NULL,
                                       navnerom text NOT NULL,
                                       versjonid text,
                                       datafangstdato timestamp with time zone,
                                       oppdateringsdato timestamp with time zone,
                                       sluttdato timestamp with time zone,
                                       gyldigfra timestamp with time zone,
                                       gyldigtil timestamp with time zone,
                                       datauttaksdato timestamp with time zone,
                                       typeendring text,
                                       tidspunktendring timestamp with time zone,
                                       opphav text,
                                       omraadeid int,
                                       originaldatavert text,
                                       kopidato timestamp with time zone,
                                       produkt text,
                                       versjon text,
                                       posisjon geometry(Point, 25833),
                                       navn text)
    WITH (OIDS=FALSE);

CREATE TABLE kontinentalsokkel (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'Kontinentalsokkel',
                                   lokalid text NOT NULL,
                                   navnerom text NOT NULL,
                                   versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   opphav text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   produkt text,
                                   versjon text,
                                   gjelderomraade text,
                                   virkeomraadenavn text,
                                   spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE kystkontur (
                            objid serial NOT NULL,
                            objtype text DEFAULT 'Kystkontur',
                            lokalid text NOT NULL,
                            navnerom text NOT NULL,
                            versjonid text,
                            datafangstdato timestamp with time zone,
                            oppdateringsdato timestamp with time zone,
                            sluttdato timestamp with time zone,
                            gyldigfra timestamp with time zone,
                            gyldigtil timestamp with time zone,
                            datauttaksdato timestamp with time zone,
                            typeendring text,
                            tidspunktendring timestamp with time zone,
                            opphav text,
                            omraadeid int,
                            originaldatavert text,
                            kopidato timestamp with time zone,
                            produkt text,
                            versjon text,
                            maalemetode text NOT NULL,
                            noeyaktighet int,
                            grense geometry(Linestring, 25833) NOT NULL,
                            hoeyde double precision,
                            kystreferanse text,
                            kysttype text)
    WITH (OIDS=FALSE);

CREATE TABLE landindrefarvann (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'LandIndreFarvann',
                                  lokalid text NOT NULL,
                                  navnerom text NOT NULL,
                                  versjonid text,
                                  datafangstdato timestamp with time zone,
                                  oppdateringsdato timestamp with time zone,
                                  sluttdato timestamp with time zone,
                                  gyldigfra timestamp with time zone,
                                  gyldigtil timestamp with time zone,
                                  datauttaksdato timestamp with time zone,
                                  typeendring text,
                                  tidspunktendring timestamp with time zone,
                                  opphav text,
                                  omraadeid int,
                                  originaldatavert text,
                                  kopidato timestamp with time zone,
                                  produkt text,
                                  versjon text,
                                  landkode text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE norgesoekonomiskesone (
                                       objid serial NOT NULL,
                                       objtype text DEFAULT 'NorgesØkonomiskeSone',
                                       lokalid text NOT NULL,
                                       navnerom text NOT NULL,
                                       versjonid text,
                                       datafangstdato timestamp with time zone,
                                       oppdateringsdato timestamp with time zone,
                                       sluttdato timestamp with time zone,
                                       gyldigfra timestamp with time zone,
                                       gyldigtil timestamp with time zone,
                                       datauttaksdato timestamp with time zone,
                                       typeendring text,
                                       tidspunktendring timestamp with time zone,
                                       opphav text,
                                       omraadeid int,
                                       originaldatavert text,
                                       kopidato timestamp with time zone,
                                       produkt text,
                                       versjon text,
                                       gjelderomraade text,
                                       virkeomraadenavn text,
                                       spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlinje (
                             objid serial NOT NULL,
                             objtype text DEFAULT 'Sektorlinje',
                             lokalid text NOT NULL,
                             navnerom text NOT NULL,
                             versjonid text,
                             datafangstdato timestamp with time zone,
                             oppdateringsdato timestamp with time zone,
                             sluttdato timestamp with time zone,
                             gyldigfra timestamp with time zone,
                             gyldigtil timestamp with time zone,
                             datauttaksdato timestamp with time zone,
                             typeendring text,
                             tidspunktendring timestamp with time zone,
                             opphav text,
                             omraadeid int,
                             originaldatavert text,
                             kopidato timestamp with time zone,
                             produkt text,
                             versjon text,
                             maalemetode text NOT NULL,
                             noeyaktighet int,
                             grense geometry(Linestring, 25833) NOT NULL,
                             gjelderomraade text,
                             grensestatus text,
                             virkeomraadenavn text,
                             transformasjonsdato date)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeroeys (
                           objid serial NOT NULL,
                           objtype text DEFAULT 'Sjørøys',
                           lokalid text NOT NULL,
                           navnerom text NOT NULL,
                           versjonid text,
                           datafangstdato timestamp with time zone,
                           oppdateringsdato timestamp with time zone,
                           sluttdato timestamp with time zone,
                           gyldigfra timestamp with time zone,
                           gyldigtil timestamp with time zone,
                           datauttaksdato timestamp with time zone,
                           typeendring text,
                           tidspunktendring timestamp with time zone,
                           opphav text,
                           omraadeid int,
                           originaldatavert text,
                           kopidato timestamp with time zone,
                           produkt text,
                           versjon text,
                           maalemetode text NOT NULL,
                           noeyaktighet int,
                           posisjon geometry(Point, 25833) NOT NULL,
                           beskrivelse text,
                           etableringsdato date,
                           egenhoeyde double precision,
                           arbeidutfoertdato date,
                           arbeidutfoertbeskrivelse text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeterritorium (
                                 objid serial NOT NULL,
                                 objtype text DEFAULT 'Sjøterritorium',
                                 lokalid text NOT NULL,
                                 navnerom text NOT NULL,
                                 versjonid text,
                                 datafangstdato timestamp with time zone,
                                 oppdateringsdato timestamp with time zone,
                                 sluttdato timestamp with time zone,
                                 gyldigfra timestamp with time zone,
                                 gyldigtil timestamp with time zone,
                                 datauttaksdato timestamp with time zone,
                                 typeendring text,
                                 tidspunktendring timestamp with time zone,
                                 opphav text,
                                 omraadeid int,
                                 originaldatavert text,
                                 kopidato timestamp with time zone,
                                 produkt text,
                                 versjon text,
                                 gjelderomraade text,
                                 virkeomraadenavn text,
                                 spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialfarvann (
                                    objid serial NOT NULL,
                                    objtype text DEFAULT 'Territorialfarvann',
                                    lokalid text NOT NULL,
                                    navnerom text NOT NULL,
                                    versjonid text,
                                    datafangstdato timestamp with time zone,
                                    oppdateringsdato timestamp with time zone,
                                    sluttdato timestamp with time zone,
                                    gyldigfra timestamp with time zone,
                                    gyldigtil timestamp with time zone,
                                    datauttaksdato timestamp with time zone,
                                    typeendring text,
                                    tidspunktendring timestamp with time zone,
                                    opphav text,
                                    omraadeid int,
                                    originaldatavert text,
                                    kopidato timestamp with time zone,
                                    produkt text,
                                    versjon text,
                                    gjelderomraade text,
                                    virkeomraadenavn text,
                                    spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialgrense (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'Territorialgrense',
                                   lokalid text NOT NULL,
                                   navnerom text NOT NULL,
                                   versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   opphav text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   produkt text,
                                   versjon text,
                                   maalemetode text NOT NULL,
                                   noeyaktighet int,
                                   grense geometry(Linestring, 25833) NOT NULL,
                                   gjelderomraade text,
                                   grensestatus text,
                                   virkeomraadenavn text,
                                   transformasjonsdato date,
                                   land text,
                                   fastsettingstype text NOT NULL,
                                   beregningsdato date)
    WITH (OIDS=FALSE);

CREATE TABLE territorialomraade (
                                    objid serial NOT NULL,
                                    objtype text DEFAULT 'Territorialområde',
                                    lokalid text NOT NULL,
                                    navnerom text NOT NULL,
                                    versjonid text,
                                    datafangstdato timestamp with time zone,
                                    oppdateringsdato timestamp with time zone,
                                    sluttdato timestamp with time zone,
                                    gyldigfra timestamp with time zone,
                                    gyldigtil timestamp with time zone,
                                    datauttaksdato timestamp with time zone,
                                    typeendring text,
                                    tidspunktendring timestamp with time zone,
                                    opphav text,
                                    omraadeid int,
                                    originaldatavert text,
                                    kopidato timestamp with time zone,
                                    produkt text,
                                    versjon text,
                                    landkode text NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE tidligereomstridtomraade (
                                          objid serial NOT NULL,
                                          objtype text DEFAULT 'TidligereOmstridtOmråde',
                                          lokalid text NOT NULL,
                                          navnerom text NOT NULL,
                                          versjonid text,
                                          datafangstdato timestamp with time zone,
                                          oppdateringsdato timestamp with time zone,
                                          sluttdato timestamp with time zone,
                                          gyldigfra timestamp with time zone,
                                          gyldigtil timestamp with time zone,
                                          datauttaksdato timestamp with time zone,
                                          typeendring text,
                                          tidspunktendring timestamp with time zone,
                                          opphav text,
                                          omraadeid int,
                                          originaldatavert text,
                                          kopidato timestamp with time zone,
                                          produkt text,
                                          versjon text,
                                          posisjon geometry(Point, 25833),
                                          omraade geometry(Polygon, 25833) NOT NULL)
    WITH (OIDS=FALSE);

CREATE TABLE tilstoetendesone (
                                  objid serial NOT NULL,
                                  objtype text DEFAULT 'TilstøtendeSone',
                                  lokalid text NOT NULL,
                                  navnerom text NOT NULL,
                                  versjonid text,
                                  datafangstdato timestamp with time zone,
                                  oppdateringsdato timestamp with time zone,
                                  sluttdato timestamp with time zone,
                                  gyldigfra timestamp with time zone,
                                  gyldigtil timestamp with time zone,
                                  datauttaksdato timestamp with time zone,
                                  typeendring text,
                                  tidspunktendring timestamp with time zone,
                                  opphav text,
                                  omraadeid int,
                                  originaldatavert text,
                                  kopidato timestamp with time zone,
                                  produkt text,
                                  versjon text,
                                  gjelderomraade text,
                                  virkeomraadenavn text,
                                  spesialisertsonetype text)
    WITH (OIDS=FALSE);

CREATE TABLE yttergrensesokkel (
                                   objid serial NOT NULL,
                                   objtype text DEFAULT 'YttergrenseSokkel',
                                   lokalid text NOT NULL,
                                   navnerom text NOT NULL,
                                   versjonid text,
                                   datafangstdato timestamp with time zone,
                                   oppdateringsdato timestamp with time zone,
                                   sluttdato timestamp with time zone,
                                   gyldigfra timestamp with time zone,
                                   gyldigtil timestamp with time zone,
                                   datauttaksdato timestamp with time zone,
                                   typeendring text,
                                   tidspunktendring timestamp with time zone,
                                   opphav text,
                                   omraadeid int,
                                   originaldatavert text,
                                   kopidato timestamp with time zone,
                                   produkt text,
                                   versjon text,
                                   maalemetode text NOT NULL,
                                   noeyaktighet int,
                                   grense geometry(Linestring, 25833) NOT NULL,
                                   gjelderomraade text,
                                   grensestatus text,
                                   virkeomraadenavn text,
                                   transformasjonsdato date,
                                   land text,
                                   fastsettingstype text NOT NULL,
                                   beregningsdato date)
    WITH (OIDS=FALSE);

CREATE TABLE yttergrensetilstoetendesone (
                                             objid serial NOT NULL,
                                             objtype text DEFAULT 'YttergrenseTilstøtendeSone',
                                             lokalid text NOT NULL,
                                             navnerom text NOT NULL,
                                             versjonid text,
                                             datafangstdato timestamp with time zone,
                                             oppdateringsdato timestamp with time zone,
                                             sluttdato timestamp with time zone,
                                             gyldigfra timestamp with time zone,
                                             gyldigtil timestamp with time zone,
                                             datauttaksdato timestamp with time zone,
                                             typeendring text,
                                             tidspunktendring timestamp with time zone,
                                             opphav text,
                                             omraadeid int,
                                             originaldatavert text,
                                             kopidato timestamp with time zone,
                                             produkt text,
                                             versjon text,
                                             maalemetode text NOT NULL,
                                             noeyaktighet int,
                                             grense geometry(Linestring, 25833) NOT NULL,
                                             gjelderomraade text,
                                             grensestatus text,
                                             virkeomraadenavn text,
                                             transformasjonsdato date,
                                             land text,
                                             fastsettingstype text NOT NULL,
                                             beregningsdato date)
    WITH (OIDS=FALSE);

CREATE TABLE kystreferanse (
                               identifier text NOT NULL,
                               description text)
    WITH (OIDS=FALSE);

INSERT INTO kystreferanse (identifier, description) VALUES ('FOTOV', 'Fotovannstand');
INSERT INTO kystreferanse (identifier, description) VALUES ('K0', 'Middelvann normalnull');
INSERT INTO kystreferanse (identifier, description) VALUES ('MHV', 'Middel høyvannstand');
INSERT INTO kystreferanse (identifier, description) VALUES ('USIKR', 'Ikke kontrollert kontur');

CREATE TABLE kysttype (
                          identifier text NOT NULL,
                          description text)
    WITH (OIDS=FALSE);

INSERT INTO kysttype (identifier, description) VALUES ('1', 'Bratt kyst');
INSERT INTO kysttype (identifier, description) VALUES ('2', 'Lav kyst');
INSERT INTO kysttype (identifier, description) VALUES ('3', 'Sandstrand');
INSERT INTO kysttype (identifier, description) VALUES ('4', 'Steinet strand');
INSERT INTO kysttype (identifier, description) VALUES ('6', 'Brefront i sjøen');

CREATE TABLE landkode (
                          identifier text NOT NULL,
                          description text)
    WITH (OIDS=FALSE);

INSERT INTO landkode (identifier, description) VALUES ('BV', 'Bouvet Island');
INSERT INTO landkode (identifier, description) VALUES ('DK', 'Denmark');
INSERT INTO landkode (identifier, description) VALUES ('FO', 'Faroe Islands');
INSERT INTO landkode (identifier, description) VALUES ('FI', 'Finland');
INSERT INTO landkode (identifier, description) VALUES ('GL', 'Greenland');
INSERT INTO landkode (identifier, description) VALUES ('IS', 'Iceland');
INSERT INTO landkode (identifier, description) VALUES ('NO', 'Norway');
INSERT INTO landkode (identifier, description) VALUES ('RU', 'Russian Federation');
INSERT INTO landkode (identifier, description) VALUES ('SJ', 'Svalbard and Jan Mayen');
INSERT INTO landkode (identifier, description) VALUES ('SE', 'Sweden');
INSERT INTO landkode (identifier, description) VALUES ('GB', 'United Kingdom');

CREATE TABLE fylke_informasjon (
                                   objid serial NOT NULL,
                                   fylke_fk int,
                                   informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE fylkesgrense_informasjon (
                                          objid serial NOT NULL,
                                          fylkesgrense_fk int,
                                          informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grensemerkeland_informasjon (
                                             objid serial NOT NULL,
                                             grensemerkeland_fk int,
                                             informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE kommune_informasjon (
                                     objid serial NOT NULL,
                                     kommune_fk int,
                                     informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE kommunegrense_informasjon (
                                           objid serial NOT NULL,
                                           kommunegrense_fk int,
                                           informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE nasjon_informasjon (
                                    objid serial NOT NULL,
                                    nasjon_fk int,
                                    informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE brytningspunkt_navn (
                                     objid serial NOT NULL,
                                     brytningspunkt_fk int,
                                     navn text)
    WITH (OIDS=FALSE);

CREATE TABLE brytningspunkt_informasjon (
                                            objid serial NOT NULL,
                                            brytningspunkt_fk int,
                                            informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE brytningspunkt_fotodokumentasjon (
                                                  objid serial NOT NULL,
                                                  brytningspunkt_fk int,
                                                  fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grenseskilt_informasjon (
                                         objid serial NOT NULL,
                                         grenseskilt_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grenseskilt_fotodokumentasjon (
                                               objid serial NOT NULL,
                                               grenseskilt_fk int,
                                               fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grensestolpe_informasjon (
                                          objid serial NOT NULL,
                                          grensestolpe_fk int,
                                          informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grensestolpe_fotodokumentasjon (
                                                objid serial NOT NULL,
                                                grensestolpe_fk int,
                                                fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE litenutligger_informasjon (
                                           objid serial NOT NULL,
                                           litenutligger_fk int,
                                           informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE litenutligger_fotodokumentasjon (
                                                 objid serial NOT NULL,
                                                 litenutligger_fk int,
                                                 fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE litenutligger_navn (
                                    objid serial NOT NULL,
                                    litenutligger_fk int,
                                    navn text)
    WITH (OIDS=FALSE);

CREATE TABLE profilpunkt_informasjon (
                                         objid serial NOT NULL,
                                         profilpunkt_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE profilpunkt_fotodokumentasjon (
                                               objid serial NOT NULL,
                                               profilpunkt_fk int,
                                               fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_ansvarligmyndighet (
                                               objid serial NOT NULL,
                                               riksgrense_fk int,
                                               ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE riksgrense_informasjon (
                                        objid serial NOT NULL,
                                        riksgrense_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE riksroeys_riksroeysnavn (
                                         objid serial NOT NULL,
                                         riksroeys_fk int,
                                         riksroeysnavn text)
    WITH (OIDS=FALSE);

CREATE TABLE riksroeys_informasjon (
                                       objid serial NOT NULL,
                                       riksroeys_fk int,
                                       informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE riksroeys_fotodokumentasjon (
                                             objid serial NOT NULL,
                                             riksroeys_fk int,
                                             fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE storutligger_informasjon (
                                          objid serial NOT NULL,
                                          storutligger_fk int,
                                          informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE storutligger_fotodokumentasjon (
                                                objid serial NOT NULL,
                                                storutligger_fk int,
                                                fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE storutligger_navn (
                                   objid serial NOT NULL,
                                   storutligger_fk int,
                                   navn text)
    WITH (OIDS=FALSE);

CREATE TABLE vassdragspunkt_informasjon (
                                            objid serial NOT NULL,
                                            vassdragspunkt_fk int,
                                            informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE vassdragspunkt_fotodokumentasjon (
                                                  objid serial NOT NULL,
                                                  vassdragspunkt_fk int,
                                                  fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE viserstein_informasjon (
                                        objid serial NOT NULL,
                                        viserstein_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE viserstein_fotodokumentasjon (
                                              objid serial NOT NULL,
                                              viserstein_fk int,
                                              fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE oevrighjelpemerke_informasjon (
                                               objid serial NOT NULL,
                                               oevrighjelpemerke_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE oevrighjelpemerke_fotodokumentasjon (
                                                     objid serial NOT NULL,
                                                     oevrighjelpemerke_fk int,
                                                     fotodokumentasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE hjertesteininformasjon_bildehjertestein (
                                                         objid serial NOT NULL,
                                                         hjertesteininformasjon_fk int,
                                                         bildehjertestein text)
    WITH (OIDS=FALSE);

CREATE TABLE delomraade_informasjon (
                                        objid serial NOT NULL,
                                        delomraade_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE delomraadegrense_informasjon (
                                              objid serial NOT NULL,
                                              delomraadegrense_fk int,
                                              informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkrets_informasjon (
                                        objid serial NOT NULL,
                                        grunnkrets_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grunnkretsgrense_informasjon (
                                              objid serial NOT NULL,
                                              grunnkretsgrense_fk int,
                                              informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorgrense_informasjon (
                                          objid serial NOT NULL,
                                          sektorgrense_fk int,
                                          informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlokalitet_informasjon (
                                             objid serial NOT NULL,
                                             sektorlokalitet_fk int,
                                             informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sektoromraade_informasjon (
                                           objid serial NOT NULL,
                                           sektoromraade_fk int,
                                           informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE bispedoemme_informasjon (
                                         objid serial NOT NULL,
                                         bispedoemme_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE bispedoemmegrense_informasjon (
                                               objid serial NOT NULL,
                                               bispedoemmegrense_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE prosti_informasjon (
                                    objid serial NOT NULL,
                                    prosti_fk int,
                                    informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE prostigrense_informasjon (
                                          objid serial NOT NULL,
                                          prostigrense_fk int,
                                          informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sokn_fellesraad (
                                 objid serial NOT NULL,
                                 sokn_fk int,
                                 fellesraad text)
    WITH (OIDS=FALSE);

CREATE TABLE sokn_informasjon (
                                  objid serial NOT NULL,
                                  sokn_fk int,
                                  informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE soknegrense_informasjon (
                                         objid serial NOT NULL,
                                         soknegrense_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraade_informasjon (
                                               objid serial NOT NULL,
                                               postnummeromraade_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postnummeromraadegrense_informasjon (
                                                     objid serial NOT NULL,
                                                     postnummeromraadegrense_fk int,
                                                     informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postomraade_informasjon (
                                         objid serial NOT NULL,
                                         postomraade_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postomraadegrense_informasjon (
                                               objid serial NOT NULL,
                                               postomraadegrense_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postregion_informasjon (
                                        objid serial NOT NULL,
                                        postregion_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postregiongrense_informasjon (
                                              objid serial NOT NULL,
                                              postregiongrense_fk int,
                                              informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postsone_informasjon (
                                      objid serial NOT NULL,
                                      postsone_fk int,
                                      informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE postsonegrense_informasjon (
                                            objid serial NOT NULL,
                                            postsonegrense_fk int,
                                            informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraade_sonetype (
                                          lovvirkeomraade_fk int,
                                          sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraade_miljoeomraade (
                                               lovvirkeomraade_fk int,
                                               miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraade_informasjon (
                                             objid serial NOT NULL,
                                             lovvirkeomraade_fk int,
                                             informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraade_ansvarligmyndighet (
                                                    objid serial NOT NULL,
                                                    lovvirkeomraade_fk int,
                                                    ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraadegrense_informasjon (
                                                   objid serial NOT NULL,
                                                   lovvirkeomraadegrense_fk int,
                                                   informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraadegrense_ansvarligmyndighet (
                                                          objid serial NOT NULL,
                                                          lovvirkeomraadegrense_fk int,
                                                          ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE lovvirkeomraadegrensepunkt_informasjon (
                                                        objid serial NOT NULL,
                                                        lovvirkeomraadegrensepunkt_fk int,
                                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE dokumentasjonsreferanse_dokumentlink (
                                                      objid serial NOT NULL,
                                                      dokumentasjonsreferanse_fk int,
                                                      dokumentlink text)
    WITH (OIDS=FALSE);

CREATE TABLE dokumentasjonsreferanse_internreferansekartverket (
                                                                   objid serial NOT NULL,
                                                                   dokumentasjonsreferanse_fk int,
                                                                   internreferansekartverket text)
    WITH (OIDS=FALSE);

CREATE TABLE avtaltavgrensningslinje_informasjon (
                                                     objid serial NOT NULL,
                                                     avtaltavgrensningslinje_fk int,
                                                     informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE avtaltavgrensningslinje_ansvarligmyndighet (
                                                            objid serial NOT NULL,
                                                            avtaltavgrensningslinje_fk int,
                                                            ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE avtaltavgrensningslinje_land (
                                              avtaltavgrensningslinje_fk int,
                                              landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE beregnetmidtlinje_informasjon (
                                               objid serial NOT NULL,
                                               beregnetmidtlinje_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE beregnetmidtlinje_ansvarligmyndighet (
                                                      objid serial NOT NULL,
                                                      beregnetmidtlinje_fk int,
                                                      ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE beregnetmidtlinje_land (
                                        beregnetmidtlinje_fk int,
                                        landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE detsaerskilteomraadet_informasjon (
                                                   objid serial NOT NULL,
                                                   detsaerskilteomraadet_fk int,
                                                   informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE detsaerskilteomraadet_land (
                                            detsaerskilteomraadet_fk int,
                                            landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE detsaerskilteomraadet_ansvarligmyndighet (
                                                          objid serial NOT NULL,
                                                          detsaerskilteomraadet_fk int,
                                                          ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE detsaerskilteomraadet_sonetype (
                                                detsaerskilteomraadet_fk int,
                                                sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE detsaerskilteomraadet_miljoeomraade (
                                                     detsaerskilteomraadet_fk int,
                                                     miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskerisone_informasjon (
                                         objid serial NOT NULL,
                                         fiskerisone_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskerisone_land (
                                  fiskerisone_fk int,
                                  landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskerisone_ansvarligmyndighet (
                                                objid serial NOT NULL,
                                                fiskerisone_fk int,
                                                ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskerisone_sonetype (
                                      fiskerisone_fk int,
                                      sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskerisone_miljoeomraade (
                                           fiskerisone_fk int,
                                           miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskevernsone_informasjon (
                                           objid serial NOT NULL,
                                           fiskevernsone_fk int,
                                           informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskevernsone_land (
                                    fiskevernsone_fk int,
                                    landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskevernsone_ansvarligmyndighet (
                                                  objid serial NOT NULL,
                                                  fiskevernsone_fk int,
                                                  ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskevernsone_sonetype (
                                        fiskevernsone_fk int,
                                        sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE fiskevernsone_miljoeomraade (
                                             fiskevernsone_fk int,
                                             miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE grense200nautiskemil_informasjon (
                                                  objid serial NOT NULL,
                                                  grense200nautiskemil_fk int,
                                                  informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grense200nautiskemil_ansvarligmyndighet (
                                                         objid serial NOT NULL,
                                                         grense200nautiskemil_fk int,
                                                         ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE grenseboeye_informasjon (
                                         objid serial NOT NULL,
                                         grenseboeye_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grensepunktsjoe_informasjon (
                                             objid serial NOT NULL,
                                             grensepunktsjoe_fk int,
                                             informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grunnlinje_informasjon (
                                        objid serial NOT NULL,
                                        grunnlinje_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE grunnlinje_ansvarligmyndighet (
                                               objid serial NOT NULL,
                                               grunnlinje_fk int,
                                               ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonegrense_informasjon (
                                            objid serial NOT NULL,
                                            graasonegrense_fk int,
                                            informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonegrense_ansvarligmyndighet (
                                                   objid serial NOT NULL,
                                                   graasonegrense_fk int,
                                                   ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonegrense_land (
                                     graasonegrense_fk int,
                                     landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonen_informasjon (
                                       objid serial NOT NULL,
                                       graasonen_fk int,
                                       informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonen_land (
                                graasonen_fk int,
                                landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonen_ansvarligmyndighet (
                                              objid serial NOT NULL,
                                              graasonen_fk int,
                                              ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonen_sonetype (
                                    graasonen_fk int,
                                    sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE graasonen_miljoeomraade (
                                         graasonen_fk int,
                                         miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE indrefarvann_informasjon (
                                          objid serial NOT NULL,
                                          indrefarvann_fk int,
                                          informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE indrefarvann_land (
                                   indrefarvann_fk int,
                                   landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE indrefarvann_ansvarligmyndighet (
                                                 objid serial NOT NULL,
                                                 indrefarvann_fk int,
                                                 ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE indrefarvann_sonetype (
                                       indrefarvann_fk int,
                                       sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE indrefarvann_miljoeomraade (
                                            indrefarvann_fk int,
                                            miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE internasjonaltfarvann_informasjon (
                                                   objid serial NOT NULL,
                                                   internasjonaltfarvann_fk int,
                                                   informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE kontinentalsokkel_informasjon (
                                               objid serial NOT NULL,
                                               kontinentalsokkel_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE kontinentalsokkel_land (
                                        kontinentalsokkel_fk int,
                                        landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE kontinentalsokkel_ansvarligmyndighet (
                                                      objid serial NOT NULL,
                                                      kontinentalsokkel_fk int,
                                                      ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE kontinentalsokkel_sonetype (
                                            kontinentalsokkel_fk int,
                                            sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE kontinentalsokkel_miljoeomraade (
                                                 kontinentalsokkel_fk int,
                                                 miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE kystkontur_informasjon (
                                        objid serial NOT NULL,
                                        kystkontur_fk int,
                                        informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE landindrefarvann_informasjon (
                                              objid serial NOT NULL,
                                              landindrefarvann_fk int,
                                              informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE norgesoekonomiskesone_informasjon (
                                                   objid serial NOT NULL,
                                                   norgesoekonomiskesone_fk int,
                                                   informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE norgesoekonomiskesone_land (
                                            norgesoekonomiskesone_fk int,
                                            landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE norgesoekonomiskesone_ansvarligmyndighet (
                                                          objid serial NOT NULL,
                                                          norgesoekonomiskesone_fk int,
                                                          ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE norgesoekonomiskesone_sonetype (
                                                norgesoekonomiskesone_fk int,
                                                sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE norgesoekonomiskesone_miljoeomraade (
                                                     norgesoekonomiskesone_fk int,
                                                     miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlinje_informasjon (
                                         objid serial NOT NULL,
                                         sektorlinje_fk int,
                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlinje_ansvarligmyndighet (
                                                objid serial NOT NULL,
                                                sektorlinje_fk int,
                                                ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE sektorlinje_land (
                                  sektorlinje_fk int,
                                  landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeroeys_informasjon (
                                       objid serial NOT NULL,
                                       sjoeroeys_fk int,
                                       informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeterritorium_informasjon (
                                             objid serial NOT NULL,
                                             sjoeterritorium_fk int,
                                             informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeterritorium_land (
                                      sjoeterritorium_fk int,
                                      landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeterritorium_ansvarligmyndighet (
                                                    objid serial NOT NULL,
                                                    sjoeterritorium_fk int,
                                                    ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeterritorium_sonetype (
                                          sjoeterritorium_fk int,
                                          sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE sjoeterritorium_miljoeomraade (
                                               sjoeterritorium_fk int,
                                               miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialfarvann_informasjon (
                                                objid serial NOT NULL,
                                                territorialfarvann_fk int,
                                                informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialfarvann_land (
                                         territorialfarvann_fk int,
                                         landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialfarvann_ansvarligmyndighet (
                                                       objid serial NOT NULL,
                                                       territorialfarvann_fk int,
                                                       ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialfarvann_sonetype (
                                             territorialfarvann_fk int,
                                             sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialfarvann_miljoeomraade (
                                                  territorialfarvann_fk int,
                                                  miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialgrense_informasjon (
                                               objid serial NOT NULL,
                                               territorialgrense_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialgrense_ansvarligmyndighet (
                                                      objid serial NOT NULL,
                                                      territorialgrense_fk int,
                                                      ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE territorialomraade_informasjon (
                                                objid serial NOT NULL,
                                                territorialomraade_fk int,
                                                informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE tidligereomstridtomraade_informasjon (
                                                      objid serial NOT NULL,
                                                      tidligereomstridtomraade_fk int,
                                                      informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE tilstoetendesone_informasjon (
                                              objid serial NOT NULL,
                                              tilstoetendesone_fk int,
                                              informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE tilstoetendesone_land (
                                       tilstoetendesone_fk int,
                                       landkode_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE tilstoetendesone_ansvarligmyndighet (
                                                     objid serial NOT NULL,
                                                     tilstoetendesone_fk int,
                                                     ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE tilstoetendesone_sonetype (
                                           tilstoetendesone_fk int,
                                           sonetype_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE tilstoetendesone_miljoeomraade (
                                                tilstoetendesone_fk int,
                                                miljoeomraade_fk text)
    WITH (OIDS=FALSE);

CREATE TABLE yttergrensesokkel_informasjon (
                                               objid serial NOT NULL,
                                               yttergrensesokkel_fk int,
                                               informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE yttergrensesokkel_ansvarligmyndighet (
                                                      objid serial NOT NULL,
                                                      yttergrensesokkel_fk int,
                                                      ansvarligmyndighet text)
    WITH (OIDS=FALSE);

CREATE TABLE yttergrensetilstoetendesone_informasjon (
                                                         objid serial NOT NULL,
                                                         yttergrensetilstoetendesone_fk int,
                                                         informasjon text)
    WITH (OIDS=FALSE);

CREATE TABLE yttergrensetilstoetendesone_ansvarligmyndighet (
                                                                objid serial NOT NULL,
                                                                yttergrensetilstoetendesone_fk int,
                                                                ansvarligmyndighet text)
    WITH (OIDS=FALSE);

ALTER TABLE identifikasjon ADD PRIMARY KEY (objid);
ALTER TABLE typeendring ADD PRIMARY KEY (identifier);
ALTER TABLE fylke ADD PRIMARY KEY (objid);
ALTER TABLE fylke_fylkesgrense ADD PRIMARY KEY (fylke_fk, fylkesgrense_fk);
ALTER TABLE fylkesgrense ADD PRIMARY KEY (objid);
ALTER TABLE avgrensning_bispedoemme ADD PRIMARY KEY (avgrensning_fk, bispedoemme_fk);
ALTER TABLE delomraade_fylkesgrense ADD PRIMARY KEY (delomraade_fk, fylkesgrense_fk);
ALTER TABLE fylkesgrense_grensepunktsjoe ADD PRIMARY KEY (fylkesgrense_fk, grensepunktsjoe_fk);
ALTER TABLE avgrensning_prosti ADD PRIMARY KEY (avgrensning_fk, prosti_fk);
ALTER TABLE stemmekrets_fylkesgrense ADD PRIMARY KEY (stemmekrets_fk, fylkesgrense_fk);
ALTER TABLE grunnkrets_fylkesgrense ADD PRIMARY KEY (grunnkrets_fk, fylkesgrense_fk);
ALTER TABLE avgrensning_sokn ADD PRIMARY KEY (avgrensning_fk, sokn_fk);
ALTER TABLE skolekrets_fylkesgrense ADD PRIMARY KEY (skolekrets_fk, fylkesgrense_fk);
ALTER TABLE fylkesgrense_grensemerkeland ADD PRIMARY KEY (fylkesgrense_fk, grensemerkeland_fk);
ALTER TABLE kommune_fylkesgrense ADD PRIMARY KEY (kommune_fk, fylkesgrense_fk);
ALTER TABLE grensemerkeland ADD PRIMARY KEY (objid);
ALTER TABLE grensemerkeland_kommunegrense ADD PRIMARY KEY (grensemerkeland_fk, kommunegrense_fk);
ALTER TABLE kommune ADD PRIMARY KEY (objid);
ALTER TABLE kommune_kommunegrense ADD PRIMARY KEY (kommune_fk, kommunegrense_fk);
ALTER TABLE kommunegrense ADD PRIMARY KEY (objid);
ALTER TABLE grensepunktsjoe_kommunegrense ADD PRIMARY KEY (grensepunktsjoe_fk, kommunegrense_fk);
ALTER TABLE grunnkrets_kommunegrense ADD PRIMARY KEY (grunnkrets_fk, kommunegrense_fk);
ALTER TABLE delomraade_kommunegrense ADD PRIMARY KEY (delomraade_fk, kommunegrense_fk);
--ALTER TABLE avgrensning_prosti ADD PRIMARY KEY (avgrensning_fk, prosti_fk);
ALTER TABLE skolekrets_kommunegrense ADD PRIMARY KEY (skolekrets_fk, kommunegrense_fk);
--ALTER TABLE avgrensning_sokn ADD PRIMARY KEY (avgrensning_fk, sokn_fk);
ALTER TABLE stemmekrets_kommunegrense ADD PRIMARY KEY (stemmekrets_fk, kommunegrense_fk);
--ALTER TABLE avgrensning_bispedoemme ADD PRIMARY KEY (avgrensning_fk, bispedoemme_fk);
ALTER TABLE nasjon ADD PRIMARY KEY (objid);
ALTER TABLE administrativenhetnavn ADD PRIMARY KEY (objid);
ALTER TABLE grensepunkttypekode ADD PRIMARY KEY (identifier);
ALTER TABLE nivaanummer ADD PRIMARY KEY (identifier);
ALTER TABLE noeyaktighetsklassekode ADD PRIMARY KEY (identifier);
ALTER TABLE spraakkode ADD PRIMARY KEY (identifier);
ALTER TABLE terrengdetaljkode ADD PRIMARY KEY (identifier);
ALTER TABLE brytningspunkt ADD PRIMARY KEY (objid);
ALTER TABLE riksgrense_grensemerke_vassdragspunkt ADD PRIMARY KEY (riksgrense_fk, grensemerke_vassdragspunkt_fk);
ALTER TABLE riksgrense_grensemerke_riksroeys ADD PRIMARY KEY (riksgrense_fk, grensemerke_riksroeys_fk);
ALTER TABLE riksgrense_grensemerke_profilpunkt ADD PRIMARY KEY (riksgrense_fk, grensemerke_profilpunkt_fk);
ALTER TABLE riksgrense_grensemerke_brytningspunkt ADD PRIMARY KEY (riksgrense_fk, grensemerke_brytningspunkt_fk);
ALTER TABLE riksgrense_grensemerke_viserstein ADD PRIMARY KEY (riksgrense_fk, grensemerke_viserstein_fk);
ALTER TABLE riksgrense_grensemerke_oevrighjelpemerke ADD PRIMARY KEY (riksgrense_fk, grensemerke_oevrighjelpemerke_fk);
ALTER TABLE riksgrense_grensemerke_storutligger ADD PRIMARY KEY (riksgrense_fk, grensemerke_storutligger_fk);
ALTER TABLE riksgrense_grensemerke_litenutligger ADD PRIMARY KEY (riksgrense_fk, grensemerke_litenutligger_fk);
ALTER TABLE riksgrense_grensemerke_grensestolpe ADD PRIMARY KEY (riksgrense_fk, grensemerke_grensestolpe_fk);
ALTER TABLE riksgrense_grensemerke_grenseskilt ADD PRIMARY KEY (riksgrense_fk, grensemerke_grenseskilt_fk);
ALTER TABLE grenseskilt ADD PRIMARY KEY (objid);
ALTER TABLE grensestolpe ADD PRIMARY KEY (objid);
ALTER TABLE litenutligger ADD PRIMARY KEY (objid);
ALTER TABLE profilpunkt ADD PRIMARY KEY (objid);
ALTER TABLE riksgrense ADD PRIMARY KEY (objid);
--ALTER TABLE avgrensning_prosti ADD PRIMARY KEY (avgrensning_fk, prosti_fk);
ALTER TABLE riksroeys ADD PRIMARY KEY (objid);
ALTER TABLE sikringspunkt ADD PRIMARY KEY (objid);
ALTER TABLE storutligger ADD PRIMARY KEY (objid);
ALTER TABLE vassdragspunkt ADD PRIMARY KEY (objid);
ALTER TABLE viserstein ADD PRIMARY KEY (objid);
ALTER TABLE oevrighjelpemerke ADD PRIMARY KEY (objid);
ALTER TABLE hjertesteininformasjon ADD PRIMARY KEY (objid);
ALTER TABLE naermestegrensemerke ADD PRIMARY KEY (objid);
ALTER TABLE registrertekoordinater ADD PRIMARY KEY (objid);
ALTER TABLE registrerthoeyde ADD PRIMARY KEY (objid);
ALTER TABLE vedlikehold ADD PRIMARY KEY (objid);
ALTER TABLE hoeydereferanse ADD PRIMARY KEY (identifier);
ALTER TABLE hoeydereferansesystem ADD PRIMARY KEY (identifier);
ALTER TABLE materialebolt ADD PRIMARY KEY (identifier);
ALTER TABLE referansesystemkode ADD PRIMARY KEY (identifier);
ALTER TABLE sikringspunkttype ADD PRIMARY KEY (identifier);
ALTER TABLE skilttype ADD PRIMARY KEY (identifier);
ALTER TABLE underlag ADD PRIMARY KEY (identifier);
ALTER TABLE delomraade ADD PRIMARY KEY (objid);
ALTER TABLE delomraade_delomraadegrens ADD PRIMARY KEY (delomraade_fk, delomraadegrens_fk);
ALTER TABLE delomraadegrense ADD PRIMARY KEY (objid);
ALTER TABLE grunnkrets_delomraadegrense ADD PRIMARY KEY (grunnkrets_fk, delomraadegrense_fk);
ALTER TABLE grunnkrets ADD PRIMARY KEY (objid);
ALTER TABLE grunnkrets_grunnkretsgrense ADD PRIMARY KEY (grunnkrets_fk, grunnkretsgrense_fk);
ALTER TABLE grunnkretsgrense ADD PRIMARY KEY (objid);
ALTER TABLE valgdistriktsgrense ADD PRIMARY KEY (objid);
ALTER TABLE stemmekrets_valgdistriktsgrense ADD PRIMARY KEY (stemmekrets_fk, valgdistriktsgrense_fk);
ALTER TABLE skolekrets ADD PRIMARY KEY (objid);
ALTER TABLE skolekrets_skolekretsgrense ADD PRIMARY KEY (skolekrets_fk, skolekretsgrense_fk);
ALTER TABLE skolekretsgrense ADD PRIMARY KEY (objid);
ALTER TABLE stemmekrets ADD PRIMARY KEY (objid);
ALTER TABLE stemmekrets_stemmekretsgrense ADD PRIMARY KEY (stemmekrets_fk, stemmekretsgrense_fk);
ALTER TABLE stemmekretsgrense ADD PRIMARY KEY (objid);
ALTER TABLE sektorgrense ADD PRIMARY KEY (objid);
ALTER TABLE sektoromraade_sektorgrense ADD PRIMARY KEY (sektoromraade_fk, sektorgrense_fk);
ALTER TABLE sektorlokalitet ADD PRIMARY KEY (objid);
ALTER TABLE sektoromraade_sektorlokalitet ADD PRIMARY KEY (sektoromraade_fk, sektorlokalitet_fk);
ALTER TABLE sektoromraade ADD PRIMARY KEY (objid);
ALTER TABLE bispedoemme ADD PRIMARY KEY (objid);
ALTER TABLE bispedoemme_avgrensning ADD PRIMARY KEY (bispedoemme_fk, avgrensning_fk);
ALTER TABLE bispedoemmegrense ADD PRIMARY KEY (objid);
ALTER TABLE prosti_avgrensning ADD PRIMARY KEY (prosti_fk, avgrensning_fk);
ALTER TABLE sokn_avgrensning ADD PRIMARY KEY (sokn_fk, avgrensning_fk);
ALTER TABLE prosti ADD PRIMARY KEY (objid);
--ALTER TABLE prosti_avgrensning ADD PRIMARY KEY (prosti_fk, avgrensning_fk);
ALTER TABLE prostigrense ADD PRIMARY KEY (objid);
--ALTER TABLE sokn_avgrensning ADD PRIMARY KEY (sokn_fk, avgrensning_fk);
ALTER TABLE sokn ADD PRIMARY KEY (objid);
--ALTER TABLE sokn_avgrensning ADD PRIMARY KEY (sokn_fk, avgrensning_fk);
ALTER TABLE soknegrense ADD PRIMARY KEY (objid);
ALTER TABLE bispedoemmenummer ADD PRIMARY KEY (identifier);
ALTER TABLE prostinummer ADD PRIMARY KEY (identifier);
ALTER TABLE soknenummer ADD PRIMARY KEY (identifier);
ALTER TABLE postnummeromraade ADD PRIMARY KEY (objid);
ALTER TABLE postnummeromraade_postsonegrense ADD PRIMARY KEY (postnummeromraade_fk, postsonegrense_fk);
ALTER TABLE postnummeromraade_postnummeromraadegrense ADD PRIMARY KEY (postnummeromraade_fk, postnummeromraadegrense_fk);
ALTER TABLE postnummeromraade_postomraadegrense ADD PRIMARY KEY (postnummeromraade_fk, postomraadegrense_fk);
ALTER TABLE postnummeromraade_postregiongrense ADD PRIMARY KEY (postnummeromraade_fk, postregiongrense_fk);
ALTER TABLE postnummeromraadegrense ADD PRIMARY KEY (objid);
ALTER TABLE postomraade ADD PRIMARY KEY (objid);
ALTER TABLE postomraade_postsonegrense ADD PRIMARY KEY (postomraade_fk, postsonegrense_fk);
ALTER TABLE postomraade_postomraadegrense ADD PRIMARY KEY (postomraade_fk, postomraadegrense_fk);
ALTER TABLE postomraade_postregiongrense ADD PRIMARY KEY (postomraade_fk, postregiongrense_fk);
ALTER TABLE postomraadegrense ADD PRIMARY KEY (objid);
ALTER TABLE postregion ADD PRIMARY KEY (objid);
ALTER TABLE postregion_postsonegrense ADD PRIMARY KEY (postregion_fk, postsonegrense_fk);
ALTER TABLE postregion_postregiongrense ADD PRIMARY KEY (postregion_fk, postregiongrense_fk);
ALTER TABLE postregiongrense ADD PRIMARY KEY (objid);
ALTER TABLE postsone ADD PRIMARY KEY (objid);
ALTER TABLE postsone_postsonegrense ADD PRIMARY KEY (postsone_fk, postsonegrense_fk);
ALTER TABLE postsonegrense ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraade ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraadegrense ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraadegrense_lovvirkeomraadegrensepunkt ADD PRIMARY KEY (lovvirkeomraadegrense_fk, lovvirkeomraadegrensepunkt_fk);
ALTER TABLE lovvirkeomraadegrensepunkt ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse ADD PRIMARY KEY (objid);
ALTER TABLE punktreferanse ADD PRIMARY KEY (objid);
ALTER TABLE administrativgrensekode ADD PRIMARY KEY (identifier);
ALTER TABLE fastsettingstype ADD PRIMARY KEY (identifier);
ALTER TABLE grensestatus ADD PRIMARY KEY (identifier);
ALTER TABLE maritimtomraadesonetype ADD PRIMARY KEY (identifier);
ALTER TABLE miljoeomraade ADD PRIMARY KEY (identifier);
ALTER TABLE sonetype ADD PRIMARY KEY (identifier);
ALTER TABLE avtaltavgrensningslinje ADD PRIMARY KEY (objid);
ALTER TABLE avtaltavgrensningslinje_grenseboeye ADD PRIMARY KEY (avtaltavgrensningslinje_fk, grenseboeye_fk);
ALTER TABLE sjoeroeys_avtaltavgrensningslinje ADD PRIMARY KEY (sjoeroeys_fk, avtaltavgrensningslinje_fk);
ALTER TABLE avtaltavgrensningslinje_grensepunktsjoe ADD PRIMARY KEY (avtaltavgrensningslinje_fk, grensepunktsjoe_fk);
ALTER TABLE beregnetmidtlinje ADD PRIMARY KEY (objid);
ALTER TABLE grensepunktsjoe_beregnetmidtlinje ADD PRIMARY KEY (grensepunktsjoe_fk, beregnetmidtlinje_fk);
ALTER TABLE detsaerskilteomraadet ADD PRIMARY KEY (objid);
ALTER TABLE fiskerisone ADD PRIMARY KEY (objid);
ALTER TABLE fiskevernsone ADD PRIMARY KEY (objid);
ALTER TABLE grense200nautiskemil ADD PRIMARY KEY (objid);
ALTER TABLE grenseboeye ADD PRIMARY KEY (objid);
ALTER TABLE grensepunktsjoe ADD PRIMARY KEY (objid);
ALTER TABLE sektorlinje_grensepunktsjoe ADD PRIMARY KEY (sektorlinje_fk, grensepunktsjoe_fk);
ALTER TABLE grunnlinje_grensepunktsjoe ADD PRIMARY KEY (grunnlinje_fk, grensepunktsjoe_fk);
ALTER TABLE graasonegrense_grensepunktsjoe ADD PRIMARY KEY (graasonegrense_fk, grensepunktsjoe_fk);
ALTER TABLE grunnlinje ADD PRIMARY KEY (objid);
ALTER TABLE graasonegrense ADD PRIMARY KEY (objid);
ALTER TABLE graasonen ADD PRIMARY KEY (objid);
ALTER TABLE indrefarvann ADD PRIMARY KEY (objid);
ALTER TABLE internasjonaltfarvann ADD PRIMARY KEY (objid);
ALTER TABLE kontinentalsokkel ADD PRIMARY KEY (objid);
ALTER TABLE kystkontur ADD PRIMARY KEY (objid);
ALTER TABLE landindrefarvann ADD PRIMARY KEY (objid);
ALTER TABLE norgesoekonomiskesone ADD PRIMARY KEY (objid);
ALTER TABLE sektorlinje ADD PRIMARY KEY (objid);
ALTER TABLE sjoeroeys ADD PRIMARY KEY (objid);
ALTER TABLE sjoeterritorium ADD PRIMARY KEY (objid);
ALTER TABLE territorialfarvann ADD PRIMARY KEY (objid);
ALTER TABLE territorialgrense ADD PRIMARY KEY (objid);
ALTER TABLE territorialomraade ADD PRIMARY KEY (objid);
ALTER TABLE tidligereomstridtomraade ADD PRIMARY KEY (objid);
ALTER TABLE tilstoetendesone ADD PRIMARY KEY (objid);
ALTER TABLE yttergrensesokkel ADD PRIMARY KEY (objid);
ALTER TABLE yttergrensetilstoetendesone ADD PRIMARY KEY (objid);
ALTER TABLE kystreferanse ADD PRIMARY KEY (identifier);
ALTER TABLE kysttype ADD PRIMARY KEY (identifier);
ALTER TABLE landkode ADD PRIMARY KEY (identifier);
ALTER TABLE fylke_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE fylkesgrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grensemerkeland_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE kommune_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE kommunegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE nasjon_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE brytningspunkt_navn ADD PRIMARY KEY (objid);
ALTER TABLE brytningspunkt_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE brytningspunkt_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE grenseskilt_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grenseskilt_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE grensestolpe_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grensestolpe_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE litenutligger_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE litenutligger_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE litenutligger_navn ADD PRIMARY KEY (objid);
ALTER TABLE profilpunkt_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE profilpunkt_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE riksgrense_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE riksgrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE riksroeys_riksroeysnavn ADD PRIMARY KEY (objid);
ALTER TABLE riksroeys_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE riksroeys_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE storutligger_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE storutligger_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE storutligger_navn ADD PRIMARY KEY (objid);
ALTER TABLE vassdragspunkt_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE vassdragspunkt_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE viserstein_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE viserstein_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE oevrighjelpemerke_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE oevrighjelpemerke_fotodokumentasjon ADD PRIMARY KEY (objid);
ALTER TABLE hjertesteininformasjon_bildehjertestein ADD PRIMARY KEY (objid);
ALTER TABLE delomraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE delomraadegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grunnkrets_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grunnkretsgrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sektorgrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sektorlokalitet_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sektoromraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE bispedoemme_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE bispedoemmegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE prosti_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE prostigrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sokn_fellesraad ADD PRIMARY KEY (objid);
ALTER TABLE sokn_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE soknegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postnummeromraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postnummeromraadegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postomraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postomraadegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postregion_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postregiongrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postsone_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE postsonegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraade_sonetype ADD PRIMARY KEY (lovvirkeomraade_fk, sonetype_fk);
ALTER TABLE lovvirkeomraade_miljoeomraade ADD PRIMARY KEY (lovvirkeomraade_fk, miljoeomraade_fk);
ALTER TABLE lovvirkeomraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraade_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraadegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraadegrense_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE lovvirkeomraadegrensepunkt_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse_dokumentlink ADD PRIMARY KEY (objid);
ALTER TABLE dokumentasjonsreferanse_internreferansekartverket ADD PRIMARY KEY (objid);
ALTER TABLE avtaltavgrensningslinje_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE avtaltavgrensningslinje_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE avtaltavgrensningslinje_land ADD PRIMARY KEY (avtaltavgrensningslinje_fk, landkode_fk);
ALTER TABLE beregnetmidtlinje_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE beregnetmidtlinje_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE beregnetmidtlinje_land ADD PRIMARY KEY (beregnetmidtlinje_fk, landkode_fk);
ALTER TABLE detsaerskilteomraadet_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE detsaerskilteomraadet_land ADD PRIMARY KEY (detsaerskilteomraadet_fk, landkode_fk);
ALTER TABLE detsaerskilteomraadet_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE detsaerskilteomraadet_sonetype ADD PRIMARY KEY (detsaerskilteomraadet_fk, sonetype_fk);
ALTER TABLE detsaerskilteomraadet_miljoeomraade ADD PRIMARY KEY (detsaerskilteomraadet_fk, miljoeomraade_fk);
ALTER TABLE fiskerisone_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE fiskerisone_land ADD PRIMARY KEY (fiskerisone_fk, landkode_fk);
ALTER TABLE fiskerisone_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE fiskerisone_sonetype ADD PRIMARY KEY (fiskerisone_fk, sonetype_fk);
ALTER TABLE fiskerisone_miljoeomraade ADD PRIMARY KEY (fiskerisone_fk, miljoeomraade_fk);
ALTER TABLE fiskevernsone_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE fiskevernsone_land ADD PRIMARY KEY (fiskevernsone_fk, landkode_fk);
ALTER TABLE fiskevernsone_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE fiskevernsone_sonetype ADD PRIMARY KEY (fiskevernsone_fk, sonetype_fk);
ALTER TABLE fiskevernsone_miljoeomraade ADD PRIMARY KEY (fiskevernsone_fk, miljoeomraade_fk);
ALTER TABLE grense200nautiskemil_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grense200nautiskemil_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE grenseboeye_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grensepunktsjoe_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grunnlinje_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE grunnlinje_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE graasonegrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE graasonegrense_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE graasonegrense_land ADD PRIMARY KEY (graasonegrense_fk, landkode_fk);
ALTER TABLE graasonen_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE graasonen_land ADD PRIMARY KEY (graasonen_fk, landkode_fk);
ALTER TABLE graasonen_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE graasonen_sonetype ADD PRIMARY KEY (graasonen_fk, sonetype_fk);
ALTER TABLE graasonen_miljoeomraade ADD PRIMARY KEY (graasonen_fk, miljoeomraade_fk);
ALTER TABLE indrefarvann_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE indrefarvann_land ADD PRIMARY KEY (indrefarvann_fk, landkode_fk);
ALTER TABLE indrefarvann_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE indrefarvann_sonetype ADD PRIMARY KEY (indrefarvann_fk, sonetype_fk);
ALTER TABLE indrefarvann_miljoeomraade ADD PRIMARY KEY (indrefarvann_fk, miljoeomraade_fk);
ALTER TABLE internasjonaltfarvann_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE kontinentalsokkel_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE kontinentalsokkel_land ADD PRIMARY KEY (kontinentalsokkel_fk, landkode_fk);
ALTER TABLE kontinentalsokkel_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE kontinentalsokkel_sonetype ADD PRIMARY KEY (kontinentalsokkel_fk, sonetype_fk);
ALTER TABLE kontinentalsokkel_miljoeomraade ADD PRIMARY KEY (kontinentalsokkel_fk, miljoeomraade_fk);
ALTER TABLE kystkontur_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE landindrefarvann_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE norgesoekonomiskesone_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE norgesoekonomiskesone_land ADD PRIMARY KEY (norgesoekonomiskesone_fk, landkode_fk);
ALTER TABLE norgesoekonomiskesone_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE norgesoekonomiskesone_sonetype ADD PRIMARY KEY (norgesoekonomiskesone_fk, sonetype_fk);
ALTER TABLE norgesoekonomiskesone_miljoeomraade ADD PRIMARY KEY (norgesoekonomiskesone_fk, miljoeomraade_fk);
ALTER TABLE sektorlinje_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sektorlinje_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE sektorlinje_land ADD PRIMARY KEY (sektorlinje_fk, landkode_fk);
ALTER TABLE sjoeroeys_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sjoeterritorium_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE sjoeterritorium_land ADD PRIMARY KEY (sjoeterritorium_fk, landkode_fk);
ALTER TABLE sjoeterritorium_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE sjoeterritorium_sonetype ADD PRIMARY KEY (sjoeterritorium_fk, sonetype_fk);
ALTER TABLE sjoeterritorium_miljoeomraade ADD PRIMARY KEY (sjoeterritorium_fk, miljoeomraade_fk);
ALTER TABLE territorialfarvann_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE territorialfarvann_land ADD PRIMARY KEY (territorialfarvann_fk, landkode_fk);
ALTER TABLE territorialfarvann_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE territorialfarvann_sonetype ADD PRIMARY KEY (territorialfarvann_fk, sonetype_fk);
ALTER TABLE territorialfarvann_miljoeomraade ADD PRIMARY KEY (territorialfarvann_fk, miljoeomraade_fk);
ALTER TABLE territorialgrense_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE territorialgrense_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE territorialomraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE tidligereomstridtomraade_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE tilstoetendesone_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE tilstoetendesone_land ADD PRIMARY KEY (tilstoetendesone_fk, landkode_fk);
ALTER TABLE tilstoetendesone_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE tilstoetendesone_sonetype ADD PRIMARY KEY (tilstoetendesone_fk, sonetype_fk);
ALTER TABLE tilstoetendesone_miljoeomraade ADD PRIMARY KEY (tilstoetendesone_fk, miljoeomraade_fk);
ALTER TABLE yttergrensesokkel_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE yttergrensesokkel_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE yttergrensetilstoetendesone_informasjon ADD PRIMARY KEY (objid);
ALTER TABLE yttergrensetilstoetendesone_ansvarligmyndighet ADD PRIMARY KEY (objid);
ALTER TABLE identifikasjon ADD FOREIGN KEY (punktreferanse_fk) REFERENCES punktreferanse(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE fylke ADD FOREIGN KEY (nivaanummer) REFERENCES nivaanummer;
ALTER TABLE fylke ADD FOREIGN KEY (spraak) REFERENCES spraakkode;
ALTER TABLE fylke ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_fylkesgrense ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylke_fylkesgrense ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE fylkesgrense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE fylkesgrense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE fylkesgrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE fylkesgrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE avgrensning_bispedoemme ADD FOREIGN KEY (avgrensning_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_bispedoemme ADD FOREIGN KEY (bispedoemme_fk) REFERENCES bispedoemme(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_fylkesgrense ADD FOREIGN KEY (delomraade_fk) REFERENCES delomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_fylkesgrense ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense_grensepunktsjoe ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense_grensepunktsjoe ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_prosti ADD FOREIGN KEY (avgrensning_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_prosti ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_fylkesgrense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_fylkesgrense ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_fylkesgrense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_fylkesgrense ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_sokn ADD FOREIGN KEY (avgrensning_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_sokn ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekrets_fylkesgrense ADD FOREIGN KEY (skolekrets_fk) REFERENCES skolekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekrets_fylkesgrense ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense_grensemerkeland ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense_grensemerkeland ADD FOREIGN KEY (grensemerkeland_fk) REFERENCES grensemerkeland(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_fylkesgrense ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_fylkesgrense ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensemerkeland ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grensemerkeland ADD FOREIGN KEY (grensemerketype) REFERENCES grensepunkttypekode;
ALTER TABLE grensemerkeland ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE grensemerkeland_kommunegrense ADD FOREIGN KEY (grensemerkeland_fk) REFERENCES grensemerkeland(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensemerkeland_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE kommune ADD FOREIGN KEY (nivaanummer) REFERENCES nivaanummer;
ALTER TABLE kommune ADD FOREIGN KEY (spraak) REFERENCES spraakkode;
ALTER TABLE kommune ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_kommunegrense ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommunegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE kommunegrense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE kommunegrense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE kommunegrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE kommunegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE grensepunktsjoe_kommunegrense ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensepunktsjoe_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_kommunegrense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_kommunegrense ADD FOREIGN KEY (delomraade_fk) REFERENCES delomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_prosti ADD FOREIGN KEY (avgrensning_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
--ALTER TABLE avgrensning_prosti ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekrets_kommunegrense ADD FOREIGN KEY (skolekrets_fk) REFERENCES skolekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekrets_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_sokn ADD FOREIGN KEY (avgrensning_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
--ALTER TABLE avgrensning_sokn ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_kommunegrense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_kommunegrense ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avgrensning_bispedoemme ADD FOREIGN KEY (avgrensning_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
--ALTER TABLE avgrensning_bispedoemme ADD FOREIGN KEY (bispedoemme_fk) REFERENCES bispedoemme(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE nasjon ADD FOREIGN KEY (nivaanummer) REFERENCES nivaanummer;
ALTER TABLE nasjon ADD FOREIGN KEY (spraak) REFERENCES spraakkode;
ALTER TABLE administrativenhetnavn ADD FOREIGN KEY (spraak) REFERENCES spraakkode;
ALTER TABLE administrativenhetnavn ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhetnavn ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE administrativenhetnavn ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE brytningspunkt ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE brytningspunkt ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE riksgrense_grensemerke_vassdragspunkt ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_vassdragspunkt ADD FOREIGN KEY (grensemerke_vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_riksroeys ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_riksroeys ADD FOREIGN KEY (grensemerke_riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_profilpunkt ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_profilpunkt ADD FOREIGN KEY (grensemerke_profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_brytningspunkt ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_brytningspunkt ADD FOREIGN KEY (grensemerke_brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_viserstein ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_viserstein ADD FOREIGN KEY (grensemerke_viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_oevrighjelpemerke ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_oevrighjelpemerke ADD FOREIGN KEY (grensemerke_oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_storutligger ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_storutligger ADD FOREIGN KEY (grensemerke_storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_litenutligger ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_litenutligger ADD FOREIGN KEY (grensemerke_litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_grensestolpe ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_grensestolpe ADD FOREIGN KEY (grensemerke_grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_grenseskilt ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_grensemerke_grenseskilt ADD FOREIGN KEY (grensemerke_grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grenseskilt ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grenseskilt ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE grenseskilt ADD FOREIGN KEY (skilttype) REFERENCES skilttype;
ALTER TABLE grensestolpe ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grensestolpe ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE litenutligger ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE litenutligger ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE profilpunkt ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE profilpunkt ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE riksgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE riksgrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE riksgrense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE riksgrense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE avgrensning_prosti ADD FOREIGN KEY (avgrensning_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
--ALTER TABLE avgrensning_prosti ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksroeys ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE riksroeys ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE sikringspunkt ADD FOREIGN KEY (sikringspunkttype) REFERENCES sikringspunkttype;
ALTER TABLE sikringspunkt ADD FOREIGN KEY (materialebolt) REFERENCES materialebolt;
ALTER TABLE sikringspunkt ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE sikringspunkt ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sikringspunkt ADD FOREIGN KEY (utligger_storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sikringspunkt ADD FOREIGN KEY (utligger_litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE storutligger ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE storutligger ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE vassdragspunkt ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE vassdragspunkt ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE viserstein ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE viserstein ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE oevrighjelpemerke ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE oevrighjelpemerke ADD FOREIGN KEY (underlag) REFERENCES underlag;
ALTER TABLE hjertesteininformasjon ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE naermestegrensemerke ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE naermestegrensemerke ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (koordinatsystem) REFERENCES referansesystemkode;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrertekoordinater ADD FOREIGN KEY (oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (hoeydereferansesystem) REFERENCES hoeydereferansesystem;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (hoeydereferanse) REFERENCES hoeydereferanse;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE registrerthoeyde ADD FOREIGN KEY (oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vedlikehold ADD FOREIGN KEY (oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE delomraade_delomraadegrens ADD FOREIGN KEY (delomraade_fk) REFERENCES delomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_delomraadegrens ADD FOREIGN KEY (delomraadegrens_fk) REFERENCES delomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraadegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE delomraadegrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE delomraadegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE grunnkrets_delomraadegrense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_delomraadegrense ADD FOREIGN KEY (delomraadegrense_fk) REFERENCES delomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grunnkrets ADD FOREIGN KEY (delomraadeinndeling_fk) REFERENCES delomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_grunnkretsgrense ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_grunnkretsgrense ADD FOREIGN KEY (grunnkretsgrense_fk) REFERENCES grunnkretsgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkretsgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grunnkretsgrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE grunnkretsgrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE valgdistriktsgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE valgdistriktsgrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE valgdistriktsgrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE stemmekrets_valgdistriktsgrense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_valgdistriktsgrense ADD FOREIGN KEY (valgdistriktsgrense_fk) REFERENCES valgdistriktsgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekrets ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE skolekrets_skolekretsgrense ADD FOREIGN KEY (skolekrets_fk) REFERENCES skolekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekrets_skolekretsgrense ADD FOREIGN KEY (skolekretsgrense_fk) REFERENCES skolekretsgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE skolekretsgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE skolekretsgrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE skolekretsgrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE stemmekrets ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE stemmekrets_stemmekretsgrense ADD FOREIGN KEY (stemmekrets_fk) REFERENCES stemmekrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekrets_stemmekretsgrense ADD FOREIGN KEY (stemmekretsgrense_fk) REFERENCES stemmekretsgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE stemmekretsgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE stemmekretsgrense ADD FOREIGN KEY (foelgerterrengdetalj) REFERENCES terrengdetaljkode;
ALTER TABLE stemmekretsgrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE sektorgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sektorgrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE sektoromraade_sektorgrense ADD FOREIGN KEY (sektoromraade_fk) REFERENCES sektoromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektoromraade_sektorgrense ADD FOREIGN KEY (sektorgrense_fk) REFERENCES sektorgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlokalitet ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sektoromraade_sektorlokalitet ADD FOREIGN KEY (sektoromraade_fk) REFERENCES sektoromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektoromraade_sektorlokalitet ADD FOREIGN KEY (sektorlokalitet_fk) REFERENCES sektorlokalitet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektoromraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sektoromraade ADD FOREIGN KEY (nivaanummer) REFERENCES nivaanummer;
ALTER TABLE bispedoemme ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE bispedoemme ADD FOREIGN KEY (bispedoemmenummer) REFERENCES bispedoemmenummer;
ALTER TABLE bispedoemme_avgrensning ADD FOREIGN KEY (bispedoemme_fk) REFERENCES bispedoemme(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE bispedoemme_avgrensning ADD FOREIGN KEY (avgrensning_fk) REFERENCES bispedoemmegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE bispedoemmegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE bispedoemmegrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE bispedoemmegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE prosti_avgrensning ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE prosti_avgrensning ADD FOREIGN KEY (avgrensning_fk) REFERENCES bispedoemmegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn_avgrensning ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn_avgrensning ADD FOREIGN KEY (avgrensning_fk) REFERENCES bispedoemmegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE prosti ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE prosti ADD FOREIGN KEY (prostinummer) REFERENCES prostinummer;
ALTER TABLE prosti ADD FOREIGN KEY (bispedoemme_fk) REFERENCES bispedoemme(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
--ALTER TABLE prosti_avgrensning ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE prosti_avgrensning ADD FOREIGN KEY (avgrensning_fk) REFERENCES prostigrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE prostigrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE prostigrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE prostigrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
--ALTER TABLE sokn_avgrensning ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn_avgrensning ADD FOREIGN KEY (avgrensning_fk) REFERENCES prostigrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sokn ADD FOREIGN KEY (soknenummer) REFERENCES soknenummer;
ALTER TABLE sokn ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
--ALTER TABLE sokn_avgrensning ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn_avgrensning ADD FOREIGN KEY (avgrensning_fk) REFERENCES soknegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE soknegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE soknegrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE soknegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE postnummeromraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postnummeromraade ADD FOREIGN KEY (postomraade_fk) REFERENCES postomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postsonegrense ADD FOREIGN KEY (postnummeromraade_fk) REFERENCES postnummeromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postsonegrense ADD FOREIGN KEY (postsonegrense_fk) REFERENCES postsonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postnummeromraadegrense ADD FOREIGN KEY (postnummeromraade_fk) REFERENCES postnummeromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postnummeromraadegrense ADD FOREIGN KEY (postnummeromraadegrense_fk) REFERENCES postnummeromraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postomraadegrense ADD FOREIGN KEY (postnummeromraade_fk) REFERENCES postnummeromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postomraadegrense ADD FOREIGN KEY (postomraadegrense_fk) REFERENCES postomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postregiongrense ADD FOREIGN KEY (postnummeromraade_fk) REFERENCES postnummeromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_postregiongrense ADD FOREIGN KEY (postregiongrense_fk) REFERENCES postregiongrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraadegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postnummeromraadegrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE postnummeromraadegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE postomraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postomraade ADD FOREIGN KEY (postregion_fk) REFERENCES postregion(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_postsonegrense ADD FOREIGN KEY (postomraade_fk) REFERENCES postomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_postsonegrense ADD FOREIGN KEY (postsonegrense_fk) REFERENCES postsonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_postomraadegrense ADD FOREIGN KEY (postomraade_fk) REFERENCES postomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_postomraadegrense ADD FOREIGN KEY (postomraadegrense_fk) REFERENCES postomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_postregiongrense ADD FOREIGN KEY (postomraade_fk) REFERENCES postomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_postregiongrense ADD FOREIGN KEY (postregiongrense_fk) REFERENCES postregiongrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraadegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postomraadegrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE postomraadegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE postregion ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postregion ADD FOREIGN KEY (postsone_fk) REFERENCES postsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregion_postsonegrense ADD FOREIGN KEY (postregion_fk) REFERENCES postregion(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregion_postsonegrense ADD FOREIGN KEY (postsonegrense_fk) REFERENCES postsonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregion_postregiongrense ADD FOREIGN KEY (postregion_fk) REFERENCES postregion(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregion_postregiongrense ADD FOREIGN KEY (postregiongrense_fk) REFERENCES postregiongrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregiongrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postregiongrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE postregiongrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE postsone ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postsone_postsonegrense ADD FOREIGN KEY (postsone_fk) REFERENCES postsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postsone_postsonegrense ADD FOREIGN KEY (postsonegrense_fk) REFERENCES postsonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postsonegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE postsonegrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE postsonegrense ADD FOREIGN KEY (noeyaktighetsklasse) REFERENCES noeyaktighetsklassekode;
ALTER TABLE lovvirkeomraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE lovvirkeomraade ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE lovvirkeomraadegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE lovvirkeomraadegrense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE lovvirkeomraadegrense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE lovvirkeomraadegrense ADD FOREIGN KEY (foelgeradministrativgrense) REFERENCES administrativgrensekode;
ALTER TABLE lovvirkeomraadegrense_lovvirkeomraadegrensepunkt ADD FOREIGN KEY (lovvirkeomraadegrense_fk) REFERENCES lovvirkeomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraadegrense_lovvirkeomraadegrensepunkt ADD FOREIGN KEY (lovvirkeomraadegrensepunkt_fk) REFERENCES lovvirkeomraadegrensepunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraadegrensepunkt ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (lovvirkeomraade_fk) REFERENCES lovvirkeomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (lovvirkeomraadegrense_fk) REFERENCES lovvirkeomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (detsaerskilteomraadet_fk) REFERENCES detsaerskilteomraadet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (fiskerisone_fk) REFERENCES fiskerisone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (fiskevernsone_fk) REFERENCES fiskevernsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (grense200nautiskemil_fk) REFERENCES grense200nautiskemil(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (grunnlinje_fk) REFERENCES grunnlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (graasonegrense_fk) REFERENCES graasonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (graasonen_fk) REFERENCES graasonen(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (indrefarvann_fk) REFERENCES indrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (kontinentalsokkel_fk) REFERENCES kontinentalsokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (norgesoekonomiskesone_fk) REFERENCES norgesoekonomiskesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (sjoeterritorium_fk) REFERENCES sjoeterritorium(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (territorialfarvann_fk) REFERENCES territorialfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (tilstoetendesone_fk) REFERENCES tilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (yttergrensesokkel_fk) REFERENCES yttergrensesokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse ADD FOREIGN KEY (yttergrensetilstoetendesone_fk) REFERENCES yttergrensetilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE punktreferanse ADD FOREIGN KEY (grensemerkeland_fk) REFERENCES grensemerkeland(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE punktreferanse ADD FOREIGN KEY (lovvirkeomraadegrensepunkt_fk) REFERENCES lovvirkeomraadegrensepunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE punktreferanse ADD FOREIGN KEY (grenseboeye_fk) REFERENCES grenseboeye(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE punktreferanse ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE punktreferanse ADD FOREIGN KEY (sjoeroeys_fk) REFERENCES sjoeroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE avtaltavgrensningslinje ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE avtaltavgrensningslinje ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE avtaltavgrensningslinje_grenseboeye ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_grenseboeye ADD FOREIGN KEY (grenseboeye_fk) REFERENCES grenseboeye(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeroeys_avtaltavgrensningslinje ADD FOREIGN KEY (sjoeroeys_fk) REFERENCES sjoeroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeroeys_avtaltavgrensningslinje ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_grensepunktsjoe ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_grensepunktsjoe ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE beregnetmidtlinje ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE beregnetmidtlinje ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE grensepunktsjoe_beregnetmidtlinje ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensepunktsjoe_beregnetmidtlinje ADD FOREIGN KEY (beregnetmidtlinje_fk) REFERENCES beregnetmidtlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE detsaerskilteomraadet ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE fiskerisone ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE fiskerisone ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE fiskevernsone ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE fiskevernsone ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE grense200nautiskemil ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grense200nautiskemil ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE grense200nautiskemil ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE grense200nautiskemil ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE grenseboeye ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grenseboeye ADD FOREIGN KEY (grunnlinje_fk) REFERENCES grunnlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grenseboeye ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensepunktsjoe ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sektorlinje_grensepunktsjoe ADD FOREIGN KEY (sektorlinje_fk) REFERENCES sektorlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlinje_grensepunktsjoe ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnlinje_grensepunktsjoe ADD FOREIGN KEY (grunnlinje_fk) REFERENCES grunnlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnlinje_grensepunktsjoe ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonegrense_grensepunktsjoe ADD FOREIGN KEY (graasonegrense_fk) REFERENCES graasonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonegrense_grensepunktsjoe ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnlinje ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE grunnlinje ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE grunnlinje ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE grunnlinje ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE graasonegrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE graasonegrense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE graasonegrense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE graasonen ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE graasonen ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE indrefarvann ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE indrefarvann ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE internasjonaltfarvann ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE kontinentalsokkel ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE kontinentalsokkel ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE kystkontur ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE kystkontur ADD FOREIGN KEY (kystreferanse) REFERENCES kystreferanse;
ALTER TABLE kystkontur ADD FOREIGN KEY (kysttype) REFERENCES kysttype;
ALTER TABLE landindrefarvann ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE landindrefarvann ADD FOREIGN KEY (landkode) REFERENCES landkode;
ALTER TABLE norgesoekonomiskesone ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE norgesoekonomiskesone ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE sektorlinje ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sektorlinje ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE sjoeroeys ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE sjoeterritorium ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE sjoeterritorium ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE territorialfarvann ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE territorialfarvann ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE territorialgrense ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE territorialgrense ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE territorialgrense ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE territorialgrense ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE territorialomraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE territorialomraade ADD FOREIGN KEY (landkode) REFERENCES landkode;
ALTER TABLE tidligereomstridtomraade ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE tilstoetendesone ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
--ALTER TABLE tilstoetendesone ADD FOREIGN KEY (spesialisertsonetype) REFERENCES spesialisertsonetype;
ALTER TABLE yttergrensesokkel ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE yttergrensesokkel ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE yttergrensesokkel ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE yttergrensesokkel ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE yttergrensetilstoetendesone ADD FOREIGN KEY (typeendring) REFERENCES typeendring;
ALTER TABLE yttergrensetilstoetendesone ADD FOREIGN KEY (grensestatus) REFERENCES grensestatus;
ALTER TABLE yttergrensetilstoetendesone ADD FOREIGN KEY (land) REFERENCES landkode;
ALTER TABLE yttergrensetilstoetendesone ADD FOREIGN KEY (fastsettingstype) REFERENCES fastsettingstype;
ALTER TABLE fylke_informasjon ADD FOREIGN KEY (fylke_fk) REFERENCES fylke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense_informasjon ADD FOREIGN KEY (fylkesgrense_fk) REFERENCES fylkesgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensemerkeland_informasjon ADD FOREIGN KEY (grensemerkeland_fk) REFERENCES grensemerkeland(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommune_informasjon ADD FOREIGN KEY (kommune_fk) REFERENCES kommune(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kommunegrense_informasjon ADD FOREIGN KEY (kommunegrense_fk) REFERENCES kommunegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nasjon_informasjon ADD FOREIGN KEY (nasjon_fk) REFERENCES nasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE brytningspunkt_navn ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE brytningspunkt_informasjon ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE brytningspunkt_fotodokumentasjon ADD FOREIGN KEY (brytningspunkt_fk) REFERENCES brytningspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grenseskilt_informasjon ADD FOREIGN KEY (grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grenseskilt_fotodokumentasjon ADD FOREIGN KEY (grenseskilt_fk) REFERENCES grenseskilt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensestolpe_informasjon ADD FOREIGN KEY (grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensestolpe_fotodokumentasjon ADD FOREIGN KEY (grensestolpe_fk) REFERENCES grensestolpe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE litenutligger_informasjon ADD FOREIGN KEY (litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE litenutligger_fotodokumentasjon ADD FOREIGN KEY (litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE litenutligger_navn ADD FOREIGN KEY (litenutligger_fk) REFERENCES litenutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE profilpunkt_informasjon ADD FOREIGN KEY (profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE profilpunkt_fotodokumentasjon ADD FOREIGN KEY (profilpunkt_fk) REFERENCES profilpunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_ansvarligmyndighet ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksgrense_informasjon ADD FOREIGN KEY (riksgrense_fk) REFERENCES riksgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksroeys_riksroeysnavn ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksroeys_informasjon ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE riksroeys_fotodokumentasjon ADD FOREIGN KEY (riksroeys_fk) REFERENCES riksroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE storutligger_informasjon ADD FOREIGN KEY (storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE storutligger_fotodokumentasjon ADD FOREIGN KEY (storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE storutligger_navn ADD FOREIGN KEY (storutligger_fk) REFERENCES storutligger(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vassdragspunkt_informasjon ADD FOREIGN KEY (vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE vassdragspunkt_fotodokumentasjon ADD FOREIGN KEY (vassdragspunkt_fk) REFERENCES vassdragspunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE viserstein_informasjon ADD FOREIGN KEY (viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE viserstein_fotodokumentasjon ADD FOREIGN KEY (viserstein_fk) REFERENCES viserstein(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE oevrighjelpemerke_informasjon ADD FOREIGN KEY (oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE oevrighjelpemerke_fotodokumentasjon ADD FOREIGN KEY (oevrighjelpemerke_fk) REFERENCES oevrighjelpemerke(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE hjertesteininformasjon_bildehjertestein ADD FOREIGN KEY (hjertesteininformasjon_fk) REFERENCES hjertesteininformasjon(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraade_informasjon ADD FOREIGN KEY (delomraade_fk) REFERENCES delomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE delomraadegrense_informasjon ADD FOREIGN KEY (delomraadegrense_fk) REFERENCES delomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkrets_informasjon ADD FOREIGN KEY (grunnkrets_fk) REFERENCES grunnkrets(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnkretsgrense_informasjon ADD FOREIGN KEY (grunnkretsgrense_fk) REFERENCES grunnkretsgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorgrense_informasjon ADD FOREIGN KEY (sektorgrense_fk) REFERENCES sektorgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlokalitet_informasjon ADD FOREIGN KEY (sektorlokalitet_fk) REFERENCES sektorlokalitet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektoromraade_informasjon ADD FOREIGN KEY (sektoromraade_fk) REFERENCES sektoromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE bispedoemme_informasjon ADD FOREIGN KEY (bispedoemme_fk) REFERENCES bispedoemme(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE bispedoemmegrense_informasjon ADD FOREIGN KEY (bispedoemmegrense_fk) REFERENCES bispedoemmegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE prosti_informasjon ADD FOREIGN KEY (prosti_fk) REFERENCES prosti(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE prostigrense_informasjon ADD FOREIGN KEY (prostigrense_fk) REFERENCES prostigrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn_fellesraad ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sokn_informasjon ADD FOREIGN KEY (sokn_fk) REFERENCES sokn(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE soknegrense_informasjon ADD FOREIGN KEY (soknegrense_fk) REFERENCES soknegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraade_informasjon ADD FOREIGN KEY (postnummeromraade_fk) REFERENCES postnummeromraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postnummeromraadegrense_informasjon ADD FOREIGN KEY (postnummeromraadegrense_fk) REFERENCES postnummeromraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraade_informasjon ADD FOREIGN KEY (postomraade_fk) REFERENCES postomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postomraadegrense_informasjon ADD FOREIGN KEY (postomraadegrense_fk) REFERENCES postomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregion_informasjon ADD FOREIGN KEY (postregion_fk) REFERENCES postregion(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postregiongrense_informasjon ADD FOREIGN KEY (postregiongrense_fk) REFERENCES postregiongrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postsone_informasjon ADD FOREIGN KEY (postsone_fk) REFERENCES postsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE postsonegrense_informasjon ADD FOREIGN KEY (postsonegrense_fk) REFERENCES postsonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraade_sonetype ADD FOREIGN KEY (lovvirkeomraade_fk) REFERENCES lovvirkeomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraade_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraade_miljoeomraade ADD FOREIGN KEY (lovvirkeomraade_fk) REFERENCES lovvirkeomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraade_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraade_informasjon ADD FOREIGN KEY (lovvirkeomraade_fk) REFERENCES lovvirkeomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraade_ansvarligmyndighet ADD FOREIGN KEY (lovvirkeomraade_fk) REFERENCES lovvirkeomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraadegrense_informasjon ADD FOREIGN KEY (lovvirkeomraadegrense_fk) REFERENCES lovvirkeomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraadegrense_ansvarligmyndighet ADD FOREIGN KEY (lovvirkeomraadegrense_fk) REFERENCES lovvirkeomraadegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE lovvirkeomraadegrensepunkt_informasjon ADD FOREIGN KEY (lovvirkeomraadegrensepunkt_fk) REFERENCES lovvirkeomraadegrensepunkt(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse_dokumentlink ADD FOREIGN KEY (dokumentasjonsreferanse_fk) REFERENCES dokumentasjonsreferanse(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE dokumentasjonsreferanse_internreferansekartverket ADD FOREIGN KEY (dokumentasjonsreferanse_fk) REFERENCES dokumentasjonsreferanse(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_informasjon ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_ansvarligmyndighet ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_land ADD FOREIGN KEY (avtaltavgrensningslinje_fk) REFERENCES avtaltavgrensningslinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE avtaltavgrensningslinje_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE beregnetmidtlinje_informasjon ADD FOREIGN KEY (beregnetmidtlinje_fk) REFERENCES beregnetmidtlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE beregnetmidtlinje_ansvarligmyndighet ADD FOREIGN KEY (beregnetmidtlinje_fk) REFERENCES beregnetmidtlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE beregnetmidtlinje_land ADD FOREIGN KEY (beregnetmidtlinje_fk) REFERENCES beregnetmidtlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE beregnetmidtlinje_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_informasjon ADD FOREIGN KEY (detsaerskilteomraadet_fk) REFERENCES detsaerskilteomraadet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_land ADD FOREIGN KEY (detsaerskilteomraadet_fk) REFERENCES detsaerskilteomraadet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_ansvarligmyndighet ADD FOREIGN KEY (detsaerskilteomraadet_fk) REFERENCES detsaerskilteomraadet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_sonetype ADD FOREIGN KEY (detsaerskilteomraadet_fk) REFERENCES detsaerskilteomraadet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_miljoeomraade ADD FOREIGN KEY (detsaerskilteomraadet_fk) REFERENCES detsaerskilteomraadet(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE detsaerskilteomraadet_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_informasjon ADD FOREIGN KEY (fiskerisone_fk) REFERENCES fiskerisone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_land ADD FOREIGN KEY (fiskerisone_fk) REFERENCES fiskerisone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_ansvarligmyndighet ADD FOREIGN KEY (fiskerisone_fk) REFERENCES fiskerisone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_sonetype ADD FOREIGN KEY (fiskerisone_fk) REFERENCES fiskerisone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_miljoeomraade ADD FOREIGN KEY (fiskerisone_fk) REFERENCES fiskerisone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskerisone_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_informasjon ADD FOREIGN KEY (fiskevernsone_fk) REFERENCES fiskevernsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_land ADD FOREIGN KEY (fiskevernsone_fk) REFERENCES fiskevernsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_ansvarligmyndighet ADD FOREIGN KEY (fiskevernsone_fk) REFERENCES fiskevernsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_sonetype ADD FOREIGN KEY (fiskevernsone_fk) REFERENCES fiskevernsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_miljoeomraade ADD FOREIGN KEY (fiskevernsone_fk) REFERENCES fiskevernsone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fiskevernsone_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense200nautiskemil_informasjon ADD FOREIGN KEY (grense200nautiskemil_fk) REFERENCES grense200nautiskemil(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grense200nautiskemil_ansvarligmyndighet ADD FOREIGN KEY (grense200nautiskemil_fk) REFERENCES grense200nautiskemil(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grenseboeye_informasjon ADD FOREIGN KEY (grenseboeye_fk) REFERENCES grenseboeye(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grensepunktsjoe_informasjon ADD FOREIGN KEY (grensepunktsjoe_fk) REFERENCES grensepunktsjoe(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnlinje_informasjon ADD FOREIGN KEY (grunnlinje_fk) REFERENCES grunnlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE grunnlinje_ansvarligmyndighet ADD FOREIGN KEY (grunnlinje_fk) REFERENCES grunnlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonegrense_informasjon ADD FOREIGN KEY (graasonegrense_fk) REFERENCES graasonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonegrense_ansvarligmyndighet ADD FOREIGN KEY (graasonegrense_fk) REFERENCES graasonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonegrense_land ADD FOREIGN KEY (graasonegrense_fk) REFERENCES graasonegrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonegrense_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_informasjon ADD FOREIGN KEY (graasonen_fk) REFERENCES graasonen(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_land ADD FOREIGN KEY (graasonen_fk) REFERENCES graasonen(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_ansvarligmyndighet ADD FOREIGN KEY (graasonen_fk) REFERENCES graasonen(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_sonetype ADD FOREIGN KEY (graasonen_fk) REFERENCES graasonen(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_miljoeomraade ADD FOREIGN KEY (graasonen_fk) REFERENCES graasonen(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE graasonen_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_informasjon ADD FOREIGN KEY (indrefarvann_fk) REFERENCES indrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_land ADD FOREIGN KEY (indrefarvann_fk) REFERENCES indrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_ansvarligmyndighet ADD FOREIGN KEY (indrefarvann_fk) REFERENCES indrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_sonetype ADD FOREIGN KEY (indrefarvann_fk) REFERENCES indrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_miljoeomraade ADD FOREIGN KEY (indrefarvann_fk) REFERENCES indrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE indrefarvann_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE internasjonaltfarvann_informasjon ADD FOREIGN KEY (internasjonaltfarvann_fk) REFERENCES internasjonaltfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_informasjon ADD FOREIGN KEY (kontinentalsokkel_fk) REFERENCES kontinentalsokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_land ADD FOREIGN KEY (kontinentalsokkel_fk) REFERENCES kontinentalsokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_ansvarligmyndighet ADD FOREIGN KEY (kontinentalsokkel_fk) REFERENCES kontinentalsokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_sonetype ADD FOREIGN KEY (kontinentalsokkel_fk) REFERENCES kontinentalsokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_miljoeomraade ADD FOREIGN KEY (kontinentalsokkel_fk) REFERENCES kontinentalsokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kontinentalsokkel_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE kystkontur_informasjon ADD FOREIGN KEY (kystkontur_fk) REFERENCES kystkontur(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE landindrefarvann_informasjon ADD FOREIGN KEY (landindrefarvann_fk) REFERENCES landindrefarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_informasjon ADD FOREIGN KEY (norgesoekonomiskesone_fk) REFERENCES norgesoekonomiskesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_land ADD FOREIGN KEY (norgesoekonomiskesone_fk) REFERENCES norgesoekonomiskesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_ansvarligmyndighet ADD FOREIGN KEY (norgesoekonomiskesone_fk) REFERENCES norgesoekonomiskesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_sonetype ADD FOREIGN KEY (norgesoekonomiskesone_fk) REFERENCES norgesoekonomiskesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_miljoeomraade ADD FOREIGN KEY (norgesoekonomiskesone_fk) REFERENCES norgesoekonomiskesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE norgesoekonomiskesone_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlinje_informasjon ADD FOREIGN KEY (sektorlinje_fk) REFERENCES sektorlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlinje_ansvarligmyndighet ADD FOREIGN KEY (sektorlinje_fk) REFERENCES sektorlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlinje_land ADD FOREIGN KEY (sektorlinje_fk) REFERENCES sektorlinje(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sektorlinje_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeroeys_informasjon ADD FOREIGN KEY (sjoeroeys_fk) REFERENCES sjoeroeys(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_informasjon ADD FOREIGN KEY (sjoeterritorium_fk) REFERENCES sjoeterritorium(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_land ADD FOREIGN KEY (sjoeterritorium_fk) REFERENCES sjoeterritorium(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_ansvarligmyndighet ADD FOREIGN KEY (sjoeterritorium_fk) REFERENCES sjoeterritorium(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_sonetype ADD FOREIGN KEY (sjoeterritorium_fk) REFERENCES sjoeterritorium(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_miljoeomraade ADD FOREIGN KEY (sjoeterritorium_fk) REFERENCES sjoeterritorium(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE sjoeterritorium_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_informasjon ADD FOREIGN KEY (territorialfarvann_fk) REFERENCES territorialfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_land ADD FOREIGN KEY (territorialfarvann_fk) REFERENCES territorialfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_ansvarligmyndighet ADD FOREIGN KEY (territorialfarvann_fk) REFERENCES territorialfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_sonetype ADD FOREIGN KEY (territorialfarvann_fk) REFERENCES territorialfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_miljoeomraade ADD FOREIGN KEY (territorialfarvann_fk) REFERENCES territorialfarvann(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialfarvann_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialgrense_informasjon ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialgrense_ansvarligmyndighet ADD FOREIGN KEY (territorialgrense_fk) REFERENCES territorialgrense(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE territorialomraade_informasjon ADD FOREIGN KEY (territorialomraade_fk) REFERENCES territorialomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tidligereomstridtomraade_informasjon ADD FOREIGN KEY (tidligereomstridtomraade_fk) REFERENCES tidligereomstridtomraade(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_informasjon ADD FOREIGN KEY (tilstoetendesone_fk) REFERENCES tilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_land ADD FOREIGN KEY (tilstoetendesone_fk) REFERENCES tilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_land ADD FOREIGN KEY (landkode_fk) REFERENCES landkode(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_ansvarligmyndighet ADD FOREIGN KEY (tilstoetendesone_fk) REFERENCES tilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_sonetype ADD FOREIGN KEY (tilstoetendesone_fk) REFERENCES tilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_sonetype ADD FOREIGN KEY (sonetype_fk) REFERENCES sonetype(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_miljoeomraade ADD FOREIGN KEY (tilstoetendesone_fk) REFERENCES tilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE tilstoetendesone_miljoeomraade ADD FOREIGN KEY (miljoeomraade_fk) REFERENCES miljoeomraade(identifier) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE yttergrensesokkel_informasjon ADD FOREIGN KEY (yttergrensesokkel_fk) REFERENCES yttergrensesokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE yttergrensesokkel_ansvarligmyndighet ADD FOREIGN KEY (yttergrensesokkel_fk) REFERENCES yttergrensesokkel(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE yttergrensetilstoetendesone_informasjon ADD FOREIGN KEY (yttergrensetilstoetendesone_fk) REFERENCES yttergrensetilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE yttergrensetilstoetendesone_ansvarligmyndighet ADD FOREIGN KEY (yttergrensetilstoetendesone_fk) REFERENCES yttergrensetilstoetendesone(objid) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE fylkesgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE fylkesgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE grensemerkeland ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grensemerkeland ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE kommunegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE kommunegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE brytningspunkt ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE brytningspunkt ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grenseskilt ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grenseskilt ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grensestolpe ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grensestolpe ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE litenutligger ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE litenutligger ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE profilpunkt ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE profilpunkt ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE riksgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE riksgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE riksroeys ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE riksroeys ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE sikringspunkt ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE sikringspunkt ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE storutligger ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE storutligger ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE vassdragspunkt ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE vassdragspunkt ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE viserstein ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE viserstein ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE oevrighjelpemerke ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE oevrighjelpemerke ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE delomraade ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE delomraade ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE delomraadegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE delomraadegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE grunnkrets ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grunnkrets ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grunnkretsgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE grunnkretsgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE valgdistriktsgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE valgdistriktsgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE skolekrets ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE skolekrets ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE skolekretsgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE skolekretsgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE stemmekrets ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE stemmekrets ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE stemmekretsgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE stemmekretsgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE sektorgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE sektorgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE sektorlokalitet ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE sektorlokalitet ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE bispedoemme ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE bispedoemme ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE bispedoemmegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE bispedoemmegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE prosti ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE prosti ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE prostigrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE prostigrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE sokn ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE sokn ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE soknegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE soknegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE postnummeromraadegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE postnummeromraadegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE postomraadegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE postomraadegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE postregiongrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE postregiongrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE postsonegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE postsonegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE lovvirkeomraadegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE lovvirkeomraadegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE lovvirkeomraadegrensepunkt ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE lovvirkeomraadegrensepunkt ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE avtaltavgrensningslinje ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE avtaltavgrensningslinje ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE beregnetmidtlinje ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE beregnetmidtlinje ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE grense200nautiskemil ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE grense200nautiskemil ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE grenseboeye ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grenseboeye ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grensepunktsjoe ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE grensepunktsjoe ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE grunnlinje ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE grunnlinje ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE graasonegrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE graasonegrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE internasjonaltfarvann ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE internasjonaltfarvann ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE kystkontur ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE kystkontur ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE sektorlinje ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE sektorlinje ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE sjoeroeys ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE sjoeroeys ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE territorialgrense ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE territorialgrense ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE tidligereomstridtomraade ADD CONSTRAINT enforce_dims_posisjon CHECK (st_ndims(posisjon) = 2);
ALTER TABLE tidligereomstridtomraade ADD CONSTRAINT enforce_srid_posisjon CHECK (st_srid(posisjon) = 25833);
ALTER TABLE tidligereomstridtomraade ADD CONSTRAINT enforce_dims_omraade CHECK (st_ndims(omraade) = 2);
ALTER TABLE tidligereomstridtomraade ADD CONSTRAINT enforce_srid_omraade CHECK (st_srid(omraade) = 25833);
ALTER TABLE yttergrensesokkel ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE yttergrensesokkel ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
ALTER TABLE yttergrensetilstoetendesone ADD CONSTRAINT enforce_dims_grense CHECK (st_ndims(grense) = 2);
ALTER TABLE yttergrensetilstoetendesone ADD CONSTRAINT enforce_srid_grense CHECK (st_srid(grense) = 25833);
