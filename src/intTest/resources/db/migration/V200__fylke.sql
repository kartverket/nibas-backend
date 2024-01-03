-- Sett et breakpoint i en test og ta opp testcontaineren i dbeaver. Kjør denne selecten og kopier ut kolonnen med insert-setninger.
-- Husk å bevare setningen med uniklokalid
-- select fylkesnummer, "version", 'INSERT INTO nibas.fylke (objid, lokalid, gyldigfra, gyldigtil, fylkesnummeruuid, fylkesnummer, datafangstdato, foerstedigitaliseringsdato,
--                          opphav, endretdato, endretav, typeendring, samiskforvaltningsomraade, nasjonlokalid, version, utledetfra, slettet, posisjon)
-- VALUES (''' ||objid|| ''', ''' ||lokalid|| ''', ''' ||gyldigfra||''', null,
--         '''||fylkesnummer || ''', ''' ||fylkesnummer ||''', null, '''||foerstedigitaliseringsdato||''', null, '''||endretdato||''', '''||endretav||''', '''||typeendring||''',
--         '||samiskforvaltningsomraade ||', null, '''||"version"||''', null, '||slettet||', '''|| st_ashexewkb(posisjon)||''');'  from fylke f
-- where gyldigtil  is NULL
-- UNION
-- select fylkesnummer, "version", 'INSERT INTO nibas.fylke (objid, lokalid, gyldigfra, gyldigtil, fylkesnummeruuid, fylkesnummer, datafangstdato, foerstedigitaliseringsdato,
--                          opphav, endretdato, endretav, typeendring, samiskforvaltningsomraade, nasjonlokalid, version, utledetfra, slettet, posisjon)
-- VALUES (''' ||objid|| ''', ''' ||lokalid|| ''', ''' ||gyldigfra||''', '''||gyldigtil||''',
--         '''||fylkesnummer || ''', ''' ||fylkesnummer ||''', null, '''||foerstedigitaliseringsdato||''', null, '''||endretdato||''', '''||endretav||''', '''||typeendring||''',
--         '||samiskforvaltningsomraade ||', null, '''||"version"||''', null, '||slettet||', '''|| st_ashexewkb(posisjon)||''');'  from fylke f
-- where gyldigtil  is NOT NULL order by 1, 2;

INSERT INTO nibas.fylke (objid, lokalid, gyldigfra, gyldigtil, fylkesnummeruuid, fylkesnummer, datafangstdato, foerstedigitaliseringsdato,
                         opphav, endretdato, endretav, typeendring, samiskforvaltningsomraade, nasjonlokalid, version, utledetfra, slettet, posisjon)
VALUES ('ny-47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1', '47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1', '2020-12-21 00:00:00+01', '2022-11-25 00:00:00+01',  
        '0e404bae-6907-411e-a16a-f019ee82e67b', '30', null, '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import',
        true, null, '1', null, false, '0101000020E964000052B81E8511730941CDCCCC7C077A5941');
INSERT INTO nibas.fylke (objid, lokalid, gyldigfra, gyldigtil, fylkesnummeruuid, fylkesnummer, datafangstdato, foerstedigitaliseringsdato,
                         opphav, endretdato, endretav, typeendring, samiskforvaltningsomraade, nasjonlokalid, version, utledetfra, slettet, posisjon)
VALUES ('f2e85556-471b-4807-a1bf-6da7478e7cda', '47f4ef26-f1c1-4bbf-9f1b-8e37e70cc6a1', '2022-11-25 00:00:00+01', null, 
        '0e404bae-6907-411e-a16a-f019ee82e67b', '30', null, '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import',
        false, null, '2', null, false, '0101000020E964000052B81E8511730941CDCCCC7C077A5941');

INSERT INTO nibas.fylke (objid, lokalid, gyldigfra, gyldigtil, fylkesnummeruuid, fylkesnummer, datafangstdato, foerstedigitaliseringsdato,
                         opphav, endretdato, endretav, typeendring, samiskforvaltningsomraade, nasjonlokalid, version, utledetfra, slettet, posisjon)
VALUES ('22642144-2de8-4e9d-bea0-0520fb44e85a', '068f6d7f-2f05-47b2-a6aa-0d9611b89ebf', '2022-11-25 00:00:00+01', null,
        'e79b7a63-13ff-4ba5-b4ef-0dfa4e6af1df', '38', null, '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import',
        false, null, '1', null, false, '0101000020E9640000B81E85EB29EA0241A4703D4ACF335941');
INSERT INTO nibas.fylke (objid, lokalid, gyldigfra, gyldigtil, fylkesnummeruuid, fylkesnummer, datafangstdato, foerstedigitaliseringsdato,
                         opphav, endretdato, endretav, typeendring, samiskforvaltningsomraade, nasjonlokalid, version, utledetfra, slettet, posisjon)
VALUES ('3a65f9e4-23d5-4c05-b188-8f4ea8463f8b', '49b80966-598f-4a93-9960-723b5d34c03d', '2022-11-25 00:00:00+01', null,
        '770b61ec-0f3a-4836-80e6-675d5d7384a1', '42', null, '2022-11-25 00:00:00+01', null, '2022-11-25 00:00:00+01', 'import', 'import',
        false, null, '1', null, false, '0101000020E9640000A4703D0A2F3BF140CDCCCC2C01E35841');

insert into nibas.uniklokalid (lokalid, flatetype)  select distinct lokalid, '0' from nibas.fylke;


