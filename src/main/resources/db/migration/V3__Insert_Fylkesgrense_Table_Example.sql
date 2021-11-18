insert into fylke (lokalid, navnerom, produkt, versjon, omraadeid, originaldatavert, kopidato,
                        nivaanummer, navn, spraak, fylkesnummer) values ('Viken', 'NO.KARTVERKET.MATRIKKEL', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                                                                     'nivaa1', 'Viken', 'nor', 30);

insert into fylke (lokalid, navnerom, produkt, versjon, omraadeid, originaldatavert, kopidato,
                        nivaanummer, navn, spraak, fylkesnummer) values ('Innlandet', 'NO.KARTVERKET.MATRIKKEL', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                        'nivaa1', 'Innlandet', 'nor', 34);

insert into fylkesgrense (lokalid, navnerom, oppdateringsdato, typeendring, tidspunktendring, maalemetode, produkt, versjon, omraadeid, originaldatavert, kopidato, grense) values (
                                                         'viken_grense', 'NO.KARTVERKET.MATRIKKEL', current_timestamp,
                                                         'type1', current_timestamp, 'maale1', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                         ST_Transform(ST_GeomFromGeoJSON(
                                                                 '{
                                                                    "type": "LineString",
                                                                    "coordinates": [
                                                                      [
                                                                        8.40728759765625,
                                                                        61.68466156225303
                                                                      ],
                                                                      [
                                                                        9.041748046875,
                                                                        61.680753006329006
                                                                      ],
                                                                      [
                                                                        9.23675537109375,
                                                                        61.822446587451196
                                                                      ],
                                                                      [
                                                                        9.42352294921875,
                                                                        61.81596114716962
                                                                      ],
                                                                      [
                                                                        9.4757080078125,
                                                                        61.89369604980593
                                                                      ]
                                                                    ]
                                                                 }'
                                                             ), 25833));
insert into fylkesgrense (lokalid, navnerom, oppdateringsdato, typeendring, tidspunktendring, maalemetode, produkt, versjon, omraadeid, originaldatavert, kopidato, grense) values (
                                                         'innlandet_grense', 'NO.KARTVERKET.MATRIKKEL', current_timestamp,
                                                         'type2', current_timestamp, 'maale2', 'produkt2', 'v1', 1, 'vert', current_timestamp,
                                                         ST_Transform(ST_GeomFromGeoJSON(
                                                                '{
                                                                    "type": "LineString",
                                                                    "coordinates": [
                                                                      [
                                                                        9.435882568359375,
                                                                        62.00541729295278
                                                                      ],
                                                                      [
                                                                        9.445109367370605,
                                                                        62.01458141167724
                                                                      ],
                                                                      [
                                                                        9.473648071289062,
                                                                        62.03521575280291
                                                                      ],
                                                                      [
                                                                        9.456825256347656,
                                                                        62.04551761899123
                                                                      ],
                                                                      [
                                                                        9.421119689941406,
                                                                        62.06176815390493
                                                                      ],
                                                                      [
                                                                        9.393310546875,
                                                                        62.07415133994334
                                                                      ],
                                                                      [
                                                                        9.414939880371094,
                                                                        62.08813665909046
                                                                      ],
                                                                      [
                                                                        9.454765319824219,
                                                                        62.1101464612736
                                                                      ],
                                                                      [
                                                                        9.420433044433592,
                                                                        62.114803422959156
                                                                      ]
                                                                    ]
                                                                 }'
                                                             ), 25833));

insert into fylkesgrense (lokalid, navnerom, oppdateringsdato, typeendring, tidspunktendring, maalemetode, produkt, versjon, omraadeid, originaldatavert, kopidato, grense) values (
                                                           'viken_og_innlandet_grense', 'NO.KARTVERKET.MATRIKKEL', current_timestamp,
                                                           'type1', current_timestamp, 'maale1', 'produkt1', 'v1', 1, 'vert', current_timestamp,
                                                           ST_Transform(ST_GeomFromGeoJSON(
                                                                   '{
                                                                        "type": "LineString",
                                                                        "coordinates": [
                                                                          [
                                                                            9.4757080078125,
                                                                            61.89369604980593
                                                                          ],
                                                                          [
                                                                            9.498796463012695,
                                                                            61.90073103614304
                                                                          ],
                                                                          [
                                                                            9.497251510620117,
                                                                            61.907966489482774
                                                                          ],
                                                                          [
                                                                            9.464035034179688,
                                                                            61.92376535490689
                                                                          ],
                                                                          [
                                                                            9.514846801757812,
                                                                            61.947670353062115
                                                                          ],
                                                                          [
                                                                            9.501800537109375,
                                                                            61.97316991407437
                                                                          ],
                                                                          [
                                                                            9.435882568359375,
                                                                            62.00541729295278
                                                                          ]
                                                                        ]
                                                                      }'
                                                               ), 25833));