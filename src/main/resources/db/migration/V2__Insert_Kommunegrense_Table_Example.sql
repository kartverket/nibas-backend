-- Sett inn et par rader med eksempeldata
insert into typeendring values ('type1', 'Endring av type 1');
insert into typeendring values ('type2', 'Endring av type 2');
insert into typeendring values ('type3', 'Endring av type 3');

insert into maalemetode values ('maale1', 'Målemetode 1');
insert into maalemetode values ('maale2', 'Målemetode 2');
insert into maalemetode values ('maale3', 'Målemetode 3');

insert into nivaanummer values ('nivaa1', 'Nivånummer 1');

insert into kommune (lokalid, navnerom, produkt, versjon, omraadeid, originaldatavert, kopidato,
                        nivaanummer, navn, spraak, kommunenummer, samiskforvaltningsomraade) values ('kommune_id1', 'NO.KARTVERKET.MATRIKKEL', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                                                                     'nivaa1', 'Ringerike', 'nor', 3007, false);

insert into kommune (lokalid, navnerom, produkt, versjon, omraadeid, originaldatavert, kopidato,
                        nivaanummer, navn, spraak, kommunenummer, samiskforvaltningsomraade) values ('kommune_id2', 'NO.KARTVERKET.MATRIKKEL', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                        'nivaa1', 'Hole', 'nor', 3038, false);

insert into kommunegrense (lokalid, navnerom, oppdateringsdato, typeendring, tidspunktendring, maalemetode, produkt, versjon, omraadeid, originaldatavert, kopidato, grense) values (
                                                         'ringerike', 'NO.KARTVERKET.MATRIKKEL', current_timestamp,
                                                         'type1', current_timestamp, 'maale1', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                         ST_Transform(ST_GeomFromGeoJSON(
                                                                 '{
                                                                    "type": "LineString",
                                                                    "coordinates": [
                                                                      [
                                                                        8.344802856445312,
                                                                        60.33442521247195
                                                                      ],
                                                                      [
                                                                        8.39630126953125,
                                                                        60.281706548146204
                                                                      ],
                                                                      [
                                                                        8.54461669921875,
                                                                        60.26332005831154
                                                                      ],
                                                                      [
                                                                        8.637313842773438,
                                                                        60.28272771685622
                                                                      ],
                                                                      [
                                                                        8.673019409179688,
                                                                        60.32320836585633
                                                                      ]
                                                                    ]
                                                                 }'
                                                             ), 25833));
insert into kommunegrense (lokalid, navnerom, oppdateringsdato, typeendring, tidspunktendring, maalemetode, produkt, versjon, omraadeid, originaldatavert, kopidato, grense) values (
                                                         'ringerike_og_hole', 'NO.KARTVERKET.MATRIKKEL', current_timestamp,
                                                         'type2', current_timestamp, 'maale2', 'produkt2', 'v1', 1, 'vert', current_timestamp,
                                                         ST_Transform(ST_GeomFromGeoJSON(
                                                                 '{
                                                                    "type": "LineString",
                                                                    "coordinates": [
                                                                      [
                                                                        8.673019409179688,
                                                                        60.32320836585633
                                                                      ],
                                                                      [
                                                                        8.676152229309082,
                                                                        60.323303980266466
                                                                      ],
                                                                      [
                                                                        8.679392337799072,
                                                                        60.32580047964767
                                                                      ],
                                                                      [
                                                                        8.68718147277832,
                                                                        60.330984089857985
                                                                      ],
                                                                      [
                                                                        8.684778213500977,
                                                                        60.336506703117855
                                                                      ],
                                                                      [
                                                                        8.672676086425781,
                                                                        60.34245308772931
                                                                      ]
                                                                    ]
                                                                 }'
                                                              ), 25833));
insert into kommunegrense (lokalid, navnerom, oppdateringsdato, typeendring, tidspunktendring, maalemetode, produkt, versjon, omraadeid, originaldatavert, kopidato, grense) values (
                                                         'hole', 'NO.KARTVERKET.MATRIKKEL', current_timestamp,
                                                         'type3', current_timestamp, 'maale3', 'produkt3', 'v1', 1, 'vert', current_timestamp,
                                                         ST_Transform(ST_GeomFromGeoJSON(
                                                                 '{
                                                                    "type": "LineString",
                                                                    "coordinates": [
                                                                              [
                                                                                8.672676086425781,
                                                                                60.34245308772931
                                                                              ],
                                                                              [
                                                                                8.690872192382812,
                                                                                60.35871516019547
                                                                              ],
                                                                              [
                                                                                8.675251007080078,
                                                                                60.36338423058478
                                                                              ],
                                                                              [
                                                                                8.687782287597656,
                                                                                60.37017441202934
                                                                              ],
                                                                              [
                                                                                8.67147445678711,
                                                                                60.374078125826664
                                                                              ],
                                                                              [
                                                                                8.693103790283203,
                                                                                60.37815106772717
                                                                              ]
                                                                            ]
                                                                 }'
                                                                  ), 25833));